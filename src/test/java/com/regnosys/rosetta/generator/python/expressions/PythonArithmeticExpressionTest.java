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
 * Every element of this test needs to check the entire generated Python.
 * This class focuses on expression-to-python logic for math operations and type conversions.
 */
@ExtendWith(InjectionExtension.class)
@InjectWith(RosettaInjectorProvider.class)
@SuppressWarnings("checkstyle:LineLength")
public class PythonArithmeticExpressionTest {

    /**
     * Test utils for generating Python.
     */
    @Inject
    private PythonGeneratorTestUtils testUtils;

    // -------------------------------------------------------------------------
    // From RosettaMathOperationTest
    // -------------------------------------------------------------------------

    /**
     * Test case for math operations.
     */
    @Test
    public void testMathOperations() {
        String generatedPython = testUtils.generatePythonFromString("""
                type TestMath:
                    a int (1..1)
                    b int (1..1)
                    condition MathCheck:
                        if a * b = 10 and a - b = 3 and a / b = 2
                        then True
                """).toString();

        testUtils.assertGeneratedContainsExpectedString(generatedPython,
                """
                class TestMath(BaseDataClass):
                    a: int = Field(..., description='')
                    b: int = Field(..., description='')

                    @rune_condition
                    def condition_0_MathCheck(self):
                        item = self
                        def _then_fn0():
                            return True

                        def _else_fn0():
                            return True

                        return if_cond_fn(((rune_all_elements((rune_resolve_attr(self, "a") * rune_resolve_attr(self, "b")), "=", 10) and rune_all_elements((rune_resolve_attr(self, "a") - rune_resolve_attr(self, "b")), "=", 3)) and rune_all_elements((rune_resolve_attr(self, "a") / rune_resolve_attr(self, "b")), "=", 2)), _then_fn0, _else_fn0)""");
    }

    /**
     * Test case for arithmetic operator.
     */
    @Test
    public void testArithmeticOperator() {
        // This was already full output style
        String generatedPython = testUtils.generatePythonFromString("""
                type ArithmeticTest:
                    a int (1..1)
                    b int (1..1)
                    condition Test:
                        if a + b = 3 then True
                        else False
                """).toString();

        testUtils.assertGeneratedContainsExpectedString(
                generatedPython,
                """
                class ArithmeticTest(BaseDataClass):
                    a: int = Field(..., description='')
                    b: int = Field(..., description='')

                    @rune_condition
                    def condition_0_Test(self):
                        item = self
                        def _then_fn0():
                            return True

                        def _else_fn0():
                            return False

                        return if_cond_fn(rune_all_elements((rune_resolve_attr(self, "a") + rune_resolve_attr(self, "b")), "=", 3), _then_fn0, _else_fn0)""");
    }

    // -------------------------------------------------------------------------
    // From RosettaConversionTest
    // -------------------------------------------------------------------------

    /**
     * Test case for basic conversions.
     */
    @Test
    public void testBasicConversions() {
        testUtils.assertBundleContainsExpectedString("""
                type TestConv:
                    val int (1..1)
                    s string (1..1)
                    condition ConvCheck:
                        val to-string = "1" and
                        s to-int = 1
                """,
                """
                        class TestConv(BaseDataClass):
                            val: int = Field(..., description='')
                            s: str = Field(..., description='')

                            @rune_condition
                            def condition_0_ConvCheck(self):
                                item = self
                                return (rune_all_elements(rune_str(rune_resolve_attr(self, "val")), "=", "1") and rune_all_elements(int(rune_resolve_attr(self, "s")), "=", 1))""");
    }

    /**
     * Test case for date conversions.
     */
    @Test
    public void testDateConversions() {
        testUtils.assertBundleContainsExpectedString("""
                type TestDateConv:
                    s string (1..1)
                    condition DateConvCheck:
                        s to-date = "2023-11-20" to-date and
                        s to-date-time = "2023-11-20 12:00:00" to-date-time and
                        s to-time = "12:00:00" to-time
                """,
                """
                        class TestDateConv(BaseDataClass):
                            s: str = Field(..., description='')

                            @rune_condition
                            def condition_0_DateConvCheck(self):
                                item = self
                                return ((rune_all_elements(datetime.datetime.strptime(rune_resolve_attr(self, "s"), "%Y-%m-%d").date(), "=", datetime.datetime.strptime("2023-11-20", "%Y-%m-%d").date()) and rune_all_elements(datetime.datetime.strptime(rune_resolve_attr(self, "s"), "%Y-%m-%d %H:%M:%S"), "=", datetime.datetime.strptime("2023-11-20 12:00:00", "%Y-%m-%d %H:%M:%S"))) and rune_all_elements(datetime.datetime.strptime(rune_resolve_attr(self, "s"), "%H:%M:%S").time(), "=", datetime.datetime.strptime("12:00:00", "%H:%M:%S").time()))""");
    }

    /**
     * Test case for enum conversion.
     */
    @Test
    public void testEnumConversion() {
        testUtils.assertBundleContainsExpectedString("""
                enum MyEnum:
                    Value1
                type TestEnumConv:
                    s string (1..1)
                    condition EnumConvCheck:
                        s to-enum MyEnum = MyEnum -> Value1
                """,
                """
                        class TestEnumConv(BaseDataClass):
                            s: str = Field(..., description='')

                            @rune_condition
                            def condition_0_EnumConvCheck(self):
                                item = self
                                return rune_all_elements(MyEnum(rune_resolve_attr(self, "s")), "=", com.rosetta.test.model.MyEnum.MyEnum.VALUE_1)""");
    }
}
