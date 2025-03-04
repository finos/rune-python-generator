package com.regnosys.rosetta.generator.python.object

import com.google.inject.Inject
import com.regnosys.rosetta.generator.python.PythonCodeGenerator
import com.regnosys.rosetta.tests.RosettaInjectorProvider
import com.regnosys.rosetta.tests.util.ModelHelper
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.extensions.InjectionExtension
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.^extension.ExtendWith
import static org.junit.jupiter.api.Assertions.*

@ExtendWith(InjectionExtension)
@InjectWith(RosettaInjectorProvider)

class ModelObjectGeneratorTest {

    @Inject extension ModelHelper
    @Inject PythonCodeGenerator generator;



    @Test
    def void testGenerateBasicTypeString() {
        val python = '''
            type Tester:
                one string (0..1)
                list string (1..*)
        '''.generatePython
        
        val expected=
        '''
        class Tester(BaseDataClass):
            one: Optional[str] = Field(None, description='')
            list: list[str] = Field([], description='', min_length=1)

        '''
        
        assertTrue(python.toString.contains(expected))
    }

    @Test
    def void testGenerateBasicTypeInt() {
        val python = '''
            type Tester:
                one int (0..1)
                list int (1..*)
        '''.generatePython

        val expected=
        '''
        class Tester(BaseDataClass):
            one: Optional[int] = Field(None, description='')
            list: list[int] = Field([], description='', min_length=1)

        '''
        
        assertTrue(python.toString.contains(expected), "expected:\n" + expected + "\npython:\n" + python)

    }

    @Test
    def void testGenerateBasicTypeNumber() {
        val python = '''
            type Tester:
                one number (0..1)
                list number (1..*)
        '''.generatePython
        
        val expected=
        '''
        class Tester(BaseDataClass):
            one: Optional[Decimal] = Field(None, description='')
            list: list[Decimal] = Field([], description='', min_length=1)

        '''
        
        assertTrue(python.toString.contains(expected))

    }

    @Test
    def void testGenerateBasicTypeBoolean() {
        val python = '''
            type Tester:
                one boolean (0..1)
                list boolean (1..*)
        '''.generatePython

        
        
        val expected=
        '''
        class Tester(BaseDataClass):
            one: Optional[bool] = Field(None, description='')
            list: list[bool] = Field([], description='', min_length=1)
        '''
        
        assertTrue(python.toString.contains(expected))
    }

    @Test
    def void testGenerateBasicTypeDate() {
        val python = '''
            type Tester:
                one date (0..1)
                list date (1..*)
        '''.generatePython

        
        
        val expected=
        '''
        class Tester(BaseDataClass):
            one: Optional[datetime.date] = Field(None, description='')
            list: list[datetime.date] = Field([], description='', min_length=1)

        '''
        
        assertTrue(python.toString.contains(expected))
    }

    @Test
    def void testGenerateBasicTypeDateTime() {
        val python = '''
            type Tester:
                one date (0..1)
                list date (1..*)
                zoned zonedDateTime (0..1)
        '''.generatePython

        val expected=
        '''
        class Tester(BaseDataClass):
            one: Optional[datetime.date] = Field(None, description='')
            list: list[datetime.date] = Field([], description='', min_length=1)
            zoned: Optional[datetime.datetime] = Field(None, description='')
        '''
        
        assertTrue(python.toString.contains(expected), "\nexpected\n" + expected + "\npython\n" + python)
    }

    @Test
    def void testGenerateBasicTypeTime() {
        val python = '''
            type Tester:
                one time (0..1)
                list time (1..*)
        '''.generatePython
    
        val expected=
        '''
        class Tester(BaseDataClass):
            one: Optional[datetime.time] = Field(None, description='')
            list: list[datetime.time] = Field([], description='', min_length=1)

        '''
        
        assertTrue(python.toString.contains(expected), "\nexpected\n" + expected + "\npython\n" + python)
    }



    @Test
    @Disabled
    def void testGenerateMetaFieldWhenAttributeSchemePresent() {
        '''
            type TestObject: <''>
                fieldOne string (0..1) [metadata scheme]
        '''.generatePython


    }

    @Test
    @Disabled
    def void testGenerateRosettaReferenceField() {
        '''
            type TestObject: <''>
                fieldOne Test2 (0..1) [metadata reference]

            type Test2:
        '''.generatePython


    }

    @Test
    @Disabled
    def void testGenerateBasicReferenceField() {
        val namespace = 'test.ns.basicref'
        '''
            namespace "«namespace»"

            // import basic types
            import com.rosetta.test.model.*

            type TestObject: <''>
                fieldOne date (0..1) [metadata reference]
        '''.generatePython


    }

    @Test
    @Disabled
    def void testCreateFieldWithReferenceTypeWhenAttributeIsReference() {
        '''

            type ComplexObject:

            type TestObject: <''>
                fieldOne ComplexObject (0..1) [metadata reference]
        '''.generatePython


    }

    @Test
    @Disabled
    def void testGenerateTypeWithMetaFieldImport() {
        val namespace = 'test.ns.metafield'
        '''
            namespace "«namespace»"
            version "test"
            
            // import basic types
            import com.rosetta.test.model.*
            
            type Foo:
                [metadata key]
                
                attr string (0..1)
        '''.generatePython


    }

    @Test
    @Disabled
    def void testImplementGlobalKeyWhenDefined() {
        '''
            type WithGlobalKey:
                [metadata key]
                bar string (1..1)
        '''.generatePython


    }

    @Test
    def void testOmitGlobalKeyAnnotationWhenNotDefined() {
        val python = '''
            type AttributeGlobalKeyTest:
                withoutGlobalKey string (1..1)
        '''.generatePython
        
        val expected=
        '''
        class AttributeGlobalKeyTest(BaseDataClass):
            withoutGlobalKey: str = Field(..., description='')

        '''
        
        assertTrue(python.toString.contains(expected))
    }

    @Test
    @Disabled
    def void testGenerateReferenceAttributeAsReference() {
        '''
            type Foo:
                [metadata key]
                bar string (1..1)

            type AttributeGlobalKeyTest:
                withGlobalKey Foo (1..1) [metadata reference]
        '''.generatePython


    }

    @Test
    def void testGenerateClasslist() {
        val python = '''
            type A extends B:
                c C (1..*)

            type B:

            type C :
                one int (0..1)
                list int (1..*)


            type D:
                s string (1..*)
        '''.generatePython

        
        val expectedA =
        '''
        class A(B):
            c: list[Annotated[com.rosetta.test.model.C.C, com.rosetta.test.model.C.C.serializer(), com.rosetta.test.model.C.C.validator()]] = Field([], description='', min_length=1)'''

        val expectedB=
        '''
        class B(BaseDataClass):
            pass'''

        val expectedC =
        '''
        class C(BaseDataClass):
            one: Optional[int] = Field(None, description='')
            list: list[int] = Field([], description='', min_length=1)'''

        val expectedD =
        '''
        class D(BaseDataClass):
            s: list[str] = Field([], description='', min_length=1)'''
        
        assertTrue(python.toString.contains(expectedA), "\nexpectedA" + expectedA + "\npython\n" + python)
        assertTrue(python.toString.contains(expectedB), "\nexpectedB" + expectedB + "\npython\n" + python)
        assertTrue(python.toString.contains(expectedC), "\nexpectedC" + expectedC + "\npython\n" + python)
        assertTrue(python.toString.contains(expectedD), "\nexpectedD" + expectedD + "\npython\n" + python)
            
    }

    @Test
    def void testExtendATypeWithSameAttribute() {
        val python = '''
            type Foo:
                a string (0..1)
                b string (0..1)
            
            type Bar extends Foo:
                a string (0..1)
        '''.generatePython
    
        val expectedFoo=
        '''
        class Foo(BaseDataClass):
            a: Optional[str] = Field(None, description='')
            b: Optional[str] = Field(None, description='')

        '''

        val expectedBar =
        '''
        class Bar(Foo):
            a: Optional[str] = Field(None, description='')

        '''
        
        assertTrue(python.toString.contains(expectedFoo))
        assertTrue(python.toString.contains(expectedBar))
    }

    @Test
    def testGenerateRosettaCalculationTypeAsString() {
        val python = '''
            type Foo:
                bar calculation (0..1)
        '''.generatePython

        
        val expected=
        '''
        class Foo(BaseDataClass):
            bar: Optional[str] = Field(None, description='')

        '''
        
        assertTrue(python.toString.contains(expected))
    }

    @Test
    def void testSetAttributesOnEmptyClassWithInheritance() {
        val python = '''
            type Foo:
                attr string (0..1)
            
            type Bar extends Foo:
        '''.generatePython
        
        val expectedFoo=
        '''
        class Foo(BaseDataClass):
            attr: Optional[str] = Field(None, description='')
        '''

        val expectedBar=
        '''
        class Bar(Foo):
            pass
        '''
        
        assertTrue(python.toString.contains(expectedFoo))
        assertTrue(python.toString.contains(expectedBar))
    }
    
    @Test
    @Disabled
    def void isProductWithEnumValueRef() {
        '''
            isProduct root Foo;
            
            enum Enum: 
                A
                B
            
            type Foo:
                attr Enum (0..1)
            
            func Qualify_FooProd:
                [qualification Product]
                inputs: foo Foo (1..1)
                output: is_product boolean (1..1)
                set is_product:
                    foo -> attr = Enum -> A
        '''.generatePython

    }

    @Test
    @Disabled
    def void internalReferenceTest() {
        '''

            type Foo:
                foo string (1..1)
                    [metadata location]

            type Bar:
                bar string (1..1)
                    [metadata address "pointsTo"=Foo->foo]

        '''.generatePython

    }

    @Test
    def void testConditions1() {
        val python = '''
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
                
                condition SecondOneOrTwo: <"FpML specifies a choice between adjustedDate and [unadjustedDate (required), dateAdjutsments (required), adjustedDate (optional)].">
                    aValue->a0 exists
                        or (intValue2 exists and intValue1 exists and intValue1 exists)
                        or (intValue2 exists and intValue1 exists and intValue1 is absent)
            '''.generatePython
        val expectedA = '''class A(BaseDataClass):
    a0: Optional[int] = Field(None, description='')
    a1: Optional[int] = Field(None, description='')
    
    @rune_condition
    def condition_0_(self):
        item = self
        return rune_check_one_of(self, 'a0', 'a1', necessity=True)'''
        
        val expectedB = '''class B(BaseDataClass):
    intValue1: Optional[int] = Field(None, description='')
    intValue2: Optional[int] = Field(None, description='')
    aValue: Annotated[com.rosetta.test.model.A.A, com.rosetta.test.model.A.A.serializer(), com.rosetta.test.model.A.A.validator()] = Field(..., description='')
    
    @rune_condition
    def condition_0_Rule(self):
        item = self
        return rune_all_elements(rune_resolve_attr(self, "intValue1"), "<", 100)
    
    @rune_condition
    def condition_1_OneOrTwo(self):
        """
        Choice rule to represent an FpML choice construct.
        """
        item = self
        return rune_check_one_of(self, 'intValue1', 'intValue2', necessity=False)
    
    @rune_condition
    def condition_2_SecondOneOrTwo(self):
        """
        FpML specifies a choice between adjustedDate and [unadjustedDate (required), dateAdjutsments (required), adjustedDate (optional)].
        """
        item = self
        return ((rune_attr_exists(rune_resolve_attr(rune_resolve_attr(self, "aValue"), "a0")) or ((rune_attr_exists(rune_resolve_attr(self, "intValue2")) and rune_attr_exists(rune_resolve_attr(self, "intValue1"))) and rune_attr_exists(rune_resolve_attr(self, "intValue1")))) or ((rune_attr_exists(rune_resolve_attr(self, "intValue2")) and rune_attr_exists(rune_resolve_attr(self, "intValue1"))) and (not rune_attr_exists(rune_resolve_attr(self, "intValue1")))))'''

        assertTrue(python.toString.contains(expectedA), "expectedA\n" + expectedA + "\npython\n" + python)
        assertTrue(python.toString.contains(expectedB), "expectedB\n" + expectedB + "\npython\n" + python)
    }
    
    
    @Test
    def void testGenerateTypes() {
        val python = '''
        type TestType: <"Test type description.">
            testTypeValue1 string (1..1) <"Test string">
            testTypeValue2 string (0..1) <"Test optional string">
            testTypeValue3 string (1..*) <"Test string list">
            testTypeValue4 TestType2 (1..1) <"Test TestType2">
            testEnum TestEnum (0..1) <"Optional test enum">

        type TestType2:
            testType2Value1 number(1..*) <"Test number list">
            testType2Value2 date(0..1) <"Test date">
            testEnum TestEnum (0..1) <"Optional test enum">

        enum TestEnum: <"Test enum description.">
            TestEnumValue1 <"Test enum value 1">
            TestEnumValue2 <"Test enum value 2">
        '''.generatePython
           
        val expectedTestType=
        '''class TestType(BaseDataClass):
    """
    Test type description.
    """
    testTypeValue1: str = Field(..., description='Test string')
    """
    Test string
    """
    testTypeValue2: Optional[str] = Field(None, description='Test optional string')
    """
    Test optional string
    """
    testTypeValue3: list[str] = Field([], description='Test string list', min_length=1)
    """
    Test string list
    """
    testTypeValue4: Annotated[com.rosetta.test.model.TestType2.TestType2, com.rosetta.test.model.TestType2.TestType2.serializer(), com.rosetta.test.model.TestType2.TestType2.validator()] = Field(..., description='Test TestType2')
    """
    Test TestType2
    """
    testEnum: Optional[com.rosetta.test.model.TestEnum.TestEnum] = Field(None, description='Optional test enum')
    """
    Optional test enum
    """'''
        val expectedTestType2=
        '''class TestType2(BaseDataClass):
    testType2Value1: list[Decimal] = Field([], description='Test number list', min_length=1)
    """
    Test number list
    """
    testType2Value2: Optional[datetime.date] = Field(None, description='Test date')
    """
    Test date
    """
    testEnum: Optional[com.rosetta.test.model.TestEnum.TestEnum] = Field(None, description='Optional test enum')
    """
    Optional test enum
    """'''
        assertTrue(python.toString.contains(expectedTestType), "expectedTestType\n" + expectedTestType + "\npython\n" + python)
        assertTrue(python.toString.contains(expectedTestType2), "expectedTestType\n" + expectedTestType2 + "\npython\n" + python)
    }        
    
    @Test
    def void testGenerateTypesMethod2() {
        val python = '''
        type UnitType: <"Defines the unit to be used for price, quantity, or other purposes">
            currency string (0..1) <"Defines the currency to be used as a unit for a price, quantity, or other purpose.">

        type MeasureBase: <"Provides an abstract base class shared by Price and Quantity.">
            amount number (1..1) <"Specifies an amount to be qualified and used in a Price or Quantity definition.">
            unitOfAmount UnitType (1..1) <"Qualifies the unit by which the amount is measured.">

        type Quantity extends MeasureBase: <"Specifies a quantity to be associated to a financial product, for example a trade amount or a cashflow amount resulting from a trade.">
            multiplier number (0..1) <"Defines the number to be multiplied by the amount to derive a total quantity.">
            multiplierUnit UnitType (0..1) <"Qualifies the multiplier with the applicable unit.  For example in the case of the Coal (API2) CIF ARA (ARGUS-McCloskey) Futures Contract on the CME, where the unitOfAmount would be contracts, the multiplier would 1,000 and the mulitiplier Unit would be 1,000 MT (Metric Tons).">
        '''.generatePython
          
        val expectedMeasureBase =
    '''class MeasureBase(BaseDataClass):
    """
    Provides an abstract base class shared by Price and Quantity.
    """
    amount: Decimal = Field(..., description='Specifies an amount to be qualified and used in a Price or Quantity definition.')
    """
    Specifies an amount to be qualified and used in a Price or Quantity definition.
    """
    unitOfAmount: Annotated[com.rosetta.test.model.UnitType.UnitType, com.rosetta.test.model.UnitType.UnitType.serializer(), com.rosetta.test.model.UnitType.UnitType.validator()] = Field(..., description='Qualifies the unit by which the amount is measured.')
    """
    Qualifies the unit by which the amount is measured.
    """'''
        
        val expectedUnitType =
        '''class UnitType(BaseDataClass):
    """
    Defines the unit to be used for price, quantity, or other purposes
    """
    currency: Optional[str] = Field(None, description='Defines the currency to be used as a unit for a price, quantity, or other purpose.')
    """
    Defines the currency to be used as a unit for a price, quantity, or other purpose.
    """'''
        
        val expectedQuantity =
    '''class Quantity(MeasureBase):
    """
    Specifies a quantity to be associated to a financial product, for example a trade amount or a cashflow amount resulting from a trade.
    """
    multiplier: Optional[Decimal] = Field(None, description='Defines the number to be multiplied by the amount to derive a total quantity.')
    """
    Defines the number to be multiplied by the amount to derive a total quantity.
    """
    multiplierUnit: Optional[Annotated[com.rosetta.test.model.UnitType.UnitType, com.rosetta.test.model.UnitType.UnitType.serializer(), com.rosetta.test.model.UnitType.UnitType.validator()]] = Field(None, description='Qualifies the multiplier with the applicable unit. For example in the case of the Coal (API2) CIF ARA (ARGUS-McCloskey) Futures Contract on the CME, where the unitOfAmount would be contracts, the multiplier would 1,000 and the mulitiplier Unit would be 1,000 MT (Metric Tons).')
    """
    Qualifies the multiplier with the applicable unit.  For example in the case of the Coal (API2) CIF ARA (ARGUS-McCloskey) Futures Contract on the CME, where the unitOfAmount would be contracts, the multiplier would 1,000 and the mulitiplier Unit would be 1,000 MT (Metric Tons).
    """'''
        assertTrue(python.toString.contains(expectedMeasureBase), "expected\n" + expectedMeasureBase + "\npython\n" + python)
        assertTrue(python.toString.contains(expectedUnitType), "expected\n" + expectedUnitType + "\npython\n" + python)
        assertTrue(python.toString.contains(expectedQuantity), "expected\n" + expectedQuantity + "\npython\n" + python)
    }

    @Test
    def void testGenerateTypesExtends() {
        val python = '''
        type TestType extends TestType2:
            TestTypeValue1 string (1..1) <"Test string">
            TestTypeValue2 int (0..1) <"Test int">

        type TestType2 extends TestType3:
            TestType2Value1 number (0..1) <"Test number">
            TestType2Value2 date (1..*) <"Test date">

        type TestType3:
            TestType3Value1 string (0..1) <"Test string">
            TestType4Value2 int (1..*) <"Test int">
        '''.generatePython
       
        val expectedTestType = '''class TestType(TestType2):
    TestTypeValue1: str = Field(..., description='Test string')
    """
    Test string
    """
    TestTypeValue2: Optional[int] = Field(None, description='Test int')
    """
    Test int
    """'''
        val expectedTestType2 ='''class TestType2(TestType3):
    TestType2Value1: Optional[Decimal] = Field(None, description='Test number')
    """
    Test number
    """
    TestType2Value2: list[datetime.date] = Field([], description='Test date', min_length=1)
    """
    Test date
    """'''
        val expectedTestType3 = '''class TestType3(BaseDataClass):
    TestType3Value1: Optional[str] = Field(None, description='Test string')
    """
    Test string
    """
    TestType4Value2: list[int] = Field([], description='Test int', min_length=1)
    """
    Test int
    """'''
        
        assertTrue(python.toString.contains(expectedTestType)) 
        assertTrue(python.toString.contains(expectedTestType2)) 
        assertTrue(python.toString.contains(expectedTestType3))        
    }

    @Test
    def void testGenerateTypesChoiceCondition() {
        val python = '''type TestType: <"Test type with one-of condition.">
            field1 string (0..1) <"Test string field 1">
            field2 string (0..1) <"Test string field 2">
            field3 number (0..1) <"Test number field 3">
            field4 number (1..*) <"Test number field 4">
            condition BusinessCentersChoice: <"Choice rule to represent an FpML choice construct.">
                    required choice field1, field2
        '''.generatePython

        val types = python.toString

        val expected ='''class TestType(BaseDataClass):
    """
    Test type with one-of condition.
    """
    field1: Optional[str] = Field(None, description='Test string field 1')
    """
    Test string field 1
    """
    field2: Optional[str] = Field(None, description='Test string field 2')
    """
    Test string field 2
    """
    field3: Optional[Decimal] = Field(None, description='Test number field 3')
    """
    Test number field 3
    """
    field4: list[Decimal] = Field([], description='Test number field 4', min_length=1)
    """
    Test number field 4
    """
    
    @rune_condition
    def condition_0_BusinessCentersChoice(self):
        """
        Choice rule to represent an FpML choice construct.
        """
        item = self
        return rune_check_one_of(self, 'field1', 'field2', necessity=True)'''
        assertTrue(types.contains(expected))
    }

    @Test
    def void testGenerateIfThenCondition() {
        val python = '''type TestType: <"Test type with one-of condition.">
            field1 string (0..1) <"Test string field 1">
            field2 string (0..1) <"Test string field 2">
            field3 number (0..1) <"Test number field 3">
            field4 number (1..*) <"Test number field 4">
            condition BusinessCentersChoice: <"Choice rule to represent an FpML choice construct.">
                    if field1 exists
                            then field3 > 0
        '''.generatePython

        val expected = '''class TestType(BaseDataClass):
    """
    Test type with one-of condition.
    """
    field1: Optional[str] = Field(None, description='Test string field 1')
    """
    Test string field 1
    """
    field2: Optional[str] = Field(None, description='Test string field 2')
    """
    Test string field 2
    """
    field3: Optional[Decimal] = Field(None, description='Test number field 3')
    """
    Test number field 3
    """
    field4: list[Decimal] = Field([], description='Test number field 4', min_length=1)
    """
    Test number field 4
    """
    
    @rune_condition
    def condition_0_BusinessCentersChoice(self):
        """
        Choice rule to represent an FpML choice construct.
        """
        item = self
        def _then_fn0():
            return rune_all_elements(rune_resolve_attr(self, "field3"), ">", 0)
        
        def _else_fn0():
            return True
        
        return if_cond_fn(rune_attr_exists(rune_resolve_attr(self, "field1")), _then_fn0, _else_fn0)'''
        assertTrue(python.toString.contains(expected), "expected\n:" + expected + "\npython\n" + python)
    }

    @Test
    def void testConditionsGeneration() {
        val python = '''type A:
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
                    or (intValue2 exists and intValue1 exists and intValue1 is absent)'''.generatePython

        val expectedA = '''class A(BaseDataClass):
    a0: Optional[int] = Field(None, description='')
    a1: Optional[int] = Field(None, description='')
    
    @rune_condition
    def condition_0_(self):
        item = self
        return rune_check_one_of(self, 'a0', 'a1', necessity=True)'''
        
        val expectedB = '''class B(BaseDataClass):
    intValue1: Optional[int] = Field(None, description='')
    intValue2: Optional[int] = Field(None, description='')
    aValue: Annotated[com.rosetta.test.model.A.A, com.rosetta.test.model.A.A.serializer(), com.rosetta.test.model.A.A.validator()] = Field(..., description='')
    
    @rune_condition
    def condition_0_Rule(self):
        item = self
        return rune_all_elements(rune_resolve_attr(self, "intValue1"), "<", 100)
    
    @rune_condition
    def condition_1_OneOrTwo(self):
        """
        Choice rule to represent an FpML choice construct.
        """
        item = self
        return rune_check_one_of(self, 'intValue1', 'intValue2', necessity=False)
    
    @rune_condition
    def condition_2_ReqOneOrTwo(self):
        """
        Choice rule to represent an FpML choice construct.
        """
        item = self
        return rune_check_one_of(self, 'intValue1', 'intValue2', necessity=True)
    
    @rune_condition
    def condition_3_SecondOneOrTwo(self):
        """
        FpML specifies a choice between adjustedDate and [unadjustedDate (required), dateAdjutsments (required), adjustedDate (optional)].
        """
        item = self
        return ((rune_attr_exists(rune_resolve_attr(rune_resolve_attr(self, "aValue"), "a0")) or ((rune_attr_exists(rune_resolve_attr(self, "intValue2")) and rune_attr_exists(rune_resolve_attr(self, "intValue1"))) and rune_attr_exists(rune_resolve_attr(self, "intValue1")))) or ((rune_attr_exists(rune_resolve_attr(self, "intValue2")) and rune_attr_exists(rune_resolve_attr(self, "intValue1"))) and (not rune_attr_exists(rune_resolve_attr(self, "intValue1")))))'''

        assertTrue(python.toString.contains(expectedA))
        assertTrue(python.toString.contains(expectedB))
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