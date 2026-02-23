package com.regnosys.rosetta.generator.python;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.emf.ecore.util.EcoreUtil;

import static org.junit.jupiter.api.Assertions.*;

class PythonCodeGeneratorCLITest {

    @TempDir
    Path tempDir;

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
        int exitCode = cli.execute(new String[] {});
        assertEquals(1, exitCode, "Should return 1 when no args provided");
    }

    @Test
    void testHelpReturnsSuccess() {
        PythonCodeGeneratorCLI cli = new PythonCodeGeneratorCLI();
        int exitCode = cli.execute(new String[] { "-h" });
        assertEquals(0, exitCode, "Should return 0 when help is requested");
    }

    @Test
    void testInvalidSourceFileReturnsError() {
        PythonCodeGeneratorCLI cli = new PythonCodeGeneratorCLI();
        int exitCode = cli.execute(new String[] { "-f", "non_existent_file.rosetta" });
        assertEquals(1, exitCode, "Should return 1 when source file does not exist");
    }

    @Test
    void testValidationErrorsFailByDefault() throws IOException {
        Path validFile = createValidRosettaFile(tempDir); // Use valid file, let MockValidator inject error
        TestCLI cli = new TestCLI();
        cli.mockValidator.setReturnError(true);

        int exitCode = cli.execute(new String[] {
                "-f", validFile.toString(),
                "-t", tempDir.resolve("python").toString()
        });

        assertEquals(1, exitCode, "Should return 1 (fail) when validation errors occur by default");
    }

    @Test
    void testAllowErrorsPasses() throws IOException {
        Path validFile = createValidRosettaFile(tempDir);
        TestCLI cli = new TestCLI();
        cli.mockValidator.setReturnError(true);

        int exitCode = cli.execute(new String[] {
                "-f", validFile.toString(),
                "-t", tempDir.resolve("python").toString(),
                "-e"
        });

        assertEquals(0, exitCode, "Should return 0 (success) when validation errors occur but --allow-errors is set");
    }

    @Test
    void testWarningsFailWithFlag() throws IOException {
        Path validFile = createValidRosettaFile(tempDir);
        TestCLI cli = new TestCLI();
        cli.mockValidator.setReturnWarning(true);

        int exitCode = cli.execute(new String[] {
                "-f", validFile.toString(),
                "-t", tempDir.resolve("python").toString(),
                "-w" // --fail-on-warnings
        });

        assertEquals(1, exitCode, "Should return 1 (fail) when warnings occur and --fail-on-warnings is set");
    }

    private Path createValidRosettaFile(Path dir) throws IOException {
        Path file = dir.resolve("valid.rosetta");
        String content = "namespace test.model\nversion \"1.0.0\"\ntype Foo:\n    attr string (1..1)\n";
        Files.writeString(file, content);
        return file;
    }

    // --- Mocks ---

    static class TestCLI extends PythonCodeGeneratorCLI {
        MockValidator mockValidator = new MockValidator();

        @Override
        protected org.eclipse.xtext.validation.IResourceValidator getValidator(com.google.inject.Injector injector) {
            return mockValidator;
        }
    }

    static class MockValidator implements org.eclipse.xtext.validation.IResourceValidator {
        private boolean returnError = false;
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
