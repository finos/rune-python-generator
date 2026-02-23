package com.regnosys.rosetta.generator.python.object;

import jakarta.inject.Inject;
import com.regnosys.rosetta.tests.RosettaInjectorProvider;
import com.regnosys.rosetta.generator.python.PythonGeneratorTestUtils;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(InjectionExtension.class)
@InjectWith(RosettaInjectorProvider.class)
public class PythonBasicTypeGeneratorTest {

    @Inject
    private PythonGeneratorTestUtils testUtils;

    @Test
    public void testGenerateBasicTypeString() {
        testUtils.assertBundleContainsExpectedString(
                """
                        type Tester:
                            one string (0..1)
                            list string (1..*)
                        """,
                """
                        class com_rosetta_test_model_Tester(BaseDataClass):
                            _FQRTN = 'com.rosetta.test.model.Tester'
                            one: Optional[str] = Field(None, description='')
                            list: list[str] = Field(..., description='', min_length=1)""");
    }

    @Test
    public void testGenerateBasicTypeInt() {
        testUtils.assertBundleContainsExpectedString(
                """
                        type Tester:
                            one int (0..1)
                            list int (1..*)
                        """,
                """
                        class com_rosetta_test_model_Tester(BaseDataClass):
                            _FQRTN = 'com.rosetta.test.model.Tester'
                            one: Optional[int] = Field(None, description='')
                            list: list[int] = Field(..., description='', min_length=1)""");
    }

    @Test
    public void testGenerateBasicTypeNumber() {
        testUtils.assertBundleContainsExpectedString(
                """
                        type Tester:
                            one number (0..1)
                            list number (1..*)
                        """,
                """
                        class com_rosetta_test_model_Tester(BaseDataClass):
                            _FQRTN = 'com.rosetta.test.model.Tester'
                            one: Optional[Decimal] = Field(None, description='')
                            list: list[Decimal] = Field(..., description='', min_length=1)""");
    }

    @Test
    public void testGenerateBasicTypeBoolean() {
        testUtils.assertBundleContainsExpectedString(
                """
                        type Tester:
                            one boolean (0..1)
                            list boolean (1..*)
                        """,
                """
                        class com_rosetta_test_model_Tester(BaseDataClass):
                            _FQRTN = 'com.rosetta.test.model.Tester'
                            one: Optional[bool] = Field(None, description='')
                            list: list[bool] = Field(..., description='', min_length=1)""");
    }

    @Test
    public void testGenerateBasicTypeDate() {
        testUtils.assertBundleContainsExpectedString(
                """
                        type Tester:
                            one date (0..1)
                            list date (1..*)
                        """,
                """
                        class com_rosetta_test_model_Tester(BaseDataClass):
                            _FQRTN = 'com.rosetta.test.model.Tester'
                            one: Optional[datetime.date] = Field(None, description='')
                            list: list[datetime.date] = Field(..., description='', min_length=1)""");
    }

    @Test
    public void testGenerateBasicTypeDateTime() {
        testUtils.assertBundleContainsExpectedString(
                """
                        type Tester:
                            one date (0..1)
                            list date (1..*)
                            zoned zonedDateTime (0..1)
                        """,
                """
                        class com_rosetta_test_model_Tester(BaseDataClass):
                            _FQRTN = 'com.rosetta.test.model.Tester'
                            one: Optional[datetime.date] = Field(None, description='')
                            list: list[datetime.date] = Field(..., description='', min_length=1)
                            zoned: Optional[datetime.datetime] = Field(None, description='')""");
    }

    @Test
    public void testGenerateBasicTypeTime() {
        testUtils.assertBundleContainsExpectedString(
                """
                        type Tester:
                            one time (0..1)
                            list time (1..*)
                        """,
                """
                        class com_rosetta_test_model_Tester(BaseDataClass):
                            _FQRTN = 'com.rosetta.test.model.Tester'
                            one: Optional[datetime.time] = Field(None, description='')
                            list: list[datetime.time] = Field(..., description='', min_length=1)""");
    }

    @Test
    public void testOmitGlobalKeyAnnotationWhenNotDefined() {
        testUtils.assertBundleContainsExpectedString(
                """
                        type AttributeGlobalKeyTest:
                            withoutGlobalKey string (1..1)
                        """,
                """
                        class com_rosetta_test_model_AttributeGlobalKeyTest(BaseDataClass):
                            _FQRTN = 'com.rosetta.test.model.AttributeGlobalKeyTest'
                            withoutGlobalKey: str = Field(..., description='')""");
    }

    @Test
    public void testGenerateRosettaCalculationTypeAsString() {
        testUtils.assertBundleContainsExpectedString(
                """
                        type Foo:
                            bar calculation (0..1)
                        """,
                """
                        class com_rosetta_test_model_Foo(BaseDataClass):
                            _FQRTN = 'com.rosetta.test.model.Foo'
                            bar: Optional[str] = Field(None, description='')""");
    }

    @Test
    public void testGenerateTypes() {
        String pythonString = testUtils.generatePythonFromString(
                """
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
                        """).toString();
        testUtils.assertGeneratedContainsExpectedString(
                pythonString,
                """
                        class com_rosetta_test_model_TestType(BaseDataClass):
                            \"""
                            Test type description.
                            \"""
                            _FQRTN = 'com.rosetta.test.model.TestType'
                            testTypeValue1: str = Field(..., description='Test string')
                            \"""
                            Test string
                            \"""
                            testTypeValue2: Optional[str] = Field(None, description='Test optional string')
                            \"""
                            Test optional string
                            \"""
                            testTypeValue3: list[str] = Field(..., description='Test string list', min_length=1)
                            \"""
                            Test string list
                            \"""
                            testTypeValue4: Annotated[com_rosetta_test_model_TestType2, com_rosetta_test_model_TestType2.serializer(), com_rosetta_test_model_TestType2.validator()] = Field(..., description='Test TestType2')
                            \"""
                            Test TestType2
                            \"""
                            testEnum: Optional[com.rosetta.test.model.TestEnum.TestEnum] = Field(None, description='Optional test enum')
                            \"""
                            Optional test enum
                            \"""
                        """);
        testUtils.assertGeneratedContainsExpectedString(
                pythonString,
                """
                        class com_rosetta_test_model_TestType2(BaseDataClass):
                            _FQRTN = 'com.rosetta.test.model.TestType2'
                            testType2Value1: list[Decimal] = Field(..., description='Test number list', min_length=1)
                            \"""
                            Test number list
                            \"""
                            testType2Value2: Optional[datetime.date] = Field(None, description='Test date')
                            \"""
                            Test date
                            \"""
                            testEnum: Optional[com.rosetta.test.model.TestEnum.TestEnum] = Field(None, description='Optional test enum')
                            \"""
                            Optional test enum
                            \"""
                        """);
    }

    @Test
    public void testGenerateTypesMethod2() {
        String pythonString = testUtils.generatePythonFromString(
                """
                        type UnitType: <"Defines the unit to be used for price, quantity, or other purposes">
                            currency string (0..1) <"Defines the currency to be used as a unit for a price, quantity, or other purpose.">

                        type MeasureBase: <"Provides an abstract base class shared by Price and Quantity.">
                            amount number (1..1) <"Specifies an amount to be qualified and used in a Price or Quantity definition.">
                            unitOfAmount UnitType (1..1) <"Qualifies the unit by which the amount is measured.">

                        type Quantity extends MeasureBase: <"Specifies a quantity to be associated to a financial product, for example a trade amount or a cashflow amount resulting from a trade.">
                            multiplier number (0..1) <"Defines the number to be multiplied by the amount to derive a total quantity.">
                            multiplierUnit UnitType (0..1) <"Qualifies the multiplier with the applicable unit.  For example in the case of the Coal (API2) CIF ARA (ARGUS-McCloskey) Futures Contract on the CME, where the unitOfAmount would be contracts, the multiplier would 1,000 and the mulitiplier Unit would be 1,000 MT (Metric Tons).">
                        """)
                .toString();
        testUtils.assertGeneratedContainsExpectedString(
                pythonString,
                """
                        class com_rosetta_test_model_MeasureBase(BaseDataClass):
                            \"""
                            Provides an abstract base class shared by Price and Quantity.
                            \"""
                            _FQRTN = 'com.rosetta.test.model.MeasureBase'
                            amount: Decimal = Field(..., description='Specifies an amount to be qualified and used in a Price or Quantity definition.')
                            \"""
                            Specifies an amount to be qualified and used in a Price or Quantity definition.
                            \"""
                            unitOfAmount: Annotated[com_rosetta_test_model_UnitType, com_rosetta_test_model_UnitType.serializer(), com_rosetta_test_model_UnitType.validator()] = Field(..., description='Qualifies the unit by which the amount is measured.')
                            \"""
                            Qualifies the unit by which the amount is measured.
                            \"""
                        """);
        testUtils.assertGeneratedContainsExpectedString(
                pythonString,
                """
                        class com_rosetta_test_model_UnitType(BaseDataClass):
                            \"""
                            Defines the unit to be used for price, quantity, or other purposes
                            \"""
                            _FQRTN = 'com.rosetta.test.model.UnitType'
                            currency: Optional[str] = Field(None, description='Defines the currency to be used as a unit for a price, quantity, or other purpose.')
                            \"""
                            Defines the currency to be used as a unit for a price, quantity, or other purpose.
                            \"""
                        """);
        testUtils.assertGeneratedContainsExpectedString(
                pythonString,
                """
                        class com_rosetta_test_model_Quantity(com_rosetta_test_model_MeasureBase):
                            \"""
                            Specifies a quantity to be associated to a financial product, for example a trade amount or a cashflow amount resulting from a trade.
                            \"""
                            _FQRTN = 'com.rosetta.test.model.Quantity'
                            multiplier: Optional[Decimal] = Field(None, description='Defines the number to be multiplied by the amount to derive a total quantity.')
                            \"""
                            Defines the number to be multiplied by the amount to derive a total quantity.
                            \"""
                            multiplierUnit: Optional[Annotated[com_rosetta_test_model_UnitType, com_rosetta_test_model_UnitType.serializer(), com_rosetta_test_model_UnitType.validator()]] = Field(None, description='Qualifies the multiplier with the applicable unit. For example in the case of the Coal (API2) CIF ARA (ARGUS-McCloskey) Futures Contract on the CME, where the unitOfAmount would be contracts, the multiplier would 1,000 and the mulitiplier Unit would be 1,000 MT (Metric Tons).')
                            \"""
                            Qualifies the multiplier with the applicable unit.  For example in the case of the Coal (API2) CIF ARA (ARGUS-McCloskey) Futures Contract on the CME, where the unitOfAmount would be contracts, the multiplier would 1,000 and the mulitiplier Unit would be 1,000 MT (Metric Tons).
                            \"""
                        """);
    }

    @Test
    public void testMultilineAttributeDefinition() {
        testUtils.assertBundleContainsExpectedString(
                """
                        type MultilineDefinition:
                            field string (1..1) <"This is a multiline
                                definition for a field.">
                        """,
                """
                        class com_rosetta_test_model_MultilineDefinition(BaseDataClass):
                            _FQRTN = 'com.rosetta.test.model.MultilineDefinition'
                            field: str = Field(..., description='This is a multiline definition for a field.')""");
    }
}
