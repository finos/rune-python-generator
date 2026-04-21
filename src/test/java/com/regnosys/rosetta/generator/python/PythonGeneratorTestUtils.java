/*
 * Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
 * SPDX-License-Identifier: Apache-2.0
 */
package com.regnosys.rosetta.generator.python;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.ecore.resource.ResourceSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.regnosys.rosetta.rosetta.RosettaModel;
import com.regnosys.rosetta.tests.util.ModelHelper;

import jakarta.inject.Inject;

/**
 * Utility class for testing Python code generation.
 */
@SuppressWarnings("checkstyle:LineLength")
public final class PythonGeneratorTestUtils {

    /** Helper for parsing Rosetta models in tests. */
    @Inject
    private ModelHelper modelHelper;
    /** The Python code generator under test. */
    @Inject
    private PythonCodeGenerator generator;

    public Map<String, CharSequence> generatePythonFromRosettaModel(
        RosettaModel m,
        ResourceSet resourceSet) {
        String version = m.getVersion();
        Map<String, CharSequence> result = new HashMap<>();
        result.putAll(generator.beforeGenerate(m.eResource(), m, version));
        result.putAll(generator.generate(m.eResource(), m, version));
        result.putAll(generator.afterGenerate(m.eResource(), m, version));
        return result;
    }

    public Map<String, CharSequence> generatePythonFromString(String modelContent) {
        return generatePythonFromString(modelContent, null, null);
    }

    public Map<String, CharSequence> generatePythonFromString(String modelContent, String namespacePrefix, String version) {
        generator.setNamespacePrefix(namespacePrefix);
        try {
            RosettaModel m = modelHelper.parseRosettaWithNoErrors(modelContent);
            ResourceSet resourceSet = m.eResource().getResourceSet();
            String resolvedVersion = (version != null) ? version : m.getVersion();
            Map<String, CharSequence> result = new HashMap<>();
            result.putAll(generator.beforeAllGenerate(resourceSet, Collections.singletonList(m), resolvedVersion));
            result.putAll(generator.beforeGenerate(m.eResource(), m, resolvedVersion));
            result.putAll(generator.generate(m.eResource(), m, resolvedVersion));
            result.putAll(generator.afterGenerate(m.eResource(), m, resolvedVersion));
            result.putAll(generator.afterAllGenerate(resourceSet, Collections.singletonList(m), resolvedVersion));
            return result;
        } finally {
            generator.setNamespacePrefix(null);
        }
    }

    /**
     * Legacy helper - will now search globally for the class if it's not in the bundle.
     *
     * @param model the Rosetta model source string to generate Python from
     * @return all generated files concatenated with {@code --- filename ---} separators
     */
    public String generatePythonAndExtractBundle(String model) {
        Map<String, CharSequence> python = generatePythonFromString(model);
        StringBuilder allFiles = new StringBuilder();
        List<String> sortedKeys = new ArrayList<>(python.keySet());
        Collections.sort(sortedKeys);
        for (String key : sortedKeys) {
            allFiles.append("--- " + key + " ---\n");
            allFiles.append(python.get(key));
            allFiles.append("\n");
        }
        return allFiles.toString();
    }

    public void assertGeneratedContainsExpectedString(String generated, String expectedString) {
        String msg = String.format("""
                generated Python does not match expected
                Expected:
                %s
                Generated:
                %s""", expectedString, generated);

        assertTrue(generated.contains(expectedString), msg);
    }

    public void assertGeneratedDoesNotContain(String generated, String unexpectedString) {
        String msg = String.format("""
                generated Python should NOT contain:
                %s
                But was found in:
                %s""", unexpectedString, generated);
        assertFalse(generated.contains(unexpectedString), msg);
    }

    public void assertImportAppearsExactlyOnce(String generated, String importStatement) {
        int count = 0;
        int idx = 0;
        while ((idx = generated.indexOf(importStatement, idx)) != -1) {
            count++;
            idx += importStatement.length();
        }
        assertEquals(1, count, String.format(
                "Expected import '%s' to appear exactly once, but found %d occurrences",
                importStatement, count));
    }

    /**
     * Asserts that {@code first} appears before {@code second} in {@code generated}.
     * Both strings must be present.
     *
     * @param generated the generated Python source to search
     * @param first     the string expected to appear first
     * @param second    the string expected to appear after {@code first}
     */
    public void assertAppearsAfter(String generated, String first, String second) {
        int firstIdx = generated.indexOf(first);
        int secondIdx = generated.indexOf(second);
        assertTrue(firstIdx >= 0,
                String.format("Expected '%s' to be present but was not found", first));
        assertTrue(secondIdx >= 0,
                String.format("Expected '%s' to be present but was not found", second));
        assertTrue(firstIdx < secondIdx, String.format(
                "Expected '%s' (at %d) to appear before '%s' (at %d)",
                first, firstIdx, second, secondIdx));
    }

    /**
     * Now searches across ALL generated files to find the expected string.
     *
     * @param model          the Rosetta model source string to generate Python from
     * @param expectedString the string that must appear somewhere in the generated output
     */
    public void assertBundleContainsExpectedString(String model, String expectedString) {
        String allFiles = generatePythonAndExtractBundle(model);
        assertGeneratedContainsExpectedString(allFiles, expectedString);
    }
}
