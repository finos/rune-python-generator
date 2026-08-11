/*
 * Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
 * SPDX-License-Identifier: Apache-2.0
 */
package com.regnosys.rosetta.generator.python.object;

import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.regnosys.rosetta.generator.python.PythonGeneratorTestUtils;
import com.regnosys.rosetta.tests.RosettaInjectorProvider;

import jakarta.inject.Inject;

/**
 * Verifies that {@code model_rebuild(force=True)} calls in a generated {@code _bundle.py}
 * appear in correct dependency order: a type that is referenced as a deferred field by another
 * type must be rebuilt before the type that references it.
 *
 * <p>Background: Pydantic v2's {@code model_rebuild} resolves forward-reference annotations
 * registered in Phase 2 (the deferred annotation update block). When class A's rebuild is
 * invoked before class B's rebuild, and A holds a deferred field of type B, A's schema bakes
 * in B's pre-rebuild (still {@code None}-typed) field definitions. Subsequent deserialization
 * then rejects real values for those fields with "Input should be None".
 *
 * <p>Root cause: rebuild calls are emitted in the same order used for class definitions,
 * which {@link com.regnosys.rosetta.generator.python.PythonCodeGenerator} derives from the
 * type dependency DAG via {@code sortSccByInheritance}. That sort respects inheritance order
 * (parent before child) but is blind to field-reference rebuild requirements. When a parent
 * class holds a deferred field whose type is the child, the child must be rebuilt first — the
 * opposite of what inheritance ordering provides.
 *
 * <p>Fix (Option B): record rebuild dependencies explicitly in
 * {@code PythonCodeGeneratorContext} at the point deferred annotation updates are generated
 * in {@code PythonAttributeProcessor}, then topologically sort and emit rebuild calls by
 * that graph independently of class-definition order.
 */
@ExtendWith(InjectionExtension.class)
@InjectWith(RosettaInjectorProvider.class)
@SuppressWarnings("LineLength")
public class PythonBundleRebuildOrderTest {

    @Inject
    private PythonGeneratorTestUtils testUtils;

    /**
     * Inheritance ordering forces the wrong rebuild order.
     *
     * <p>Model:
     * <pre>
     *   type Parent { child Child (0..1) }   ← deferred field; Parent.child = None at def time
     *   type Child extends Parent {           ← inheritance cycle: Parent ↔ Child → both bundled
     *       extra Child (0..1) }             ← self-deferred field; Child.extra = None at def time
     * </pre>
     *
     * <p>Both types are in the same SCC (Parent→Child edge from {@code extends}; Child→Parent
     * edge from {@code child} field). {@code sortSccByInheritance} orders Parent before Child
     * because Child extends Parent — so class definitions AND rebuild calls are emitted in the
     * order [Parent, Child].
     *
     * <p>Correct rebuild order: Child BEFORE Parent. Parent.child references Child; when
     * Parent.model_rebuild() is called, Pydantic inspects Child's schema. If Child has not yet
     * been rebuilt, Child.extra is still {@code None}, and deserializing a Parent instance with a
     * nested Child that has a non-null {@code extra} yields "Input should be None".
     *
     * <p>With the current buggy generator this assertion FAILS: {@code Parent.model_rebuild} is
     * emitted first (inheritance order: parent before child). After the Option-B fix, rebuild
     * calls are sorted by the explicit rebuild-dependency graph and this assertion PASSES.
     */
    @Test
    public void testRebuildOrderInheritanceCycleWithDeferredField() {
        String model = """
                type Parent:
                    child Child (0..1)

                type Child extends Parent:
                    extra Child (0..1)
                """;

        String bundle = testUtils.generatePythonAndExtractBundle(model);

        // Both Parent and Child must have deferred fields that trigger model_rebuild calls.
        testUtils.assertBundleContainsExpectedString(model, "com_rosetta_test_model_Parent.model_rebuild(force=True)");
        testUtils.assertBundleContainsExpectedString(model, "com_rosetta_test_model_Child.model_rebuild(force=True)");

        // Child.model_rebuild must appear BEFORE Parent.model_rebuild.
        // The buggy code emits Parent first (inheritance sort), causing Pydantic to see
        // Child's unresolved None-typed "extra" field when rebuilding Parent's schema.
        testUtils.assertAppearsAfter(bundle,
                "com_rosetta_test_model_Child.model_rebuild(force=True)",
                "com_rosetta_test_model_Parent.model_rebuild(force=True)");
    }
}
