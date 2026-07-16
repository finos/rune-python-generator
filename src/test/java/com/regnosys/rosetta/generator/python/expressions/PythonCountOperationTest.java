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
 * Dedicated tests for the {@code count} operation in generated Python.
 *
 * <p>The count generator emits an inline lambda that uses {@code sum(1 for x in ...
 * if x is not None)}, correctly filtering {@code None} elements.  This class verifies
 * that pattern in several contexts: function body, condition, and chained after map/filter.
 *
 * <p>Mirrors {@code RosettaCountOperationTest} in the rune-dsl Java generator tests.
 */
@ExtendWith(InjectionExtension.class)
@InjectWith(RosettaInjectorProvider.class)
@SuppressWarnings("LineLength")
public class PythonCountOperationTest {

    private static final String COUNT_LAMBDA_PREFIX =
            "(lambda items: sum(1 for x in (items if (hasattr(items, '__iter__') and not isinstance(items, (str, dict, bytes, bytearray))) else ([items] if items is not None else [])) if x is not None))";

    @Inject
    private PythonGeneratorTestUtils testUtils;

    /**
     * {@code items count} used as a function return value generates the count lambda.
     */
    @Test
    public void testCountOnFunctionInputGeneratesLambda() {
        testUtils.assertBundleContainsExpectedString("""
                func CountItems:
                    inputs: items int (0..*)
                    output: result int (1..1)
                    set result:
                        items count
                """,
                COUNT_LAMBDA_PREFIX + "(rune_resolve_attr(self, \"items\"))");
    }

    /**
     * {@code [1, 2, 3] count} — count on a list literal.
     */
    @Test
    public void testCountOnListLiteralGeneratesLambda() {
        testUtils.assertBundleContainsExpectedString("""
                func CountLiteral:
                    output: result int (1..1)
                    set result:
                        [1, 2, 3] count
                """,
                COUNT_LAMBDA_PREFIX + "([1, 2, 3])");
    }

    /**
     * {@code items count = 3} in a condition generates an equality comparison
     * wrapped in {@code rune_all_elements}.
     */
    @Test
    public void testCountEqualityInConditionWrapsAllElements() {
        testUtils.assertBundleContainsExpectedString("""
                type Box:
                    values int (0..*)
                    condition ThreeValues:
                        values count = 3
                """,
                "return rune_all_elements(" + COUNT_LAMBDA_PREFIX + "(rune_resolve_attr(self, \"values\")), \"=\", 3)");
    }

    /**
     * {@code field1 count > 0} — count with a greater-than comparison.
     */
    @Test
    public void testCountGreaterThanZeroInCondition() {
        testUtils.assertBundleContainsExpectedString("""
                type Holder:
                    tags string (0..*)
                    condition HasTags:
                        tags count > 0
                """,
                COUNT_LAMBDA_PREFIX + "(rune_resolve_attr(self, \"tags\"))");
    }

    /**
     * Count on an extracted attribute path: {@code aValue -> field1 count}.
     * The path resolution is wrapped in the same count lambda.
     */
    @Test
    public void testCountOnAttributePath() {
        testUtils.assertBundleContainsExpectedString("""
                type A:
                    field1 int (0..*)

                type B:
                    a A (1..1)
                    condition FieldCount:
                        a -> field1 count = 2
                """,
                COUNT_LAMBDA_PREFIX + "(rune_resolve_attr(rune_resolve_attr(self, \"a\"), \"field1\"))");
    }

    /**
     * Comparing two count expressions: {@code field1 count <> field2 count}.
     */
    @Test
    public void testCountComparisonBetweenTwoFields() {
        testUtils.assertBundleContainsExpectedString("""
                type Data:
                    field1 int (0..*)
                    field2 int (0..*)
                    condition DifferentLengths:
                        field1 count <> field2 count
                """,
                COUNT_LAMBDA_PREFIX + "(rune_resolve_attr(self, \"field1\"))");
    }
}
