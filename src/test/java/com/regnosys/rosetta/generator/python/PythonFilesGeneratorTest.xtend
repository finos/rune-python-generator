package com.regnosys.rosetta.generator.python

import com.google.inject.Inject
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.extensions.InjectionExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.DynamicTest
import org.slf4j.LoggerFactory
import com.regnosys.rosetta.tests.RosettaInjectorProvider
import java.io.IOException
import java.nio.file.Paths
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.^extension.ExtendWith
import static org.junit.jupiter.api.Assertions.*
import java.util.ArrayList
import org.junit.jupiter.api.Disabled

// TODO: add generated unit tests for number operations with arithmetic operators -,*,/ and for number and string operations with +
/*
 * File based unit tests 
 * 
 * includes support for the generation of Python from CDM and other Rune definitions
 * 
 */
// TODO: move source rune test code to the test directory

@ExtendWith(InjectionExtension)
@InjectWith(RosettaInjectorProvider)
class PythonFilesGeneratorTest {

    static val LOGGER = LoggerFactory.getLogger(PythonFilesGeneratorTest)

    @Inject PythonGeneratorTestUtils utils

    /*
     * generate CDM from Rosetta files.  Should be disabled for releases
     */
    @Disabled
    @Test
    def void generateCDMPythonFromRosetta() {
        try {
            LOGGER.info('generateCDMPythonFromRosetta ... start')
            // Retrieve properties
            val rosettaSourcePath = utils.getPropertyOrMessage('cdm.rosetta.source.path')
            val pythonOutputPath  = utils.getPropertyOrMessage('cdm.python.output.path')
            if (rosettaSourcePath !== null && pythonOutputPath !== null) {
                // Proceed with the test
                utils.generatePythonFromDSLFiles(utils.getFileList(rosettaSourcePath, 'rosetta'), pythonOutputPath)
            }
            LOGGER.info('generateCDMPythonFromRosetta ... done')
        } 
        catch (IOException ioE) {
            LOGGER.error('generateCDMPythonFromRosetta ... processing failed with an IO Exception')
        }
        catch (ClassCastException ccE) {
            LOGGER.error('generateCDMPythonFromRosetta ... processing failed with a ClassCastException')
        }
        catch(Exception e) {
            LOGGER.error('generateCDMPythonFromRosetta ... processing failed with an Exception')
            LOGGER.error('generateCDMPythonFromRosetta ... exception:', e)
        }
    }
    /*
     * generate Python Unit Tests from Rosetta files.
     */
//    @Disabled
    @Test
    def void generatePythonUnitTests() {
        try {
            LOGGER.info('generatePythonUnitTests ... start')
            // Retrieve properties
            val rosettaSourcePath = utils.getPropertyOrMessage ('unittest.rosetta.source.path')
            val pythonOutputPath  = utils.getPropertyOrMessage ('unittest.python.output.path')
            // Proceed with the test
            if (rosettaSourcePath !== null && pythonOutputPath !== null) {
                utils.generatePythonFromDSLFiles(utils.getFileList(rosettaSourcePath, 'rosetta'), pythonOutputPath)
            }
            LOGGER.info('generatePythonUnitTests ... done')
        } 
        catch (IOException ioE) {
            LOGGER.error('generatePythonUnitTests ... processing failed with an IO Exception')
            LOGGER.error('\n' + ioE.toString())
        }
        catch (ClassCastException ccE) {
            LOGGER.error('generatePythonUnitTests ... processing failed with a ClassCastException')
            LOGGER.error('\n' + ccE.toString())
        }
        catch(Exception e) {
            LOGGER.error('generatePythonUnitTestsFromRosetta ... processing failed with an Exception')
            LOGGER.error('\n' + e.toString())
        }
    }

    /*
     * generate Serialization Python Unit Tests from Rosetta files.
     */
//    @Disabled
    @Test
    def void generatePythonSerializationUnitTests() {
        try {
            LOGGER.info('generatePythonSerializationUnitTests ... start')
            // Retrieve properties
            val rosettaRoundTripSourcePath  = utils.getPropertyOrMessage ('serialization.test.rune.roundtrip.source.path')
            val rosettaShouldFailSourcePath = utils.getPropertyOrMessage ('serialization.test.rune.shouldfail.source.path')
            val pythonOutputPath  = utils.getPropertyOrMessage ('serialization.test.python.output.path')
            if (pythonOutputPath !== null) {
                var rosettaFiles = new ArrayList<Path>();
                if (rosettaRoundTripSourcePath !== null) {
                    rosettaFiles.addAll (utils.getFileListWithRecursion(rosettaRoundTripSourcePath, 'rosetta'))
                }
                if (rosettaShouldFailSourcePath !== null) {
                    rosettaFiles.addAll (utils.getFileListWithRecursion(rosettaShouldFailSourcePath, 'rosetta'))
                }
                if (rosettaFiles.length () > 0) {
                    LOGGER.info('generatePythonSerializationUnitTests ... generate python start')
                    utils.generatePythonFromDSLFiles(rosettaFiles, pythonOutputPath)
                    val path = Paths.get(pythonOutputPath + '/__init__.py')
                    if (!Files.exists(path)) {
                        Files.createFile(path)
                    }
                    LOGGER.info('generatePythonSerializationUnitTests ... generate python end')
                }
            }
            LOGGER.info('generatePythonSerializationUnitTests ... done')
        } 
        catch (IOException ioE) {
            LOGGER.error('generatePythonSerializationUnitTests ... processing failed with an IO Exception')
            LOGGER.error('\n' + ioE.toString())
        }
        catch (ClassCastException ccE) {
            LOGGER.error('generatePythonSerializationUnitTests ... processing failed with a ClassCastException')
            LOGGER.error('\n' + ccE.toString())
        }
        catch(Exception e) {
            LOGGER.error('generatePythonSerializationUnitTests ... processing failed with an Exception')
            LOGGER.error('\n' + e.toString())
        }
    }

    /*
     * Test generated syntax matching expectations
     */
    @Disabled
    @TestFactory
    def Iterable<DynamicTest> testGeneratedSyntax() {
        val tests = newArrayList

        try {
            LOGGER.info('testGeneratedSyntax ... start')

            // Retrieve properties
            val sourcePath   = utils.getPropertyOrMessage('unittest.generated.syntax.source.path')
            val targetPath   = utils.getPropertyOrMessage('unittest.generated.syntax.target.path')
            val expectedPath = utils.getPropertyOrMessage('unittest.generated.syntax.expected.path')
            if (sourcePath !== null && targetPath !== null && expectedPath !== null) {
                // Generate Python from DSL files
                utils.generatePythonFromDSLFiles(utils.getFileListWithRecursion(sourcePath, 'rosetta'), targetPath)

                // Verify generated code against expected code
                val generatedFiles = utils.getFileListWithRecursion(targetPath, 'py')

                for (generatedFile : generatedFiles) {
                    val fileName = generatedFile.getFileName.toString
                    if (fileName != "__init__.py" && fileName != "version.py") {
                        // Calculate the relative path from the targetPath
                        val expectedFilePathString = generatedFile.toString.replace(targetPath, expectedPath)
                        val expectedFilePath = Paths.get(expectedFilePathString)

                        tests.add(DynamicTest.dynamicTest("Test for " + fileName, [
                            if (Files.exists(expectedFilePath)) {
                                val expectedCode = Files.readString(expectedFilePath)
                                val generatedCode = Files.readString(generatedFile)

                                // Assert that the expected code matches the generated code
                                assertTrue(generatedCode.contains(expectedCode),
                                    "Mismatch in generated code for file: " + generatedFile.toString +
                                    "\nExpected:\n" + expectedCode +
                                    "\nGenerated:\n" + generatedCode)
                            } else {
                                fail("Expected file does not exist ... generated file: " + generatedFile.toString + " expected path: " + expectedFilePath.toString)
                            }
                        ]))
                    }
                }
            }
            LOGGER.info('testGeneratedSyntax ... done')
        } catch (IOException ioE) {
            LOGGER.error('generatePythonCodeGeneratorUnitTests ... processing failed with an IO Exception')
            LOGGER.error('\n' + ioE.getMessage())
        } catch (ClassCastException ccE) {
            LOGGER.error('generatePythonCodeGeneratorUnitTests ... processing failed with a ClassCastException')
            LOGGER.error('\n' + ccE.getMessage())
        } catch (Exception e) {
            LOGGER.error('generatePythonCodeGeneratorUnitTests ... processing failed with an Exception')
            LOGGER.error('\n' + e.getMessage())
        }
        return tests
    }

}