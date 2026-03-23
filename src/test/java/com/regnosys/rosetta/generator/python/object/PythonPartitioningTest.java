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
 *
 * Also verifies that the _bundle.py header contains exactly the imports it needs —
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
public class PythonPartitioningTest {

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
        testUtils.assertGeneratedContainsExpectedString(bundlePython, "com_rosetta_test_model_CycleA.model_rebuild(force=True)");
        testUtils.assertGeneratedContainsExpectedString(bundlePython, "com_rosetta_test_model_CycleB.model_rebuild(force=True)");

        // The standalone type must not be defined inside the bundle
        testUtils.assertGeneratedDoesNotContain(bundlePython, "class SimpleType(BaseDataClass):");
        testUtils.assertGeneratedDoesNotContain(bundlePython, "class com_rosetta_test_model_SimpleType");
    }

    /**
     * A self-referential type (attribute whose type is the same class, via
     * [metadata reference]) must NOT generate a self-import in its standalone file.
     * Emitting {@code from <module> import <Class>} inside the file that defines
     * {@code <Class>} causes a Python circular-import error on partially-initialized
     * modules.
     */
    @Test
    public void testSelfReferentialTypeHasNoSelfImport() {
        Map<String, CharSequence> gf = testUtils.generatePythonFromString(
                """
                namespace com.rosetta.test.model

                type Node:
                    [metadata key]
                    value string (1..1)
                    parent Node (0..1)
                        [metadata reference]
                """);

        String nodePython = gf.get("src/com/rosetta/test/model/Node.py").toString();

        // The self-referential attribute is legal — class must still be generated
        testUtils.assertGeneratedContainsExpectedString(nodePython, "class Node(BaseDataClass):");

        // No self-import must appear in the file that defines Node
        testUtils.assertGeneratedDoesNotContain(nodePython,
                "from com.rosetta.test.model.Node import Node");
    }

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
    // Test 12 — standalone attribute-type imports are deferred;
    //           standalone base-class imports stay in the header
    // -----------------------------------------------------------------------
    /**
     * When a standalone type S depends on a bundled type B1 (from one SCC), and a
     * bundled type B2 (from a different SCC) depends on S *as an attribute type*, the
     * bundle must import S AFTER defining all bundled classes — not in the header.
     *
     * However, if a bundled type extends a standalone type (uses it as a direct base
     * class), that standalone import MUST remain in the header: Python evaluates
     * base-class expressions immediately at class-definition time, unlike attribute
     * annotations which are lazy strings under PEP 563.
     *
     * Model structure (attribute-type deferral):
     *   CycleA1 ↔ CycleA2   (first SCC — bundled)
     *   CycleB1 ↔ CycleB2   (second SCC — bundled; CycleB1 has attribute of type Standalone)
     *   Standalone           (singleton SCC — standalone; depends on CycleA1)
     *
     * Model structure (base-class in header):
     *   CycleX ↔ CycleY     (bundled)
     *   StandaloneBase       (standalone — used as base class of CycleX)
     */
    @Test
    public void testDeferredStandaloneImportAppearsAfterBundledClassDefinitions() {
        Map<String, CharSequence> gf = testUtils.generatePythonFromString("""
                namespace com.rosetta.test.model

                type CycleA1:
                    a2 CycleA2 (0..1)

                type CycleA2:
                    a1 CycleA1 (0..1)

                type CycleB1:
                    b2 CycleB2 (0..1)
                    s Standalone (0..1)

                type CycleB2:
                    b1 CycleB1 (0..1)

                type Standalone:
                    a1 CycleA1 (0..1)
                """);

        String bundlePython = gf.get("src/com/_bundle.py").toString();

        // Standalone must be generated as its own file (not bundled)
        String standalonePython = gf.get("src/com/rosetta/test/model/Standalone.py").toString();
        testUtils.assertGeneratedContainsExpectedString(standalonePython, "class Standalone(BaseDataClass):");

        // Bundle must contain all four bundled classes
        testUtils.assertGeneratedContainsExpectedString(bundlePython, "class com_rosetta_test_model_CycleA1(BaseDataClass):");
        testUtils.assertGeneratedContainsExpectedString(bundlePython, "class com_rosetta_test_model_CycleB1(BaseDataClass):");

        // The deferred section marker must be present
        testUtils.assertGeneratedContainsExpectedString(bundlePython,
                "# Standalone type imports (deferred to avoid circular import at bundle load time)");

        // The Standalone import must appear AFTER the bundled class definitions
        testUtils.assertAppearsAfter(bundlePython,
                "class com_rosetta_test_model_CycleA1(BaseDataClass):",
                "from com.rosetta.test.model.Standalone import Standalone");
        testUtils.assertAppearsAfter(bundlePython,
                "class com_rosetta_test_model_CycleB1(BaseDataClass):",
                "from com.rosetta.test.model.Standalone import Standalone");

        // The Standalone import must NOT appear in the header (before first class def)
        int firstClassDef = bundlePython.indexOf("class com_rosetta_test_model_");
        int standaloneImport = bundlePython.indexOf("from com.rosetta.test.model.Standalone import Standalone");
        org.junit.jupiter.api.Assertions.assertTrue(standaloneImport > firstClassDef,
                "Standalone import must appear after bundled class definitions, not in the header");
    }

    /**
     * A standalone type used as a direct base class of a bundled type must be imported
     * inline in the bundle body, immediately before the bundled class that extends it.
     * This ensures all bundled types the standalone depends on are already defined when
     * its file is loaded (avoiding the circular-import error that would arise if the
     * import were in the header).
     *
     * Model: CycleX ↔ CycleY (bundled); CycleX extends StandaloneBase (standalone).
     */
    @Test
    public void testStandaloneBaseClassImportIsInlineBeforeSubclass() {
        Map<String, CharSequence> gf = testUtils.generatePythonFromString("""
                namespace com.rosetta.test.model

                type StandaloneBase:
                    value int (1..1)

                type CycleX extends StandaloneBase:
                    y CycleY (0..1)

                type CycleY:
                    x CycleX (0..1)
                """);

        String bundlePython = gf.get("src/com/_bundle.py").toString();

        // CycleX's class statement must reference StandaloneBase by short name
        testUtils.assertGeneratedContainsExpectedString(bundlePython,
                "class com_rosetta_test_model_CycleX(StandaloneBase):");

        // The import must be present in the bundle
        testUtils.assertGeneratedContainsExpectedString(bundlePython,
                "from com.rosetta.test.model.StandaloneBase import StandaloneBase");

        // The import must appear just before the class that uses it as a base
        testUtils.assertAppearsAfter(bundlePython,
                "from com.rosetta.test.model.StandaloneBase import StandaloneBase",
                "class com_rosetta_test_model_CycleX(StandaloneBase):");

        // The import must NOT be in the header (i.e. it must appear after some class def,
        // or at least after the deferred-imports comment — here we simply verify ordering)
        // StandaloneBase import must come AFTER header (no class defs precede it in header)
        // and BEFORE CycleX class def.
        int standaloneImport = bundlePython.indexOf("from com.rosetta.test.model.StandaloneBase import StandaloneBase");
        int cycleXDef = bundlePython.indexOf("class com_rosetta_test_model_CycleX(StandaloneBase):");
        org.junit.jupiter.api.Assertions.assertTrue(standaloneImport < cycleXDef,
                "StandaloneBase import must appear before the class that extends it");
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
