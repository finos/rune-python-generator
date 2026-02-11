package com.regnosys.rosetta.generator.python;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.emf.ecore.resource.ResourceSet;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.regnosys.rosetta.rosetta.RosettaModel;
import com.regnosys.rosetta.tests.util.ModelHelper;

import jakarta.inject.Inject;

public final class PythonGeneratorTestUtils {

    /**
     * Model helper for parsing Rosetta models.
     */
    @Inject
    private ModelHelper modelHelper;

    /**
     * Python code generator.
     */
    @Inject
    private PythonCodeGenerator generator;

    /**
     * Generate Python from a Rosetta model.
     *
     * @param m
     * @param resourceSet
     * @return result
     */
    public Map<String, CharSequence> generatePythonFromRosettaModel(RosettaModel m, ResourceSet resourceSet) {
        String version = m.getVersion();
        Map<String, CharSequence> result = new HashMap<>();
        result.putAll(generator.beforeGenerate(m.eResource(), m, version));
        result.putAll(generator.generate(m.eResource(), m, version));
        result.putAll(generator.afterGenerate(m.eResource(), m, version));
        return result;
    }

    /**
     * Generate Python from a Rosetta model string.
     *
     * @param modelContent
     * @return result
     */
    public Map<String, CharSequence> generatePythonFromString(String modelContent) {
        // model.parseRosettaWithNoErrors in Xtend ->
        // modelHelper.parseRosettaWithNoErrors(modelContent)
        RosettaModel m = modelHelper.parseRosettaWithNoErrors(modelContent);
        ResourceSet resourceSet = m.eResource().getResourceSet();
        String version = m.getVersion();

        Map<String, CharSequence> result = new HashMap<>();
        result.putAll(generator.beforeAllGenerate(resourceSet, Collections.singletonList(m), version));
        result.putAll(generator.beforeGenerate(m.eResource(), m, version));
        result.putAll(generator.generate(m.eResource(), m, version));
        result.putAll(generator.afterGenerate(m.eResource(), m, version));
        result.putAll(generator.afterAllGenerate(resourceSet, Collections.singletonList(m), version));
        return result;
    }

    /**
     * Generate Python and extract the bundle.
     *
     * @param model
     * @return result
     */
    public String generatePythonAndExtractBundle(String model) {
        Map<String, CharSequence> python = generatePythonFromString(model);
        return python.get("src/com/_bundle.py").toString();
    }

    /**
     * Assert that the generated Python contains the expected string.
     *
     * @param generated
     * @param expectedString
     */
    public void assertGeneratedContainsExpectedString(String generated, String expectedString) {
        String msg = String.format("""
                generated Python does not match expected
                Expected:
                %s
                Generated:
                %s""", expectedString, generated);

        assertTrue(generated.contains(expectedString), msg);
    }

    /**
     * Assert that the generated bundle contains the expected string.
     *
     * @param model
     * @param expectedString
     */
    public void assertBundleContainsExpectedString(String model, String expectedString) {
        // Generate the bundle using the existing function
        String generatedBundle = generatePythonAndExtractBundle(model);
        assertGeneratedContainsExpectedString(generatedBundle, expectedString);
    }
}
