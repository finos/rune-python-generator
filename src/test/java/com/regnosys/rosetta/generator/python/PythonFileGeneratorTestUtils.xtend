package com.regnosys.rosetta.generator.python

import com.google.inject.Inject
import com.google.inject.Provider
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.lang.CharSequence
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.FileVisitOption
import java.nio.file.Path
import java.nio.file.Paths
import java.util.ArrayList
import java.util.Collection
import java.util.Map
import java.util.Properties
import java.util.stream.Collectors
import org.apache.commons.io.FileUtils
import org.apache.maven.model.io.xpp3.MavenXpp3Reader
import org.eclipse.emf.common.util.URI
import org.eclipse.xtext.resource.XtextResourceSet
import org.eclipse.xtext.testing.util.ParseHelper
import com.regnosys.rosetta.rosetta.RosettaModel
import com.regnosys.rosetta.tests.util.ModelHelper
import org.slf4j.LoggerFactory

class PythonFileGeneratorTestUtils {

    static val LOGGER = LoggerFactory.getLogger(PythonFileGeneratorTestUtils)

    @Inject Provider<XtextResourceSet> resourceSetProvider

    @Inject extension ParseHelper<RosettaModel>
    
    @Inject PythonCodeGenerator generator   

    def Properties getProperties() throws Exception {
        val reader = new MavenXpp3Reader()
        val model = reader.read(new FileReader("pom.xml"))
        return model.getProperties()
    }

    def String getProperty(String property) throws Exception {
        val reader = new MavenXpp3Reader()
        val model = reader.read(new FileReader("pom.xml"))
        return model.getProperties().getProperty(property)
    }

    def String getPropertyOrMessage (String propertyName) {
        // Check if property exists
        var property = null as String;
        try {
            property =  getProperty(propertyName)
            if (property === null || property.isEmpty()) {
                LOGGER.error("Property:" + propertyName + " does not exist or is empty")
            }
        } catch (Exception e) {
            LOGGER.error("Exception when getting property:" + propertyName + "exception:" + e.getMessage())
        }
        return property;
    }

    def void cleanFolder(String folderPath) {
        val folder = new File(folderPath + File.separator + "src")
        if (folder.exists() && folder.isDirectory()) {
            try {
                FileUtils.cleanDirectory(folder)
            } catch (IOException e) {
                LOGGER.error("Failed to delete folder content: " + e.message)
            }
        } else {
            LOGGER.error(folderPath + " does not exist or is not a directory")
        }
    }

    def void writeFiles(String pythonTgtPath, Map<String, ? extends CharSequence> generatedFiles) {
        for (entry : generatedFiles.entrySet) {
            val filePath = entry.key
            val fileContents = entry.value.toString
            val outputPath = Path.of(pythonTgtPath + File.separator + filePath)
            Files.createDirectories(outputPath.parent)
            Files.write(outputPath, fileContents.getBytes(StandardCharsets.UTF_8))
        }
        LOGGER.info("Write Files ... wrote: {}", generatedFiles.size())
    }

    def Map<String, CharSequence> generatePythonFromRosettaModel(RosettaModel m, org.eclipse.emf.ecore.resource.ResourceSet resourceSet) {
        val version = m.version
        val result = newHashMap
        result.putAll(generator.beforeGenerate(m.eResource, m, version))
        result.putAll(generator.generate(m.eResource, m, version))
        result.putAll(generator.afterGenerate(m.eResource, m, version))
        result
    }

    def ArrayList<Path> getFileList(String dslSourceDir, String suffix) {
        LOGGER.info("getFileList ... looking for files with suffix {} in {}", suffix, dslSourceDir)

        if (dslSourceDir === null) {
            throw new Exception('Initialization failure: source dsl path not specified')
        }

        if (suffix === null) {
            throw new Exception('Initialization failure: extension not specified')
        }

        val sourcePath = Paths.get(dslSourceDir)
        if (!Files.exists(sourcePath)) {
            throw new Exception("Unable to generate Python from non-existent source directory: " + dslSourceDir)
        }

        val result = new ArrayList<Path>()
        val directory = new File(dslSourceDir)
        if (directory.isDirectory) {
            directory.listFiles.filter [ file |
                file.isFile && file.name.endsWith(suffix)
            ].forEach [ file |
                result.add(file.toPath)
            ]
        }
        LOGGER.info("getFileList ... found {} files in {}", result.size.toString, dslSourceDir)
        return result    
    }

    def ArrayList<Path> getFileListWithRecursion(String dslSourceDir, String suffix) {
        LOGGER.info("getFileListWithRecursion ... looking for files with suffix {} in {}", suffix, dslSourceDir)

        if (dslSourceDir === null) {
            throw new Exception('Initialization failure: source dsl path not specified')
        }

        if (suffix === null) {
            throw new Exception('Initialization failure: extension not specified')
        }

        val sourcePath = Paths.get(dslSourceDir)
        if (!Files.exists(sourcePath)) {
            throw new Exception("Unable to generate Python from non-existent source directory: " + dslSourceDir)
        }

        val result = new ArrayList<Path>()

        try {
            val stream = Files.walk(sourcePath, FileVisitOption.FOLLOW_LINKS)
            try {
                result.addAll(
                    stream
                        .filter [ path | 
                            Files.isRegularFile(path) && path.toString.endsWith(suffix)
                        ]
                        .collect(Collectors.toList)
                )
            } finally {
                stream.close()
            }
        } catch (IOException e) {
			throw e;
        }
        LOGGER.info("getFileListWithRecursion ... found {} files in {}", result.size.toString, dslSourceDir)
        return result    
    }

    def void generatePythonFromDSLFiles(ArrayList<Path> dslFilePathList, String outputPath) {
        LOGGER.info("generatePythonFromDSLFiles ... generating Python from {} rosetta files", dslFilePathList.length.toString())                  
        val resourceSet = resourceSetProvider.get 
        parse(ModelHelper.commonTestTypes, resourceSet)
        resourceSet.getResource(URI.createURI('classpath:/model/basictypes.rosetta'), true)
        resourceSet.getResource(URI.createURI('classpath:/model/annotations.rosetta'), true)

        val resources = dslFilePathList
            .map[resourceSet.getResource(URI.createURI(it.toString()), true)]
            .toList
        LOGGER.info("generatePythonFromDSLFiles ... converted to resources")                  
        val rosettaModels = resources
            .flatMap[contents.filter(RosettaModel)]
            .toList as Collection<RosettaModel>
        LOGGER.info("generatePythonFromDSLFiles ... created {} rosetta models", rosettaModels.length.toString())                  
        val m = rosettaModels.head() as RosettaModel
        val version = m.version
        val generatedFiles = newHashMap

        generator.beforeAllGenerate(resourceSet, rosettaModels, version)
        for (model : rosettaModels) {
            LOGGER.info("generatePythonFromDSLFiles ... processing model: {}", model.name)
            val python = generatePythonFromRosettaModel(model, resourceSet)
            generatedFiles.putAll(python)
        }
        generatedFiles.putAll(generator.afterAllGenerate(resourceSet, rosettaModels, version))

        cleanFolder(outputPath)
        writeFiles(outputPath, generatedFiles)
        LOGGER.info("generatePythonFromDSLFiles ... done")
    }
}