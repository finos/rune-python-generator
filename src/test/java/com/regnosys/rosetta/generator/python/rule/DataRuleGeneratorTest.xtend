package com.regnosys.rosetta.generator.python.rule

import com.google.inject.Inject
import com.regnosys.rosetta.generator.python.PythonCodeGenerator
import com.regnosys.rosetta.tests.RosettaInjectorProvider
import com.regnosys.rosetta.tests.util.ModelHelper
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.extensions.InjectionExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.^extension.ExtendWith
import static org.junit.jupiter.api.Assertions.*

@ExtendWith(InjectionExtension)
@InjectWith(RosettaInjectorProvider)

class DataRuleGeneratorTest {

    @Inject extension ModelHelper
    @Inject PythonCodeGenerator generator;

    @Test
    def void shouldGenerateConditionWithIfElseIf() {
        val python = '''
            type Foo:
                bar string (0..1)
                baz string (0..1)

                condition:
                    if bar="Y" then baz exists
                    else if (bar="I" or bar="N") then baz is absent
        '''.generatePython

        val expected=
        '''
        class Foo(BaseDataClass):
            bar: Optional[str] = Field(None, description='')
            baz: Optional[str] = Field(None, description='')
            
            @rune_condition
            def condition_0_(self):
                item = self
                def _then_fn1():
                    return (not rune_attr_exists(rune_resolve_attr(self, "baz")))
                
                def _else_fn1():
                    return True
                
                def _then_fn0():
                    return rune_attr_exists(rune_resolve_attr(self, "baz"))
                
                def _else_fn0():
                    return if_cond_fn((rune_all_elements(rune_resolve_attr(self, "bar"), "=", "I") or rune_all_elements(rune_resolve_attr(self, "bar"), "=", "N")), _then_fn1, _else_fn1)
                
                return if_cond_fn(rune_all_elements(rune_resolve_attr(self, "bar"), "=", "Y"), _then_fn0, _else_fn0)
        '''
        
        assertTrue(python.toString.contains(expected))
    }

    @Test
    def void shouldGenerateConditionWithNestedIfElseIf() {
        val python = '''
            type Foo:
                bar string (0..1)
                baz string (0..1)

                condition:
                    if bar exists then
                        if bar="Y" then baz exists
                        else if (bar="I" or bar="N") then baz is absent
        '''.generatePython
        
        val expected=
        '''
        class Foo(BaseDataClass):
            bar: Optional[str] = Field(None, description='')
            baz: Optional[str] = Field(None, description='')
            
            @rune_condition
            def condition_0_(self):
                item = self
                def _then_fn2():
                    return (not rune_attr_exists(rune_resolve_attr(self, "baz")))
                
                def _else_fn2():
                    return True
                
                def _then_fn1():
                    return rune_attr_exists(rune_resolve_attr(self, "baz"))
                
                def _else_fn1():
                    return if_cond_fn((rune_all_elements(rune_resolve_attr(self, "bar"), "=", "I") or rune_all_elements(rune_resolve_attr(self, "bar"), "=", "N")), _then_fn2, _else_fn2)
                
                def _then_fn0():
                    return if_cond_fn(rune_all_elements(rune_resolve_attr(self, "bar"), "=", "Y"), _then_fn1, _else_fn1)
                
                def _else_fn0():
                    return True
                
                return if_cond_fn(rune_attr_exists(rune_resolve_attr(self, "bar")), _then_fn0, _else_fn0)
        '''
        assertTrue(python.toString.contains(expected))
    }

    @Test
    def void quoteExists() {
        val python = '''
                type Quote:
                    quotePrice QuotePrice (0..1)
                    condition Quote_Price:
                        if quotePrice exists
                        then quotePrice -> bidPrice exists or quotePrice -> offerPrice exists
                
                type QuotePrice:
                    bidPrice number (0..1)
                    offerPrice number (0..1)
        '''.generatePython
        
        val expectedQuote='''class Quote(BaseDataClass):
    quotePrice: Optional[Annotated[com.rosetta.test.model.QuotePrice.QuotePrice, com.rosetta.test.model.QuotePrice.QuotePrice.serializer(), com.rosetta.test.model.QuotePrice.QuotePrice.validator()]] = Field(None, description='')
    
    @rune_condition
    def condition_0_Quote_Price(self):
        item = self
        def _then_fn0():
            return (rune_attr_exists(rune_resolve_attr(rune_resolve_attr(self, "quotePrice"), "bidPrice")) or rune_attr_exists(rune_resolve_attr(rune_resolve_attr(self, "quotePrice"), "offerPrice")))
        
        def _else_fn0():
            return True
        
        return if_cond_fn(rune_attr_exists(rune_resolve_attr(self, "quotePrice")), _then_fn0, _else_fn0)
        '''

        val expectedQuotePrice= '''class QuotePrice(BaseDataClass):
    bidPrice: Optional[Decimal] = Field(None, description='')
    offerPrice: Optional[Decimal] = Field(None, description='')
        '''
        
        assertTrue(python.toString.contains(expectedQuote))
        assertTrue(python.toString.contains(expectedQuotePrice))
    }

    @Test
    def void nestedAnds() {
        val python = '''
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

        '''.generatePython
        
        val expectedQuote='''class Quote(BaseDataClass):
    quotePrice: Optional[Annotated[com.rosetta.test.model.QuotePrice.QuotePrice, com.rosetta.test.model.QuotePrice.QuotePrice.serializer(), com.rosetta.test.model.QuotePrice.QuotePrice.validator()]] = Field(None, description='')
    
    @rune_condition
    def condition_0_Quote_Price(self):
        item = self
        def _then_fn0():
            return ((rune_attr_exists(rune_resolve_attr(rune_resolve_attr(self, "quotePrice"), "price1")) and rune_attr_exists(rune_resolve_attr(rune_resolve_attr(self, "quotePrice"), "price2"))) and rune_attr_exists(rune_resolve_attr(rune_resolve_attr(self, "quotePrice"), "price3")))
        
        def _else_fn0():
            return True
        
        return if_cond_fn(rune_attr_exists(rune_resolve_attr(self, "quotePrice")), _then_fn0, _else_fn0)'''
        val expectedQuotePrice='''class QuotePrice(BaseDataClass):
    price1: Optional[Decimal] = Field(None, description='')
    price2: Optional[Decimal] = Field(None, description='')
    price3: Optional[Decimal] = Field(None, description='')'''
        
        assertTrue(python.toString.contains(expectedQuote))
        assertTrue(python.toString.contains(expectedQuotePrice))
    }

    @Test
    def void numberAttributeisHandled() {
        val python = '''
                type Quote:
                    quotePrice QuotePrice (0..1)
                    condition Quote_Price:
                        if quotePrice exists
                        then quotePrice -> bidPrice = 0.0

                type QuotePrice:
                    bidPrice number (0..1)
        '''.generatePython
        
        val expectedQuote=
        '''
        class Quote(BaseDataClass):
            quotePrice: Optional[Annotated[com.rosetta.test.model.QuotePrice.QuotePrice, com.rosetta.test.model.QuotePrice.QuotePrice.serializer(), com.rosetta.test.model.QuotePrice.QuotePrice.validator()]] = Field(None, description='')
            
            @rune_condition
            def condition_0_Quote_Price(self):
                item = self
                def _then_fn0():
                    return rune_all_elements(rune_resolve_attr(rune_resolve_attr(self, "quotePrice"), "bidPrice"), "=", 0.0)
                
                def _else_fn0():
                    return True
                
                return if_cond_fn(rune_attr_exists(rune_resolve_attr(self, "quotePrice")), _then_fn0, _else_fn0)

        '''

        val expectedQuotePrice=
        '''
        class QuotePrice(BaseDataClass):
            bidPrice: Optional[Decimal] = Field(None, description='')
        '''
        
        assertTrue(python.toString.contains(expectedQuote))
        assertTrue(python.toString.contains(expectedQuotePrice))
    }
    
    @Test
    def void dataRuleWithDoIfAndFunction() {
        val python = '''
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
        '''.generatePython

        val expectedQuote='''
        class Quote(BaseDataClass):
            price: Optional[Decimal] = Field(None, description='')
            
            @rune_condition
            def condition_0_(self):
                item = self
                def _then_fn0():
                    return rune_all_elements(Foo(rune_resolve_attr(self, "price")), "=", 5.0)
                
                def _else_fn0():
                    return True
                
                return if_cond_fn(rune_attr_exists(rune_resolve_attr(self, "price")), _then_fn0, _else_fn0)
        '''

        assertTrue(python.toString.contains(expectedQuote))
    }

    @Test
    def void dataRuleWithDoIfAndFunctionAndElse() {
        val python = '''
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
        '''.generatePython
        
        val expectedQuoute=
        '''
        class Quote(BaseDataClass):
            price: Optional[Decimal] = Field(None, description='')
            
            @rune_condition
            def condition_0_(self):
                item = self
                def _then_fn0():
                    return rune_all_elements(Foo(rune_resolve_attr(self, "price")), "=", 5.0)
                
                def _else_fn0():
                    return True
                
                return if_cond_fn(rune_attr_exists(rune_resolve_attr(self, "price")), _then_fn0, _else_fn0)
        '''

        assertTrue(python.toString.contains(expectedQuoute))
    }
    
    @Test
    def void dataRuleCoinHead() {
        val python = '''
            type Coin:
                head boolean (0..1)
                tail boolean (0..1)
                
                condition CoinHeadRule:
                    if head = True
                    then tail = False

        '''.generatePython
        
        val expected=
        '''
        class Coin(BaseDataClass):
            head: Optional[bool] = Field(None, description='')
            tail: Optional[bool] = Field(None, description='')
            
            @rune_condition
            def condition_0_CoinHeadRule(self):
                item = self
                def _then_fn0():
                    return rune_all_elements(rune_resolve_attr(self, "tail"), "=", False)
                
                def _else_fn0():
                    return True
                
                return if_cond_fn(rune_all_elements(rune_resolve_attr(self, "head"), "=", True), _then_fn0, _else_fn0)
        '''
        
        assertTrue(python.toString.contains(expected))
    }

    @Test
    def void dataRuleCoinTail() {
        val python = '''
            type Coin:
                head boolean (0..1)
                tail boolean (0..1)

                condition CoinTailRule:
                    if tail = True
                    then head = False
        '''.generatePython

        val expected=
        '''
        class Coin(BaseDataClass):
            head: Optional[bool] = Field(None, description='')
            tail: Optional[bool] = Field(None, description='')
            
            @rune_condition
            def condition_0_CoinTailRule(self):
                item = self
                def _then_fn0():
                    return rune_all_elements(rune_resolve_attr(self, "head"), "=", False)
                
                def _else_fn0():
                    return True
                
                return if_cond_fn(rune_all_elements(rune_resolve_attr(self, "tail"), "=", True), _then_fn0, _else_fn0)
        '''
        
        assertTrue(python.toString.contains(expected))
    }
    
    @Test
    def void dataRuleCoinEdge() {
        val python = '''
            type Coin:
                head boolean (0..1)
                tail boolean (0..1)
                
                condition EdgeRule:
                    if tail = False
                    then head = False
        '''.generatePython

        val expected=
        '''
        class Coin(BaseDataClass):
            head: Optional[bool] = Field(None, description='')
            tail: Optional[bool] = Field(None, description='')
            
            @rune_condition
            def condition_0_EdgeRule(self):
                item = self
                def _then_fn0():
                    return rune_all_elements(rune_resolve_attr(self, "head"), "=", False)
                
                def _else_fn0():
                    return True
                
                return if_cond_fn(rune_all_elements(rune_resolve_attr(self, "tail"), "=", False), _then_fn0, _else_fn0)
        '''
        
        assertTrue(python.toString.contains(expected))
    }


    @Test
    def void conditionCount() {
        val python = '''
            type CondTest:
                multiAttr number (1..*)

                condition:
                    multiAttr count >= 0
        '''.generatePython
        
        val expected = '''class CondTest(BaseDataClass):
    multiAttr: list[Decimal] = Field([], description='', min_length=1)
    
    @rune_condition
    def condition_0_(self):
        item = self
        return rune_all_elements(rune_count(rune_resolve_attr(self, "multiAttr")), ">=", 0)
    '''
        assertTrue(python.toString.contains(expected))
    }

    @Test
    def void checkConditionWithInheritedAttribute() {
        val python = '''
            type Foo:
                x string (0..1)
                y string (0..1)

                condition:
                    x exists

            type Bar extends Foo:
                z string (0..1)

                condition:
                    y exists
        '''.generatePython

        val expectedFoo=
        '''
        class Foo(BaseDataClass):
            x: Optional[str] = Field(None, description='')
            y: Optional[str] = Field(None, description='')
            
            @rune_condition
            def condition_0_(self):
                item = self
                return rune_attr_exists(rune_resolve_attr(self, "x"))
        '''

        val expectedBar=
        '''
        class Bar(Foo):
            z: Optional[str] = Field(None, description='')
            
            @rune_condition
            def condition_0_(self):
                item = self
                return rune_attr_exists(rune_resolve_attr(self, "y"))
        '''

        assertTrue(python.toString.contains(expectedFoo), 'expected\n' + expectedFoo + '\npython\n' + python)
        assertTrue(python.toString.contains(expectedBar), 'expected\n' + expectedBar + '\npython\n' + python)
    }

    @Test
    def void shouldCheckInheritedCondition() {
        val python = '''
            type Foo:
                x string (0..1)
                y string (0..1)

                condition:
                    x exists

            type Bar extends Foo:
                z string (0..1)

                condition:
                    y exists
        '''.generatePython

        val expectedFoo=
        '''
        class Foo(BaseDataClass):
            x: Optional[str] = Field(None, description='')
            y: Optional[str] = Field(None, description='')
            
            @rune_condition
            def condition_0_(self):
                item = self
                return rune_attr_exists(rune_resolve_attr(self, "x"))
        '''

        val expectedBar=
        '''
        class Bar(Foo):
            z: Optional[str] = Field(None, description='')
            
            @rune_condition
            def condition_0_(self):
                item = self
                return rune_attr_exists(rune_resolve_attr(self, "y"))
        '''
        
        assertTrue(python.toString.contains(expectedFoo))
        assertTrue(python.toString.contains(expectedBar))    
    }
        
    
    
    def generatePython(CharSequence model) {
        val m = model.parseRosettaWithNoErrors
        val resourceSet = m.eResource.resourceSet
        val version = m.version
        
        val result = newHashMap
        result.putAll(generator.beforeAllGenerate(resourceSet, #{m}, version))
        result.putAll(generator.beforeGenerate(m.eResource, m, version))
        result.putAll(generator.generate(m.eResource, m, version))
        result.putAll(generator.afterGenerate(m.eResource, m, version))
        result.putAll(generator.afterAllGenerate(resourceSet, #{m}, version))
        
        result
    }
}