/*
 * Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
 * SPDX-License-Identifier: Apache-2.0
 */
package com.regnosys.rosetta.generator.python;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

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
     * {@code rune_resolve_attr_values}.
     */
    @Test
    void testTomlRequiresRuntimeVersionTwoTwoOrLater() {
        Map<String, CharSequence> python = testUtils.generatePythonFromString(MODEL);
        String toml = python.get("pyproject.toml").toString();
        assertTrue(toml.contains("\"rune.runtime>=2.2.0,<3.0.0\""),
                "Expected pyproject.toml to require rune.runtime>=2.2.0,<3.0.0 but was:\n" + toml);
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
}
