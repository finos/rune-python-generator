package com.regnosys.rosetta.generator.python.object;

import java.util.Map;

import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.regnosys.rosetta.generator.python.PythonGeneratorTestUtils;
import com.regnosys.rosetta.tests.RosettaInjectorProvider;

import jakarta.inject.Inject;

/**
 * Verifies that the _bundle.py header contains exactly the imports it needs —
 * no more, no less.
 *
 * Specifically:
 *   - Standalone class cross-namespace deps must not leak into the bundle header.
 *   - Runtime imports that are always present via createImports() must appear
 *     exactly once, even when generator logic could have added them a second time.
 *   - A bundle whose bundled classes have no external deps must have no
 *     user-defined type import lines.
 */
@ExtendWith(InjectionExtension.class)
@InjectWith(RosettaInjectorProvider.class)
@SuppressWarnings("LineLength")
public class PythonBundleImportsTest {

    @Inject
    private PythonGeneratorTestUtils testUtils;

    // -----------------------------------------------------------------------
    // Test 7 — Standalone class dependency does not appear in bundle header
    // -----------------------------------------------------------------------
    @Test
    public void testStandaloneClassDependencyAbsentFromBundleHeader() {
        // Foo (standalone) → Bar (standalone): this cross-type dep must not
        // leak into the bundle header.  CycleA ↔ CycleB forces a bundle to exist.
        Map<String, CharSequence> gf = testUtils.generatePythonFromString("""
                type Foo:
                    bar Bar (1..1)

                type Bar:
                    value int (1..1)

                type CycleA:
                    b CycleB (1..1)

                type CycleB:
                    a CycleA (1..1)
                """);

        String bundlePython = gf.get("src/com/_bundle.py").toString();

        // Bundled classes must still be present
        testUtils.assertGeneratedContainsExpectedString(bundlePython, "class com_rosetta_test_model_CycleA(BaseDataClass):");
        testUtils.assertGeneratedContainsExpectedString(bundlePython, "class com_rosetta_test_model_CycleB(BaseDataClass):");

        // The import that Foo needs for Bar must NOT appear in the bundle header
        testUtils.assertGeneratedDoesNotContain(bundlePython, "from com.rosetta.test.model.Bar import Bar");
    }

    // -----------------------------------------------------------------------
    // Test 8 — native_registry import appears exactly once in the bundle
    // -----------------------------------------------------------------------
    @Test
    public void testNativeRegistryImportAppearsExactlyOnce() {
        Map<String, CharSequence> gf = testUtils.generatePythonFromString("""
                namespace rosetta_dsl.test.functions

                enum RoundingModeEnum:
                    Down
                    Up

                func RoundToNearest:
                    [codeImplementation]
                    inputs:
                        value number (1..1)
                        nearest number (1..1)
                        roundingMode RoundingModeEnum (1..1)
                    output:
                        roundedValue number (1..1)
                """);

        String bundlePython = gf.get("src/rosetta_dsl/_bundle.py").toString();

        // The combined import from createImports() is the only occurrence allowed
        testUtils.assertImportAppearsExactlyOnce(bundlePython,
                "from rune.runtime.native_registry import");
    }

    // -----------------------------------------------------------------------
    // Test 9 — ObjectBuilder import appears exactly once in the bundle
    // -----------------------------------------------------------------------
    @Test
    public void testObjectBuilderImportAppearsExactlyOnce() {
        // A circular pair guarantees a bundle is generated; the bundle always
        // receives createImports() which includes the ObjectBuilder import.
        // Before the fix, addAdditionalImport() would have added it a second time.
        Map<String, CharSequence> gf = testUtils.generatePythonFromString("""
                type CycleA:
                    b CycleB (1..1)

                type CycleB:
                    a CycleA (1..1)
                """);

        String bundlePython = gf.get("src/com/_bundle.py").toString();

        testUtils.assertImportAppearsExactlyOnce(bundlePython,
                "from rune.runtime.object_builder import ObjectBuilder");
    }

    // -----------------------------------------------------------------------
    // Test 10 — Bundle with only circular types has no user-defined type imports
    // -----------------------------------------------------------------------
    @Test
    public void testBundleWithOnlyCircularTypesHasNoUserTypeImports() {
        // CycleA and CycleB reference only each other — no external deps.
        // The bundle header should therefore contain no "from com.rosetta..." lines.
        Map<String, CharSequence> gf = testUtils.generatePythonFromString("""
                type CycleA:
                    value int (1..1)
                    b CycleB (1..1)

                type CycleB:
                    value int (1..1)
                    a CycleA (1..1)
                """);

        String bundlePython = gf.get("src/com/_bundle.py").toString();

        testUtils.assertGeneratedDoesNotContain(bundlePython, "from com.rosetta.test.model.");
    }

    // -----------------------------------------------------------------------
    // Test 11 — no bundle is generated for a standalone-only model
    // -----------------------------------------------------------------------
    @Test
    public void testNoBundleGeneratedForStandaloneOnlyModel() {
        // A model with only acyclic, standalone types requires no bundle at all.
        Map<String, CharSequence> gf = testUtils.generatePythonFromString("""
                type SimpleType:
                    value int (1..1)
                """);

        org.junit.jupiter.api.Assertions.assertFalse(
                gf.containsKey("src/com/_bundle.py"),
                "No _bundle.py should be generated when there are no bundled classes or native functions");
    }
}
