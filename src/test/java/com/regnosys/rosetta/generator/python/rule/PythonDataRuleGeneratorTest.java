package com.regnosys.rosetta.generator.python.rule;

import jakarta.inject.Inject;
import com.regnosys.rosetta.tests.RosettaInjectorProvider;
import com.regnosys.rosetta.generator.python.PythonGeneratorTestUtils;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(InjectionExtension.class)
@InjectWith(RosettaInjectorProvider.class)
public class PythonDataRuleGeneratorTest {

    @Inject
    private PythonGeneratorTestUtils testUtils;

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

        assertTrue(pythonString.contains("class com_rosetta_test_model_Foo(BaseDataClass):"), "Class Foo definition");
        assertTrue(pythonString.contains("bar: Optional[str] = Field(None, description='')"), "Field bar");
        assertTrue(pythonString.contains("baz: Optional[str] = Field(None, description='')"), "Field baz");
        assertTrue(pythonString.contains("@rune_condition"), "Condition annotation");
        assertTrue(pythonString.contains("def condition_0_(self):"), "Condition method");
        assertTrue(pythonString.contains(
                "return if_cond_fn(rune_all_elements(rune_resolve_attr(self, \"bar\"), \"=\", \"Y\"), _then_fn0, _else_fn0)"),
                "Condition return logic");
    }

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

        assertTrue(pythonString.contains("class com_rosetta_test_model_Foo(BaseDataClass):"), "Class Foo definition");
        assertTrue(pythonString.contains("def condition_0_(self):"), "Condition method");
        assertTrue(
                pythonString.contains(
                        "return if_cond_fn(rune_attr_exists(rune_resolve_attr(self, \"bar\")), _then_fn0, _else_fn0)"),
                "Nested condition return logic");
    }

    @Test
    public void quoteExists() {
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
                        """).toString();

        // Phase 1: Clean Body
        assertTrue(pythonString.contains("class com_rosetta_test_model_Quote(BaseDataClass):"),
                "Class Quote definition");
        assertTrue(
                pythonString.contains(
                        "quotePrice: Optional[com_rosetta_test_model_QuotePrice] = Field(None, description='')"),
                "Field Quote.quotePrice (clean)");

        assertTrue(pythonString.contains("class com_rosetta_test_model_QuotePrice(BaseDataClass):"),
                "Class QuotePrice definition");
        assertTrue(pythonString.contains("bidPrice: Optional[Decimal] = Field(None, description='')"),
                "Field bidPrice");

        // Phase 2: Delayed Update
        assertTrue(pythonString.contains(
                "com_rosetta_test_model_Quote.__annotations__[\"quotePrice\"] = Optional[Annotated[com_rosetta_test_model_QuotePrice, com_rosetta_test_model_QuotePrice.serializer(), com_rosetta_test_model_QuotePrice.validator()]]"),
                "Delayed update for quotePrice");

        // Phase 3: Rebuild
        assertTrue(pythonString.contains("com_rosetta_test_model_Quote.model_rebuild()"), "Rebuild Quote");

        // Condition
        assertTrue(pythonString.contains("def condition_0_Quote_Price(self):"), "Condition Quote_Price");
    }

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

        assertTrue(pythonString.contains("class com_rosetta_test_model_Quote(BaseDataClass):"),
                "Class Quote definition");
        assertTrue(
                pythonString.contains(
                        "quotePrice: Optional[com_rosetta_test_model_QuotePrice] = Field(None, description='')"),
                "Field Quote.quotePrice (clean)");
        assertTrue(pythonString.contains(
                "com_rosetta_test_model_Quote.__annotations__[\"quotePrice\"] = Optional[Annotated[com_rosetta_test_model_QuotePrice, com_rosetta_test_model_QuotePrice.serializer(), com_rosetta_test_model_QuotePrice.validator()]]"),
                "Delayed update for quotePrice");
        assertTrue(pythonString.contains("def condition_0_Quote_Price(self):"), "Condition Quote_Price");
    }

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

        assertTrue(
                pythonString.contains(
                        "quotePrice: Optional[com_rosetta_test_model_QuotePrice] = Field(None, description='')"),
                "Field Quote.quotePrice (clean)");
        assertTrue(pythonString.contains(
                "com_rosetta_test_model_Quote.__annotations__[\"quotePrice\"] = Optional[Annotated[com_rosetta_test_model_QuotePrice, com_rosetta_test_model_QuotePrice.serializer(), com_rosetta_test_model_QuotePrice.validator()]]"),
                "Delayed update for quotePrice");
        assertTrue(pythonString.contains(
                "return rune_all_elements(rune_resolve_attr(rune_resolve_attr(self, \"quotePrice\"), \"bidPrice\"), \"=\", Decimal('0.0'))"),
                "Condition return logic");
    }

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

        assertTrue(pythonString.contains("class com_rosetta_test_model_Quote(BaseDataClass):"),
                "Class Quote definition");
        assertTrue(pythonString.contains(
                "return rune_all_elements(com_rosetta_test_model_functions_Foo(rune_resolve_attr(self, \"price\")), \"=\", Decimal('5.0'))"),
                "Function call in condition");
    }

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

        assertTrue(pythonString.contains("class com_rosetta_test_model_Quote(BaseDataClass):"),
                "Class Quote definition");
        assertTrue(pythonString.contains(
                "return if_cond_fn(rune_attr_exists(rune_resolve_attr(self, \"price\")), _then_fn0, _else_fn0)"),
                "Condition return logic");
    }

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

        assertTrue(pythonString.contains("class com_rosetta_test_model_Coin(BaseDataClass):"), "Class Coin definition");
        assertTrue(pythonString.contains(
                "return if_cond_fn(rune_all_elements(rune_resolve_attr(self, \"head\"), \"=\", True), _then_fn0, _else_fn0)"),
                "Condition return logic");
    }

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

        assertTrue(pythonString.contains("class com_rosetta_test_model_Coin(BaseDataClass):"), "Class Coin definition");
        assertTrue(pythonString.contains(
                "return if_cond_fn(rune_all_elements(rune_resolve_attr(self, \"tail\"), \"=\", True), _then_fn0, _else_fn0)"),
                "Condition return logic");
    }

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

        assertTrue(pythonString.contains("class com_rosetta_test_model_Coin(BaseDataClass):"), "Class Coin definition");
        assertTrue(pythonString.contains(
                "return if_cond_fn(rune_all_elements(rune_resolve_attr(self, \"tail\"), \"=\", False), _then_fn0, _else_fn0)"),
                "Condition return logic");
    }

    @Test
    public void conditionCount() {
        String pythonString = testUtils.generatePythonFromString(
                """
                        type CondTest:
                            multiAttr number (1..*)

                            condition:
                                multiAttr count >= 0
                        """).toString();

        assertTrue(pythonString.contains("class com_rosetta_test_model_CondTest(BaseDataClass):"),
                "Class CondTest definition");
        assertTrue(
                pythonString.contains(
                        "return rune_all_elements(rune_count(rune_resolve_attr(self, \"multiAttr\")), \">=\", 0)"),
                "Condition return logic");
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

        assertTrue(pythonString.contains("class com_rosetta_test_model_Foo(BaseDataClass):"), "Class Foo definition");
        assertTrue(pythonString.contains("class com_rosetta_test_model_Bar(com_rosetta_test_model_Foo):"),
                "Class Bar definition");
        assertTrue(pythonString.contains("return rune_attr_exists(rune_resolve_attr(self, \"y\"))"),
                "Condition on inherited attribute");
    }

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

        assertTrue(pythonString.contains("class com_rosetta_test_model_Foo(BaseDataClass):"), "Class Foo definition");
        assertTrue(pythonString.contains("class com_rosetta_test_model_Bar(com_rosetta_test_model_Foo):"),
                "Class Bar definition");
        assertTrue(pythonString.contains("return rune_attr_exists(rune_resolve_attr(self, \"y\"))"),
                "Condition on inherited attribute");
    }
}
