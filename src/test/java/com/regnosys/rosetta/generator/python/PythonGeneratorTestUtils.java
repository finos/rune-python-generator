package com.regnosys.rosetta.generator.python;

import com.google.inject.Provider;
import com.regnosys.rosetta.rosetta.RosettaModel;
import com.regnosys.rosetta.tests.util.ModelHelper;
import jakarta.inject.Inject;
import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.eclipse.xtext.testing.util.ParseHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class PythonGeneratorTestUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(PythonGeneratorTestUtils.class);

    @Inject
    private ModelHelper modelHelper;
    @Inject
    private Provider<XtextResourceSet> resourceSetProvider;
    @Inject
    private ParseHelper<RosettaModel> parseHelper;
    @Inject
    private PythonCodeGenerator generator;

    public Properties getProperties() throws Exception {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        Model model = reader.read(new FileReader("pom.xml"));
        return model.getProperties();
    }

    public String getProperty(String property) throws Exception {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        Model model = reader.read(new FileReader("pom.xml"));
        return model.getProperties().getProperty(property);
    }

    public String getPropertyOrMessage(String propertyName) {
        // Check if property exists
        String property = null;
        try {
            property = getProperty(propertyName);
            if (property == null || property.isEmpty()) {
                LOGGER.error("Property:{} does not exist or is empty", propertyName);
            }
        } catch (Exception e) {
            LOGGER.error("Exception when getting property:{} exception:{}", propertyName, e.getMessage());
        }
        return property;
    }

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

    public void writeFiles(String pythonTgtPath, Map<String, ? extends CharSequence> generatedFiles)
            throws IOException {
        for (Map.Entry<String, ? extends CharSequence> entry : generatedFiles.entrySet()) {
            String filePath = entry.getKey();
            String fileContents = entry.getValue().toString();
            Path outputPath = Path.of(pythonTgtPath + File.separator + filePath);
            Files.createDirectories(outputPath.getParent());
            Files.write(outputPath, fileContents.getBytes(StandardCharsets.UTF_8));
        }
        LOGGER.info("Write Files ... wrote: {}", generatedFiles.size());
    }

    public Map<String, CharSequence> generatePythonFromRosettaModel(RosettaModel m, ResourceSet resourceSet) {
        String version = m.getVersion();
        Map<String, CharSequence> result = new HashMap<>();
        result.putAll(generator.beforeGenerate(m.eResource(), m, version));
        result.putAll(generator.generate(m.eResource(), m, version));
        result.putAll(generator.afterGenerate(m.eResource(), m, version));
        return result;
    }

    public List<Path> getFileList(String dslSourceDir, String suffix) throws Exception {
        LOGGER.info("getFileList ... looking for files with suffix {} in {}", suffix, dslSourceDir);

        if (dslSourceDir == null) {
            throw new Exception("Initialization failure: source dsl path not specified");
        }

        if (suffix == null) {
            throw new Exception("Initialization failure: extension not specified");
        }

        Path sourcePath = Paths.get(dslSourceDir);
        if (!Files.exists(sourcePath)) {
            throw new Exception("Unable to generate Python from non-existent source directory: " + dslSourceDir);
        }

        List<Path> result = new ArrayList<>();
        File directory = new File(dslSourceDir);
        if (directory.isDirectory()) {
            File[] files = directory.listFiles(file -> file.isFile() && file.getName().endsWith(suffix));
            if (files != null) {
                for (File file : files) {
                    result.add(file.toPath());
                }
            }
        }
        LOGGER.info("getFileList ... found {} files in {}", result.size(), dslSourceDir);
        return result;
    }

    public List<Path> getFileListWithRecursion(String dslSourceDir, String suffix) throws Exception {
        LOGGER.info("getFileListWithRecursion ... looking for files with suffix {} in {}", suffix, dslSourceDir);

        if (dslSourceDir == null) {
            throw new Exception("Initialization failure: source dsl path not specified");
        }

        if (suffix == null) {
            throw new Exception("Initialization failure: extension not specified");
        }

        Path sourcePath = Paths.get(dslSourceDir);
        if (!Files.exists(sourcePath)) {
            throw new Exception("Unable to generate Python from non-existent source directory: " + dslSourceDir);
        }

        List<Path> result = new ArrayList<>();

        try (Stream<Path> stream = Files.walk(sourcePath, FileVisitOption.FOLLOW_LINKS)) {
            result.addAll(
                    stream
                            .filter(path -> Files.isRegularFile(path) && path.toString().endsWith(suffix))
                            .collect(Collectors.toList()));
        } catch (IOException e) {
            throw e;
        }
        LOGGER.info("getFileListWithRecursion ... found {} files in {}", result.size(), dslSourceDir);
        return result;
    }

    public void generatePythonFromDSLFiles(List<Path> dslFilePathList, String outputPath) throws Exception {
        LOGGER.info("generatePythonFromDSLFiles ... generating Python from {} rosetta files", dslFilePathList.size());
        XtextResourceSet resourceSet = resourceSetProvider.get();
        // Assuming parseHelper.parse is what is meant by 'parse' in the Xtend file,
        // but Xtend had 'extension ParseHelper', so 'parse' calls ParseHelper.parse.
        parseHelper.parse(ModelHelper.commonTestTypes, resourceSet);

        resourceSet.getResource(URI.createURI("classpath:/model/basictypes.rosetta"), true);
        resourceSet.getResource(URI.createURI("classpath:/model/annotations.rosetta"), true);

        List<Resource> resources = dslFilePathList.stream()
                .map(it -> resourceSet.getResource(URI.createURI(it.toString()), true))
                .collect(Collectors.toList());

        LOGGER.info("generatePythonFromDSLFiles ... converted to resources");

        List<RosettaModel> rosettaModels = resources.stream()
                .flatMap(r -> r.getContents().stream())
                .filter(RosettaModel.class::isInstance)
                .map(RosettaModel.class::cast)
                .collect(Collectors.toList());

        LOGGER.info("generatePythonFromDSLFiles ... created {} rosetta models", rosettaModels.size());

        if (rosettaModels.isEmpty()) {
            return;
        }

        RosettaModel m = rosettaModels.get(0);
        String version = m.getVersion();
        Map<String, CharSequence> generatedFiles = new HashMap<>();

        generator.beforeAllGenerate(resourceSet, rosettaModels, version);
        for (RosettaModel model : rosettaModels) {
            LOGGER.info("generatePythonFromDSLFiles ... processing model: {}", model.getName());
            Map<String, CharSequence> python = generatePythonFromRosettaModel(model, resourceSet);
            generatedFiles.putAll(python);
        }
        generatedFiles.putAll(generator.afterAllGenerate(resourceSet, rosettaModels, version));

        cleanFolder(outputPath);
        writeFiles(outputPath, generatedFiles);
        LOGGER.info("generatePythonFromDSLFiles ... done");
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
