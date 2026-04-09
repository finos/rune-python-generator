package com.regnosys.rosetta.generator.python;

import jakarta.inject.Inject;
import com.regnosys.rosetta.tests.RosettaInjectorProvider;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(InjectionExtension.class)
@InjectWith(RosettaInjectorProvider.class)
public class PythonTomlGeneratorTest {

    private static final String PYPROJECT_TOML = "pyproject.toml";

    @Inject
    private PythonGeneratorTestUtils testUtils;

    @Test
    public void testTomlFileIsGenerated() {
        Map<String, CharSequence> generated = testUtils.generatePythonFromString(
                """
                type Foo:
                    bar string (0..1)
                """);

        assertTrue(generated.containsKey(PYPROJECT_TOML),
                "Expected pyproject.toml to be present in generated output");
    }

    @Test
    public void testTomlContainsBuildSystem() {
        String toml = generateToml();

        assertTrue(toml.contains("[build-system]"), "Missing [build-system] section");
        assertTrue(toml.contains("requires = [\"setuptools>=62.0\"]"), "Missing setuptools requirement");
        assertTrue(toml.contains("build-backend = \"setuptools.build_meta\""), "Missing build-backend");
    }

    @Test
    public void testTomlContainsProjectSection() {
        String toml = generateToml();

        assertTrue(toml.contains("[project]"), "Missing [project] section");
        assertTrue(toml.contains("requires-python = \">= 3.11\""), "Missing requires-python");
    }

    @Test
    public void testTomlProjectNameDerivedFromNamespace() {
        // The default test namespace is com.rosetta.test.model, so the first segment is "com"
        String toml = generateToml();

        assertTrue(toml.contains("name = \"python-com\""),
                "Expected project name 'python-com' derived from namespace 'com.rosetta.test.model'");
    }

    @Test
    public void testTomlDefaultVersionIsZeroZeroZero() {
        // When no version is supplied the generator receives the model's raw version
        // string (${project.version} in tests), which cleanVersion() maps to 0.0.0.
        // This mirrors the CLI default: if -v is omitted, DEFAULT_VERSION ("0.0.0") is used.
        String toml = generateToml(null);

        assertTrue(toml.contains("version = \"0.0.0\""),
                "Expected default version '0.0.0' when no version is specified");
    }

    @Test
    public void testTomlVersionWhenSpecified() {
        String toml = generateToml("1.2.3");

        assertTrue(toml.contains("version = \"1.2.3\""),
                "Expected version '1.2.3' when explicitly specified");
    }

    @Test
    public void testTomlContainsDependencies() {
        String toml = generateToml();

        assertTrue(toml.contains("\"pydantic<3.0.0\""), "Missing pydantic dependency");
        assertTrue(toml.contains("\"rune.runtime>=1.0.0,<2.0.0\""), "Missing rune.runtime dependency");
    }

    @Test
    public void testTomlContainsPackagesFindSection() {
        String toml = generateToml();

        assertTrue(toml.contains("[tool.setuptools.packages.find]"), "Missing [tool.setuptools.packages.find] section");
        assertTrue(toml.contains("where = [\"src\"]"), "Missing where = [\"src\"] in packages.find");
    }

    private String generateToml() {
        return generateToml(null);
    }

    private String generateToml(String version) {
        Map<String, CharSequence> generated = testUtils.generatePythonFromString(
                """
                type Foo:
                    bar string (0..1)
                """, null, version);
        CharSequence toml = generated.get(PYPROJECT_TOML);
        assertNotNull(toml, "pyproject.toml was not generated");
        return toml.toString();
    }
}
