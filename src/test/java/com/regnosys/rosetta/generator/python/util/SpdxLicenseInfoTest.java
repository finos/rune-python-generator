/*
 * Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
 * SPDX-License-Identifier: Apache-2.0
 */
package com.regnosys.rosetta.generator.python.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

@SuppressWarnings("checkstyle:LineLength")
class SpdxLicenseInfoTest {

    /** A complete SPDX 2.3 document, as proposed for CDM. */
    private static final String FULL = """
            SPDXVersion: SPDX-2.3
            DataLicense: CC0-1.0
            SPDXID: SPDXRef-DOCUMENT
            DocumentName: common-domain-model
            DocumentNamespace: https://github.com/finos/common-domain-model/spdx
            Creator: Organization: FINOS
            Created: 2026-11-24T00:00:00Z

            PackageName: common-domain-model
            SPDXID: SPDXRef-Package
            PackageDownloadLocation: https://github.com/finos/common-domain-model
            FilesAnalyzed: false
            PackageOriginator: Organization: FINOS
            PackageHomePage: https://github.com/finos/common-domain-model
            PackageLicenseDeclared: Community-Spec-1.0
            PackageCopyrightText: Copyright 2021 FINOS and CDM Participants
            """;

    @Test
    void readsTheFieldsOfACompleteDocument() {
        SpdxLicenseInfo info = parse(FULL, "LICENSE.spdx");
        assertEquals("Community-Spec-1.0", info.licenseId());
        assertEquals("Copyright 2021 FINOS and CDM Participants", info.copyrightText());
        assertEquals("common-domain-model", info.packageName());
        assertEquals("https://github.com/finos/common-domain-model", info.homePage());
        assertEquals("FINOS", info.originatorName());
        assertNull(info.originatorEmail());
    }

    @Test
    void originatorWithTypeAndEmailIsSplit() {
        SpdxLicenseInfo info = parse("""
                PackageOriginator: Person: Jane Doe (jane@example.org)
                PackageLicenseDeclared: MIT
                PackageCopyrightText: Copyright 2026 Jane Doe
                """, "person.spdx");
        assertEquals("Jane Doe", info.originatorName());
        assertEquals("jane@example.org", info.originatorEmail());
    }

    @Test
    void originatorWithEmptyEmailOrNoTypeIsAccepted() {
        SpdxLicenseInfo emptyEmail = parse("""
                PackageOriginator: Organization: CLOUDRISK Limited and FT Advisory LLC ()
                PackageLicenseDeclared: Apache-2.0
                PackageCopyrightText: Copyright 2023-2026 CLOUDRISK Limited and FT Advisory LLC
                """, "empty-email.spdx");
        assertEquals("CLOUDRISK Limited and FT Advisory LLC", emptyEmail.originatorName());
        assertNull(emptyEmail.originatorEmail());

        SpdxLicenseInfo noType = parse("""
                PackageOriginator: Example Ltd
                PackageLicenseDeclared: MIT
                PackageCopyrightText: Copyright 2026 Example Ltd
                """, "no-type.spdx");
        assertEquals("Example Ltd", noType.originatorName());
    }

    @Test
    void noAssertionOriginatorGivesNoAuthor() {
        SpdxLicenseInfo info = parse("""
                PackageOriginator: NOASSERTION
                PackageLicenseDeclared: MIT
                PackageCopyrightText: Copyright 2026 X
                """, "noassertion-originator.spdx");
        assertNull(info.originatorName());
    }

    @Test
    void headerCarriesCopyrightLicenceAndAttribution() {
        SpdxLicenseInfo info = parse(FULL, "LICENSE.spdx");
        assertEquals("""
                #
                # Copyright 2021 FINOS and CDM Participants
                # SPDX-License-Identifier: Community-Spec-1.0
                # Generated from common-domain-model 7.11.0 (https://github.com/finos/common-domain-model)
                #
                """, info.header("7.11.0"));
    }

    @Test
    void shortFileWithoutNameOrHomePageHasNoAttributionLine() {
        SpdxLicenseInfo info = parse("""
                PackageLicenseDeclared: Apache-2.0
                PackageCopyrightText: Copyright 2026 Example Ltd
                """, "short.spdx");
        assertNull(info.packageName());
        assertNull(info.homePage());
        assertEquals("""
                #
                # Copyright 2026 Example Ltd
                # SPDX-License-Identifier: Apache-2.0
                #
                """, info.header("1.0.0"));
    }

    @Test
    void multiLineCopyrightTextIsWrittenLineByLine() {
        SpdxLicenseInfo info = parse("""
                PackageLicenseDeclared: MIT
                PackageCopyrightText: <text>Copyright 2024 A Ltd
                Copyright 2025 B Ltd</text>
                """, "multi.spdx");
        assertEquals("Copyright 2024 A Ltd\nCopyright 2025 B Ltd", info.copyrightText());
        assertTrue(info.header("1.0.0").startsWith("#\n# Copyright 2024 A Ltd\n# Copyright 2025 B Ltd\n# SPDX-License-Identifier: MIT\n"),
                info.header("1.0.0"));
    }

    @Test
    void singleLineTextValueIsUnwrapped() {
        SpdxLicenseInfo info = parse("""
                PackageLicenseDeclared: MIT
                PackageCopyrightText: <text>Copyright 2024 A Ltd</text>
                """, "single.spdx");
        assertEquals("Copyright 2024 A Ltd", info.copyrightText());
    }

    @Test
    void firstPackageWinsWhenTheFileDescribesSeveral() {
        SpdxLicenseInfo info = parse("""
                PackageName: first
                PackageLicenseDeclared: MIT
                PackageCopyrightText: Copyright first

                PackageName: second
                PackageLicenseDeclared: Apache-2.0
                PackageCopyrightText: Copyright second
                """, "two.spdx");
        assertEquals("first", info.packageName());
        assertEquals("MIT", info.licenseId());
        assertEquals("Copyright first", info.copyrightText());
    }

    /** Warnings reported by the last call to {@link #parse}. */
    private final List<String> warnings = new ArrayList<>();

    private SpdxLicenseInfo parse(String content, String source) {
        warnings.clear();
        return SpdxLicenseInfo.parse(content, source, warnings::add);
    }

    private boolean warned(String... parts) {
        return warnings.stream().anyMatch(w -> java.util.Arrays.stream(parts).allMatch(w::contains));
    }

    @Test
    void completeFileGivesNoWarnings() {
        parse(FULL, "LICENSE.spdx");
        assertTrue(warnings.isEmpty(), warnings.toString());
    }

    @Test
    void missingLicenceWarnsAndGivesNoLicenceTerms() {
        assertNull(parse("PackageCopyrightText: Copyright 2026 X\n", "no-licence.spdx"));
        assertTrue(warned("no-licence.spdx", "PackageLicenseDeclared"), warnings.toString());
        assertTrue(warned("no licence terms will be written"), warnings.toString());
    }

    @Test
    void missingCopyrightWarnsAndGivesNoLicenceTerms() {
        assertNull(parse("PackageLicenseDeclared: MIT\n", "no-copyright.spdx"));
        assertTrue(warned("PackageCopyrightText"), warnings.toString());
        assertTrue(warned("no licence terms will be written"), warnings.toString());
    }

    @Test
    void bothRequiredFieldsMissingAreBothReported() {
        assertNull(parse("PackageName: x\n", "empty.spdx"));
        assertTrue(warned("PackageLicenseDeclared"), warnings.toString());
        assertTrue(warned("PackageCopyrightText"), warnings.toString());
    }

    @Test
    void noAssertionAndNoneAreNotUsableValues() {
        assertNull(parse("""
                PackageLicenseDeclared: NOASSERTION
                PackageCopyrightText: Copyright 2026 X
                """, "noassertion.spdx"));
        assertTrue(warned("PackageLicenseDeclared", "NOASSERTION"), warnings.toString());
        assertNull(parse("""
                PackageLicenseDeclared: MIT
                PackageCopyrightText: NONE
                """, "none.spdx"));
        assertTrue(warned("PackageCopyrightText"), warnings.toString());
    }

    @Test
    void unterminatedTextValueIsMalformed() {
        SpdxLicenseInfo.SpdxFormatException e = assertThrows(SpdxLicenseInfo.SpdxFormatException.class, () -> parse("""
                PackageLicenseDeclared: MIT
                PackageCopyrightText: <text>Copyright 2026 X
                """, "open.spdx"));
        assertTrue(e.getMessage().contains("open.spdx") && e.getMessage().contains("PackageCopyrightText")
                && e.getMessage().contains("line 2") && e.getMessage().contains("</text>"), e.getMessage());
    }

    @Test
    void lineWithoutTagIsMalformed() {
        SpdxLicenseInfo.SpdxFormatException e = assertThrows(SpdxLicenseInfo.SpdxFormatException.class, () -> parse("""
                SPDXVersion: SPDX-2.3
                PackageLicenseDeclared Apache-2.0
                PackageCopyrightText: Copyright 2026 X
                """, "typo.spdx"));
        assertTrue(e.getMessage().contains("line 2") && e.getMessage().contains("PackageLicenseDeclared Apache-2.0"),
                e.getMessage());
    }

    @Test
    void markdownLicenceIsNotSpdx() {
        assertThrows(SpdxLicenseInfo.SpdxFormatException.class, () -> parse("""
                # Community Specification License 1.0

                **The Purpose of this License.** This License sets forth the terms.
                """, "LICENSE.md"));
    }

    @Test
    void jsonSpdxIsNotTagValue() {
        assertThrows(SpdxLicenseInfo.SpdxFormatException.class, () -> parse("""
                {
                  "spdxVersion": "SPDX-2.3",
                  "packages": [ { "licenseDeclared": "MIT" } ]
                }
                """, "LICENSE.spdx.json"));
    }

    @Test
    void fileWithoutSpdxTagsIsNotSpdx() {
        SpdxLicenseInfo.SpdxFormatException e = assertThrows(SpdxLicenseInfo.SpdxFormatException.class,
                () -> parse("Name: x\nVersion: 1\n", "other.txt"));
        assertTrue(e.getMessage().contains("not an SPDX tag-value file"), e.getMessage());
        assertThrows(SpdxLicenseInfo.SpdxFormatException.class, () -> parse("", "empty.spdx"));
    }

    @Test
    void commentsAndBlankLinesAreAllowed() {
        SpdxLicenseInfo info = parse("""
                ## Document
                SPDXVersion: SPDX-2.3

                # Package
                PackageLicenseDeclared: MIT
                PackageCopyrightText: Copyright 2026 X
                """, "comments.spdx");
        assertEquals("MIT", info.licenseId());
    }

    @Test
    void missingOptionalFieldsWarnButKeepTheLicenceTerms() {
        SpdxLicenseInfo info = parse("""
                PackageLicenseDeclared: Apache-2.0
                PackageCopyrightText: Copyright 2026 Example Ltd
                """, "short.spdx");
        assertEquals("Apache-2.0", info.licenseId());
        assertTrue(warned("PackageName or PackageHomePage", "no attribution line"), warnings.toString());
        assertTrue(warned("PackageOriginator", "no authors"), warnings.toString());
        assertEquals(2, warnings.size(), warnings.toString());
    }

    @Test
    void missingHomePageAloneIsNamed() {
        parse("""
                PackageName: x
                PackageOriginator: Organization: X
                PackageLicenseDeclared: MIT
                PackageCopyrightText: Copyright 2026 X
                """, "no-home.spdx");
        assertEquals(List.of("SPDX file no-home.spdx has no usable PackageHomePage, so the file header will have no attribution line"),
                warnings);
    }
}
