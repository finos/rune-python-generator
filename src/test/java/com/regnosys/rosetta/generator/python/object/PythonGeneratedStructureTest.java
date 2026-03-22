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
 * Verifies that standalone and bundled generated classes each contain exactly
 * the structural elements they should — and none that belong to the other form.
 *
 * Standalone classes  → short class name, inline Annotated field, no _FQRTN, no Phase 2/3.
 * Bundled classes     → flattened class name, _FQRTN, Phase 2/3 annotation blocks.
 */
@ExtendWith(InjectionExtension.class)
@InjectWith(RosettaInjectorProvider.class)
@SuppressWarnings("LineLength")
public class PythonGeneratedStructureTest {

    @Inject
    private PythonGeneratorTestUtils testUtils;

    // -----------------------------------------------------------------------
    // Test 2 — _FQRTN is absent from a standalone class file
    // -----------------------------------------------------------------------
    @Test
    public void testFqrtnAbsentFromStandaloneClass() {
        Map<String, CharSequence> gf = testUtils.generatePythonFromString("""
                type SimpleType:
                    value int (1..1)
                """);

        String standalonePython = gf.get("src/com/rosetta/test/model/SimpleType.py").toString();

        testUtils.assertGeneratedContainsExpectedString(standalonePython, "class SimpleType(BaseDataClass):");
        testUtils.assertGeneratedDoesNotContain(standalonePython, "_FQRTN");
    }

    // -----------------------------------------------------------------------
    // Test 4 — Phase 2 / Phase 3 blocks are absent from a standalone class file
    // -----------------------------------------------------------------------
    @Test
    public void testPhase2And3AbsentFromStandaloneClass() {
        // Container → Element is acyclic, so both are standalone
        Map<String, CharSequence> gf = testUtils.generatePythonFromString("""
                type Container:
                    element Element (1..1)

                type Element:
                    value int (1..1)
                """);

        String containerPython = gf.get("src/com/rosetta/test/model/Container.py").toString();

        testUtils.assertGeneratedContainsExpectedString(containerPython, "class Container(BaseDataClass):");
        testUtils.assertGeneratedDoesNotContain(containerPython, "# Phase 2: Delayed Annotation Updates");
        testUtils.assertGeneratedDoesNotContain(containerPython, "model_rebuild()");
    }

    // -----------------------------------------------------------------------
    // Test 5 — Flattened class name is absent from a standalone class file
    // -----------------------------------------------------------------------
    @Test
    public void testFlattenedClassNameAbsentFromStandaloneClass() {
        Map<String, CharSequence> gf = testUtils.generatePythonFromString("""
                type SimpleType:
                    value int (1..1)
                """);

        String standalonePython = gf.get("src/com/rosetta/test/model/SimpleType.py").toString();

        testUtils.assertGeneratedContainsExpectedString(standalonePython, "class SimpleType(BaseDataClass):");
        testUtils.assertGeneratedDoesNotContain(standalonePython, "com_rosetta_test_model_SimpleType");
    }

    // -----------------------------------------------------------------------
    // Test 6 — Mixed model: correct structural elements in each output
    // -----------------------------------------------------------------------
    @Test
    public void testMixedModelStandaloneAndBundledStructure() {
        Map<String, CharSequence> gf = testUtils.generatePythonFromString("""
                type SimpleType:
                    value int (1..1)

                type CycleA:
                    b CycleB (1..1)

                type CycleB:
                    a CycleA (1..1)
                """);

        // --- Standalone file assertions ---
        String standalonePython = gf.get("src/com/rosetta/test/model/SimpleType.py").toString();

        testUtils.assertGeneratedContainsExpectedString(standalonePython, "class SimpleType(BaseDataClass):");
        testUtils.assertGeneratedDoesNotContain(standalonePython, "_FQRTN");
        testUtils.assertGeneratedDoesNotContain(standalonePython, "# Phase 2: Delayed Annotation Updates");
        testUtils.assertGeneratedDoesNotContain(standalonePython, "model_rebuild()");
        testUtils.assertGeneratedDoesNotContain(standalonePython, "com_rosetta_test_model_SimpleType");

        // --- Bundle file assertions ---
        String bundlePython = gf.get("src/com/_bundle.py").toString();

        testUtils.assertGeneratedContainsExpectedString(bundlePython, "class com_rosetta_test_model_CycleA(BaseDataClass):");
        testUtils.assertGeneratedContainsExpectedString(bundlePython, "_FQRTN = 'com.rosetta.test.model.CycleA'");
        testUtils.assertGeneratedContainsExpectedString(bundlePython, "class com_rosetta_test_model_CycleB(BaseDataClass):");
        testUtils.assertGeneratedContainsExpectedString(bundlePython, "_FQRTN = 'com.rosetta.test.model.CycleB'");
        testUtils.assertGeneratedContainsExpectedString(bundlePython, "# Phase 2: Delayed Annotation Updates");
        testUtils.assertGeneratedContainsExpectedString(bundlePython, "com_rosetta_test_model_CycleA.model_rebuild()");
        testUtils.assertGeneratedContainsExpectedString(bundlePython, "com_rosetta_test_model_CycleB.model_rebuild()");

        // The standalone type must not be defined inside the bundle
        testUtils.assertGeneratedDoesNotContain(bundlePython, "class SimpleType(BaseDataClass):");
        testUtils.assertGeneratedDoesNotContain(bundlePython, "class com_rosetta_test_model_SimpleType");
    }
}
