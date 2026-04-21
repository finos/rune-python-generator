/*
 * Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
 * SPDX-License-Identifier: Apache-2.0
 */
package com.regnosys.rosetta.generator.python.expressions;

import com.regnosys.rosetta.generator.python.PythonGeneratorTestUtils;
import com.regnosys.rosetta.tests.RosettaInjectorProvider;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import jakarta.inject.Inject;

@ExtendWith(InjectionExtension.class)
@InjectWith(RosettaInjectorProvider.class)
@SuppressWarnings("LineLength")
public class PythonExistsExpressionTest {

    /**
     * Test utils for generating Python.
     */
    @Inject
    private PythonGeneratorTestUtils testUtils;

    /**
     * Test case for exists basic.
     */
    @Test
    public void testExistsBasic() {
        testUtils.assertBundleContainsExpectedString(
                """
                        type Bar:
                            field number (0..1)

                        func ExistsBasic:
                            inputs: bar Bar (1..1)
                            output: result boolean (1..1)
                            set result:
                                bar -> field exists
                        """,
                """
                        @replaceable
                        @validate_call
                        def ExistsBasic(bar: Bar) -> bool:
                            \"\"\"

                            Parameters
                            ----------
                            bar : com.rosetta.test.model.Bar

                            Returns
                            -------
                            result : bool

                            \"\"\"
                            self = inspect.currentframe()

                            bar = rune_cow(bar)


                            result = rune_attr_exists(rune_resolve_attr(rune_resolve_attr(self, "bar"), "field"))


                            return result""");
    }

    /**
     * Test case for absent basic.
     */
    @Test
    public void testAbsentBasic() {
        testUtils.assertBundleContainsExpectedString(
                """
                        type Bar:
                            field number (0..1)

                        func AbsentBasic:
                            inputs: bar Bar (1..1)
                            output: result boolean (1..1)
                            set result:
                                bar -> field is absent
                        """,
                """
                        @replaceable
                        @validate_call
                        def AbsentBasic(bar: Bar) -> bool:
                            \"\"\"

                            Parameters
                            ----------
                            bar : com.rosetta.test.model.Bar

                            Returns
                            -------
                            result : bool

                            \"\"\"
                            self = inspect.currentframe()

                            bar = rune_cow(bar)


                            result = (not rune_attr_exists(rune_resolve_attr(rune_resolve_attr(self, "bar"), "field")))


                            return result""");
    }

    /**
     * Test case for single exists.
     */
    @Test
    public void testSingleExists() {
        testUtils.assertBundleContainsExpectedString(
                """
                        type Bar:
                            field number (0..1)

                        func SingleExists:
                            inputs: bar Bar (1..1)
                            output: result boolean (1..1)
                            set result:
                                bar -> field single exists
                        """,
                """
                        @replaceable
                        @validate_call
                        def SingleExists(bar: Bar) -> bool:
                            \"\"\"

                            Parameters
                            ----------
                            bar : com.rosetta.test.model.Bar

                            Returns
                            -------
                            result : bool

                            \"\"\"
                            self = inspect.currentframe()

                            bar = rune_cow(bar)


                            result = rune_attr_exists(rune_resolve_attr(rune_resolve_attr(self, "bar"), "field"), "single")


                            return result""");
    }

    /**
     * Test case for multiple exists.
     */
    @Test
    public void testMultipleExists() {
        testUtils.assertBundleContainsExpectedString(
                """
                        type Bar:
                            fieldList number (0..*)

                        func MultipleExists:
                            inputs: bar Bar (1..1)
                            output: result boolean (1..1)
                            set result:
                                bar -> fieldList multiple exists
                        """,
                """
                        @replaceable
                        @validate_call
                        def MultipleExists(bar: Bar) -> bool:
                            \"\"\"

                            Parameters
                            ----------
                            bar : com.rosetta.test.model.Bar

                            Returns
                            -------
                            result : bool

                            \"\"\"
                            self = inspect.currentframe()

                            bar = rune_cow(bar)


                            result = rune_attr_exists(rune_resolve_attr(rune_resolve_attr(self, "bar"), "fieldList"), "multiple")


                            return result""");
    }

    /**
     * Test case for exists with metadata.
     */
    @Test
    public void testExistsWithMetadata() {
        testUtils.assertBundleContainsExpectedString(
                """
                        type Bar:
                            field number (0..1)
                                [metadata scheme]

                        func ExistsWithMetadata:
                            inputs: bar Bar (1..1)
                            output: result boolean (1..1)
                            set result:
                                bar -> field exists
                        """,
                """
                        @replaceable
                        @validate_call
                        def ExistsWithMetadata(bar: Bar) -> bool:
                            \"\"\"

                            Parameters
                            ----------
                            bar : com.rosetta.test.model.Bar

                            Returns
                            -------
                            result : bool

                            \"\"\"
                            self = inspect.currentframe()

                            bar = rune_cow(bar)


                            result = rune_attr_exists(rune_resolve_attr(rune_resolve_attr(self, "bar"), "field"))


                            return result""");
    }

    /**
     * Test case for exists with logical operators.
     */
    @Test
    public void testExistsWithLogicalOperators() {
        testUtils.assertBundleContainsExpectedString(
                """
                        type Bar:
                            field1 number (0..1)
                            field2 number (0..1)

                        func ExistsWithLogical:
                            inputs: bar Bar (1..1)
                            output: result boolean (1..1)
                            set result:
                                bar -> field1 exists and bar -> field2 exists
                        """,
                """
                        @replaceable
                        @validate_call
                        def ExistsWithLogical(bar: Bar) -> bool:
                            \"\"\"

                            Parameters
                            ----------
                            bar : com.rosetta.test.model.Bar

                            Returns
                            -------
                            result : bool

                            \"\"\"
                            self = inspect.currentframe()

                            bar = rune_cow(bar)


                            result = (rune_attr_exists(rune_resolve_attr(rune_resolve_attr(self, "bar"), "field1")) and rune_attr_exists(rune_resolve_attr(rune_resolve_attr(self, "bar"), "field2")))


                            return result""");
    }

    /**
     * Test case for deep path single exists.
     */
    @Test
    public void testDeepPathSingleExists() {
        testUtils.assertBundleContainsExpectedString(
                """
                        type Sub:
                            field number (0..1)
                        type Bar:
                            sub Sub (0..1)

                        func DeepExists:
                            inputs: bar Bar (1..1)
                            output: result boolean (1..1)
                            set result:
                                bar -> sub -> field single exists
                        """,
                """
                        @replaceable
                        @validate_call
                        def DeepExists(bar: Bar) -> bool:
                            \"\"\"

                            Parameters
                            ----------
                            bar : com.rosetta.test.model.Bar

                            Returns
                            -------
                            result : bool

                            \"\"\"
                            self = inspect.currentframe()

                            bar = rune_cow(bar)


                            result = rune_attr_exists(rune_resolve_attr(rune_resolve_attr(rune_resolve_attr(self, "bar"), "sub"), "field"), "single")


                            return result""");
    }

    /**
     * Test case for exists in function arguments.
     */
    @Test
    public void testExistsInFunctionArguments() {
        testUtils.assertBundleContainsExpectedString(
                """
                        func ExistsArg:
                            inputs:
                                arg1 number (0..1)
                                arg2 number (0..1)
                            output:
                                result boolean (1..1)
                            set result:
                                arg1 exists or arg2 exists
                        """,
                """
                        @replaceable
                        @validate_call
                        def ExistsArg(arg1: Decimal | None, arg2: Decimal | None) -> bool:
                            \"\"\"

                            Parameters
                            ----------
                            arg1 : Decimal

                            arg2 : Decimal

                            Returns
                            -------
                            result : bool

                            \"\"\"
                            self = inspect.currentframe()

                            arg1 = rune_cow(arg1)
                            arg2 = rune_cow(arg2)


                            result = (rune_attr_exists(rune_resolve_attr(self, "arg1")) or rune_attr_exists(rune_resolve_attr(self, "arg2")))


                            return result""");
    }

    /**
     * Test case for only-exists single path.
     */
    @Test
    public void testOnlyExistsSinglePath() {
        testUtils.assertBundleContainsExpectedString("""
                type A:
                    field1 number (0..1)

                type Test:
                    aValue A (1..1)

                    condition TestCond:
                        if aValue -> field1 exists
                            then aValue -> field1 only exists
                """,
                "return rune_check_one_of(self, rune_resolve_attr(rune_resolve_attr(self, \"aValue\"), \"field1\"))");
    }

    /**
     * Test case for only-exists multiple paths.
     */
    @Test
    public void testOnlyExistsMultiplePaths() {
        testUtils.assertBundleContainsExpectedString("""
                type Bar:
                    before number (0..1)
                    after number (0..1)

                func OnlyExistsMultiplePaths:
                    inputs: bar Bar (1..1)
                    output: result boolean (1..1)
                    set result:
                        ( bar -> before, bar -> after ) only exists
                """,
                "result = rune_check_one_of(self, rune_resolve_attr(rune_resolve_attr(self, \"bar\"), \"before\"), rune_resolve_attr(rune_resolve_attr(self, \"bar\"), \"after\"))");
    }

    /**
     * Test case for only-exists with metadata.
     */
    @Test
    public void testOnlyExistsWithMetadata() {
        testUtils.assertBundleContainsExpectedString("""
                type Bar:
                    before number (0..1)
                        [metadata scheme]

                func OnlyExistsWithMetadata:
                    inputs: bar Bar (1..1)
                    output: result boolean (1..1)
                    set result:
                        bar -> before only exists
                """,
                "result = rune_check_one_of(self, rune_resolve_attr(rune_resolve_attr(self, \"bar\"), \"before\"))");
    }

    /**
     * Test case for only-exists three paths.
     */
    @Test
    public void testOnlyExistsThreePaths() {
        testUtils.assertBundleContainsExpectedString("""
                type Bar:
                    a number (0..1)
                    b number (0..1)
                    c number (0..1)

                func OnlyExistsThree:
                    inputs: bar Bar (1..1)
                    output: result boolean (1..1)
                    set result:
                        ( bar -> a, bar -> b, bar -> c ) only exists
                """,
                "result = rune_check_one_of(self, rune_resolve_attr(rune_resolve_attr(self, \"bar\"), \"a\"), rune_resolve_attr(rune_resolve_attr(self, \"bar\"), \"b\"), rune_resolve_attr(rune_resolve_attr(self, \"bar\"), \"c\"))");
    }
}
