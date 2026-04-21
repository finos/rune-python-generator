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

@ExtendWith(InjectionExtension.class)
@InjectWith(RosettaInjectorProvider.class)
@SuppressWarnings("LineLength")
public class PythonConditionalExpressionTest {

    /**
     * Test utils for generating Python.
     */
    @Inject
    private PythonGeneratorTestUtils testUtils;

    // -------------------------------------------------------------------------
    // From RosettaConditionalExpressionTest
    // -------------------------------------------------------------------------

    /**
     * Test case for if-then condition.
     */
    @Test
    public void testGenerateIfThenCondition() {
        testUtils.assertBundleContainsExpectedString(
            """
            type Test1: <"Test if-then condition.">
                field1 string (0..1) <"Test string field 1">
                field2 number (0..1) <"Test number field 2">
                condition TestCond: <"Test condition">
                    if field1 exists
                        then field2=0
            """,
            """
            class Test1(BaseDataClass):
                \"\"\"
                Test if-then condition.
                \"\"\"
                field1: Optional[str] = Field(None, description='Test string field 1')
                \"\"\"
                Test string field 1
                \"\"\"
                field2: Optional[Decimal] = Field(None, description='Test number field 2')
                \"\"\"
                Test number field 2
                \"\"\"

                @rune_condition
                def condition_0_TestCond(self):
                    \"\"\"
                    Test condition
                    \"\"\"
                    item = self
                    def _then_fn0():
                        return rune_all_elements(rune_resolve_attr(self, "field2"), "=", 0)

                    def _else_fn0():
                        return True

                    return if_cond_fn(rune_attr_exists(rune_resolve_attr(self, "field1")), _then_fn0, _else_fn0)""");
    }

    @Test
    public void testGenerateIfThenElseCondition() {
        testUtils.assertBundleContainsExpectedString(
            """
            type Test1: <"Test if-then-else condition.">
            field1 string (0..1) <"Test string field 1">
            field2 number (0..1) <"Test number field 2">
            condition TestCond: <"Test condition">
                if field1 exists
                    then field2=0
                    else field2=1
            """,
            """
            class Test1(BaseDataClass):
                \"\"\"
                Test if-then-else condition.
                \"\"\"
                field1: Optional[str] = Field(None, description='Test string field 1')
                \"\"\"
                Test string field 1
                \"\"\"
                field2: Optional[Decimal] = Field(None, description='Test number field 2')
                \"\"\"
                Test number field 2
                \"\"\"

                @rune_condition
                def condition_0_TestCond(self):
                    \"\"\"
                    Test condition
                    \"\"\"
                    item = self
                    def _then_fn0():
                        return rune_all_elements(rune_resolve_attr(self, "field2"), "=", 0)

                    def _else_fn0():
                        return rune_all_elements(rune_resolve_attr(self, "field2"), "=", 1)

                    return if_cond_fn(rune_attr_exists(rune_resolve_attr(self, "field1")), _then_fn0, _else_fn0)""");
    }

    @Test
    public void testGenerateBooleanCondition() {
        testUtils.assertBundleContainsExpectedString(
            """
            type Test1: <"Test boolean condition.">
            field1 boolean (1..1) <"Test booelan field 1">
            field2 number (0..1) <"Test number field 2">
            condition TestCond: <"Test condition">
                if field1= True
                    then field2=0
                    else field2=5
            """,
            """
            class Test1(BaseDataClass):
                \"\"\"
                Test boolean condition.
                \"\"\"
                field1: bool = Field(..., description='Test booelan field 1')
                \"\"\"
                Test booelan field 1
                \"\"\"
                field2: Optional[Decimal] = Field(None, description='Test number field 2')
                \"\"\"
                Test number field 2
                \"\"\"

                @rune_condition
                def condition_0_TestCond(self):
                    \"\"\"
                    Test condition
                    \"\"\"
                    item = self
                    def _then_fn0():
                        return rune_all_elements(rune_resolve_attr(self, "field2"), "=", 0)

                    def _else_fn0():
                        return rune_all_elements(rune_resolve_attr(self, "field2"), "=", 5)

                    return if_cond_fn(rune_all_elements(rune_resolve_attr(self, "field1"), "=", True), _then_fn0, _else_fn0)""");
    }

    @Test
    public void testGenerateAbsentCondition() {
        testUtils.assertBundleContainsExpectedString(
            """
            type Test1: <"Test absent condition.">
            field1 boolean (1..1) <"Test booelan field 1">
            field2 number (0..1) <"Test number field 2">
            condition TestCond: <"Test condition">
                if field1= True
                    then field2=0
                    else field2 is absent
            """,
            """
            class Test1(BaseDataClass):
                \"\"\"
                Test absent condition.
                \"\"\"
                field1: bool = Field(..., description='Test booelan field 1')
                \"\"\"
                Test booelan field 1
                \"\"\"
                field2: Optional[Decimal] = Field(None, description='Test number field 2')
                \"\"\"
                Test number field 2
                \"\"\"

                @rune_condition
                def condition_0_TestCond(self):
                    \"\"\"
                    Test condition
                    \"\"\"
                    item = self
                    def _then_fn0():
                        return rune_all_elements(rune_resolve_attr(self, "field2"), "=", 0)

                    def _else_fn0():
                        return (not rune_attr_exists(rune_resolve_attr(self, "field2")))

                    return if_cond_fn(rune_all_elements(rune_resolve_attr(self, "field1"), "=", True), _then_fn0, _else_fn0)""");
    }

    // -------------------------------------------------------------------------
    // From RosettaSwitchExpressionTest
    // -------------------------------------------------------------------------

    /**
     * Test case for switch basic.
     */
    @Test
    public void testGenerateSwitch() {
        testUtils.assertBundleContainsExpectedString(
            """
            type FooTest:
                a int (1..1) <"Test field a">
                condition Test:
                    a switch
                        1 then True,
                        2 then True,
                        default False
            """,
            """
            class FooTest(BaseDataClass):
                a: int = Field(..., description='Test field a')
                \"\"\"
                Test field a
                \"\"\"

                @rune_condition
                def condition_0_Test(self):
                    item = self
                    def _switch_fn_0():
                        def _then_1():
                            return True
                        def _then_2():
                            return True
                        def _then_default():
                            return False
                        switchAttribute = rune_resolve_attr(self, "a")
                        if switchAttribute == 1:
                            return _then_1()
                        elif switchAttribute == 2:
                            return _then_2()
                        else:
                            return _then_default()

                    return _switch_fn_0()
            """);
    }

    // -------------------------------------------------------------------------
    // From RosettaChoiceExpressionTest
    // -------------------------------------------------------------------------

    /**
     * Test case for choice expression.
     */
    @Test
    public void testGenerateChoiceCondition() {
        testUtils.assertBundleContainsExpectedString("""
                type Test1:<"Test choice condition.">
                field1 string (0..1) <"Test string field 1">
                field2 string (0..1) <"Test string field 2">
                field3 string (0..1) <"Test string field 3">
                condition TestChoice: optional choice field1, field2, field3
                """,
                """
                        class Test1(BaseDataClass):
                            \"""
                            Test choice condition.
                            \"""
                            field1: Optional[str] = Field(None, description='Test string field 1')
                            \"""
                            Test string field 1
                            \"""
                            field2: Optional[str] = Field(None, description='Test string field 2')
                            \"""
                            Test string field 2
                            \"""
                            field3: Optional[str] = Field(None, description='Test string field 3')
                            \"""
                            Test string field 3
                            \"""

                            @rune_condition
                            def condition_0_TestChoice(self):
                                item = self
                                return rune_check_one_of(self, 'field1', 'field2', 'field3', necessity=False)""");
    }

    /**
     * Test case for one-of condition.
     */
    @Test
    public void testGenerateOneOfCondition() {
        testUtils.assertBundleContainsExpectedString("""
                type Test1:<"Test one-of condition.">
                    field1 string (0..1) <"Test string field 1">
                    condition OneOf: one-of
                """,
                """
                        class Test1(BaseDataClass):
                            _CHOICE_ALIAS_MAP ={"field1":[]}
                            \"""
                            Test one-of condition.
                            \"""
                            field1: Optional[str] = Field(None, description='Test string field 1')
                            \"""
                            Test string field 1
                            \"""

                            @rune_condition
                            def condition_0_OneOf(self):
                                item = self
                                return rune_check_one_of(self, 'field1', necessity=True)""");
    }
}
