package com.regnosys.rosetta.generator.python.object;

import jakarta.inject.Inject;
import com.regnosys.rosetta.tests.RosettaInjectorProvider;
import com.regnosys.rosetta.generator.python.PythonGeneratorTestUtils;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests for Python inheritance code generation using the standalone partitioning strategy.
 */
@ExtendWith(InjectionExtension.class)
@InjectWith(RosettaInjectorProvider.class)
@SuppressWarnings("checkstyle:LineLength")
public class PythonInheritanceGeneratorTest {

    @Inject
    private PythonGeneratorTestUtils testUtils;

    @Test
    public void testInheritance() {
        testUtils.assertBundleContainsExpectedString(
            """
            type A extends B:
                a string (1..1)

            type B:
                b string (1..1)
            """,
            """
            class B(BaseDataClass):
                b: str = Field(..., description='')
            """);

        testUtils.assertBundleContainsExpectedString(
            """
            type A extends B:
                a string (1..1)

            type B:
                b string (1..1)
            """,
            """
            class A(B):
                a: str = Field(..., description='')
            """);
    }

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
            class Foo(BaseDataClass):
                a: Optional[str] = Field(None, description='')
                b: Optional[str] = Field(None, description='')
            """);

        testUtils.assertBundleContainsExpectedString(
            """
            type Foo:
                a string (0..1)
                b string (0..1)

            type Bar extends Foo:
                a string (0..1)
            """,
            """
            class Bar(Foo):
                a: Optional[str] = Field(None, description='')
            """);
    }

    @Test
    public void testSetAttributesOnEmptyClassWithInheritance() {
        testUtils.assertBundleContainsExpectedString(
            """
            type A extends B:

            type B:
                b string (1..1)
            """,
            """
            class A(B):
                pass
            """);
    }

    @Test
    public void testInheritanceWithDelayedUpdates() {
        String model = """
            type MeasureBase: <"Provides an abstract type to define a measure as a number associated to a unit. This type is abstract because all its attributes are optional. The types that extend it can specify further existence constraints.">
                value number (0..1) <"Specifies the value of the measure as a number. Optional because in a measure vector or schedule, this single value may be omitted.">
                unit UnitType (0..1) <"Qualifies the unit by which the amount is measured. Optional because a measure may be unit-less (e.g. when representing a ratio between amounts in the same unit).">

            type Measure extends MeasureBase: <"Defines a concrete measure as a number associated to a unit. It extends MeasureBase by requiring the value attribute to be present. A measure may be unit-less so the unit attribute is still optional.">
                condition ValueExists:
                    value exists

            type UnitType: <"Defines the unit to be used for price, quantity, or other purposes">
                capacityUnit CapacityUnitEnum (0..1) <"Provides an enumerated value for a capacity unit, generally used in the context of defining quantities for commodities.">
                weatherUnit WeatherUnitEnum (0..1) <"Provides an enumerated values for a weather unit, generally used in the context of defining quantities for commodities.">
                financialUnit FinancialUnitEnum (0..1) <"Provides an enumerated value for financial units, generally used in the context of defining quantities for securities.">
                currency string (0..1) <"Defines the currency to be used as a unit for a price, quantity, or other purpose."> [metadata scheme]
                condition UnitType:
                    one-of

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
            """;

        testUtils.assertBundleContainsExpectedString(model,
            """
            class MeasureBase(BaseDataClass):
                \"\"\"
                Provides an abstract type to define a measure as a number associated to a unit. This type is abstract because all its attributes are optional. The types that extend it can specify further existence constraints.
                \"\"\"
                value: Optional[Decimal] = Field(None, description='Specifies the value of the measure as a number. Optional because in a measure vector or schedule, this single value may be omitted.')
                \"\"\"
                Specifies the value of the measure as a number. Optional because in a measure vector or schedule, this single value may be omitted.
                \"\"\"
                unit: Optional[UnitType] = Field(None, description='Qualifies the unit by which the amount is measured. Optional because a measure may be unit-less (e.g. when representing a ratio between amounts in the same unit).')
                \"\"\"
                Qualifies the unit by which the amount is measured. Optional because a measure may be unit-less (e.g. when representing a ratio between amounts in the same unit).
                \"\"\"
            """);

        testUtils.assertBundleContainsExpectedString(model,
            """
            class Measure(MeasureBase):
                \"\"\"
                Defines a concrete measure as a number associated to a unit. It extends MeasureBase by requiring the value attribute to be present. A measure may be unit-less so the unit attribute is still optional.
                \"\"\"
            
                @rune_condition
                def condition_0_ValueExists(self):
                    item = self
                    return rune_attr_exists(rune_resolve_attr(self, "value"))
            """);
            
        testUtils.assertBundleContainsExpectedString(model,
            """
            class UnitType(BaseDataClass):
                \"\"\"
                Defines the unit to be used for price, quantity, or other purposes
                \"\"\"
                capacityUnit: Optional[com.rosetta.test.model.CapacityUnitEnum.CapacityUnitEnum] = Field(None, description='Provides an enumerated value for a capacity unit, generally used in the context of defining quantities for commodities.')
                \"\"\"
                Provides an enumerated value for a capacity unit, generally used in the context of defining quantities for commodities.
                \"\"\"
                weatherUnit: Optional[com.rosetta.test.model.WeatherUnitEnum.WeatherUnitEnum] = Field(None, description='Provides an enumerated values for a weather unit, generally used in the context of defining quantities for commodities.')
                \"\"\"
                Provides an enumerated values for a weather unit, generally used in the context of defining quantities for commodities.
                \"\"\"
                financialUnit: Optional[com.rosetta.test.model.FinancialUnitEnum.FinancialUnitEnum] = Field(None, description='Provides an enumerated value for financial units, generally used in the context of defining quantities for securities.')
                \"\"\"
                Provides an enumerated value for financial units, generally used in the context of defining quantities for securities.
                \"\"\"
                currency: Annotated[Optional[StrWithMeta], StrWithMeta.serializer(), StrWithMeta.validator(('@scheme', ))] = Field(None, description='Defines the currency to be used as a unit for a price, quantity, or other purpose.')
                \"\"\"
                Defines the currency to be used as a unit for a price, quantity, or other purpose.
                \"\"\"
            
                @rune_condition
                def condition_0_UnitType(self):
                    item = self
                    return rune_check_one_of(self, 'capacityUnit', 'weatherUnit', 'financialUnit', 'currency', necessity=True)
            """);
    }
}
