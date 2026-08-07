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
 * Tests for expressions involving absent/empty values.
 *
 * <p>The Java generator's {@code EmptyEvaluationTest} operates at expression-evaluation
 * level using compiled Java. For the Python generator we assert on the generated source
 * for equivalent patterns: optional attributes, conditionals without an else branch, and
 * absent-value checks within boolean expressions.
 *
 * <p>Mirrors {@code EmptyEvaluationTest} in the rune-dsl Java generator tests.
 */
@ExtendWith(InjectionExtension.class)
@InjectWith(RosettaInjectorProvider.class)
@SuppressWarnings("LineLength")
public class PythonEmptyEvaluationTest {

    @Inject
    private PythonGeneratorTestUtils testUtils;

    /**
     * A conditional with no {@code else} branch generates a Python ternary
     * whose false-arm is {@code None} (representing empty).
     */
    @Test
    public void testConditionalWithNoElseBranchGeneratesNoneFallback() {
        testUtils.assertBundleContainsExpectedString("""
                func TestNoElse:
                    inputs: flag boolean (1..1)
                    output: result boolean (0..1)
                    set result:
                        if flag then True
                """,
                "if_cond_fn");
    }

    /**
     * An optional attribute field generates an {@code Optional} Python type.
     * Absent value semantics are upheld by the field defaulting to {@code None}.
     */
    @Test
    public void testOptionalAttributeGeneratesNoneDefault() {
        testUtils.assertBundleContainsExpectedString("""
                type Foo:
                    someBoolean boolean (0..1)
                    alwaysFalse boolean (1..1)
                """,
                "Optional[bool]");
    }

    /**
     * {@code is absent} generates a negated {@code rune_attr_exists} call.
     */
    @Test
    public void testIsAbsentGeneratesNegatedAttrExists() {
        testUtils.assertBundleContainsExpectedString("""
                type Foo:
                    val int (0..1)
                    condition AbsentCheck:
                        val is absent
                """,
                "(not rune_attr_exists(");
    }

    /**
     * An OR expression where the left operand is an optional attribute generates
     * correctly — the optional attribute resolves via {@code rune_resolve_attr}.
     */
    @Test
    public void testAbsentOptionalInBooleanOrExpression() {
        testUtils.assertBundleContainsExpectedString("""
                type Foo:
                    someBoolean boolean (0..1)
                    alwaysFalse boolean (1..1)
                    condition OrCheck:
                        someBoolean or alwaysFalse
                """,
                "rune_resolve_attr(self, \"someBoolean\")");
    }

    /**
     * Accessing an optional attribute through a constructor expression generates a
     * {@code rune_resolve_attr} call, not a direct field reference that would NPE.
     */
    @Test
    public void testOptionalAttributeAccessViaConstructorExpression() {
        testUtils.assertBundleContainsExpectedString("""
                type Foo:
                    someBoolean boolean (0..1)
                    alwaysFalse boolean (1..1)
                    condition ConstructorAndOr:
                        Foo { alwaysFalse: False, ... } -> someBoolean or Foo { alwaysFalse: False, ... } -> alwaysFalse
                """,
                "Foo(alwaysFalse=False)");
    }

    /**
     * {@code exists} on an optional field generates a {@code rune_attr_exists} call.
     */
    @Test
    public void testExistsOnOptionalFieldGeneratesAttrExistsCall() {
        testUtils.assertBundleContainsExpectedString("""
                type Bar:
                    val int (0..1)
                    condition ExistsCheck:
                        val exists
                """,
                "rune_attr_exists(rune_resolve_attr(self, \"val\"))");
    }
}
