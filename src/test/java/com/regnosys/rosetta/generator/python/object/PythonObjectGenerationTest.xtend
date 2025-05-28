package com.regnosys.rosetta.generator.python.object

import jakarta.inject.Inject
import com.regnosys.rosetta.tests.RosettaInjectorProvider
import com.regnosys.rosetta.generator.python.PythonGeneratorTestUtils
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.extensions.InjectionExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.^extension.ExtendWith
import org.junit.jupiter.api.Disabled

@ExtendWith(InjectionExtension)
@InjectWith(RosettaInjectorProvider)
class PythonObjectGenerationTest {

    @Inject PythonGeneratorTestUtils testUtils

    @Test
    def void testMultilineAttributeDefinition() {
        val pythonString = testUtils.generatePythonFromString(
        '''
        type Foo:
            attr int (1..1) 
            <"This is a
        multiline
        definition">
        ''').toString()
        val expectedFoo = 
        '''
        class com_rosetta_test_model_Foo(BaseDataClass):
            _FQRTN = 'com.rosetta.test.model.Foo'
            attr: int = Field(..., description='This is a multiline definition')
            """
            This is a
            multiline
            definition
            """'''
        testUtils.assertGeneratedContainsExpectedString(pythonString, expectedFoo)
    }
    
    @Test
    def void testConditions1() {
        val pythonString = testUtils.generatePythonFromString(
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

            condition OneOrTwo: <"Explicit choice rule">
                optional choice intValue1, intValue2

            condition SecondOneOrTwo: <"Implicit choice rule">
                aValue->a0 exists
                or (intValue2 exists and intValue1 is absent)
                or (intValue1 exists and intValue2 is absent)
        ''').toString()
        
        val expectedA =
        '''
        class com_rosetta_test_model_A(BaseDataClass):
            _FQRTN = 'com.rosetta.test.model.A'
            a0: Optional[int] = Field(None, description='')
            a1: Optional[int] = Field(None, description='')
            
            @rune_condition
            def condition_0_(self):
                item = self
                return rune_check_one_of(self, 'a0', 'a1', necessity=True)'''

        val expectedB = 
        '''
        class com_rosetta_test_model_B(BaseDataClass):
            _FQRTN = 'com.rosetta.test.model.B'
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
                Explicit choice rule
                """
                item = self
                return rune_check_one_of(self, 'intValue1', 'intValue2', necessity=False)
            
            @rune_condition
            def condition_2_SecondOneOrTwo(self):
                """
                Implicit choice rule
                """
                item = self
                return ((rune_attr_exists(rune_resolve_attr(rune_resolve_attr(self, "aValue"), "a0")) or (rune_attr_exists(rune_resolve_attr(self, "intValue2")) and (not rune_attr_exists(rune_resolve_attr(self, "intValue1"))))) or (rune_attr_exists(rune_resolve_attr(self, "intValue1")) and (not rune_attr_exists(rune_resolve_attr(self, "intValue2")))))'''
        testUtils.assertGeneratedContainsExpectedString(pythonString, expectedA)
        testUtils.assertGeneratedContainsExpectedString(pythonString, expectedB)
    }

    @Test
    def void testGenerateTypes1() {
        val pythonString = testUtils.generatePythonFromString(
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
        ''').toString()
          
        val expectedTestType = 
        '''
        class com_rosetta_test_model_TestType(BaseDataClass):
            """
            Test type description.
            """
            _FQRTN = 'com.rosetta.test.model.TestType'
            testTypeValue1: str = Field(..., description='Test string')
            """
            Test string
            """
            testTypeValue2: Optional[str] = Field(None, description='Test optional string')
            """
            Test optional string
            """
            testTypeValue3: list[str] = Field(..., description='Test string list', min_length=1)
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
        val expectedTestType2 =
        '''
        class com_rosetta_test_model_TestType2(BaseDataClass):
            _FQRTN = 'com.rosetta.test.model.TestType2'
            testType2Value1: list[Decimal] = Field(..., description='Test number list', min_length=1)
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

        val expectedTestEnum =
        '''
        class TestEnum(rune.runtime.metadata.EnumWithMetaMixin, Enum):
            """
            Test enum description.
            """
            TEST_ENUM_VALUE_1 = "TestEnumValue1"
            """
            Test enum value 1
            """
            TEST_ENUM_VALUE_2 = "TestEnumValue2"
            """
            Test enum value 2
            """'''
        testUtils.assertGeneratedContainsExpectedString(pythonString, expectedTestType)
        testUtils.assertGeneratedContainsExpectedString(pythonString, expectedTestType2)
        testUtils.assertGeneratedContainsExpectedString(pythonString, expectedTestEnum)
    }
    
    @Test
    def void testGenerateTypesExtends1() {
        val pythonString = testUtils.generatePythonFromString(
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
        ''').toString()

        val expectedTestType = 
        '''
        class com_rosetta_test_model_TestType(com_rosetta_test_model_TestType2):
            _FQRTN = 'com.rosetta.test.model.TestType'
            TestTypeValue1: str = Field(..., description='Test string')
            """
            Test string
            """
            TestTypeValue2: Optional[int] = Field(None, description='Test int')
            """
            Test int
            """'''
        val expectedTestType2 = 
        '''
        class com_rosetta_test_model_TestType2(com_rosetta_test_model_TestType3):
            _FQRTN = 'com.rosetta.test.model.TestType2'
            TestType2Value1: Optional[Decimal] = Field(None, description='Test number')
            """
            Test number
            """
            TestType2Value2: list[datetime.date] = Field(..., description='Test date', min_length=1)
            """
            Test date
            """'''
        val expectedTestType3 =
        '''
        class com_rosetta_test_model_TestType3(BaseDataClass):
            _FQRTN = 'com.rosetta.test.model.TestType3'
            TestType3Value1: Optional[str] = Field(None, description='Test string')
            """
            Test string
            """
            TestType4Value2: list[int] = Field(..., description='Test int', min_length=1)
            """
            Test int
            """'''
        testUtils.assertGeneratedContainsExpectedString(pythonString, expectedTestType)
        testUtils.assertGeneratedContainsExpectedString(pythonString, expectedTestType2)
        testUtils.assertGeneratedContainsExpectedString(pythonString, expectedTestType3)
    }

    @Test
    def void testGenerateTypesChoiceCondition() {
        val pythonString = testUtils.generatePythonFromString(
        '''
        type TestType: <"Test type with one-of condition.">
            field1 string (0..1) <"Test string field 1">
            field2 string (0..1) <"Test string field 2">
            field3 number (0..1) <"Test number field 3">
            field4 number (1..*) <"Test number field 4">
            condition BusinessCentersChoice: <"Choice rule to represent an FpML choice construct.">
                    required choice field1, field2
        ''').toString()

        val expected =
        '''
        class com_rosetta_test_model_TestType(BaseDataClass):
            """
            Test type with one-of condition.
            """
            _FQRTN = 'com.rosetta.test.model.TestType'
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
            field4: list[Decimal] = Field(..., description='Test number field 4', min_length=1)
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
        testUtils.assertGeneratedContainsExpectedString(pythonString, expected)
    }

    @Test
    def void testGenerateIfThenCondition() {
        val pythonString = testUtils.generatePythonFromString(
        '''
        type TestType: <"Test type with one-of condition.">
            field1 string (0..1) <"Test string field 1">
            field2 string (0..1) <"Test string field 2">
            field3 number (0..1) <"Test number field 3">
            field4 number (1..*) <"Test number field 4">
            condition BusinessCentersChoice: <"Choice rule to represent an FpML choice construct.">
                    if field1 exists
                            then field3 > 0
        ''').toString()

        val expected =
        '''
        class com_rosetta_test_model_TestType(BaseDataClass):
            """
            Test type with one-of condition.
            """
            _FQRTN = 'com.rosetta.test.model.TestType'
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
            field4: list[Decimal] = Field(..., description='Test number field 4', min_length=1)
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
        testUtils.assertGeneratedContainsExpectedString(pythonString, expected)
    }

    @Test
    def void testGenerateIfThenElseCondition() {
        val pythonString = testUtils.generatePythonFromString(
        '''
        type TestType: <"Test type with one-of condition.">
            field1 string (0..1) <"Test string field 1">
            field2 string (0..1) <"Test string field 2">
            field3 number (0..1) <"Test number field 3">
            field4 number (1..*) <"Test number field 4">
            condition BusinessCentersChoice: <"Choice rule to represent an FpML choice construct.">
                    if field1 exists
                            then field3 > 0
                    else field4 > 0
        ''').toString()

        val expected =
        '''
        class com_rosetta_test_model_TestType(BaseDataClass):
            """
            Test type with one-of condition.
            """
            _FQRTN = 'com.rosetta.test.model.TestType'
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
            field4: list[Decimal] = Field(..., description='Test number field 4', min_length=1)
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
                    return rune_all_elements(rune_resolve_attr(self, "field4"), ">", 0)
                
                return if_cond_fn(rune_attr_exists(rune_resolve_attr(self, "field1")), _then_fn0, _else_fn0)'''
        testUtils.assertGeneratedContainsExpectedString(pythonString, expected)
    }

    @Test
    def void testConditionLessOrEqual() {
        val pythonString = testUtils.generatePythonFromString(
        '''
        type DateRange: <"A class defining a contiguous series of calendar dates. The date range is defined as all the dates between and including the start and the end date. The start date must fall on or before the end date.">

            startDate date (1..1) <"The first date of a date range.">
            endDate date (1..1) <"The last date of a date range.">

            condition DatesOrdered: <"The start date must fall on or before the end date (a date range of only one date is allowed).">
                startDate <= endDate
        ''').toString()

        val expectedCondition = 
        '''
        class com_rosetta_test_model_DateRange(BaseDataClass):
            """
            A class defining a contiguous series of calendar dates. The date range is defined as all the dates between and including the start and the end date. The start date must fall on or before the end date.
            """
            _FQRTN = 'com.rosetta.test.model.DateRange'
            startDate: datetime.date = Field(..., description='The first date of a date range.')
            """
            The first date of a date range.
            """
            endDate: datetime.date = Field(..., description='The last date of a date range.')
            """
            The last date of a date range.
            """
            
            @rune_condition
            def condition_0_DatesOrdered(self):
                """
                The start date must fall on or before the end date (a date range of only one date is allowed).
                """
                item = self
                return rune_all_elements(rune_resolve_attr(self, "startDate"), "<=", rune_resolve_attr(self, "endDate"))'''
        testUtils.assertGeneratedContainsExpectedString(pythonString, expectedCondition)
    }

    @Test
    def void testConditionsGeneration1() {
        val pythonString = testUtils.generatePythonFromString(
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
                    or (intValue2 exists and intValue1 exists and intValue1 is absent)
        ''').toString()
        
        val expectedA = 
        '''
        class com_rosetta_test_model_A(BaseDataClass):
            _FQRTN = 'com.rosetta.test.model.A'
            a0: Optional[int] = Field(None, description='')
            a1: Optional[int] = Field(None, description='')
            
            @rune_condition
            def condition_0_(self):
                item = self
                return rune_check_one_of(self, 'a0', 'a1', necessity=True)'''

        val expectedB =
        '''
        class com_rosetta_test_model_B(BaseDataClass):
            _FQRTN = 'com.rosetta.test.model.B'
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
        
        testUtils.assertGeneratedContainsExpectedString(pythonString, expectedA)
        testUtils.assertGeneratedContainsExpectedString(pythonString, expectedB)
    }

    @Test
    def void testGenerateTypesMethod2() {
        val pythonString = testUtils.generatePythonFromString(
        '''
        type UnitType: <"Defines the unit to be used for price, quantity, or other purposes">
            currency string (0..1) <"Defines the currency to be used as a unit for a price, quantity, or other purpose.">

        type MeasureBase: <"Provides an abstract base class shared by Price and Quantity.">
            amount number (1..1) <"Specifies an amount to be qualified and used in a Price or Quantity definition.">
            unitOfAmount UnitType (1..1) <"Qualifies the unit by which the amount is measured.">

        type Quantity extends MeasureBase: <"Specifies a quantity to be associated to a financial product, for example a trade amount or a cashflow amount resulting from a trade.">
            multiplier number (0..1) <"Defines the number to be multiplied by the amount to derive a total quantity.">
            multiplierUnit UnitType (0..1) <"Qualifies the multiplier with the applicable unit.  For example in the case of the Coal (API2) CIF ARA (ARGUS-McCloskey) Futures Contract on the CME, where the unitOfAmount would be contracts, the multiplier would 1,000 and the mulitiplier Unit would be 1,000 MT (Metric Tons).">
        ''').toString()

        val expectedMeasureBase =
        '''
        class com_rosetta_test_model_MeasureBase(BaseDataClass):
            """
            Provides an abstract base class shared by Price and Quantity.
            """
            _FQRTN = 'com.rosetta.test.model.MeasureBase'
            amount: Decimal = Field(..., description='Specifies an amount to be qualified and used in a Price or Quantity definition.')
            """
            Specifies an amount to be qualified and used in a Price or Quantity definition.
            """
            unitOfAmount: Annotated[com_rosetta_test_model_UnitType, com_rosetta_test_model_UnitType.serializer(), com_rosetta_test_model_UnitType.validator()] = Field(..., description='Qualifies the unit by which the amount is measured.')
            """
            Qualifies the unit by which the amount is measured.
            """
            '''
        val expectedUnitType = 
        '''
        class com_rosetta_test_model_UnitType(BaseDataClass):
            """
            Defines the unit to be used for price, quantity, or other purposes
            """
            _FQRTN = 'com.rosetta.test.model.UnitType'
            currency: Optional[str] = Field(None, description='Defines the currency to be used as a unit for a price, quantity, or other purpose.')
            """
            Defines the currency to be used as a unit for a price, quantity, or other purpose.
            """'''
        val expectedQuantity =
        '''
        class com_rosetta_test_model_Quantity(com_rosetta_test_model_MeasureBase):
            """
            Specifies a quantity to be associated to a financial product, for example a trade amount or a cashflow amount resulting from a trade.
            """
            _FQRTN = 'com.rosetta.test.model.Quantity'
            multiplier: Optional[Decimal] = Field(None, description='Defines the number to be multiplied by the amount to derive a total quantity.')
            """
            Defines the number to be multiplied by the amount to derive a total quantity.
            """
            multiplierUnit: Optional[Annotated[com_rosetta_test_model_UnitType, com_rosetta_test_model_UnitType.serializer(), com_rosetta_test_model_UnitType.validator()]] = Field(None, description='Qualifies the multiplier with the applicable unit. For example in the case of the Coal (API2) CIF ARA (ARGUS-McCloskey) Futures Contract on the CME, where the unitOfAmount would be contracts, the multiplier would 1,000 and the mulitiplier Unit would be 1,000 MT (Metric Tons).')
            """
            Qualifies the multiplier with the applicable unit.  For example in the case of the Coal (API2) CIF ARA (ARGUS-McCloskey) Futures Contract on the CME, where the unitOfAmount would be contracts, the multiplier would 1,000 and the mulitiplier Unit would be 1,000 MT (Metric Tons).
            """'''
        testUtils.assertGeneratedContainsExpectedString(pythonString, expectedMeasureBase)
        testUtils.assertGeneratedContainsExpectedString(pythonString, expectedUnitType)
        testUtils.assertGeneratedContainsExpectedString(pythonString, expectedQuantity)
    }

    @Disabled("testGenerateTypes3")
    @Test
    def void testGenerateTypes3() {
        val pythonString = testUtils.generatePythonFromString(
        '''
        enum AncillaryRoleEnum: <"Defines the enumerated values to specify the ancillary roles to the transaction. The product is agnostic to the actual parties involved in the transaction, with the party references abstracted away from the product definition and replaced by the AncillaryRoleEnum. The AncillaryRoleEnum can then be positioned in the product and the AncillaryParty type, which is positioned outside of the product definition, allows the AncillaryRoleEnum to be associated with an actual party reference.">
            DisruptionEventsDeterminingParty <"Specifies the party which determines additional disruption events.">
            ExtraordinaryDividendsParty <"Specifies the party which determines if dividends are extraordinary in relation to normal levels.">

        enum TelephoneTypeEnum: <"The enumerated values to specify the type of telephone number, e.g. work vs. mobile.">
            Work <"A number used primarily for work-related calls. Includes home office numbers used primarily for work purposes.">
            Mobile <"A number on a mobile telephone that is often or usually used for work-related calls. This type of number can be used for urgent work related business when a work number is not sufficient to contact the person or firm.">

        type LegalEntity: <"A class to specify a legal entity, with a required name and an optional entity identifier (such as the LEI).">
            [metadata key]
            entityId string (0..*) <"A legal entity identifier (e.g. RED entity code).">
                [metadata scheme]
            name string (1..1) <"The legal entity name.">
                [metadata scheme]

        type TelephoneNumber: <"A class to specify a telephone number as a type of phone number (e.g. work, personal, ...) alongside with the actual number.">
            _FQRTN = 'com.rosetta.test.model.TelephoneNumber'
            telephoneNumberType TelephoneTypeEnum (0..1) <"The type of telephone number, e.g. work, mobile.">
            number string (1..1) <"The actual telephone number.">

        type AncillaryEntity: <"Holds an identifier for an ancillary entity, either identified directly via its ancillary role or directly as a legal entity.">
            _FQRTN = 'com.rosetta.test.model.AncillaryEntity'
            ancillaryParty AncillaryRoleEnum (0..1) <"Identifies a party via its ancillary role on a transaction (e.g. CCP or DCO through which the trade test be cleared.)">
            legalEntity LegalEntity (0..1)

            condition: one-of
        ''').toString()

        val expectedTestType1 =
        '''
        class com_rosetta_test_model_LegalEntity(BaseDataClass):
            """
            A class to specify a legal entity, with a required name and an optional entity identifier (such as the LEI).
            """
            _FQRTN = 'com.rosetta.test.model.LegalEntity'
            entityId: list[AttributeWithMeta[str] | str] = Field([], description='A legal entity identifier (e.g. RED entity code).')
            """
            A legal entity identifier (e.g. RED entity code).
            """
            name: AttributeWithMeta[str] | str = Field(..., description='The legal entity name.')
            """
            The legal entity name.
            """'''
        val expectedTestType2 =
        '''
        class com_rosetta_test_model_TelephoneNumber(BaseDataClass):
            """
            A class to specify a telephone number as a type of phone number (e.g. work, personal, ...) alongside with the actual number.
            """
            _FQRTN = 'com.rosetta.test.model.TelephoneNumber'
            telephoneNumberType: Optional[com.rosetta.test.model.TelephoneTypeEnum.TelephoneTypeEnum] = Field(None, description='The type of telephone number, e.g. work, mobile.')
            """
            The type of telephone number, e.g. work, mobile.
            """
            number: str = Field(..., description='The actual telephone number.')
            """
            The actual telephone number.
            """'''
        val expectedTestType3 =
        '''
        class com_rosetta_test_model_AncillaryEntity(BaseDataClass):
            """
            Holds an identifier for an ancillary entity, either identified directly via its ancillary role or directly as a legal entity.
            """
            _FQRTN = 'com.rosetta.test.model.AncillaryEntity'
            ancillaryParty: Optional[com.rosetta.test.model.AncillaryRoleEnum.AncillaryRoleEnum] = Field(None, description='Identifies a party via its ancillary role on a transaction (e.g. CCP or DCO through which the trade test be cleared.)')
            """
            Identifies a party via its ancillary role on a transaction (e.g. CCP or DCO through which the trade test be cleared.)
            """
            legalEntity: Optional[com_rosetta_test_model_LegalEntity] = Field(None, description='')

            @rune_condition
            def condition_0_(self):
                item = self
                return rune_check_one_of(self, 'ancillaryParty', 'legalEntity', necessity=True)'''

        val expectedTestType4 = 
        '''
        class AncillaryRoleEnum(rune.runtime.metadata.EnumWithMetaMixin, Enum):
            """
            Defines the enumerated values to specify the ancillary roles to the transaction. The product is agnostic to the actual parties involved in the transaction, with the party references abstracted away from the product definition and replaced by the AncillaryRoleEnum. The AncillaryRoleEnum can then be positioned in the product and the AncillaryParty type, which is positioned outside of the product definition, allows the AncillaryRoleEnum to be associated with an actual party reference.
            """
            DISRUPTION_EVENTS_DETERMINING_PARTY = "DisruptionEventsDeterminingParty"
            """
            Specifies the party which determines additional disruption events.
            """
            EXTRAORDINARY_DIVIDENDS_PARTY = "ExtraordinaryDividendsParty"
            """
            Specifies the party which determines if dividends are extraordinary in relation to normal levels.
            """'''
        val expectedTestType5 = 
        '''
        class TelephoneTypeEnum(rune.runtime.metadata.EnumWithMetaMixin, Enum):
            """
            The enumerated values to specify the type of telephone number, e.g. work vs. mobile.
            """
            MOBILE = "Mobile"
            """
            A number on a mobile telephone that is often or usually used for work-related calls. This type of number can be used for urgent work related business when a work number is not sufficient to contact the person or firm.
            """
            WORK = "Work"
            """
            A number used primarily for work-related calls. Includes home office numbers used primarily for work purposes.
            """'''
        testUtils.assertGeneratedContainsExpectedString(pythonString, expectedTestType1)
        testUtils.assertGeneratedContainsExpectedString(pythonString, expectedTestType2)
        testUtils.assertGeneratedContainsExpectedString(pythonString, expectedTestType3)
        testUtils.assertGeneratedContainsExpectedString(pythonString, expectedTestType4)
        testUtils.assertGeneratedContainsExpectedString(pythonString, expectedTestType5)
    }

    @Disabled("testGenerateTypesExtends2")
    @Test
    def void testGenerateTypesExtends2() {
        val pythonString = testUtils.generatePythonFromString(
        '''
        enum CapacityUnitEnum: <"Provides enumerated values for capacity units, generally used in the context of defining quantities for commodities.">
            ALW <"Denotes Allowances as standard unit.">
            BBL <"Denotes a Barrel as a standard unit.">
            BCF <"Denotes Billion Cubic Feet as a standard unit.">

        enum WeatherUnitEnum: <"Provides enumerated values for weather units, generally used in the context of defining quantities for commodities.">
            CDD <"Denotes Cooling Degree Days as a standard unit.">
            CPD <"Denotes Critical Precipitation Day as a standard unit.">
            HDD <"Heating Degree Day as a standard unit.">

        enum FinancialUnitEnum: <"Provides enumerated values for financial units, generally used in the context of defining quantities for securities.">
            Contract <"Denotes financial contracts, such as listed futures and options.">
            ContractualProduct <"Denotes a Contractual Product as defined in the CDM.  This unit type would be used when the price applies to the whole product, for example, in the case of a premium expressed as a cash amount.">
            IndexUnit <"Denotes a price expressed in index points, e.g. for a stock index.">

        type UnitType: <"Defines the unit to be used for price, quantity, or other purposes">
            capacityUnit CapacityUnitEnum (0..1) <"Provides an enumerated value for a capacity unit, generally used in the context of defining quantities for commodities.">
            weatherUnit WeatherUnitEnum (0..1) <"Provides an enumerated values for a weather unit, generally used in the context of defining quantities for commodities.">
            financialUnit FinancialUnitEnum (0..1) <"Provides an enumerated value for financial units, generally used in the context of defining quantities for securities.">
            currency string (0..1) <"Defines the currency to be used as a unit for a price, quantity, or other purpose.">
                [metadata scheme]

            condition UnitType: <"Requires that a unit type must be set.">
                one-of

        type Measure extends MeasureBase: <"Defines a concrete measure as a number associated to a unit. It extends MeasureBase by requiring the value attribute to be present. A measure may be unit-less so the unit attribute is still optional.">

            condition ValueExists: <"The value attribute must be present in a concrete measure.">
                value exists

        type MeasureBase: <"Provides an abstract type to define a measure as a number associated to a unit. This type is abstract because all its attributes are optional. The types that extend it can specify further existence constraints.">

            value number (0..1) <"Specifies the value of the measure as a number. Optional because in a measure vector or schedule, this single value may be omitted.">
            unit UnitType (0..1) <"Qualifies the unit by which the amount is measured. Optional because a measure may be unit-less (e.g. when representing a ratio between amounts in the same unit).">
        ''').toString()

        val expectedTestType1 =
        '''
        class com_rosetta_test_model_MeasureBase(BaseDataClass):
            """
            Provides an abstract type to define a measure as a number associated to a unit. This type is abstract because all its attributes are optional. The types that extend it can specify further existence constraints.
            """
            _FQRTN = 'com.rosetta.test.model.MeasureBase'

            value: Optional[Decimal] = Field(None, description='Specifies the value of the measure as a number. Optional because in a measure vector or schedule, this single value may be omitted.')
            """
            Specifies the value of the measure as a number. Optional because in a measure vector or schedule, this single value may be omitted.
            """
            unit: Optional[com_rosetta_test_model_UnitType] = Field(None, description='Qualifies the unit by which the amount is measured. Optional because a measure may be unit-less (e.g. when representing a ratio between amounts in the same unit).')
            """
            Qualifies the unit by which the amount is measured. Optional because a measure may be unit-less (e.g. when representing a ratio between amounts in the same unit).
            """'''

        val expectedTestType2 =
        '''
        class com_rosetta_test_model_Measure(com_rosetta_test_model_MeasureBase):
            """
            Defines a concrete measure as a number associated to a unit. It extends MeasureBase by requiring the value attribute to be present. A measure may be unit-less so the unit attribute is still optional.
            """
            _FQRTN = 'com.rosetta.test.model.Measure'

            @rune_condition
            def condition_0_ValueExists(self):
                """
                The value attribute must be present in a concrete measure.
                """
                item = self
                return rune_attr_exists(rune_resolve_attr(self, "value"))'''
        
        val expectedTestType3 =
        '''
        class WeatherUnitEnum(rune.runtime.metadata.EnumWithMetaMixin, Enum):
            """
            Provides enumerated values for weather units, generally used in the context of defining quantities for commodities.
            """
            CDD = "CDD"
            """
            Denotes Cooling Degree Days as a standard unit.
            """
            CPD = "CPD"
            """
            Denotes Critical Precipitation Day as a standard unit.
            """
            HDD = "HDD"
            """
            Heating Degree Day as a standard unit.
            """'''
            
        val expectedTestType4 =
        '''
        class FinancialUnitEnum(rune.runtime.metadata.EnumWithMetaMixin, Enum):
            """
            Provides enumerated values for financial units, generally used in the context of defining quantities for securities.
            """
            CONTRACT = "Contract"
            """
            Denotes financial contracts, such as listed futures and options.
            """
            CONTRACTUAL_PRODUCT = "ContractualProduct"
            """
            Denotes a Contractual Product as defined in the CDM.  This unit type would be used when the price applies to the whole product, for example, in the case of a premium expressed as a cash amount.
            """
            INDEX_UNIT = "IndexUnit"
            """
            Denotes a price expressed in index points, e.g. for a stock index.
            """'''
        val expectedTestType5 = 
        '''
        class UnitType(BaseDataClass):
            """
            Defines the unit to be used for price, quantity, or other purposes
            """
            _FQRTN = 'com.rosetta.test.model.UnitType'

            capacityUnit: Optional[com.rosetta.test.model.CapacityUnitEnum.CapacityUnitEnum] = Field(None, description='Provides an enumerated value for a capacity unit, generally used in the context of defining quantities for commodities.')
            """
            Provides an enumerated value for a capacity unit, generally used in the context of defining quantities for commodities.
            """
            weatherUnit: Optional[com.rosetta.test.model.WeatherUnitEnum.WeatherUnitEnum] = Field(None, description='Provides an enumerated values for a weather unit, generally used in the context of defining quantities for commodities.')
            """
            Provides an enumerated values for a weather unit, generally used in the context of defining quantities for commodities.
            """
            financialUnit: Optional[com.rosetta.test.model.FinancialUnitEnum.FinancialUnitEnum] = Field(None, description='Provides an enumerated value for financial units, generally used in the context of defining quantities for securities.')
            """
            Provides an enumerated value for financial units, generally used in the context of defining quantities for securities.
            """
            currency: Optional[AttributeWithMeta[str] | str] = Field(None, description='Defines the currency to be used as a unit for a price, quantity, or other purpose.')
            """
            Defines the currency to be used as a unit for a price, quantity, or other purpose.
            """

            @rune_condition
            def condition_0_UnitType(self):
                """
                Requires that a unit type must be set.
                """
                item = self
                return rune_check_one_of(self, 'capacityUnit', 'weatherUnit', 'financialUnit', 'currency', necessity=True)'''
        
        val expectedTestType6 = 
        '''
        class CapacityUnitEnum(rune.runtime.metadata.EnumWithMetaMixin, Enum):
            """
            Provides enumerated values for capacity units, generally used in the context of defining quantities for commodities.
            """
            ALW = "ALW"
            """
            Denotes Allowances as standard unit.
            """
            BBL = "BBL"
            """
            Denotes a Barrel as a standard unit.
            """
            BCF = "BCF"
            """
            Denotes Billion Cubic Feet as a standard unit.
            """'''

        testUtils.assertGeneratedContainsExpectedString(pythonString, expectedTestType1)
        testUtils.assertGeneratedContainsExpectedString(pythonString, expectedTestType2)
        testUtils.assertGeneratedContainsExpectedString(pythonString, expectedTestType3)
        testUtils.assertGeneratedContainsExpectedString(pythonString, expectedTestType4)
        testUtils.assertGeneratedContainsExpectedString(pythonString, expectedTestType5)
        testUtils.assertGeneratedContainsExpectedString(pythonString, expectedTestType6)
    }
    // TODO: tests disabled to align to new meta data support - add them back
    @Disabled("testGenerateTypes2")
    @Test 
    def void testGenerateTypes2() {
        val pythonString = testUtils.generatePythonFromString(
        '''
        enum CapacityUnitEnum: <"Provides enumerated values for capacity units, generally used in the context of defining quantities for commodities.">
            ALW <"Denotes Allowances as standard unit.">
            BBL <"Denotes a Barrel as a standard unit.">
        
        enum WeatherUnitEnum: <"Provides enumerated values for weather units, generally used in the context of defining quantities for commodities.">
            CDD <"Denotes Cooling Degree Days as a standard unit.">
            CPD <"Denotes Critical Precipitation Day as a standard unit.">
        
        enum FinancialUnitEnum: <"Provides enumerated values for financial units, generally used in the context of defining quantities for securities.">
            Contract <"Denotes financial contracts, such as listed futures and options.">
            ContractualProduct <"Denotes a Contractual Product as defined in the CDM.  This unit type would be used when the price applies to the whole product, for example, in the case of a premium expressed as a cash amount.">
        
        type UnitType: <"Defines the unit to be used for price, quantity, or other purposes">
            capacityUnit CapacityUnitEnum (0..1) <"Provides an enumerated value for a capacity unit, generally used in the context of defining quantities for commodities.">
            weatherUnit WeatherUnitEnum (0..1) <"Provides an enumerated values for a weather unit, generally used in the context of defining quantities for commodities.">
            [metadata scheme]

        condition UnitType: <"Requires that a unit type must be set.">
            one-of
        ''').toString()

        val expectedTestType =
        '''
        class class com_rosetta_test_model_UnitType(BaseDataClass):
            """
            Defines the unit to be used for price, quantity, or other purposes
            """
            _FQRTN = 'com.rosetta.test.model.UnitType'

            capacityUnit: Optional[com.rosetta.test.model.CapacityUnitEnum.CapacityUnitEnum] = Field(None, description='Provides an enumerated value for a capacity unit, generally used in the context of defining quantities for commodities.')
            """
            Provides an enumerated value for a capacity unit, generally used in the context of defining quantities for commodities.
            """
            weatherUnit: Optional[com.rosetta.test.model.WeatherUnitEnum.WeatherUnitEnum] = Field(None, description='Provides an enumerated values for a weather unit, generally used in the context of defining quantities for commodities.')
            """
            Provides an enumerated values for a weather unit, generally used in the context of defining quantities for commodities.
            """'''
        val expectedTestType2 =
        '''
        class FinancialUnitEnum(rune.runtime.metadata.EnumWithMetaMixin, Enum):
            """
            Provides enumerated values for financial units, generally used in the context of defining quantities for securities.
            """
            CONTRACT = "Contract"
            """
            Denotes financial contracts, such as listed futures and options.
            """
            CONTRACTUAL_PRODUCT = "ContractualProduct"
            """

            @rune_condition
            def condition_0_UnitType(self):
                """
                Requires that a unit type must be set.
                """
                item = self
                return rune_check_one_of(self, 'capacityUnit', 'weatherUnit', 'financialUnit', 'currency', necessity=True)'''
        val expectedTestType3 =
        '''
        class WeatherUnitEnum(rune.runtime.metadata.EnumWithMetaMixin, Enum):
            """
            Provides enumerated values for weather units, generally used in the context of defining quantities for commodities.
            """
            CDD = "CDD"
            """
            Denotes Cooling Degree Days as a standard unit.
            """
            CPD = "CPD"
            """'''
        val expectedTestType4 =
        '''
        class CapacityUnitEnum(rune.runtime.metadata.EnumWithMetaMixin, Enum):
        """
        Provides enumerated values for capacity units, generally used in the context of defining quantities for commodities.
        """
        ALW = "ALW"
        """
        Denotes Allowances as standard unit.
        """
        BBL = "BBL"
        """
        Denotes a Barrel as a standard unit.
        """'''

        testUtils.assertGeneratedContainsExpectedString(pythonString, expectedTestType)
        testUtils.assertGeneratedContainsExpectedString(pythonString, expectedTestType2)
        testUtils.assertGeneratedContainsExpectedString(pythonString, expectedTestType3)
        testUtils.assertGeneratedContainsExpectedString(pythonString, expectedTestType4)
    }
}