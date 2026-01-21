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

        String expectedFoo = """
                class com_rosetta_test_model_Foo(BaseDataClass):
                    _FQRTN = 'com.rosetta.test.model.Foo'
                    bar: Optional[str] = Field(None, description='')
                    baz: Optional[str] = Field(None, description='')
                \s\s\s\s
                    @rune_condition
                    def condition_0_(self):
                        item = self
                        def _then_fn1():
                            return (not rune_attr_exists(rune_resolve_attr(self, "baz")))
                \s\s\s\s\s\s\s\s
                        def _else_fn1():
                            return True
                \s\s\s\s\s\s\s\s
                        def _then_fn0():
                            return rune_attr_exists(rune_resolve_attr(self, "baz"))
                \s\s\s\s\s\s\s\s
                        def _else_fn0():
                            return if_cond_fn((rune_all_elements(rune_resolve_attr(self, "bar"), "=", "I") or rune_all_elements(rune_resolve_attr(self, "bar"), "=", "N")), _then_fn1, _else_fn1)
                \s\s\s\s\s\s\s\s
                        return if_cond_fn(rune_all_elements(rune_resolve_attr(self, "bar"), "=", "Y"), _then_fn0, _else_fn0)""";
        testUtils.assertGeneratedContainsExpectedString(pythonString, expectedFoo);
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

        String expectedFoo = """
                class com_rosetta_test_model_Foo(BaseDataClass):
                    _FQRTN = 'com.rosetta.test.model.Foo'
                    bar: Optional[str] = Field(None, description='')
                    baz: Optional[str] = Field(None, description='')
                \s\s\s\s
                    @rune_condition
                    def condition_0_(self):
                        item = self
                        def _then_fn2():
                            return (not rune_attr_exists(rune_resolve_attr(self, "baz")))
                \s\s\s\s\s\s\s\s
                        def _else_fn2():
                            return True
                \s\s\s\s\s\s\s\s
                        def _then_fn1():
                            return rune_attr_exists(rune_resolve_attr(self, "baz"))
                \s\s\s\s\s\s\s\s
                        def _else_fn1():
                            return if_cond_fn((rune_all_elements(rune_resolve_attr(self, "bar"), "=", "I") or rune_all_elements(rune_resolve_attr(self, "bar"), "=", "N")), _then_fn2, _else_fn2)
                \s\s\s\s\s\s\s\s
                        def _then_fn0():
                            return if_cond_fn(rune_all_elements(rune_resolve_attr(self, "bar"), "=", "Y"), _then_fn1, _else_fn1)
                \s\s\s\s\s\s\s\s
                        def _else_fn0():
                            return True
                \s\s\s\s\s\s\s\s
                        return if_cond_fn(rune_attr_exists(rune_resolve_attr(self, "bar")), _then_fn0, _else_fn0)""";
        testUtils.assertGeneratedContainsExpectedString(pythonString, expectedFoo);
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

        String expectedQuote = """
                class com_rosetta_test_model_Quote(BaseDataClass):
                    _FQRTN = 'com.rosetta.test.model.Quote'
                    quotePrice: Optional[Annotated[com_rosetta_test_model_QuotePrice, com_rosetta_test_model_QuotePrice.serializer(), com_rosetta_test_model_QuotePrice.validator()]] = Field(None, description='')
                \s\s\s\s
                    @rune_condition
                    def condition_0_Quote_Price(self):
                        item = self
                        def _then_fn0():
                            return (rune_attr_exists(rune_resolve_attr(rune_resolve_attr(self, "quotePrice"), "bidPrice")) or rune_attr_exists(rune_resolve_attr(rune_resolve_attr(self, "quotePrice"), "offerPrice")))
                \s\s\s\s\s\s\s\s
                        def _else_fn0():
                            return True
                \s\s\s\s\s\s\s\s
                        return if_cond_fn(rune_attr_exists(rune_resolve_attr(self, "quotePrice")), _then_fn0, _else_fn0)""";

        String expectedQuotePrice = """
                class com_rosetta_test_model_QuotePrice(BaseDataClass):
                    _FQRTN = 'com.rosetta.test.model.QuotePrice'
                    bidPrice: Optional[Decimal] = Field(None, description='')
                    offerPrice: Optional[Decimal] = Field(None, description='')""";

        testUtils.assertGeneratedContainsExpectedString(pythonString, expectedQuote);
        testUtils.assertGeneratedContainsExpectedString(pythonString, expectedQuotePrice);
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
                        \s
                        type QuotePrice:
                            price1 number (0..1)
                            price2 number (0..1)
                            price3 number (0..1)

                        """).toString();

        String expectedQuote = """
                class com_rosetta_test_model_Quote(BaseDataClass):
                    _FQRTN = 'com.rosetta.test.model.Quote'
                    quotePrice: Optional[Annotated[com_rosetta_test_model_QuotePrice, com_rosetta_test_model_QuotePrice.serializer(), com_rosetta_test_model_QuotePrice.validator()]] = Field(None, description='')
                \s\s\s\s
                    @rune_condition
                    def condition_0_Quote_Price(self):
                        item = self
                        def _then_fn0():
                            return ((rune_attr_exists(rune_resolve_attr(rune_resolve_attr(self, "quotePrice"), "price1")) and rune_attr_exists(rune_resolve_attr(rune_resolve_attr(self, "quotePrice"), "price2"))) and rune_attr_exists(rune_resolve_attr(rune_resolve_attr(self, "quotePrice"), "price3")))
                \s\s\s\s\s\s\s\s
                        def _else_fn0():
                            return True
                \s\s\s\s\s\s\s\s
                        return if_cond_fn(rune_attr_exists(rune_resolve_attr(self, "quotePrice")), _then_fn0, _else_fn0)""";
        String expectedQuotePrice = """
                class com_rosetta_test_model_QuotePrice(BaseDataClass):
                    _FQRTN = 'com.rosetta.test.model.QuotePrice'
                    price1: Optional[Decimal] = Field(None, description='')
                    price2: Optional[Decimal] = Field(None, description='')
                    price3: Optional[Decimal] = Field(None, description='')""";

        testUtils.assertGeneratedContainsExpectedString(pythonString, expectedQuote);
        testUtils.assertGeneratedContainsExpectedString(pythonString, expectedQuotePrice);
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
                        \s
                        type QuotePrice:
                            bidPrice number (0..1)
                        """).toString();

        String expectedQuote = """
                class com_rosetta_test_model_Quote(BaseDataClass):
                    _FQRTN = 'com.rosetta.test.model.Quote'
                    quotePrice: Optional[Annotated[com_rosetta_test_model_QuotePrice, com_rosetta_test_model_QuotePrice.serializer(), com_rosetta_test_model_QuotePrice.validator()]] = Field(None, description='')
                \s\s\s\s
                    @rune_condition
                    def condition_0_Quote_Price(self):
                        item = self
                        def _then_fn0():
                            return rune_all_elements(rune_resolve_attr(rune_resolve_attr(self, "quotePrice"), "bidPrice"), "=", 0.0)
                \s\s\s\s\s\s\s\s
                        def _else_fn0():
                            return True
                \s\s\s\s\s\s\s\s
                        return if_cond_fn(rune_attr_exists(rune_resolve_attr(self, "quotePrice")), _then_fn0, _else_fn0)""";

        String expectedQuotePrice = """
                class com_rosetta_test_model_QuotePrice(BaseDataClass):
                    _FQRTN = 'com.rosetta.test.model.QuotePrice'
                    bidPrice: Optional[Decimal] = Field(None, description='')""";

        testUtils.assertGeneratedContainsExpectedString(pythonString, expectedQuote);
        testUtils.assertGeneratedContainsExpectedString(pythonString, expectedQuotePrice);
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
                        \s
                        type Quote:
                            price number (0..1)
                            \s
                            condition:
                                if price exists
                                then Foo( price ) = 5.0
                        """).toString();

        String expectedQuote = """
                class com_rosetta_test_model_Quote(BaseDataClass):
                    _FQRTN = 'com.rosetta.test.model.Quote'
                    price: Optional[Decimal] = Field(None, description='')
                \s\s\s\s
                    @rune_condition
                    def condition_0_(self):
                        item = self
                        def _then_fn0():
                            return rune_all_elements(Foo(rune_resolve_attr(self, "price")), "=", 5.0)
                \s\s\s\s\s\s\s\s
                        def _else_fn0():
                            return True
                \s\s\s\s\s\s\s\s
                        return if_cond_fn(rune_attr_exists(rune_resolve_attr(self, "price")), _then_fn0, _else_fn0)""";

        testUtils.assertGeneratedContainsExpectedString(pythonString, expectedQuote);
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
                        \s
                        type Quote:
                            price number (0..1)
                        \s
                            condition:
                                if price exists
                                then Foo( price ) = 5.0
                                else True
                        """).toString();

        String expectedQuote = """
                class com_rosetta_test_model_Quote(BaseDataClass):
                    _FQRTN = 'com.rosetta.test.model.Quote'
                    price: Optional[Decimal] = Field(None, description='')
                \s\s\s\s
                    @rune_condition
                    def condition_0_(self):
                        item = self
                        def _then_fn0():
                            return rune_all_elements(Foo(rune_resolve_attr(self, "price")), "=", 5.0)
                \s\s\s\s\s\s\s\s
                        def _else_fn0():
                            return True
                \s\s\s\s\s\s\s\s
                        return if_cond_fn(rune_attr_exists(rune_resolve_attr(self, "price")), _then_fn0, _else_fn0)""";

        testUtils.assertGeneratedContainsExpectedString(pythonString, expectedQuote);
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

        String expected = """
                class com_rosetta_test_model_Coin(BaseDataClass):
                    _FQRTN = 'com.rosetta.test.model.Coin'
                    head: Optional[bool] = Field(None, description='')
                    tail: Optional[bool] = Field(None, description='')
                \s\s\s\s
                    @rune_condition
                    def condition_0_CoinHeadRule(self):
                        item = self
                        def _then_fn0():
                            return rune_all_elements(rune_resolve_attr(self, "tail"), "=", False)
                \s\s\s\s\s\s\s\s
                        def _else_fn0():
                            return True
                \s\s\s\s\s\s\s\s
                        return if_cond_fn(rune_all_elements(rune_resolve_attr(self, "head"), "=", True), _then_fn0, _else_fn0)""";

        testUtils.assertGeneratedContainsExpectedString(pythonString, expected);
    }

    @Test
    public void dataRuleCoinTail() {
        String pythonString = testUtils.generatePythonFromString(
                """
                        type Coin:
                            head boolean (0..1)
                            tail boolean (0..1)
                        \s
                            condition CoinTailRule:
                                if tail = True
                                then head = False
                        """).toString();

        String expected = """
                class com_rosetta_test_model_Coin(BaseDataClass):
                    _FQRTN = 'com.rosetta.test.model.Coin'
                    head: Optional[bool] = Field(None, description='')
                    tail: Optional[bool] = Field(None, description='')
                \s\s\s\s
                    @rune_condition
                    def condition_0_CoinTailRule(self):
                        item = self
                        def _then_fn0():
                            return rune_all_elements(rune_resolve_attr(self, "head"), "=", False)
                \s\s\s\s\s\s\s\s
                        def _else_fn0():
                            return True
                \s\s\s\s\s\s\s\s
                        return if_cond_fn(rune_all_elements(rune_resolve_attr(self, "tail"), "=", True), _then_fn0, _else_fn0)""";

        testUtils.assertGeneratedContainsExpectedString(pythonString, expected);
    }

    @Test
    public void dataRuleCoinEdge() {
        String pythonString = testUtils.generatePythonFromString(
                """
                        type Coin:
                            head boolean (0..1)
                            tail boolean (0..1)
                            \s
                            condition EdgeRule:
                                if tail = False
                                then head = False
                        """).toString();

        String expected = """
                class com_rosetta_test_model_Coin(BaseDataClass):
                    _FQRTN = 'com.rosetta.test.model.Coin'
                    head: Optional[bool] = Field(None, description='')
                    tail: Optional[bool] = Field(None, description='')
                \s\s\s\s
                    @rune_condition
                    def condition_0_EdgeRule(self):
                        item = self
                        def _then_fn0():
                            return rune_all_elements(rune_resolve_attr(self, "head"), "=", False)
                \s\s\s\s\s\s\s\s
                        def _else_fn0():
                            return True
                \s\s\s\s\s\s\s\s
                        return if_cond_fn(rune_all_elements(rune_resolve_attr(self, "tail"), "=", False), _then_fn0, _else_fn0)""";

        testUtils.assertGeneratedContainsExpectedString(pythonString, expected);
    }

    @Test
    public void conditionCount() {
        String pythonString = testUtils.generatePythonFromString(
                """
                        type CondTest:
                            multiAttr number (1..*)
                        \s
                            condition:
                                multiAttr count >= 0
                        """).toString();

        String expected = """
                class com_rosetta_test_model_CondTest(BaseDataClass):
                    _FQRTN = 'com.rosetta.test.model.CondTest'
                    multiAttr: list[Decimal] = Field(..., description='', min_length=1)
                \s\s\s\s
                    @rune_condition
                    def condition_0_(self):
                        item = self
                        return rune_all_elements(rune_count(rune_resolve_attr(self, "multiAttr")), ">=", 0)""";
        testUtils.assertGeneratedContainsExpectedString(pythonString, expected);
    }

    @Test
    public void checkConditionWithInheritedAttribute() {
        String pythonString = testUtils.generatePythonFromString(
                """
                        type Foo:
                            x string (0..1)
                            y string (0..1)
                        \s
                            condition:
                                x exists
                        \s
                        type Bar extends Foo:
                            z string (0..1)
                        \s
                            condition:
                                y exists
                        """).toString();

        String expectedFoo = """
                class com_rosetta_test_model_Foo(BaseDataClass):
                    _FQRTN = 'com.rosetta.test.model.Foo'
                    x: Optional[str] = Field(None, description='')
                    y: Optional[str] = Field(None, description='')
                \s\s\s\s
                    @rune_condition
                    def condition_0_(self):
                        item = self
                        return rune_attr_exists(rune_resolve_attr(self, "x"))""";

        String expectedBar = """
                class com_rosetta_test_model_Bar(com_rosetta_test_model_Foo):
                    _FQRTN = 'com.rosetta.test.model.Bar'
                    z: Optional[str] = Field(None, description='')
                \s\s\s\s
                    @rune_condition
                    def condition_0_(self):
                        item = self
                        return rune_attr_exists(rune_resolve_attr(self, "y"))""";

        testUtils.assertGeneratedContainsExpectedString(pythonString, expectedFoo);
        testUtils.assertGeneratedContainsExpectedString(pythonString, expectedBar);
    }

    @Test
    public void shouldCheckInheritedCondition() {
        String pythonString = testUtils.generatePythonFromString(
                """
                        type Foo:
                            x string (0..1)
                            y string (0..1)
                        \s
                            condition:
                                x exists
                        \s
                        type Bar extends Foo:
                            z string (0..1)
                        \s
                            condition:
                                y exists
                        """).toString();

        String expectedFoo = """
                class com_rosetta_test_model_Foo(BaseDataClass):
                    _FQRTN = 'com.rosetta.test.model.Foo'
                    x: Optional[str] = Field(None, description='')
                    y: Optional[str] = Field(None, description='')
                \s\s\s\s
                    @rune_condition
                    def condition_0_(self):
                        item = self
                        return rune_attr_exists(rune_resolve_attr(self, "x"))""";

        String expectedBar = """
                class com_rosetta_test_model_Bar(com_rosetta_test_model_Foo):
                    _FQRTN = 'com.rosetta.test.model.Bar'
                    z: Optional[str] = Field(None, description='')
                \s\s\s\s
                    @rune_condition
                    def condition_0_(self):
                        item = self
                        return rune_attr_exists(rune_resolve_attr(self, "y"))""";

        testUtils.assertGeneratedContainsExpectedString(pythonString, expectedFoo);
        testUtils.assertGeneratedContainsExpectedString(pythonString, expectedBar);
    }
}
