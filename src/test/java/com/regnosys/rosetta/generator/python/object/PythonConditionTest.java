/*
 * Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
 * SPDX-License-Identifier: Apache-2.0
 */
package com.regnosys.rosetta.generator.python.object;

import jakarta.inject.Inject;
import com.regnosys.rosetta.tests.RosettaInjectorProvider;
import com.regnosys.rosetta.generator.python.PythonGeneratorTestUtils;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import java.util.Map;

/**
 * Tests for Python condition code generation, covering data rules, object conditions,
 * and choice conditions.
 */
@ExtendWith(InjectionExtension.class)
@InjectWith(RosettaInjectorProvider.class)
@SuppressWarnings("LineLength")
public class PythonConditionTest {

    /**
     * PythonGeneratorTestUtils is used to generate Python code from Rosetta models.
     */
    @Inject
    private PythonGeneratorTestUtils testUtils;

    // -----------------------------------------------------------------------
    // Methods from PythonDataRuleGeneratorTest (renamed)
    // -----------------------------------------------------------------------

    /**
     * Test case for conditions with if-else-if statements.
     */
    @Test
    public void testIfElseIfCondition() {
        String pythonString = testUtils.generatePythonFromString(
            """
            type Foo:
                bar string (0..1)
                baz string (0..1)

                condition:
                    if bar="Y" then baz exists
                    else if (bar="I" or bar="N") then baz is absent
            """).toString();

        testUtils.assertGeneratedContainsExpectedString(pythonString,
            "class Foo(BaseDataClass):");
        testUtils.assertGeneratedContainsExpectedString(pythonString,
            "bar: Optional[str] = Field(None, description='')");
        testUtils.assertGeneratedContainsExpectedString(pythonString,
            "baz: Optional[str] = Field(None, description='')");
        testUtils.assertGeneratedContainsExpectedString(pythonString, "@rune_condition");
        testUtils.assertGeneratedContainsExpectedString(pythonString, "def condition_0_(self):");
        testUtils.assertGeneratedContainsExpectedString(pythonString,
            "return if_cond_fn(rune_all_elements(rune_resolve_attr(self, \"bar\"), \"=\", \"Y\"), _then_fn0, _else_fn0)");
    }

    /**
     * Test case for conditions with nested if-else-if statements.
     */
    @Test
    public void testNestedIfElseIfCondition() {
        String pythonString = testUtils.generatePythonFromString(
            """
            type Foo:
                bar string (0..1)
                baz string (0..1)

                condition:
                    if bar exists then
                        if bar="Y" then baz exists
                        else if (bar="I" or bar="N") then baz is absent
            """).toString();

        testUtils.assertGeneratedContainsExpectedString(pythonString,
            "class Foo(BaseDataClass):");
        testUtils.assertGeneratedContainsExpectedString(pythonString, "def condition_0_(self):");
        testUtils.assertGeneratedContainsExpectedString(pythonString,
            "return if_cond_fn(rune_attr_exists(rune_resolve_attr(self, \"bar\")), _then_fn0, _else_fn0)");
    }

    /**
     * Test case for conditions with exists statements.
     * Quote depends on QuotePrice (one-way); both are standalone.
     * No Phase 2/3 needed for standalone classes.
     */
    @Test
    public void testExistsCondition() {
        String pythonString = testUtils.generatePythonFromString(
            """
            type Quote:
                quotePrice QuotePrice (0..1)
                condition Quote_Price:
                if quotePrice exists
                then quotePrice -> bidPrice exists or quotePrice -> offerPrice exists

            type QuotePrice:
                bidPrice number (0..1)
                offerPrice number (0..1)
            """)
            .toString();

        testUtils.assertGeneratedContainsExpectedString(pythonString,
            "class Quote(BaseDataClass):");
        testUtils.assertGeneratedContainsExpectedString(pythonString,
            "quotePrice: Optional[QuotePrice] = Field(None, description='')");

        testUtils.assertGeneratedContainsExpectedString(pythonString,
            "class QuotePrice(BaseDataClass):");
        testUtils.assertGeneratedContainsExpectedString(pythonString,
            "bidPrice: Optional[Decimal] = Field(None, description='')");

        testUtils.assertGeneratedContainsExpectedString(pythonString, "def condition_0_Quote_Price(self):");
    }

    /**
     * Test case for conditions with nested and statements.
     */
    @Test
    public void testNestedAndCondition() {
        String pythonString = testUtils.generatePythonFromString(
            """
            type Quote:
                quotePrice QuotePrice (0..1)
                condition Quote_Price:
                if quotePrice exists
                then (
                    quotePrice -> price1 exists
                    and quotePrice -> price2 exists
                    and quotePrice -> price3 exists
                )

            type QuotePrice:
                price1 number (0..1)
                price2 number (0..1)
                price3 number (0..1)
            """).toString();

        testUtils.assertGeneratedContainsExpectedString(pythonString,
            "class Quote(BaseDataClass):");
        testUtils.assertGeneratedContainsExpectedString(pythonString,
            "quotePrice: Optional[QuotePrice] = Field(None, description='')");
        testUtils.assertGeneratedContainsExpectedString(pythonString, "def condition_0_Quote_Price(self):");
    }

    /**
     * Test case for conditions with number attribute statements.
     */
    @Test
    public void testNumberAttributeCondition() {
        String pythonString = testUtils.generatePythonFromString(
            """
            type Quote:
                quotePrice QuotePrice (0..1)
                condition Quote_Price:
                if quotePrice exists
                then quotePrice -> bidPrice = 0.0

            type QuotePrice:
                bidPrice number (0..1)
            """).toString();

        testUtils.assertGeneratedContainsExpectedString(pythonString,
            "quotePrice: Optional[QuotePrice] = Field(None, description='')");
        testUtils.assertGeneratedContainsExpectedString(pythonString,
            "return rune_all_elements(rune_resolve_attr(rune_resolve_attr(self, \"quotePrice\"), \"bidPrice\"), \"=\", Decimal('0.0'))");
    }

    /**
     * Test case for conditions with function statements.
     */
    @Test
    public void testConditionWithFunctionCall() {
        String pythonString = testUtils.generatePythonFromString(
            """
            func Foo:
                inputs:
                    price number (0..1)
                output:
                    something number (1..1)

            type Quote:
                price number (0..1)

                condition:
                    if price exists
                    then Foo( price ) = 5.0
            """).toString();

        testUtils.assertGeneratedContainsExpectedString(pythonString,
            "class Quote(BaseDataClass):");
        testUtils.assertGeneratedContainsExpectedString(pythonString,
            "return rune_all_elements(rune_call_unchecked(Foo, rune_resolve_attr(self, \"price\")), \"=\", Decimal('5.0'))");
    }

    /**
     * Test case for conditions with function statements and else.
     */
    @Test
    public void testConditionWithFunctionCallAndElse() {
        String pythonString = testUtils.generatePythonFromString(
            """
            func Foo:
                inputs:
                    price number (0..1)
                output:
                    something number (1..1)

            type Quote:
                price number (0..1)

                condition:
                    if price exists
                    then Foo( price ) = 5.0
                    else True
            """).toString();

        testUtils.assertGeneratedContainsExpectedString(pythonString,
            "class Quote(BaseDataClass):");
        testUtils.assertGeneratedContainsExpectedString(pythonString,
            "return if_cond_fn(rune_attr_exists(rune_resolve_attr(self, \"price\")), _then_fn0, _else_fn0)");
    }

    /**
     * Test case for conditions with boolean attribute statements.
     */
    @Test
    public void testBooleanConditionTrue() {
        String pythonString = testUtils.generatePythonFromString(
            """
            type Coin:
                head boolean (0..1)
                tail boolean (0..1)

                condition CoinHeadRule:
                    if head = True
                    then tail = False
            """).toString();

        testUtils.assertGeneratedContainsExpectedString(pythonString,
            "class Coin(BaseDataClass):");
        testUtils.assertGeneratedContainsExpectedString(pythonString,
            "return if_cond_fn(rune_all_elements(rune_resolve_attr(self, \"head\"), \"=\", True), _then_fn0, _else_fn0)");
    }

    /**
     * Test case for conditions with boolean attribute statements and else.
     */
    @Test
    public void testBooleanConditionFalse() {
        String pythonString = testUtils.generatePythonFromString(
            """
            type Coin:
                head boolean (0..1)
                tail boolean (0..1)

                condition CoinTailRule:
                    if tail = True
                    then head = False
            """).toString();

        testUtils.assertGeneratedContainsExpectedString(pythonString,
            "class Coin(BaseDataClass):");
        testUtils.assertGeneratedContainsExpectedString(pythonString,
            "return if_cond_fn(rune_all_elements(rune_resolve_attr(self, \"tail\"), \"=\", True), _then_fn0, _else_fn0)");
    }

    /**
     * Test case for conditions with boolean attribute statements and else.
     */
    @Test
    public void testBooleanConditionDefault() {
        String pythonString = testUtils.generatePythonFromString(
            """
            type Coin:
                head boolean (0..1)
                tail boolean (0..1)

                condition EdgeRule:
                    if tail = False
                    then head = False
            """).toString();

        testUtils.assertGeneratedContainsExpectedString(pythonString,
            "class Coin(BaseDataClass):");
        testUtils.assertGeneratedContainsExpectedString(pythonString,
            "return if_cond_fn(rune_all_elements(rune_resolve_attr(self, \"tail\"), \"=\", False), _then_fn0, _else_fn0)");
    }

    /**
     * Test case for conditions with count statements.
     */
    @Test
    public void testCountCondition() {
        String pythonString = testUtils.generatePythonFromString(
            """
            type CondTest:
                multiAttr number (1..*)

                condition:
                    multiAttr count >= 0
            """).toString();

        testUtils.assertGeneratedContainsExpectedString(pythonString,
            "class CondTest(BaseDataClass):");
        testUtils.assertGeneratedContainsExpectedString(pythonString,
            "return rune_all_elements((lambda items: sum(1 for x in (items if (hasattr(items, '__iter__') and not isinstance(items, (str, dict, bytes, bytearray))) else ([items] if items is not None else [])) if x is not None))(rune_resolve_attr(self, \"multiAttr\")), \">=\", 0)");
    }

    /**
     * Bar extends Foo (one-way inheritance); both standalone.
     */
    @Test
    public void testConditionReferencingInheritedAttribute() {
        String pythonString = testUtils.generatePythonFromString(
            """
            type Foo:
                x string (0..1)
                y string (0..1)

                condition:
                    x exists

            type Bar extends Foo:
                z string (0..1)

                condition:
                    y exists
            """).toString();

        testUtils.assertGeneratedContainsExpectedString(pythonString,
            "class Foo(BaseDataClass):");
        testUtils.assertGeneratedContainsExpectedString(pythonString,
            "class Bar(Foo):");
        testUtils.assertGeneratedContainsExpectedString(pythonString,
            "return rune_attr_exists(rune_resolve_attr(self, \"y\"))");
    }

    /**
     * Test case for conditions with inherited attribute statements.
     */
    @Test
    public void testConditionOnInheritedAttribute() {
        String pythonString = testUtils.generatePythonFromString(
            """
            type Foo:
                x string (0..1)
                y string (0..1)

                condition:
                    x exists

            type Bar extends Foo:
                z string (0..1)

                condition:
                    y exists
            """).toString();

        testUtils.assertGeneratedContainsExpectedString(pythonString,
            "class Foo(BaseDataClass):");
        testUtils.assertGeneratedContainsExpectedString(pythonString,
            "class Bar(Foo):");
        testUtils.assertGeneratedContainsExpectedString(pythonString,
            "return rune_attr_exists(rune_resolve_attr(self, \"y\"))");
    }

    // -----------------------------------------------------------------------
    // Methods from PythonObjectConditionGeneratorTest (unchanged names)
    // -----------------------------------------------------------------------

    /**
     * Test case for conditions.
     */
    @Test
    public void testConditions1() {
        testUtils.assertBundleContainsExpectedString(
                """
                type A:
                    a0 string (0..1)
                    a1 string (0..1)

                    condition C1:
                        a0 exists or a1 exists
                """,
                """
                class A(BaseDataClass):
                    a0: Optional[str] = Field(None, description='')
                    a1: Optional[str] = Field(None, description='')

                    @rune_condition
                    def condition_0_C1(self):
                        item = self
                        return (rune_attr_exists(rune_resolve_attr(self, "a0")) or rune_attr_exists(rune_resolve_attr(self, "a1")))""");
    }

    @Test
    public void testGenerateTypesChoiceCondition() {
        String pythonString = testUtils.generatePythonFromString(
                """
                type TestType: <"Test type description.">
                    testTypeValue1 string (0..1) <"Test string">
                    testTypeValue2 string (0..1) <"Test optional string">

                    condition TestChoice: <"Test choice description.">
                        optional choice testTypeValue1, testTypeValue2
                """)
                .toString();
        testUtils.assertGeneratedContainsExpectedString(
                pythonString,
                """
                class TestType(BaseDataClass):
                    \"""
                    Test type description.
                    \"""
                    testTypeValue1: Optional[str] = Field(None, description='Test string')
                    \"""
                    Test string
                    \"""
                    testTypeValue2: Optional[str] = Field(None, description='Test optional string')
                    \"""
                    Test optional string
                    \"""

                    @rune_condition
                    def condition_0_TestChoice(self):
                        \"""
                        Test choice description.
                        \"""
                        item = self
                        return rune_check_one_of(self, 'testTypeValue1', 'testTypeValue2', necessity=False)
                """);
    }

    @Test
    public void testGenerateIfThenCondition() {
        testUtils.assertBundleContainsExpectedString(
                """
                type AttributeIfThenTest:
                    attr1 string (0..1)
                    attr2 string (0..1)

                    condition TestIfThen:
                        if attr1 exists
                        then attr2 exists
                """,
                """
                class AttributeIfThenTest(BaseDataClass):
                    attr1: Optional[str] = Field(None, description='')
                    attr2: Optional[str] = Field(None, description='')

                    @rune_condition
                    def condition_0_TestIfThen(self):
                        item = self
                        def _then_fn0():
                            return rune_attr_exists(rune_resolve_attr(self, "attr2"))

                        def _else_fn0():
                            return True

                        return if_cond_fn(rune_attr_exists(rune_resolve_attr(self, "attr1")), _then_fn0, _else_fn0)""");
    }

    @Test
    public void testConditionsGeneration() {
        String pythonString = testUtils.generatePythonFromString(
                """
                type A:
                    a0 int (0..1)
                    a1 int (0..1)
                    condition: one-of
                type B:
                    intValue1 int (0..1)
                    intValue2 int (0..1)
                    aValue A (1..1)
                    condition Rule:
                        intValue1 < 100
                    condition OneOrTwo: <"Choice rule to represent an FpML choice construct.">
                        optional choice intValue1, intValue2
                    condition ReqOneOrTwo: <"Choice rule to represent an FpML choice construct.">
                        required choice intValue1, intValue2
                    condition SecondOneOrTwo: <"FpML specifies a choice between adjustedDate and [unadjustedDate (required), dateAdjutsments (required), adjustedDate (optional)].">
                        aValue->a0 exists
                            or (intValue2 exists and intValue1 exists and intValue1 exists)
                            or (intValue2 exists and intValue1 exists and intValue1 is absent)
                        """)
                .toString();
        testUtils.assertGeneratedContainsExpectedString(
                pythonString,
                """
                class A(BaseDataClass):
                    a0: Optional[int] = Field(None, description='')
                    a1: Optional[int] = Field(None, description='')

                    @rune_condition
                    def condition_0_(self):
                        item = self
                        return rune_check_one_of(self, 'a0', 'a1', necessity=True)
                """);
        testUtils.assertGeneratedContainsExpectedString(
                pythonString,
                """
                class B(BaseDataClass):
                    intValue1: Optional[int] = Field(None, description='')
                    intValue2: Optional[int] = Field(None, description='')
                    aValue: A = Field(..., description='')

                    @rune_condition
                    def condition_0_Rule(self):
                        item = self
                        return rune_all_elements(rune_resolve_attr(self, "intValue1"), "<", 100)

                    @rune_condition
                    def condition_1_OneOrTwo(self):
                        \"""
                        Choice rule to represent an FpML choice construct.
                        \"""
                        item = self
                        return rune_check_one_of(self, 'intValue1', 'intValue2', necessity=False)

                    @rune_condition
                    def condition_2_ReqOneOrTwo(self):
                        \"""
                        Choice rule to represent an FpML choice construct.
                        \"""
                        item = self
                        return rune_check_one_of(self, 'intValue1', 'intValue2', necessity=True)

                    @rune_condition
                    def condition_3_SecondOneOrTwo(self):
                        \"""
                        FpML specifies a choice between adjustedDate and [unadjustedDate (required), dateAdjutsments (required), adjustedDate (optional)].
                        \"""
                        item = self
                        return ((rune_attr_exists(rune_resolve_attr(rune_resolve_attr(self, "aValue"), "a0")) or ((rune_attr_exists(rune_resolve_attr(self, "intValue2")) and rune_attr_exists(rune_resolve_attr(self, "intValue1"))) and rune_attr_exists(rune_resolve_attr(self, "intValue1")))) or ((rune_attr_exists(rune_resolve_attr(self, "intValue2")) and rune_attr_exists(rune_resolve_attr(self, "intValue1"))) and (not rune_attr_exists(rune_resolve_attr(self, "intValue1")))))
                """);
    }

    @Test
    public void testGenerateIfThenElseCondition() {
        testUtils.assertBundleContainsExpectedString(
                """
                type AttributeIfThenElseTest:
                    attr1 string (0..1)
                    attr2 string (0..1)
                    attr3 string (0..1)

                    condition TestIfThenElse:
                        if attr1 exists
                        then attr2 exists
                        else attr3 exists
                """,
                """
                class AttributeIfThenElseTest(BaseDataClass):
                    attr1: Optional[str] = Field(None, description='')
                    attr2: Optional[str] = Field(None, description='')
                    attr3: Optional[str] = Field(None, description='')

                    @rune_condition
                    def condition_0_TestIfThenElse(self):
                        item = self
                        def _then_fn0():
                            return rune_attr_exists(rune_resolve_attr(self, "attr2"))

                        def _else_fn0():
                            return rune_attr_exists(rune_resolve_attr(self, "attr3"))

                        return if_cond_fn(rune_attr_exists(rune_resolve_attr(self, "attr1")), _then_fn0, _else_fn0)""");
    }

    @Test
    public void testConditionLessOrEqual() {
        testUtils.assertBundleContainsExpectedString(
                """
                type Foo:
                    a number (0..1)
                    b number (0..1)

                    condition:
                        a <= b
                """,
                """
                class Foo(BaseDataClass):
                    a: Optional[Decimal] = Field(None, description='')
                    b: Optional[Decimal] = Field(None, description='')

                    @rune_condition
                    def condition_0_(self):
                        item = self
                        return rune_all_elements(rune_resolve_attr(self, "a"), "<=", rune_resolve_attr(self, "b"))""");
    }

    // -----------------------------------------------------------------------
    // Method from PythonChoiceGeneratorTest (renamed)
    // -----------------------------------------------------------------------

    /**
     * Test case for generating one-of choice condition.
     * Choice is acyclic — standalone. The class is written directly to
     * Choice.py; there is no proxy stub and no bundle entry.
     */
    @Test
    public void testOneOfChoiceCondition() {
        Map<String, CharSequence> python = testUtils.generatePythonFromString(
            """
            namespace test.generated_syntax.semantic : <"generate Python unit tests from Rosetta.">

            type Choice:
                intType int (0..1)
                stringType string (0..1)
                condition Choice: one-of
            """);

        // Standalone file contains the class directly (not a proxy stub)
        String choicePython = python.get("src/test/generated_syntax/semantic/Choice.py").toString();
        String expectedChoice = """
            class Choice(BaseDataClass):
                intType: Optional[int] = Field(None, description='')
                stringType: Optional[str] = Field(None, description='')

                @rune_condition
                def condition_0_Choice(self):
                    item = self
                    return rune_check_one_of(self, 'intType', 'stringType', necessity=True)
            """;
        testUtils.assertGeneratedContainsExpectedString(choicePython, expectedChoice);
    }
}
