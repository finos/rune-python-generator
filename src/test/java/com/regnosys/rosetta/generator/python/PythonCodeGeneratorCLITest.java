/*
 * Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
 * SPDX-License-Identifier: Apache-2.0
 */
package com.regnosys.rosetta.generator.python;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.emf.ecore.util.EcoreUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("checkstyle:LineLength")
class PythonCodeGeneratorCLITest {

    /**
     * Temporary directory for test files.
     */
    @TempDir
    private Path tempDir;

    @BeforeAll
    static void setup() {
        System.out.println(
                ">>> Starting PythonCodeGeneratorCLITest. Expected error and warning logs may follow as part of validation testing.");
    }

    @AfterAll
    static void tearDown() {
        System.out.println(">>> Finished PythonCodeGeneratorCLITest.");
    }

    @Test
    void testMissingArgsReturnsError() {
        PythonCodeGeneratorCLI cli = new PythonCodeGeneratorCLI();
        int exitCode = cli.run(new String[] {});
        assertEquals(1, exitCode, "Should return 1 when no args provided");
    }

    @Test
    void testHelpReturnsSuccess() {
        PythonCodeGeneratorCLI cli = new PythonCodeGeneratorCLI();
        int exitCode = cli.run(new String[] {"-h"});
        assertEquals(0, exitCode, "Should return 0 when help is requested");
    }

    @Test
    void testInvalidSourceFileReturnsError() {
        PythonCodeGeneratorCLI cli = new PythonCodeGeneratorCLI();
        int exitCode = cli.run(new String[] {"-f", "non_existent_file.rosetta"});
        assertEquals(1, exitCode, "Should return 1 when source file does not exist");
    }

    @Test
    void testMissingProjectNameReturnsError() throws IOException {
        Path validFile = createValidRosettaFile(tempDir);
        PythonCodeGeneratorCLI cli = new PythonCodeGeneratorCLI();
        int exitCode = cli.run(new String[] {
                "-f", validFile.toString(),
                "-t", tempDir.resolve("python").toString()
        });
        assertEquals(1, exitCode, "Should return 1 when -p is not provided");
    }

    @Test
    void testValidationErrorsFailByDefault() throws IOException {
        Path validFile = createValidRosettaFile(tempDir); // Use valid file, let MockValidator inject error
        TestCLI cli = new TestCLI();
        cli.getMockValidator().setReturnError(true);

        int exitCode = cli.run(new String[] {
                "-f", validFile.toString(),
                "-t", tempDir.resolve("python").toString(),
                "-p", "test-project"
        });

        assertEquals(1, exitCode, "Should return 1 (fail) when validation errors occur by default");
    }

    @Test
    void testAllowErrorsPasses() throws IOException {
        Path validFile = createValidRosettaFile(tempDir);
        TestCLI cli = new TestCLI();
        cli.getMockValidator().setReturnError(true);

        int exitCode = cli.run(new String[] {
                "-f", validFile.toString(),
                "-t", tempDir.resolve("python").toString(),
                "-p", "test-project",
                "-e"
        });

        assertEquals(0, exitCode, "Should return 0 (success) when validation errors occur but --allow-errors is set");
    }

    @Test
    void testWarningsFailWithFlag() throws IOException {
        Path validFile = createValidRosettaFile(tempDir);
        TestCLI cli = new TestCLI();
        cli.getMockValidator().setReturnWarning(true);

        int exitCode = cli.run(new String[] {
                "-f", validFile.toString(),
                "-t", tempDir.resolve("python").toString(),
                "-p", "test-project",
                "-w" // --fail-on-warnings
        });

        assertEquals(1, exitCode, "Should return 1 (fail) when warnings occur and --fail-on-warnings is set");
    }

    // --- Version option tests ---

    @Test
    void testInvalidVersionFormatTooFewSegmentsReturnsError() {
        PythonCodeGeneratorCLI cli = new PythonCodeGeneratorCLI();
        int exitCode = cli.run(new String[] {"-v", "1.0"});
        assertEquals(1, exitCode, "Should return 1 for version with fewer than 3 segments");
    }

    @Test
    void testInvalidVersionFormatNonNumericReturnsError() {
        PythonCodeGeneratorCLI cli = new PythonCodeGeneratorCLI();
        int exitCode = cli.run(new String[] {"-v", "1.a.0"});
        assertEquals(1, exitCode, "Should return 1 for version with non-numeric segment");
    }

    @Test
    void testInvalidVersionFormatWithSnapshotSuffixReturnsError() {
        PythonCodeGeneratorCLI cli = new PythonCodeGeneratorCLI();
        int exitCode = cli.run(new String[] {"-v", "1.0.0-SNAPSHOT"});
        assertEquals(1, exitCode, "Should return 1 for version with SNAPSHOT suffix");
    }

    @Test
    void testDevVersionFormatIsAcceptedAndConverted() throws IOException {
        Path validFile = createValidRosettaFile(tempDir);
        Path pythonDir = tempDir.resolve("python");
        TestCLI cli = new TestCLI();

        int exitCode = cli.run(new String[] {
                "-f", validFile.toString(),
                "-t", pythonDir.toString(),
                "-p", "test-project",
                "-v", "1.2.3-dev.4"
        });

        assertEquals(0, exitCode, "Should return 0 when version is in #.#.#-dev.# format");
        String toml = Files.readString(pythonDir.resolve("pyproject.toml"));
        assertTrue(toml.contains("version = \"1.2.3.dev4\""),
                "pyproject.toml should contain the PEP 440 dev version, but was:\n" + toml);
    }

    @Test
    void testDevVersionWithNonNumericDevCounterReturnsError() {
        PythonCodeGeneratorCLI cli = new PythonCodeGeneratorCLI();
        int exitCode = cli.run(new String[] {"-v", "1.2.3-dev.abc"});
        assertEquals(1, exitCode, "Should return 1 when dev counter is not numeric");
    }

    @Test
    void testValidVersionAppearsInToml() throws IOException {
        Path validFile = createValidRosettaFile(tempDir);
        Path pythonDir = tempDir.resolve("python");
        TestCLI cli = new TestCLI();

        int exitCode = cli.run(new String[] {
                "-f", validFile.toString(),
                "-t", pythonDir.toString(),
                "-p", "test-project",
                "-v", "1.2.3"
        });

        assertEquals(0, exitCode, "Should return 0 when valid version is specified");
        Path tomlPath = pythonDir.resolve("pyproject.toml");
        assertTrue(Files.exists(tomlPath), "pyproject.toml should have been generated");
        String toml = Files.readString(tomlPath);
        assertTrue(toml.contains("version = \"1.2.3\""),
                "pyproject.toml should contain the specified version, but was:\n" + toml);
    }

    /**
     * When a namespace prefix is specified via the future {@code -x} option, the
     * generated files should reflect the prefix in their namespace paths.
     *
     * <p>Disabled until the namespace-prefix feature is implemented in the CLI and
     * generator.
     */
    @Test
    void testNamespacePrefixAppearsInGeneratedNamespace() throws IOException {
        Path validFile = createValidRosettaFile(tempDir);
        Path pythonDir = tempDir.resolve("python");
        TestCLI cli = new TestCLI();

        int exitCode = cli.run(new String[] {
                "-f", validFile.toString(),
                "-t", pythonDir.toString(),
                "-p", "test-project",
                "-x", "finos"
        });

        assertEquals(0, exitCode, "Should return 0 when namespace prefix is specified");
        assertTrue(Files.walk(pythonDir)
                .filter(Files::isRegularFile)
                .anyMatch(p -> p.toString().contains("finos")),
                "Expected at least one generated file path to contain 'finos'");
    }

    @Test
    void testNativeFunctionsAreCopiedUnderNamespacePrefix() throws IOException {
        Path validFile = createValidRosettaFile(tempDir);
        Path pythonDir = tempDir.resolve("python");
        Path nativeDir = createNativeDir(tempDir);
        TestCLI cli = new TestCLI();

        int exitCode = cli.run(new String[] {
                "-f", validFile.toString(),
                "-t", pythonDir.toString(),
                "-p", "test-project",
                "-x", "finos",
                "-n", nativeDir.toString()
        });

        assertEquals(0, exitCode, "Should return 0 when native functions are copied with a namespace prefix");
        assertTrue(Files.exists(pythonDir.resolve("src/finos/rune/native/test/model/functions/NativeFunc.py")),
                "Native functions should be copied under src/finos/rune/native when a namespace prefix is set");
        assertTrue(Files.exists(pythonDir.resolve("src/finos/rune/__init__.py")),
                "The prefixed rune package should receive an __init__.py when a namespace prefix is set");
        assertTrue(Files.readString(pythonDir.resolve("src/finos/rune/__init__.py")).contains("# native root"),
                "The source rune/__init__.py should be copied into the prefixed rune package");
        assertFalse(Files.exists(pythonDir.resolve("src/rune/__init__.py")),
                "The top-level src/rune/__init__.py must not be created when the rune package is copied under a namespace prefix");
        assertFalse(Files.exists(pythonDir.resolve("src/rune/native/test/model/functions/NativeFunc.py")),
                "Native functions must not be copied into the top-level src/rune/native path when a namespace prefix is set");
    }

    @Test
    void testNativeFunctionsDoNotCreateTopLevelRuneInitWithoutNamespacePrefix() throws IOException {
        Path validFile = createValidRosettaFile(tempDir);
        Path pythonDir = tempDir.resolve("python");
        Path nativeDir = createNativeDir(tempDir);
        TestCLI cli = new TestCLI();

        int exitCode = cli.run(new String[] {
                "-f", validFile.toString(),
                "-t", pythonDir.toString(),
                "-p", "test-project",
                "-n", nativeDir.toString()
        });

        assertEquals(0, exitCode, "Should return 0 when native functions are copied without a namespace prefix");
        assertTrue(Files.exists(pythonDir.resolve("src/rune/native/test/model/functions/NativeFunc.py")),
                "Native functions should be copied under src/rune/native when no namespace prefix is set");
        assertFalse(Files.exists(pythonDir.resolve("src/rune/__init__.py")),
                "The top-level src/rune/__init__.py must not be created when rune is copied as a top-level package");
        assertTrue(Files.exists(pythonDir.resolve("src/rune/native/__init__.py")),
                "The native package subtree should still receive __init__.py files under src/rune/native");
    }

    private Path createValidRosettaFile(Path dir) throws IOException {
        Path file = dir.resolve("valid.rosetta");
        String content = "namespace test.model\nversion \"1.0.0\"\ntype Foo:\n    attr string (1..1)\n";
        Files.writeString(file, content);
        return file;
    }

    private Path createNativeDir(Path dir) throws IOException {
        Path nativeDir = dir.resolve("native");
        Path nativeFile = nativeDir.resolve("rune/native/test/model/functions/NativeFunc.py");
        Files.createDirectories(nativeFile.getParent());
        Files.writeString(nativeFile, "def NativeFunc():\n    return 1\n");

        Path runeInit = nativeDir.resolve("rune/__init__.py");
        Files.createDirectories(runeInit.getParent());
        Files.writeString(runeInit, "# native root\n");
        return nativeDir;
    }

    // --- Mocks ---

    static class TestCLI extends PythonCodeGeneratorCLI {
        /**
         * The mock validator.
         */
        private final MockValidator mockValidator = new MockValidator();

        /**
         * Gets the mock validator.
         *
         * @return the mock validator
         */
        public MockValidator getMockValidator() {
            return mockValidator;
        }

        @Override
        protected org.eclipse.xtext.validation.IResourceValidator getValidator(com.google.inject.Injector injector) {
            return mockValidator;
        }
    }

    static class MockValidator implements org.eclipse.xtext.validation.IResourceValidator {
        /**
         * Whether to return an error.
         */
        private boolean returnError = false;
        /**
         * Whether to return a warning.
         */
        private boolean returnWarning = false;

        public void setReturnError(boolean returnError) {
            this.returnError = returnError;
        }

        public void setReturnWarning(boolean returnWarning) {
            this.returnWarning = returnWarning;
        }

        @Override
        public java.util.List<org.eclipse.xtext.validation.Issue> validate(
                org.eclipse.emf.ecore.resource.Resource resource,
                org.eclipse.xtext.validation.CheckMode mode, org.eclipse.xtext.util.CancelIndicator indicator) {
            java.util.List<org.eclipse.xtext.validation.Issue> issues = new java.util.ArrayList<>();

            org.eclipse.emf.common.util.URI uri = null;
            if (!resource.getContents().isEmpty()) {
                org.eclipse.emf.ecore.EObject root = resource.getContents().get(0);
                uri = EcoreUtil.getURI(root);
            } else {
                uri = org.eclipse.emf.common.util.URI.createURI("dummy#//");
            }

            if (returnError) {
                issues.add(createIssue(org.eclipse.xtext.diagnostics.Severity.ERROR, "Mock Error", uri));
            }
            if (returnWarning) {
                issues.add(createIssue(org.eclipse.xtext.diagnostics.Severity.WARNING, "Mock Warning", uri));
            }
            return issues;
        }

        private org.eclipse.xtext.validation.Issue createIssue(org.eclipse.xtext.diagnostics.Severity severity,
                String message, org.eclipse.emf.common.util.URI uri) {
            org.eclipse.xtext.validation.Issue.IssueImpl issue = new org.eclipse.xtext.validation.Issue.IssueImpl();
            issue.setSeverity(severity);
            issue.setMessage(message);
            issue.setLineNumber(1);
            issue.setUriToProblem(uri);
            return issue;
        }
    }
}
