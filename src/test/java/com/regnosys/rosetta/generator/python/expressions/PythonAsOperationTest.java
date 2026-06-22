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

    // -------------------------------------------------------------------------
    // Nested choice narrowing
    // -------------------------------------------------------------------------

    /**
     * Choice narrowing through two levels — `as Opt2` on `NestedFoo` must walk
     * through the intermediate `NestedBar` choice before reaching `Opt2`.
     * The generator's path-finder must emit a chained {@code rune_resolve_attr}
     * for each step in the path, not just the leaf option name directly.
     */
    @Test
    public void testAsNestedChoiceNarrowingSingle() {
        testUtils.assertBundleContainsExpectedString(
            """
            type Opt1:
                opt1Attr int (1..1)

            type Opt2:
                opt2Attr int (1..1)

            type Opt3:
                opt3Attr int (1..1)

            choice NestedBar:
                Opt2
                Opt3

            choice NestedFoo:
                Opt1
                NestedBar

            func TestAsNestedSingle:
                inputs: outer NestedFoo (1..1)
                output: result Opt2 (0..1)
                set result:
                    outer as Opt2
            """,
            """
            result = rune_resolve_attr(rune_resolve_attr(rune_resolve_attr(self, "outer"), "NestedBar"), "Opt2")
            """);
    }

    /**
     * Multi-cardinality nested choice narrowing — the generated list comprehension must
     * guard each path step individually with a walrus assignment and {@code is not None},
     * so that elements whose intermediate choice does not hold the expected option are
     * excluded rather than causing an attribute error.
     */
    @Test
    public void testAsNestedChoiceNarrowingMulti() {
        testUtils.assertBundleContainsExpectedString(
            """
            type Opt1:
                opt1Attr int (1..1)

            type Opt2:
                opt2Attr int (1..1)

            type Opt3:
                opt3Attr int (1..1)

            choice NestedBar:
                Opt2
                Opt3

            choice NestedFoo:
                Opt1
                NestedBar

            func TestAsNestedMulti:
                inputs: outers NestedFoo (0..*)
                output: result Opt2 (0..*)
                set result:
                    outers as Opt2
            """,
            """
            result = [_v for _x in (rune_resolve_attr(self, "outers") or []) if (_t0 := rune_resolve_attr(_x, "NestedBar")) is not None if (_v := rune_resolve_attr(_t0, "Opt2")) is not None]
            """);
    }

    // -------------------------------------------------------------------------
    // Choice option with metadata
    // -------------------------------------------------------------------------

    /**
     * `as` narrowing to a choice option carrying scheme metadata, followed by
     * `-> scheme`, must emit the {@code get_meta("scheme")} lambda accessor pattern
     * that preserves {@code None} when the narrowing does not match.
     */
    @Test
    public void testAsChoiceOptionWithMetadataScheme() {
        testUtils.assertBundleContainsExpectedString(
            """
            typeAlias MyString: string(maxLength: 42)

            choice ChoiceFooMeta:
                number
                MyString
                    [metadata scheme]

            func TestAsChoiceWithMetadata:
                inputs: c ChoiceFooMeta (0..1)
                output: result string (0..1)
                set result: c as MyString -> scheme
            """,
            """
            result = (lambda _r: _r.get_meta("scheme") if _r is not None else None)(rune_resolve_attr(rune_resolve_attr(self, "c"), "MyString"))
            """);
    }

    // -------------------------------------------------------------------------
    // Inline construction as argument to `as`
    // -------------------------------------------------------------------------

    /**
     * Data type narrowing where the argument is an inline conditional constructor —
     * confirms the {@code isinstance} check wraps the {@code if_cond_fn} result correctly.
     */
    @Test
    public void testAsInlineDataTypeConstruction() {
        testUtils.assertBundleContainsExpectedString(
            """
            type Foo:

            type Bar extends Foo:
                barAttr int (1..1)
                inner Qux (1..1)

            type Qux:
                quxAttr int (1..1)
                attr int (1..1)

            func TestAsInlineData:
                output: result int (0..1)
                set result:
                    (if True
                    then Bar { barAttr: 42, inner: Qux { quxAttr: 0, attr: 0 } }
                    else Foo {})
                        as Bar -> barAttr
            """,
            """
            rune_resolve_attr((_x if isinstance(rune_unwrap(_x := (if_cond_fn(True, _then_fn0, _else_fn0))), Bar) else None), "barAttr")
            """);
    }

    /**
     * Choice narrowing where the argument is an inline choice constructor —
     * confirms {@code rune_resolve_attr} is applied directly to the constructed value.
     */
    @Test
    public void testAsInlineChoiceConstruction() {
        testUtils.assertBundleContainsExpectedString(
            """
            type ChoiceBar:
                barAttr int (1..1)

            type ChoiceQux:
                quxAttr int (1..1)

            choice ChoiceFoo:
                ChoiceBar
                ChoiceQux

            func TestAsInlineChoice:
                output: result int (0..1)
                set result:
                    ChoiceFoo { ChoiceBar: ChoiceBar { barAttr: 42 }, ... }
                        as ChoiceBar -> barAttr
            """,
            """
            result = rune_resolve_attr(rune_resolve_attr(ChoiceFoo(ChoiceBar=ChoiceBar(barAttr=42)), "ChoiceBar"), "barAttr")
            """);
    }
}
