/*
 * Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
 * SPDX-License-Identifier: Apache-2.0
 */
package com.regnosys.rosetta.generator.python.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The licence terms of a model, read from an SPDX tag-value file (for example the model's
 * {@code LICENSE.spdx}) and written into the generated Python.
 * <p>
 * Only the fields the generator uses are read; all others are ignored, so both a short
 * FINOS-style file and a complete SPDX 2.3 document are accepted. Licence terms are written only
 * when both required fields are present; each missing field is reported as a warning. A file that
 * is not a well-formed SPDX tag-value file is rejected with a {@link SpdxFormatException}. When a file describes more
 * than one package, the first value of each field is used.
 * </p>
 * <ul>
 * <li>{@code PackageLicenseDeclared} (required): the {@code SPDX-License-Identifier} line of the
 * file header, and {@code license} in {@code pyproject.toml}</li>
 * <li>{@code PackageCopyrightText} (required): the copyright line(s) of the file header</li>
 * <li>{@code PackageName} and {@code PackageHomePage} (optional): an attribution line naming the
 * model, its version and its source, written only when both are present</li>
 * <li>{@code PackageOriginator} (optional): {@code authors} in {@code pyproject.toml}, from a value
 * of the form {@code Organization: <name> (<email>)} or {@code Person: <name> (<email>)}, where
 * the email is optional</li>
 * </ul>
 *
 * @param licenseId       the declared SPDX licence expression
 * @param copyrightText   the copyright text, possibly spanning several lines
 * @param packageName     the model's name, or {@code null}
 * @param homePage        the model's home page, or {@code null}
 * @param originatorName  the name of the model's originator, or {@code null}
 * @param originatorEmail the originator's email, or {@code null}
 */
public record SpdxLicenseInfo(String licenseId, String copyrightText, String packageName, String homePage,
        String originatorName, String originatorEmail) {

    /** SPDX tag for the declared licence. */
    static final String LICENSE_TAG = "PackageLicenseDeclared";
    /** SPDX tag for the copyright text. */
    static final String COPYRIGHT_TAG = "PackageCopyrightText";
    /** SPDX tag for the package name. */
    static final String NAME_TAG = "PackageName";
    /** SPDX tag for the package home page. */
    static final String HOME_PAGE_TAG = "PackageHomePage";
    /** SPDX tag for the package originator. */
    static final String ORIGINATOR_TAG = "PackageOriginator";

    /** An originator value: an optional {@code Organization:} or {@code Person:} type, a name and an optional email. */
    private static final Pattern ORIGINATOR = Pattern.compile(
            "^(?:(?:Organization|Person)\\s*:\\s*)?(.*?)\\s*(?:\\(([^()]*)\\))?$");

    /** Logger for warnings about missing licence information. */
    private static final Logger LOGGER = LoggerFactory.getLogger(SpdxLicenseInfo.class);

    /** The longest part of a malformed line quoted in an error. */
    private static final int EXCERPT_LENGTH = 60;

    /** A tag: a letter followed by letters, digits or hyphens. */
    private static final Pattern TAG = Pattern.compile("[A-Za-z][A-Za-z0-9-]*");

    /** Opening marker of a multi-line SPDX value. */
    private static final String TEXT_OPEN = "<text>";
    /** Closing marker of a multi-line SPDX value. */
    private static final String TEXT_CLOSE = "</text>";

    /**
     * Reads the licence terms from an SPDX tag-value file, logging a warning for each piece of
     * information that is missing.
     *
     * @param file the SPDX file
     * @return the licence terms, or {@code null} when a required field is missing, in which case
     *         no licence terms are to be written
     * @throws IOException          if the file cannot be read
     * @throws SpdxFormatException  if the file is not a well-formed SPDX tag-value file
     */
    public static SpdxLicenseInfo read(Path file) throws IOException {
        return parse(Files.readString(file), file.toString(), LOGGER::warn);
    }

    /**
     * Parses the licence terms from SPDX tag-value content.
     * <p>
     * Both {@code PackageLicenseDeclared} and {@code PackageCopyrightText} are needed. If either is
     * missing, empty, {@code NONE} or {@code NOASSERTION}, a warning is reported for it and
     * {@code null} is returned: no header, {@code license} or {@code authors} is written. A missing
     * optional field is also reported, and only the output that depends on it is left out.
     * </p>
     *
     * @param content  the SPDX tag-value text
     * @param source   a name for the content, used in the warnings
     * @param warnings receives a message for each piece of missing information
     * @return the licence terms, or {@code null} when a required field is missing
     * @throws SpdxFormatException if the content is not a well-formed SPDX tag-value file: it has
     *                             no SPDX tags, a line that is neither {@code Tag: value}, a comment
     *                             nor blank, or a {@code <text>} value with no closing tag
     */
    public static SpdxLicenseInfo parse(String content, String source, Consumer<String> warnings) {
        Map<String, String> values = readTags(content, source, warnings);
        String licenseId = required(values, LICENSE_TAG, source, warnings);
        String copyright = required(values, COPYRIGHT_TAG, source, warnings);
        if (licenseId == null || copyright == null) {
            warnings.accept("SPDX file " + source + " lacks the required licence information, so no licence terms"
                    + " will be written: no file header, and no license or authors in pyproject.toml");
            return null;
        }
        String packageName = usable(values.get(NAME_TAG));
        String homePage = usable(values.get(HOME_PAGE_TAG));
        if (packageName == null || homePage == null) {
            warnings.accept("SPDX file " + source + " has no usable "
                    + (packageName == null && homePage == null ? NAME_TAG + " or " + HOME_PAGE_TAG
                            : packageName == null ? NAME_TAG : HOME_PAGE_TAG)
                    + ", so the file header will have no attribution line");
        }
        String originatorName = null;
        String originatorEmail = null;
        String originator = usable(values.get(ORIGINATOR_TAG));
        if (originator != null) {
            Matcher m = ORIGINATOR.matcher(originator);
            if (m.matches()) {
                originatorName = usable(m.group(1));
                originatorEmail = m.group(2) != null ? usable(m.group(2).strip()) : null;
            }
        }
        if (originatorName == null) {
            warnings.accept("SPDX file " + source + " has no usable " + ORIGINATOR_TAG
                    + ", so pyproject.toml will have no authors");
        }
        return new SpdxLicenseInfo(licenseId, copyright, packageName, homePage, originatorName, originatorEmail);
    }

    /**
     * Returns the header written at the top of every generated {@code .py} and {@code .toml} file.
     *
     * @param version the version of the generated package, used in the attribution line
     * @return the header, ending with a newline
     */
    public String header(String version) {
        StringBuilder sb = new StringBuilder("#\n");
        for (String line : copyrightText.split("\\R")) {
            sb.append(line.isBlank() ? "#" : "# " + line.strip()).append('\n');
        }
        sb.append("# SPDX-License-Identifier: ").append(licenseId).append('\n');
        if (packageName != null && homePage != null) {
            sb.append("# Generated from ").append(packageName);
            if (version != null && !version.isBlank()) {
                sb.append(' ').append(version);
            }
            sb.append(" (").append(homePage).append(")\n");
        }
        return sb.append("#\n").toString();
    }

    private static Map<String, String> readTags(String content, String source, Consumer<String> warnings) {
        Map<String, String> values = new HashMap<>();
        List<String> lines = content.lines().toList();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank() || line.stripLeading().startsWith("#")) {
                continue;
            }
            int colon = line.indexOf(':');
            String tag = colon < 0 ? "" : line.substring(0, colon).strip();
            if (!TAG.matcher(tag).matches()) {
                throw new SpdxFormatException("Licence file " + source + " is not a well-formed SPDX tag-value file:"
                        + " line " + (i + 1) + " is not of the form 'Tag: value': " + excerpt(line));
            }
            String value = line.substring(colon + 1).strip();
            if (value.startsWith(TEXT_OPEN)) {
                List<String> text = new ArrayList<>();
                String rest = value.substring(TEXT_OPEN.length());
                int start = i;
                while (!rest.contains(TEXT_CLOSE) && i + 1 < lines.size()) {
                    text.add(rest);
                    i++;
                    rest = lines.get(i);
                }
                if (!rest.contains(TEXT_CLOSE)) {
                    throw new SpdxFormatException("Licence file " + source + " is not a well-formed SPDX tag-value file:"
                            + " the value of " + tag + " on line " + (start + 1) + " has no closing " + TEXT_CLOSE);
                }
                text.add(rest.substring(0, rest.indexOf(TEXT_CLOSE)));
                value = String.join("\n", text).strip();
            }
            values.putIfAbsent(tag, value);
        }
        if (values.keySet().stream().noneMatch(SpdxLicenseInfo::isSpdxTag)) {
            throw new SpdxFormatException("Licence file " + source + " is not an SPDX tag-value file:"
                    + " it has none of the tags SPDXVersion, DataLicense, SPDXID or Package*");
        }
        return values;
    }

    private static String excerpt(String line) {
        String text = line.strip();
        return text.length() <= EXCERPT_LENGTH ? text : text.substring(0, EXCERPT_LENGTH) + "...";
    }

    private static boolean isSpdxTag(String tag) {
        return tag.equals("SPDXVersion") || tag.equals("DataLicense") || tag.equals("SPDXID")
                || tag.startsWith("Package");
    }

    /**
     * Thrown when a licence file is not a well-formed SPDX tag-value file.
     */
    public static final class SpdxFormatException extends IllegalArgumentException {
        private static final long serialVersionUID = 1L;

        /**
         * Creates the exception.
         *
         * @param message what is wrong with the file, naming it
         */
        public SpdxFormatException(String message) {
            super(message);
        }
    }

    private static String required(Map<String, String> values, String tag, String source,
            Consumer<String> warnings) {
        String value = usable(values.get(tag));
        if (value == null) {
            warnings.accept("SPDX file " + source + " has no usable " + tag
                    + " (missing, empty, NONE or NOASSERTION)");
        }
        return value;
    }

    private static String usable(String value) {
        if (value == null || value.isBlank() || value.equals("NONE") || value.equals("NOASSERTION")) {
            return null;
        }
        return value;
    }
}
