package com.regnosys.rosetta.generator.python.rule;

import jakarta.inject.Inject;
import com.regnosys.rosetta.tests.RosettaInjectorProvider;
import com.regnosys.rosetta.generator.python.PythonGeneratorTestUtils;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(InjectionExtension.class)
@InjectWith(RosettaInjectorProvider.class)
@SuppressWarnings("LineLength")
public class PythonDataRuleGeneratorTest {

    /**
     * PythonGeneratorTestUtils is used to generate Python code from Rosetta models.
     */
    @Inject
    private PythonGeneratorTestUtils testUtils;

    /**
     * Test case for conditions with if-else-if statements.
     */
    @Test
    public void shouldGenerateConditionWithIfElseIf() {
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
                "class com_rosetta_test_model_Foo(BaseDataClass):");
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
    public void shouldGenerateConditionWithNestedIfElseIf() {
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
                "class com_rosetta_test_model_Foo(BaseDataClass):");
        testUtils.assertGeneratedContainsExpectedString(pythonString, "def condition_0_(self):");
        testUtils.assertGeneratedContainsExpectedString(pythonString,
                "return if_cond_fn(rune_attr_exists(rune_resolve_attr(self, \"bar\")), _then_fn0, _else_fn0)");
    }

    /**
     * Test case for conditions with exists statements.
     */
    @Test
    public void testExists() {
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

        // Phase 1: Clean Body
        testUtils.assertGeneratedContainsExpectedString(pythonString,
                "class com_rosetta_test_model_Quote(BaseDataClass):");
        testUtils.assertGeneratedContainsExpectedString(pythonString,
                "quotePrice: Optional[com_rosetta_test_model_QuotePrice] = Field(None, description='')");

        testUtils.assertGeneratedContainsExpectedString(pythonString,
                "class com_rosetta_test_model_QuotePrice(BaseDataClass):");
        testUtils.assertGeneratedContainsExpectedString(pythonString,
                "bidPrice: Optional[Decimal] = Field(None, description='')");

        // Phase 2: Delayed Update
        testUtils.assertGeneratedContainsExpectedString(pythonString,
                "com_rosetta_test_model_Quote.__annotations__[\"quotePrice\"] = Annotated[Optional[com_rosetta_test_model_QuotePrice], com_rosetta_test_model_QuotePrice.serializer(), com_rosetta_test_model_QuotePrice.validator()]");

        // Phase 3: Rebuild
        testUtils.assertGeneratedContainsExpectedString(pythonString,
                "com_rosetta_test_model_Quote.model_rebuild()");

        // Condition
        testUtils.assertGeneratedContainsExpectedString(pythonString, "def condition_0_Quote_Price(self):");
    }

    /**
     * Test case for conditions with nested and statements.
     */
    @Test
    public void nestedAnds() {
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
                "class com_rosetta_test_model_Quote(BaseDataClass):");
        testUtils.assertGeneratedContainsExpectedString(pythonString,
                "quotePrice: Optional[com_rosetta_test_model_QuotePrice] = Field(None, description='')");
        testUtils.assertGeneratedContainsExpectedString(pythonString,
                "com_rosetta_test_model_Quote.__annotations__[\"quotePrice\"] = Annotated[Optional[com_rosetta_test_model_QuotePrice], com_rosetta_test_model_QuotePrice.serializer(), com_rosetta_test_model_QuotePrice.validator()]");
        testUtils.assertGeneratedContainsExpectedString(pythonString, "def condition_0_Quote_Price(self):");
    }

    /**
     * Test case for conditions with number attribute statements.
     */
    @Test
    public void numberAttributeisHandled() {
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
                "quotePrice: Optional[com_rosetta_test_model_QuotePrice] = Field(None, description='')");
        testUtils.assertGeneratedContainsExpectedString(pythonString,
                "com_rosetta_test_model_Quote.__annotations__[\"quotePrice\"] = Annotated[Optional[com_rosetta_test_model_QuotePrice], com_rosetta_test_model_QuotePrice.serializer(), com_rosetta_test_model_QuotePrice.validator()]");
        testUtils.assertGeneratedContainsExpectedString(pythonString,
                "return rune_all_elements(rune_resolve_attr(rune_resolve_attr(self, \"quotePrice\"), \"bidPrice\"), \"=\", Decimal('0.0'))");
    }

    /**
     * Test case for conditions with function statements.
     */
    @Test
    public void dataRuleWithDoIfAndFunction() {
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
                "class com_rosetta_test_model_Quote(BaseDataClass):");
        testUtils.assertGeneratedContainsExpectedString(pythonString,
                "return rune_all_elements(com_rosetta_test_model_functions_Foo(rune_resolve_attr(self, \"price\")), \"=\", Decimal('5.0'))");
    }

    /**
     * Test case for conditions with function statements and else.
     */
    @Test
    public void dataRuleWithDoIfAndFunctionAndElse() {
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
                "class com_rosetta_test_model_Quote(BaseDataClass):");
        testUtils.assertGeneratedContainsExpectedString(pythonString,
                "return if_cond_fn(rune_attr_exists(rune_resolve_attr(self, \"price\")), _then_fn0, _else_fn0)");
    }

    /**
     * Test case for conditions with boolean attribute statements.
     */
    @Test
    public void dataRuleCoinHead() {
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
                "class com_rosetta_test_model_Coin(BaseDataClass):");
        testUtils.assertGeneratedContainsExpectedString(pythonString,
                "return if_cond_fn(rune_all_elements(rune_resolve_attr(self, \"head\"), \"=\", True), _then_fn0, _else_fn0)");
    }

    /**
     * Test case for conditions with boolean attribute statements and else.
     */
    @Test
    public void dataRuleCoinTail() {
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
                "class com_rosetta_test_model_Coin(BaseDataClass):");
        testUtils.assertGeneratedContainsExpectedString(pythonString,
                "return if_cond_fn(rune_all_elements(rune_resolve_attr(self, \"tail\"), \"=\", True), _then_fn0, _else_fn0)");
    }

    /**
     * Test case for conditions with boolean attribute statements and else.
     */
    @Test
    public void dataRuleCoinEdge() {
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
                "class com_rosetta_test_model_Coin(BaseDataClass):");
        testUtils.assertGeneratedContainsExpectedString(pythonString,
                "return if_cond_fn(rune_all_elements(rune_resolve_attr(self, \"tail\"), \"=\", False), _then_fn0, _else_fn0)");
    }

    /**
     * Test case for conditions with count statements.
     */
    @Test
    public void conditionCount() {
        String pythonString = testUtils.generatePythonFromString(
                """
                        type CondTest:
                            multiAttr number (1..*)

                            condition:
                                multiAttr count >= 0
                        """).toString();

        testUtils.assertGeneratedContainsExpectedString(pythonString,
                "class com_rosetta_test_model_CondTest(BaseDataClass):");
        testUtils.assertGeneratedContainsExpectedString(pythonString,
                "return rune_all_elements(rune_count(rune_resolve_attr(self, \"multiAttr\")), \">=\", 0)");
    }

    @Test
    public void checkConditionWithInheritedAttribute() {
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
                "class com_rosetta_test_model_Foo(BaseDataClass):");
        testUtils.assertGeneratedContainsExpectedString(pythonString,
                "class com_rosetta_test_model_Bar(com_rosetta_test_model_Foo):");
        testUtils.assertGeneratedContainsExpectedString(pythonString,
                "return rune_attr_exists(rune_resolve_attr(self, \"y\"))");
    }

    /**
     * Test case for conditions with inherited attribute statements.
     */
    @Test
    public void shouldCheckInheritedCondition() {
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
                "class com_rosetta_test_model_Foo(BaseDataClass):");
        testUtils.assertGeneratedContainsExpectedString(pythonString,
                "class com_rosetta_test_model_Bar(com_rosetta_test_model_Foo):");
        testUtils.assertGeneratedContainsExpectedString(pythonString,
                "return rune_attr_exists(rune_resolve_attr(self, \"y\"))");
    }
}
