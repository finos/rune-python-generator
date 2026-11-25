/*
 * Copyright (c) 2023 CLOUDRISK Limited and FT Advisory LLC
 * SPDX-License-Identifier: Apache-2.0
 */
package com.regnosys.rosetta.generator.python.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.regnosys.rosetta.generator.python.PythonGeneratorTestUtils;
import com.regnosys.rosetta.tests.RosettaInjectorProvider;

import jakarta.inject.Inject;

@ExtendWith(InjectionExtension.class)
@InjectWith(RosettaInjectorProvider.class)
@SuppressWarnings("checkstyle:LineLength")
class PythonTomlGeneratorTest {

    /** Injected test utilities. */
    @Inject
    private PythonGeneratorTestUtils testUtils;

    /** Rosetta model used across tests in this class. */
    private static final String MODEL = """
            namespace test.model
            version "${project.version}"
            type Foo:
                bar string (1..1)
            """;

    /**
     * When no version is provided via CLI (or when the model carries the Maven
     * placeholder), the generated pyproject.toml must default to "0.0.0".
     */
    @Test
    void testTomlDefaultVersionIsZeroZeroZero() {
        // model version is "${project.version}" — cleanVersion maps it to "0.0.0"
        Map<String, CharSequence> python = testUtils.generatePythonFromString(MODEL);
        String toml = python.get("pyproject.toml").toString();
        assertTrue(toml.contains("version = \"0.0.0\""),
                "Expected pyproject.toml to contain version = \"0.0.0\" but was:\n" + toml);
    }

    /**
     * When the caller supplies an explicit version (e.g. from the CLI {@code -v}
     * option), that version must appear verbatim in the generated pyproject.toml.
     */
    @Test
    void testTomlVersionWhenSpecified() {
        Map<String, CharSequence> python = testUtils.generatePythonFromString(MODEL, null, "1.2.3");
        String toml = python.get("pyproject.toml").toString();
        assertTrue(toml.contains("version = \"1.2.3\""),
                "Expected pyproject.toml to contain version = \"1.2.3\" but was:\n" + toml);
    }

    /**
     * Generated projects require the runtime version that introduced
     * {@code rune_sorted}.
     */
    @Test
    void testTomlRequiresRuntimeVersionThreeOrLater() {
        Map<String, CharSequence> python = testUtils.generatePythonFromString(MODEL);
        String toml = python.get("pyproject.toml").toString();
        assertTrue(toml.contains("\"rune.runtime>=3.1.0,<4.0.0\""),
                "Expected pyproject.toml to require rune.runtime>=3.1.0,<4.0.0 but was:\n" + toml);
    }

    /**
     * When a namespace prefix is supplied (via the future {@code -x} CLI option),
     * the pyproject.toml project name should incorporate the prefix as the first
     * segment (e.g. {@code python-finos} for prefix {@code finos}).
     *
     * <p>Disabled until the namespace-prefix feature is implemented in the
     * generator.
     */
    @Test
    void testTomlProjectNameWithNamespacePrefix() {
        Map<String, CharSequence> python = testUtils.generatePythonFromString(MODEL, "finos", null);
        String toml = python.get("pyproject.toml").toString();
        assertTrue(toml.contains("name = \"python-finos\""),
                "Expected pyproject.toml to contain name = \"python-finos\" but was:\n" + toml);
    }

    /**
     * pyproject.toml names README.md as the package description, so the generator must write it.
     * Without it, building the package warns that the file cannot be found.
     */
    @Test
    void testReadmeNamedByTomlIsGenerated() {
        Map<String, CharSequence> python = testUtils.generatePythonFromString(MODEL, null, "1.2.3");
        String toml = python.get("pyproject.toml").toString();
        assertTrue(toml.contains("readme = \"README.md\""),
                "Expected pyproject.toml to name README.md but was:\n" + toml);
        CharSequence readme = python.get("README.md");
        assertNotNull(readme, "Expected README.md to be generated alongside pyproject.toml");
        assertTrue(readme.toString().contains("# python-test"),
                "Expected README.md to name the package but was:\n" + readme);
        assertTrue(readme.toString().contains("Version: 1.2.3"),
                "Expected README.md to give the version but was:\n" + readme);
    }

    /**
     * The README names the same package as pyproject.toml, including any namespace prefix.
     */
    @Test
    void testReadmeUsesProjectNameWithNamespacePrefix() {
        Map<String, CharSequence> python = testUtils.generatePythonFromString(MODEL, "finos", null);
        String readme = python.get("README.md").toString();
        assertTrue(readme.contains("# python-finos"),
                "Expected README.md to name python-finos but was:\n" + readme);
    }

    /**
     * Without a licence file the generated pyproject.toml declares no licence and no authors.
     */
    @Test
    void testTomlHasNoLicenseOrAuthorsByDefault() {
        Map<String, CharSequence> python = testUtils.generatePythonFromString(MODEL);
        String toml = python.get("pyproject.toml").toString();
        assertFalse(toml.contains("license"),
                "Expected pyproject.toml to have no license entry but was:\n" + toml);
        assertFalse(toml.contains("authors"),
                "Expected pyproject.toml to have no authors entry but was:\n" + toml);
    }

    /**
     * With licence terms, pyproject.toml declares the licence and the originator as the author.
     */
    @Test
    void testTomlDeclaresLicenseAndAuthorFromLicenseInfo() {
        SpdxLicenseInfo info = new SpdxLicenseInfo("Community-Spec-1.0", "Copyright 2021 FINOS and CDM Participants",
                null, null, "FINOS", null);
        String toml = PythonCodeGeneratorUtil.createPYProjectTomlFile(null, "1.0.0", "python-cdm", info);
        assertTrue(toml.contains("version = \"1.0.0\"\nlicense = \"Community-Spec-1.0\"\nrequires-python"),
                "Expected pyproject.toml to declare the licence after the version but was:\n" + toml);
        assertTrue(toml.contains("authors = [\n    { name = \"FINOS\" }\n]\n"),
                "Expected pyproject.toml to name FINOS as the author but was:\n" + toml);
    }

    /**
     * An originator email becomes the author's email, and quotes are escaped.
     */
    @Test
    void testTomlAuthorWithEmailAndQuotes() {
        SpdxLicenseInfo info = new SpdxLicenseInfo("MIT", "Copyright 2026 X", null, null,
                "Example \"Q\" Ltd", "info@example.org");
        String toml = PythonCodeGeneratorUtil.createPYProjectTomlFile(null, "1.0.0", "python-x", info);
        assertTrue(toml.contains("{ name = \"Example \\\"Q\\\" Ltd\", email = \"info@example.org\" }"),
                "Expected an escaped name and an email but was:\n" + toml);
    }

    /**
     * Licence terms without an originator declare the licence but no authors.
     */
    @Test
    void testTomlHasNoAuthorsWithoutOriginator() {
        SpdxLicenseInfo info = new SpdxLicenseInfo("MIT", "Copyright 2026 X", null, null, null, null);
        String toml = PythonCodeGeneratorUtil.createPYProjectTomlFile(null, "1.0.0", "python-x", info);
        assertTrue(toml.contains("license = \"MIT\""), toml);
        assertFalse(toml.contains("authors"), "Expected no authors entry but was:\n" + toml);
    }
}
