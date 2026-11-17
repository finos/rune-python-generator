/*
 * Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
 * SPDX-License-Identifier: Apache-2.0
 */
package com.regnosys.rosetta.generator.python.object;

import java.util.List;
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

    /** Injected test utilities. */
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
        testUtils.assertGeneratedContainsExpectedString(bundlePython, "_FQRTN: ClassVar[str] = 'com.rosetta.test.model.CycleA'");
        testUtils.assertGeneratedContainsExpectedString(bundlePython, "class com_rosetta_test_model_CycleB(BaseDataClass):");
        testUtils.assertGeneratedContainsExpectedString(bundlePython, "_FQRTN: ClassVar[str] = 'com.rosetta.test.model.CycleB'");
        testUtils.assertGeneratedContainsExpectedString(bundlePython, "# Phase 2: Delayed Annotation Updates");
        // Bundled classes use defer_build=True (cheaper schema build) + Phase 3 model_rebuild (correctness)
        testUtils.assertGeneratedContainsExpectedString(bundlePython, "model_rebuild(force=True)");
        testUtils.assertGeneratedContainsExpectedString(bundlePython, "model_config = ConfigDict(defer_build=True)");

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
    // Test 8 — native function: no bundle generated, no registration in __init__.py
    // -----------------------------------------------------------------------
    @Test
    public void testNativeFunctionNoBundleAndNoRegistrationInInit() {
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

        // No bundled classes or functions — bundle should not be generated.
        org.junit.jupiter.api.Assertions.assertFalse(
                gf.containsKey("src/rosetta_dsl/_bundle.py"),
                "No _bundle.py should be generated when there are no bundled classes or functions");

        // Registration must not appear in __init__.py.
        String initPython = gf.get("src/rosetta_dsl/__init__.py").toString();
        testUtils.assertGeneratedDoesNotContain(initPython, "rune_register_native");
        testUtils.assertGeneratedDoesNotContain(initPython, "rune/native");
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
    // Test 12 — standalone attribute types stay standalone with a deferred import,
    //           unless they are on a path between bundled types
    // -----------------------------------------------------------------------
    /**
     * A standalone own-type used only as an attribute type (not as a direct base class)
     * of a bundled type, and which does not depend on a bundled type, is NOT promoted.
     * Attribute annotations are lazy strings under PEP 563 and do not need the type at
     * class-definition time. The import is deferred to a consolidated section after all
     * bundled class definitions.
     *
     * Model structure:
     *   CycleA1 ↔ CycleA2   (first SCC — bundled)
     *   CycleB1 ↔ CycleB2   (second SCC — bundled; CycleB1 has attribute of type Standalone)
     *   Standalone           (singleton SCC — stays standalone; import is deferred)
     */
    @Test
    public void testStandaloneAttributeTypeRemainsStandaloneWithDeferredImport() {
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
                    value int (0..1)
                """);

        String bundlePython = gf.get("src/com/_bundle.py").toString();

        // Standalone is NOT promoted — it stays as a standalone class file
        String standalonePython = gf.get("src/com/rosetta/test/model/Standalone.py").toString();
        testUtils.assertGeneratedContainsExpectedString(standalonePython, "class Standalone(BaseDataClass):");
        testUtils.assertGeneratedDoesNotContain(standalonePython, "__getattr__");

        // Bundle must contain the four bundled types but NOT Standalone
        testUtils.assertGeneratedContainsExpectedString(bundlePython, "class com_rosetta_test_model_CycleA1(BaseDataClass):");
        testUtils.assertGeneratedContainsExpectedString(bundlePython, "class com_rosetta_test_model_CycleB1(BaseDataClass):");
        testUtils.assertGeneratedDoesNotContain(bundlePython, "class com_rosetta_test_model_Standalone");

        // The Standalone import is deferred — it appears after all bundled class definitions
        testUtils.assertAppearsAfter(bundlePython,
                "class com_rosetta_test_model_CycleB1(BaseDataClass):",
                "from com.rosetta.test.model.Standalone import Standalone");
    }

    /**
     * Types on a path between bundled types are promoted, however long the path. The two
     * cycle groups are separate in the dependency graph, so the path is not a cycle, but
     * they share one _bundle.py, so leaving Outer or Inner standalone would make the files
     * circular.
     *
     * Model structure:
     *   CycleB1 ↔ CycleB2   (first SCC — bundled; CycleB1 has attribute of type Outer)
     *   Outer → Inner → CycleA1 (promoted — each depends on a bundled type)
     *   CycleA1 ↔ CycleA2   (second SCC — bundled)
     *   Unrelated            (used by CycleB1, depends on nothing — stays standalone)
     */
    @Test
    public void testTypesOnPathBetweenBundledTypesArePromoted() {
        Map<String, CharSequence> gf = testUtils.generatePythonFromString("""
                namespace com.rosetta.test.model

                type CycleA1:
                    a2 CycleA2 (0..1)

                type CycleA2:
                    a1 CycleA1 (0..1)

                type CycleB1:
                    b2 CycleB2 (0..1)
                    outer Outer (0..1)
                    unrelated Unrelated (0..1)

                type CycleB2:
                    b1 CycleB1 (0..1)

                type Outer:
                    inner Inner (0..1)

                type Inner:
                    a1 CycleA1 (0..1)

                type Unrelated:
                    value int (0..1)
                """);

        String bundlePython = gf.get("src/com/_bundle.py").toString();

        for (String promoted : List.of("Outer", "Inner")) {
            testUtils.assertGeneratedContainsExpectedString(bundlePython,
                    "class com_rosetta_test_model_" + promoted + "(BaseDataClass):");
            testUtils.assertGeneratedContainsExpectedString(bundlePython,
                    "com_rosetta_test_model_" + promoted + ".model_rebuild(force=True)");
            testUtils.assertGeneratedDoesNotContain(bundlePython,
                    "from com.rosetta.test.model." + promoted + " import " + promoted);

            String stubPython = gf.get("src/com/rosetta/test/model/" + promoted + ".py").toString();
            testUtils.assertGeneratedContainsExpectedString(stubPython, "__getattr__");
            testUtils.assertGeneratedDoesNotContain(stubPython, "class " + promoted + "(BaseDataClass):");
        }

        // A type the bundle uses, but which does not lead back to it, stays standalone
        String unrelatedPython = gf.get("src/com/rosetta/test/model/Unrelated.py").toString();
        testUtils.assertGeneratedContainsExpectedString(unrelatedPython, "class Unrelated(BaseDataClass):");
        testUtils.assertGeneratedContainsExpectedString(bundlePython,
                "from com.rosetta.test.model.Unrelated import Unrelated");
    }

    /**
     * A standalone own-type used as a direct base class of a bundled type is
     * reverse-promoted into the bundle. The bundled subclass references the base
     * using the flattened bundle name, and no inline import appears in the bundle body.
     *
     * Model: CycleX ↔ CycleY (bundled); CycleX extends StandaloneBase (reverse-promoted).
     */
    @Test
    public void testStandaloneBaseClassIsReversePromotedToBundled() {
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

        // StandaloneBase is reverse-promoted: its file is a proxy stub
        String baseProxy = gf.get("src/com/rosetta/test/model/StandaloneBase.py").toString();
        testUtils.assertGeneratedContainsExpectedString(baseProxy, "__getattr__");
        testUtils.assertGeneratedDoesNotContain(baseProxy, "class StandaloneBase(BaseDataClass):");

        // Bundle must contain StandaloneBase with its flattened name
        testUtils.assertGeneratedContainsExpectedString(bundlePython,
                "class com_rosetta_test_model_StandaloneBase(BaseDataClass):");

        // CycleX uses the flattened bundle name as its base class
        testUtils.assertGeneratedContainsExpectedString(bundlePython,
                "class com_rosetta_test_model_CycleX(com_rosetta_test_model_StandaloneBase):");

        // No inline import for StandaloneBase anywhere in the bundle
        testUtils.assertGeneratedDoesNotContain(bundlePython,
                "from com.rosetta.test.model.StandaloneBase import StandaloneBase");
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
                "No _bundle.py should be generated when there are no bundled classes or functions");
    }
}
