package com.regnosys.rosetta.generator.python.object

import com.google.inject.Inject
import com.regnosys.rosetta.tests.RosettaInjectorProvider
import com.regnosys.rosetta.generator.python.PythonGeneratorTestUtils
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.extensions.InjectionExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.^extension.ExtendWith

@ExtendWith(InjectionExtension)
@InjectWith(RosettaInjectorProvider)

class PythonObjectGeneratorTest {

    @Inject PythonGeneratorTestUtils testUtils

    @Test
    def void testGenerateBasicTypeString() {
        testUtils.assertBundleContainsExpectedString(
            '''
            type Tester:
                one string (0..1)
                list string (1..*)
            ''',
            '''
            class com_rosetta_test_model_Tester(BaseDataClass):
                one: Optional[str] = Field(None, description='')
                list: list[str] = Field([], description='', min_length=1)'''
        )
    }

    @Test
    def void testGenerateBasicTypeInt() {
        testUtils.assertBundleContainsExpectedString(
            '''
            type Tester:
                one int (0..1)
                list int (1..*)
            ''',
            '''
            class com_rosetta_test_model_Tester(BaseDataClass):
                one: Optional[int] = Field(None, description='')
                list: list[int] = Field([], description='', min_length=1)'''
        )
    }

    @Test
    def void testGenerateBasicTypeNumber() {
        testUtils.assertBundleContainsExpectedString(
            '''
            type Tester:
                one number (0..1)
                list number (1..*)
            ''',
            '''
            class com_rosetta_test_model_Tester(BaseDataClass):
                one: Optional[Decimal] = Field(None, description='')
                list: list[Decimal] = Field([], description='', min_length=1)'''
        )
    }

    @Test
    def void testGenerateBasicTypeBoolean() {
        testUtils.assertBundleContainsExpectedString(
            '''
            type Tester:
                one boolean (0..1)
                list boolean (1..*)
            ''',
            '''
            class com_rosetta_test_model_Tester(BaseDataClass):
                one: Optional[bool] = Field(None, description='')
                list: list[bool] = Field([], description='', min_length=1)'''
        )
    }

    @Test
    def void testGenerateBasicTypeDate() {
        testUtils.assertBundleContainsExpectedString(
            '''
            type Tester:
                one date (0..1)
                list date (1..*)
            ''',
            '''
            class com_rosetta_test_model_Tester(BaseDataClass):
                one: Optional[datetime.date] = Field(None, description='')
                list: list[datetime.date] = Field([], description='', min_length=1)'''
        )
    }

    @Test
    def void testGenerateBasicTypeDateTime() {
        testUtils.assertBundleContainsExpectedString(
            '''
            type Tester:
                one date (0..1)
                list date (1..*)
                zoned zonedDateTime (0..1)
            ''',
            '''
            class com_rosetta_test_model_Tester(BaseDataClass):
                one: Optional[datetime.date] = Field(None, description='')
                list: list[datetime.date] = Field([], description='', min_length=1)
                zoned: Optional[datetime.datetime] = Field(None, description='')'''
        )
    }

    @Test
    def void testGenerateBasicTypeTime() {
        testUtils.assertBundleContainsExpectedString(
            '''
            type Tester:
                one time (0..1)
                list time (1..*)
            ''',
            '''
            class com_rosetta_test_model_Tester(BaseDataClass):
                one: Optional[datetime.time] = Field(None, description='')
                list: list[datetime.time] = Field([], description='', min_length=1)'''
        )
    }

    @Test
    def void testOmitGlobalKeyAnnotationWhenNotDefined() {
        testUtils.assertBundleContainsExpectedString(
            '''
            type AttributeGlobalKeyTest:
                withoutGlobalKey string (1..1)
            ''',
            '''
            class com_rosetta_test_model_AttributeGlobalKeyTest(BaseDataClass):
                withoutGlobalKey: str = Field(..., description='')'''
        )
    }

    @Test
    def void testGenerateClasslist() {
        testUtils.assertBundleContainsExpectedString(
            '''
            type A extends B:
                c C (1..*)

            type B:

            type C :
                one int (0..1)
                list int (1..*)

            type D:
                s string (1..*)
            ''',
            '''
            class com_rosetta_test_model_A(com_rosetta_test_model_B):
                c: list[Annotated[com_rosetta_test_model_C, com_rosetta_test_model_C.serializer(), com_rosetta_test_model_C.validator()]] = Field([], description='', min_length=1)'''
        )
        testUtils.assertBundleContainsExpectedString(
            '''
            type A extends B:
                c C (1..*)

            type B:

            type C :
                one int (0..1)
                list int (1..*)

            type D:
                s string (1..*)
            ''',
            '''
            class com_rosetta_test_model_B(BaseDataClass):
                pass'''
        )
        testUtils.assertBundleContainsExpectedString(
            '''
            type A extends B:
                c C (1..*)

            type B:

            type C :
                one int (0..1)
                list int (1..*)

            type D:
                s string (1..*)
            ''',
            '''
            class com_rosetta_test_model_C(BaseDataClass):
                one: Optional[int] = Field(None, description='')
                list: list[int] = Field([], description='', min_length=1)'''
        )
        testUtils.assertBundleContainsExpectedString(
            '''
            type A extends B:
                c C (1..*)

            type B:

            type C :
                one int (0..1)
                list int (1..*)

            type D:
                s string (1..*)
            ''',
            '''
            class com_rosetta_test_model_D(BaseDataClass):
                s: list[str] = Field([], description='', min_length=1)'''
        )
    }

    @Test
    def void testExtendATypeWithSameAttribute() {
        testUtils.assertBundleContainsExpectedString(
            '''
            type Foo:
                a string (0..1)
                b string (0..1)
            
            type Bar extends Foo:
                a string (0..1)
            ''',
            '''
            class com_rosetta_test_model_Foo(BaseDataClass):
                a: Optional[str] = Field(None, description='')
                b: Optional[str] = Field(None, description='')'''
        )
        testUtils.assertBundleContainsExpectedString(
            '''
            type Foo:
                a string (0..1)
                b string (0..1)
            
            type Bar extends Foo:
                a string (0..1)
            ''',
            '''
            class com_rosetta_test_model_Bar(com_rosetta_test_model_Foo):
                a: Optional[str] = Field(None, description='')'''
        )
    }

    @Test
    def testGenerateRosettaCalculationTypeAsString() {
        testUtils.assertBundleContainsExpectedString(
            '''
            type Foo:
                bar calculation (0..1)
            ''',
            '''
            class com_rosetta_test_model_Foo(BaseDataClass):
                bar: Optional[str] = Field(None, description='')'''
        )
    }

    @Test
    def void testSetAttributesOnEmptyClassWithInheritance() {
        testUtils.assertBundleContainsExpectedString(
            '''
            type Foo:
                attr string (0..1)
            
            type Bar extends Foo:
            ''',
            '''
            class com_rosetta_test_model_Foo(BaseDataClass):
                attr: Optional[str] = Field(None, description='')'''
        )
        testUtils.assertBundleContainsExpectedString(
            '''
            type Foo:
                attr string (0..1)
            
            type Bar extends Foo:
            ''',
            '''
            class com_rosetta_test_model_Bar(com_rosetta_test_model_Foo):
                pass'''
        )
    }

    @Test
    def void testConditions1() {
        testUtils.assertBundleContainsExpectedString(
            '''
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
            ''',
            '''
            class com_rosetta_test_model_A(BaseDataClass):
                a0: Optional[int] = Field(None, description='')
                a1: Optional[int] = Field(None, description='')
                
                @rune_condition
                def condition_0_(self):
                    item = self
                    return rune_check_one_of(self, 'a0', 'a1', necessity=True)'''
        )
        testUtils.assertBundleContainsExpectedString(
            '''
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
            ''',
            '''
            class com_rosetta_test_model_B(BaseDataClass):
                intValue1: Optional[int] = Field(None, description='')
                intValue2: Optional[int] = Field(None, description='')
                aValue: Annotated[com_rosetta_test_model_A, com_rosetta_test_model_A.serializer(), com_rosetta_test_model_A.validator()] = Field(..., description='')
                
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
        )
    }

    @Test
    def void testGenerateTypes() {
        testUtils.assertBundleContainsExpectedString(
            '''
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
            ''',
            '''
            class com_rosetta_test_model_TestType(BaseDataClass):
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
                testTypeValue4: Annotated[com_rosetta_test_model_TestType2, com_rosetta_test_model_TestType2.serializer(), com_rosetta_test_model_TestType2.validator()] = Field(..., description='Test TestType2')
                """
                Test TestType2
                """
                testEnum: Optional[com.rosetta.test.model.TestEnum.TestEnum] = Field(None, description='Optional test enum')
                """
                Optional test enum
                """'''
        )
        testUtils.assertBundleContainsExpectedString(
            '''
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
            ''',
            '''
            class com_rosetta_test_model_TestType2(BaseDataClass):
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
        )
    }

    @Test
    def void testGenerateTypesMethod2() {
        testUtils.assertBundleContainsExpectedString(
            '''
            type UnitType: <"Defines the unit to be used for price, quantity, or other purposes">
                currency string (0..1) <"Defines the currency to be used as a unit for a price, quantity, or other purpose.">

            type MeasureBase: <"Provides an abstract base class shared by Price and Quantity.">
                amount number (1..1) <"Specifies an amount to be qualified and used in a Price or Quantity definition.">
                unitOfAmount UnitType (1..1) <"Qualifies the unit by which the amount is measured.">

            type Quantity extends MeasureBase: <"Specifies a quantity to be associated to a financial product, for example a trade amount or a cashflow amount resulting from a trade.">
                multiplier number (0..1) <"Defines the number to be multiplied by the amount to derive a total quantity.">
                multiplierUnit UnitType (0..1) <"Qualifies the multiplier with the applicable unit.  For example in the case of the Coal (API2) CIF ARA (ARGUS-McCloskey) Futures Contract on the CME, where the unitOfAmount would be contracts, the multiplier would 1,000 and the mulitiplier Unit would be 1,000 MT (Metric Tons).">
            ''',
            '''
            class com_rosetta_test_model_MeasureBase(BaseDataClass):
                """
                Provides an abstract base class shared by Price and Quantity.
                """
                amount: Decimal = Field(..., description='Specifies an amount to be qualified and used in a Price or Quantity definition.')
                """
                Specifies an amount to be qualified and used in a Price or Quantity definition.
                """
                unitOfAmount: Annotated[com_rosetta_test_model_UnitType, com_rosetta_test_model_UnitType.serializer(), com_rosetta_test_model_UnitType.validator()] = Field(..., description='Qualifies the unit by which the amount is measured.')
                """
                Qualifies the unit by which the amount is measured.
                """'''
        )
        testUtils.assertBundleContainsExpectedString(
            '''
            type UnitType: <"Defines the unit to be used for price, quantity, or other purposes">
                currency string (0..1) <"Defines the currency to be used as a unit for a price, quantity, or other purpose.">

            type MeasureBase: <"Provides an abstract base class shared by Price and Quantity.">
                amount number (1..1) <"Specifies an amount to be qualified and used in a Price or Quantity definition.">
                unitOfAmount UnitType (1..1) <"Qualifies the unit by which the amount is measured.">

            type Quantity extends MeasureBase: <"Specifies a quantity to be associated to a financial product, for example a trade amount or a cashflow amount resulting from a trade.">
                multiplier number (0..1) <"Defines the number to be multiplied by the amount to derive a total quantity.">
                multiplierUnit UnitType (0..1) <"Qualifies the multiplier with the applicable unit.  For example in the case of the Coal (API2) CIF ARA (ARGUS-McCloskey) Futures Contract on the CME, where the unitOfAmount would be contracts, the multiplier would 1,000 and the mulitiplier Unit would be 1,000 MT (Metric Tons).">
            ''',
            '''
            class com_rosetta_test_model_UnitType(BaseDataClass):
                """
                Defines the unit to be used for price, quantity, or other purposes
                """
                currency: Optional[str] = Field(None, description='Defines the currency to be used as a unit for a price, quantity, or other purpose.')
                """
                Defines the currency to be used as a unit for a price, quantity, or other purpose.
                """'''
        )
        testUtils.assertBundleContainsExpectedString(
            '''
            type UnitType: <"Defines the unit to be used for price, quantity, or other purposes">
                currency string (0..1) <"Defines the currency to be used as a unit for a price, quantity, or other purpose.">

            type MeasureBase: <"Provides an abstract base class shared by Price and Quantity.">
                amount number (1..1) <"Specifies an amount to be qualified and used in a Price or Quantity definition.">
                unitOfAmount UnitType (1..1) <"Qualifies the unit by which the amount is measured.">

            type Quantity extends MeasureBase: <"Specifies a quantity to be associated to a financial product, for example a trade amount or a cashflow amount resulting from a trade.">
                multiplier number (0..1) <"Defines the number to be multiplied by the amount to derive a total quantity.">
                multiplierUnit UnitType (0..1) <"Qualifies the multiplier with the applicable unit.  For example in the case of the Coal (API2) CIF ARA (ARGUS-McCloskey) Futures Contract on the CME, where the unitOfAmount would be contracts, the multiplier would 1,000 and the mulitiplier Unit would be 1,000 MT (Metric Tons).">
            ''',
            '''
            class com_rosetta_test_model_Quantity(com_rosetta_test_model_MeasureBase):
                """
                Specifies a quantity to be associated to a financial product, for example a trade amount or a cashflow amount resulting from a trade.
                """
                multiplier: Optional[Decimal] = Field(None, description='Defines the number to be multiplied by the amount to derive a total quantity.')
                """
                Defines the number to be multiplied by the amount to derive a total quantity.
                """
                multiplierUnit: Optional[Annotated[com_rosetta_test_model_UnitType, com_rosetta_test_model_UnitType.serializer(), com_rosetta_test_model_UnitType.validator()]] = Field(None, description='Qualifies the multiplier with the applicable unit. For example in the case of the Coal (API2) CIF ARA (ARGUS-McCloskey) Futures Contract on the CME, where the unitOfAmount would be contracts, the multiplier would 1,000 and the mulitiplier Unit would be 1,000 MT (Metric Tons).')
                """
                Qualifies the multiplier with the applicable unit.  For example in the case of the Coal (API2) CIF ARA (ARGUS-McCloskey) Futures Contract on the CME, where the unitOfAmount would be contracts, the multiplier would 1,000 and the mulitiplier Unit would be 1,000 MT (Metric Tons).
                """'''
        )
    }

    @Test
    def void testGenerateTypesExtends() {
        testUtils.assertBundleContainsExpectedString(
            '''
            type TestType extends TestType2:
                TestTypeValue1 string (1..1) <"Test string">
                TestTypeValue2 int (0..1) <"Test int">

            type TestType2 extends TestType3:
                TestType2Value1 number (0..1) <"Test number">
                TestType2Value2 date (1..*) <"Test date">

            type TestType3:
                TestType3Value1 string (0..1) <"Test string">
                TestType4Value2 int (1..*) <"Test int">
            ''',
            '''
            class com_rosetta_test_model_TestType(com_rosetta_test_model_TestType2):
                TestTypeValue1: str = Field(..., description='Test string')
                """
                Test string
                """
                TestTypeValue2: Optional[int] = Field(None, description='Test int')
                """
                Test int
                """'''
        )
        testUtils.assertBundleContainsExpectedString(
            '''
            type TestType extends TestType2:
                TestTypeValue1 string (1..1) <"Test string">
                TestTypeValue2 int (0..1) <"Test int">

            type TestType2 extends TestType3:
                TestType2Value1 number (0..1) <"Test number">
                TestType2Value2 date (1..*) <"Test date">

            type TestType3:
                TestType3Value1 string (0..1) <"Test string">
                TestType4Value2 int (1..*) <"Test int">
            ''',
            '''
            class com_rosetta_test_model_TestType2(com_rosetta_test_model_TestType3):
                TestType2Value1: Optional[Decimal] = Field(None, description='Test number')
                """
                Test number
                """
                TestType2Value2: list[datetime.date] = Field([], description='Test date', min_length=1)
                """
                Test date
                """'''
        )
        testUtils.assertBundleContainsExpectedString(
            '''
            type TestType extends TestType2:
                TestTypeValue1 string (1..1) <"Test string">
                TestTypeValue2 int (0..1) <"Test int">

            type TestType2 extends TestType3:
                TestType2Value1 number (0..1) <"Test number">
                TestType2Value2 date (1..*) <"Test date">

            type TestType3:
                TestType3Value1 string (0..1) <"Test string">
                TestType4Value2 int (1..*) <"Test int">
            ''',
            '''
            class com_rosetta_test_model_TestType3(BaseDataClass):
                TestType3Value1: Optional[str] = Field(None, description='Test string')
                """
                Test string
                """
                TestType4Value2: list[int] = Field([], description='Test int', min_length=1)
                """
                Test int
                """'''
        )
    }

    @Test
    def void testGenerateTypesChoiceCondition() {
        testUtils.assertBundleContainsExpectedString(
            '''
            type TestType: <"Test type with one-of condition.">
                field1 string (0..1) <"Test string field 1">
                field2 string (0..1) <"Test string field 2">
                field3 number (0..1) <"Test number field 3">
                field4 number (1..*) <"Test number field 4">
                condition BusinessCentersChoice: <"Choice rule to represent an FpML choice construct.">
                        required choice field1, field2
            ''',
            '''
            class com_rosetta_test_model_TestType(BaseDataClass):
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
        )
    }

    @Test
    def void testGenerateIfThenCondition() {
        testUtils.assertBundleContainsExpectedString(
            '''
            type TestType: <"Test type with one-of condition.">
                field1 string (0..1) <"Test string field 1">
                field2 string (0..1) <"Test string field 2">
                field3 number (0..1) <"Test number field 3">
                field4 number (1..*) <"Test number field 4">
                condition BusinessCentersChoice: <"Choice rule to represent an FpML choice construct.">
                        if field1 exists
                                then field3 > 0
            ''',
            '''
            class com_rosetta_test_model_TestType(BaseDataClass):
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
        )
    }

    @Test
    def void testConditionsGeneration() {
        testUtils.assertBundleContainsExpectedString(
            '''
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
                        or (intValue2 exists and intValue1 exists and intValue1 is absent)''',
            '''
            class com_rosetta_test_model_A(BaseDataClass):
                a0: Optional[int] = Field(None, description='')
                a1: Optional[int] = Field(None, description='')
                
                @rune_condition
                def condition_0_(self):
                    item = self
                    return rune_check_one_of(self, 'a0', 'a1', necessity=True)'''
        )
        testUtils.assertBundleContainsExpectedString(
            '''
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
                        or (intValue2 exists and intValue1 exists and intValue1 is absent)''',
            '''
            class com_rosetta_test_model_B(BaseDataClass):
                intValue1: Optional[int] = Field(None, description='')
                intValue2: Optional[int] = Field(None, description='')
                aValue: Annotated[com_rosetta_test_model_A, com_rosetta_test_model_A.serializer(), com_rosetta_test_model_A.validator()] = Field(..., description='')
                
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
        )
    }
}