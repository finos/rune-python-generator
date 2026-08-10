/*
 * Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
 * SPDX-License-Identifier: Apache-2.0
 */
package com.regnosys.rosetta.generator.python.expressions;

import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.regnosys.rosetta.generator.python.PythonGeneratorTestUtils;
import com.regnosys.rosetta.tests.RosettaInjectorProvider;

import jakarta.inject.Inject;

/**
 * Tests for deep-path expressions ({@code item->>attr}), which resolve an attribute
 * across a polymorphic choice structure.
 *
 * <p>The deep-path operator is valid when the receiver type is a choice (one-of) whose
 * branches all provide the named attribute (directly or transitively).  Each test
 * therefore uses a type with two optional branches and a {@code one-of} condition.
 *
 * <p>Mirrors {@code DeepPathTest} in the rune-dsl Java generator tests.
 */
@ExtendWith(InjectionExtension.class)
@InjectWith(RosettaInjectorProvider.class)
@SuppressWarnings("LineLength")
public class PythonDeepPathTest {

    @Inject
    private PythonGeneratorTestUtils testUtils;

    /**
     * A container type with two choice branches each having a common attribute generates
     * {@code rune_resolve_deep_attr} for {@code item->>val}.
     */
    @Test
    public void testSimpleDeepPathInCondition() {
        testUtils.assertBundleContainsExpectedString("""
                type BranchA:
                    val int (1..1)

                type BranchB:
                    val int (1..1)

                type Container:
                    a BranchA (0..1)
                    b BranchB (0..1)
                    condition Choice: one-of
                    condition DeepCheck:
                        item->>val = 5
                """,
                "rune_resolve_deep_attr(self, \"val\")");
    }

    /**
     * Deep path followed by a feature call: {@code item->>leaf->val} where both
     * branches of the choice type expose the nested {@code leaf} attribute.
     */
    @Test
    public void testDeepPathFollowedByFeatureCall() {
        testUtils.assertBundleContainsExpectedString("""
                type Leaf:
                    val int (1..1)

                type BranchX:
                    leaf Leaf (1..1)

                type BranchY:
                    leaf Leaf (1..1)

                type Root:
                    x BranchX (0..1)
                    y BranchY (0..1)
                    condition Choice: one-of
                    condition DeepAttrCheck:
                        item->>leaf->val = 10
                """,
                "rune_resolve_deep_attr(self, \"leaf\")");
    }

    /**
     * Deep path used in a multi-level type hierarchy condition (the passing baseline
     * from the rune-dsl test suite).
     */
    @Test
    public void testDeepPathAcrossInheritanceHierarchy() {
        testUtils.assertBundleContainsExpectedString("""
                type Deep1:
                    attr int (1..1)

                type Bar1:
                    deep1 Deep1 (1..1)
                    b1 int (1..1)
                    a int (1..1)

                type Bar2:
                    deep1 Deep1 (1..1)
                    b1 int (1..1)
                    c int (1..1)

                type Bar4:
                    deep1 Deep1 (1..1)
                    b1 int (1..1)

                type Bar3:
                    bar2 Bar2 (0..1)
                    bar4 Bar4 (0..1)
                    condition Choice: one-of

                type Foo:
                    bar1 Bar1 (0..1)
                    bar3 Bar3 (0..1)
                    condition Choice: one-of
                    condition Test:
                        item->>deep1->attr = 3
                """,
                "rune_resolve_deep_attr(self, \"deep1\")");
    }

    /**
     * Deep path on a named (non-{@code item}) receiver: {@code inner->>val} where
     * {@code inner} is a choice type whose branches each have {@code val}.
     */
    @Test
    public void testDeepPathOnExplicitReceiver() {
        testUtils.assertBundleContainsExpectedString("""
                type LeftBranch:
                    val int (1..1)

                type RightBranch:
                    val int (1..1)

                type Inner:
                    left LeftBranch (0..1)
                    right RightBranch (0..1)
                    condition Choice: one-of

                type Wrapper:
                    inner Inner (1..1)

                    condition DeepCheck:
                        inner->>val = 7
                """,
                "rune_resolve_deep_attr");
    }
}
