/*
 * Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
 * SPDX-License-Identifier: Apache-2.0
 */
package com.regnosys.rosetta.generator.python.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PythonCodeGeneratorUtilTest {

    @Test
    void testCreateVersionFileStableVersion() {
        String result = PythonCodeGeneratorUtil.createVersionFile("7.0.0");
        assertTrue(result.contains("version = (7,0,0)"), "version tuple should contain only numeric parts");
        assertTrue(result.contains("version_str = '7.0.0'"), "version_str should match stable version");
        assertTrue(result.contains("__version__ = '7.0.0'"), "__version__ should match stable version");
    }

    @Test
    void testCreateVersionFileDevVersion() {
        String result = PythonCodeGeneratorUtil.createVersionFile("7.0.0.dev98");
        assertTrue(result.contains("version = (7,0,0)"), "version tuple should contain only numeric base, not dev suffix");
        assertTrue(result.contains("version_str = '7.0.0.dev98'"), "version_str should include dev designation");
        assertTrue(result.contains("__version__ = '7.0.0.dev98'"), "__version__ should be PEP 440 dev version");
        assertFalse(result.contains("dev98,"), "dev suffix must not appear unquoted in the version tuple");
    }

    @Test
    void testCreateVersionFileDevVersionTupleIsValidPython() {
        String result = PythonCodeGeneratorUtil.createVersionFile("7.0.0.dev98");
        // Ensure the tuple line contains only digits and commas (no bare identifiers)
        String tupleLine = result.lines()
                .filter(l -> l.startsWith("version = ("))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No version tuple line found"));
        String tupleContent = tupleLine.replaceAll("version = \\((.*)\\)", "$1");
        assertTrue(tupleContent.matches("[\\d,]+"), "version tuple must contain only digits and commas, got: " + tupleContent);
    }

    @Test
    void testCleanVersionDevFormat() {
        // CLI converts "7.0.0-dev.98" to "7.0.0.dev98" before calling cleanVersion
        assertTrue(PythonCodeGeneratorUtil.cleanVersion("7.0.0.dev98").equals("7.0.0.dev98"),
                "cleanVersion should preserve PEP 440 dev versions unchanged");
    }

    @Test
    void testCleanVersionStable() {
        assertTrue(PythonCodeGeneratorUtil.cleanVersion("7.0.0").equals("7.0.0"),
                "cleanVersion should preserve stable versions unchanged");
    }

    @Test
    void testCleanVersionNullReturnsDefault() {
        assertTrue(PythonCodeGeneratorUtil.cleanVersion(null).equals("0.0.0"),
                "cleanVersion should return 0.0.0 for null input");
    }

    @Test
    void testCleanVersionMavenPlaceholderReturnsDefault() {
        assertTrue(PythonCodeGeneratorUtil.cleanVersion("${project.version}").equals("0.0.0"),
                "cleanVersion should return 0.0.0 for Maven placeholder");
    }
}
