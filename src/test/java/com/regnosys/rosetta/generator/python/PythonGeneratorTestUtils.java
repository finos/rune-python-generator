package com.regnosys.rosetta.generator.python;

import com.regnosys.rosetta.rosetta.RosettaModel;
import com.regnosys.rosetta.tests.util.ModelHelper;
import jakarta.inject.Inject;
import org.apache.commons.io.FileUtils;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class PythonGeneratorTestUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(PythonGeneratorTestUtils.class);

    @Inject
    private ModelHelper modelHelper;
    @Inject
    private PythonCodeGenerator generator;

    public void cleanFolder(String folderPath) {
        File folder = new File(folderPath + File.separator + "src");
        if (folder.exists() && folder.isDirectory()) {
            try {
                FileUtils.cleanDirectory(folder);
            } catch (IOException e) {
                LOGGER.error("Failed to delete folder content: " + e.getMessage());
            }
        } else {
            LOGGER.info("{} does not exist or is not a directory", folderPath);
        }
    }

    public Map<String, CharSequence> generatePythonFromRosettaModel(RosettaModel m, ResourceSet resourceSet) {
        String version = m.getVersion();
        Map<String, CharSequence> result = new HashMap<>();
        result.putAll(generator.beforeGenerate(m.eResource(), m, version));
        result.putAll(generator.generate(m.eResource(), m, version));
        result.putAll(generator.afterGenerate(m.eResource(), m, version));
        return result;
    }

    public Map<String, CharSequence> generatePythonFromString(String modelContent) throws Exception {
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

    public String generatePythonAndExtractBundle(String model) throws Exception {
        Map<String, CharSequence> python = generatePythonFromString(model);
        return python.get("src/com/_bundle.py").toString();
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

    public void assertBundleContainsExpectedString(String model, String expectedString) throws Exception {
        // Generate the bundle using the existing function
        String generatedBundle = generatePythonAndExtractBundle(model);
        assertGeneratedContainsExpectedString(generatedBundle, expectedString);
    }
}
