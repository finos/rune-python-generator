package com.regnosys.rosetta.generator.python.object;

import jakarta.inject.Inject;
import com.regnosys.rosetta.tests.RosettaInjectorProvider;
import com.regnosys.rosetta.generator.python.PythonGeneratorTestUtils;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * This test class contains a mix of Anchor and Component tests.
 * Anchor tests (e.g., testGenerateTypesExtends) must verify the complete
 * three-phase output.
 */
@ExtendWith(InjectionExtension.class)
@InjectWith(RosettaInjectorProvider.class)
public class PythonInheritanceGeneratorTest {

    /**
     * PythonGeneratorTestUtils is used to generate Python code from Rosetta models.
     */
    @Inject
    private PythonGeneratorTestUtils testUtils;

    /**
     * Test case for extending a type with the same attribute.
     */
    @Test
    public void testExtendATypeWithSameAttribute() {
        testUtils.assertBundleContainsExpectedString(
                """
                        type Foo:
                            a string (0..1)
                            b string (0..1)

                        type Bar extends Foo:
                            a string (0..1)
                        """,
                """
                        class com_rosetta_test_model_Foo(BaseDataClass):
                            _FQRTN = 'com.rosetta.test.model.Foo'
                            a: Optional[str] = Field(None, description='')
                            b: Optional[str] = Field(None, description='')""");
        testUtils.assertBundleContainsExpectedString(
                """
                        type Foo:
                            a string (0..1)
                            b string (0..1)

                        type Bar extends Foo:
                            a string (0..1)
                        """,
                """
                        class com_rosetta_test_model_Bar(com_rosetta_test_model_Foo):
                            _FQRTN = 'com.rosetta.test.model.Bar'
                            a: Optional[str] = Field(None, description='')""");
    }

    /**
     * Test case for setting attributes on an empty class with inheritance.
     */
    @Test
    public void testSetAttributesOnEmptyClassWithInheritance() {
        testUtils.assertBundleContainsExpectedString(
                """
                        type B:
                            b string (1..1)

                        type A extends B:
                        """,
                """
                        class com_rosetta_test_model_A(com_rosetta_test_model_B):
                            _FQRTN = 'com.rosetta.test.model.A'
                            pass""");
    }

    /**
     * Test case for generating types with inheritance.
     */
    @Test
    public void testGenerateTypesExtends() {
        String pythonString = testUtils.generatePythonFromString(
                """
                        type TestType extends TestType2:
                            TestTypeValue1 string (1..1) <"Test string">
                            TestTypeValue2 int (0..1) <"Test int">

                        type TestType2 extends TestType3:
                            TestType2Value1 number (0..1) <"Test number">
                            TestType2Value2 date (1..*) <"Test date">

                        type TestType3:
                            TestType3Value1 string (0..1) <"Test string">
                            TestType4Value2 int (1..*) <"Test int">
                        """).toString();
        testUtils.assertGeneratedContainsExpectedString(
                pythonString,
                """
                        class com_rosetta_test_model_TestType3(BaseDataClass):
                            _FQRTN = 'com.rosetta.test.model.TestType3'
                            TestType3Value1: Optional[str] = Field(None, description='Test string')
                            \"""
                            Test string
                            \"""
                            TestType4Value2: list[int] = Field(..., description='Test int', min_length=1)
                            \"""
                            Test int
                            \"""


                        class com_rosetta_test_model_TestType2(com_rosetta_test_model_TestType3):
                            _FQRTN = 'com.rosetta.test.model.TestType2'
                            TestType2Value1: Optional[Decimal] = Field(None, description='Test number')
                            \"""
                            Test number
                            \"""
                            TestType2Value2: list[datetime.date] = Field(..., description='Test date', min_length=1)
                            \"""
                            Test date
                            \"""


                        class com_rosetta_test_model_TestType(com_rosetta_test_model_TestType2):
                            _FQRTN = 'com.rosetta.test.model.TestType'
                            TestTypeValue1: str = Field(..., description='Test string')
                            \"""
                            Test string
                            \"""
                            TestTypeValue2: Optional[int] = Field(None, description='Test int')
                            \"""
                            Test int
                            \"""
                        """);
    }

    /**
     * Test case for inheritance with delayed updates.
     */
    @Test
    public void testInheritanceWithDelayedUpdates() {
        String pythonString = testUtils.generatePythonFromString(
                """
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
                        """)
                .toString();

        String expectedTestType1 = """
                class com_rosetta_test_model_MeasureBase(BaseDataClass):
                    \"""
                    Provides an abstract type to define a measure as a number associated to a unit. This type is abstract because all its attributes are optional. The types that extend it can specify further existence constraints.
                    \"""
                    _FQRTN = 'com.rosetta.test.model.MeasureBase'
                    value: Optional[Decimal] = Field(None, description='Specifies the value of the measure as a number. Optional because in a measure vector or schedule, this single value may be omitted.')
                    \"""
                    Specifies the value of the measure as a number. Optional because in a measure vector or schedule, this single value may be omitted.
                    \"""
                    unit: Optional[com_rosetta_test_model_UnitType] = Field(None, description='Qualifies the unit by which the amount is measured. Optional because a measure may be unit-less (e.g. when representing a ratio between amounts in the same unit).')
                """;

        String expectedTestType2 = """
                class com_rosetta_test_model_Measure(com_rosetta_test_model_MeasureBase):
                    \"""
                    Defines a concrete measure as a number associated to a unit. It extends MeasureBase by requiring the value attribute to be present. A measure may be unit-less so the unit attribute is still optional.
                    \"""
                    _FQRTN = 'com.rosetta.test.model.Measure'

                    @rune_condition
                    def condition_0_ValueExists(self):
                        \"""
                        The value attribute must be present in a concrete measure.
                        \"""
                        item = self
                        return rune_attr_exists(rune_resolve_attr(self, "value"))
                """;

        String expectedTestType5 = """
                class com_rosetta_test_model_UnitType(BaseDataClass):
                    \"""
                    Defines the unit to be used for price, quantity, or other purposes
                    \"""
                    _FQRTN = 'com.rosetta.test.model.UnitType'
                    capacityUnit: Optional[com.rosetta.test.model.CapacityUnitEnum.CapacityUnitEnum] = Field(None, description='Provides an enumerated value for a capacity unit, generally used in the context of defining quantities for commodities.')
                    \"""
                    Provides an enumerated value for a capacity unit, generally used in the context of defining quantities for commodities.
                    \"""
                    weatherUnit: Optional[com.rosetta.test.model.WeatherUnitEnum.WeatherUnitEnum] = Field(None, description='Provides an enumerated values for a weather unit, generally used in the context of defining quantities for commodities.')
                    \"""
                    Provides an enumerated values for a weather unit, generally used in the context of defining quantities for commodities.
                    \"""
                    financialUnit: Optional[com.rosetta.test.model.FinancialUnitEnum.FinancialUnitEnum] = Field(None, description='Provides an enumerated value for financial units, generally used in the context of defining quantities for securities.')
                    \"""
                    Provides an enumerated value for financial units, generally used in the context of defining quantities for securities.
                    \"""
                    currency: Optional[Annotated[StrWithMeta, StrWithMeta.serializer(), StrWithMeta.validator(('@scheme', ))]] = Field(None, description='Defines the currency to be used as a unit for a price, quantity, or other purpose.')
                    \"""
                    Defines the currency to be used as a unit for a price, quantity, or other purpose.
                    \"""

                    @rune_condition
                    def condition_0_UnitType(self):
                        \"""
                        Requires that a unit type must be set.
                        \"""
                        item = self
                        return rune_check_one_of(self, 'capacityUnit', 'weatherUnit', 'financialUnit', 'currency', necessity=True)
                """;

        String expectedPhase2 = """
                # Phase 2: Delayed Annotation Updates
                com_rosetta_test_model_MeasureBase.__annotations__["unit"] = Optional[Annotated[com_rosetta_test_model_UnitType, com_rosetta_test_model_UnitType.serializer(), com_rosetta_test_model_UnitType.validator()]]

                # Phase 3: Rebuild
                com_rosetta_test_model_MeasureBase.model_rebuild()
                """;

        testUtils.assertGeneratedContainsExpectedString(pythonString, expectedTestType1);
        testUtils.assertGeneratedContainsExpectedString(pythonString, expectedTestType2);
        testUtils.assertGeneratedContainsExpectedString(pythonString, expectedTestType5);
        testUtils.assertGeneratedContainsExpectedString(pythonString, expectedPhase2);
    }
}
