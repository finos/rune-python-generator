/*
 * Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
 * SPDX-License-Identifier: Apache-2.0
 */
package com.regnosys.rosetta.generator.python;

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
}
