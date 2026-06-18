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
 * Tests for the {@code as} operator, per docs/AS_OPERATOR_IMPACT.md.
 *
 * <p>These tests assert on generated Python source only (no Python execution), so the
 * "match" vs. "no match" runtime distinction described in the impact analysis collapses
 * into a single generated-code shape per cardinality/mode combination here. The narrowing
 * behaviour itself (returning the value vs. {@code None}) is exercised by the Python
 * runtime's own test suite.
 */
@ExtendWith(InjectionExtension.class)
@InjectWith(RosettaInjectorProvider.class)
@SuppressWarnings("LineLength")
public class PythonAsOperationTest {

    /**
     * Test utils for generating Python.
     */
    @Inject
    private PythonGeneratorTestUtils testUtils;

    // -------------------------------------------------------------------------
    // Choice narrowing
    // -------------------------------------------------------------------------

    /**
     * Choice narrowing, single — `as` extracts the named option's value via rune_resolve_attr.
     */
    @Test
    public void testAsChoiceNarrowingSingle() {
        testUtils.assertBundleContainsExpectedString(
            """
            type Bar:
                barAttr int (1..1)

            type Qux:
                quxAttr int (1..1)

            choice Foo:
                Bar
                Qux

            func TestAsChoiceSingle:
                inputs: foo Foo (1..1)
                output: result Bar (0..1)
                set result:
                    foo as Bar
            """,
            """
            result = rune_resolve_attr(rune_resolve_attr(self, "foo"), "Bar")
            """);
    }

    /**
     * Choice narrowing, multi — `as` filters a list to elements holding the named option.
     */
    @Test
    public void testAsChoiceNarrowingMulti() {
        testUtils.assertBundleContainsExpectedString(
            """
            type Bar:
                barAttr int (1..1)

            type Qux:
                quxAttr int (1..1)

            choice Foo:
                Bar
                Qux

            func TestAsChoiceMulti:
                inputs: foos Foo (0..*)
                output: result Bar (0..*)
                set result:
                    foos as Bar
            """,
            """
            result = [_v for _x in (rune_resolve_attr(self, "foos") or []) if (_v := rune_resolve_attr(_x, "Bar")) is not None]
            """);
    }

    // -------------------------------------------------------------------------
    // Data type narrowing
    // -------------------------------------------------------------------------

    /**
     * Data type narrowing, single — `as` performs an isinstance check and returns the narrowed
     * value or None.
     */
    @Test
    public void testAsDataTypeNarrowingSingle() {
        testUtils.assertBundleContainsExpectedString(
            """
            type Foo:

            type Bar extends Foo:
                barAttr int (1..1)

            type Qux extends Foo:
                quxAttr int (1..1)

            func TestAsDataSingle:
                inputs: foo Foo (1..1)
                output: result Bar (0..1)
                set result:
                    foo as Bar
            """,
            """
            result = (_x if isinstance(rune_unwrap(_x := (rune_resolve_attr(self, "foo"))), Bar) else None)
            """);
    }

    /**
     * Data type narrowing, multi — `as` filters a list to instances of the target type.
     */
    @Test
    public void testAsDataTypeNarrowingMulti() {
        testUtils.assertBundleContainsExpectedString(
            """
            type Foo:

            type Bar extends Foo:
                barAttr int (1..1)

            type Qux extends Foo:
                quxAttr int (1..1)

            func TestAsDataMulti:
                inputs: foos Foo (0..*)
                output: result Bar (0..*)
                set result:
                    foos as Bar
            """,
            """
            result = [_x for _x in (rune_resolve_attr(self, "foos") or []) if isinstance(rune_unwrap(_x), Bar)]
            """);
    }

    // -------------------------------------------------------------------------
    // Chaining
    // -------------------------------------------------------------------------

    /**
     * Chained `as` followed by `->` attribute access operates on the narrowed type.
     */
    @Test
    public void testAsChainedWithAttributeAccess() {
        testUtils.assertBundleContainsExpectedString(
            """
            type Foo:

            type Bar extends Foo:
                inner Qux (1..1)

            type Qux:
                attr int (1..1)

            func TestAsChained:
                inputs: foo Foo (1..1)
                output: result int (0..1)
                set result:
                    foo as Bar -> inner -> attr
            """,
            """
            result = rune_resolve_attr(rune_resolve_attr((_x if isinstance(rune_unwrap(_x := (rune_resolve_attr(self, "foo"))), Bar) else None), "inner"), "attr")
            """);
    }
}
