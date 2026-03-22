package com.regnosys.rosetta.generator.python.object;

import com.regnosys.rosetta.generator.python.PythonGeneratorTestUtils;
import com.regnosys.rosetta.tests.RosettaInjectorProvider;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import jakarta.inject.Inject;
import java.util.Map;

/**
 * Every element of this test needs to check the entire generated Python.
 * Enums remain inline and fully resolved during definition.
 */
@ExtendWith(InjectionExtension.class)
@InjectWith(RosettaInjectorProvider.class)
@SuppressWarnings("LineLength")
public class PythonEnumTest {

    /**
     * Test utils for generating Python.
     */
    @Inject
    private PythonGeneratorTestUtils testUtils;

    /**
     * Test case for complex model with enum cycles.
     */
    @Test
    public void testComplexModelWithEnumCycles() {
        String pythonString = testUtils.generatePythonFromString(
            """
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
                telephoneNumberType TelephoneTypeEnum (0..1) <"The type of telephone number, e.g. work, mobile.">
                number string (1..1) <"The actual telephone number.">

            type AncillaryEntity: <"Holds an identifier for an ancillary entity, either identified directly via its ancillary role or directly as a legal entity.">
                ancillaryParty AncillaryRoleEnum (0..1) <"Identifies a party via its ancillary role on a transaction (e.g. CCP or DCO through which the trade test be cleared.)">
                legalEntity LegalEntity (0..1)

                condition: one-of
            """)
            .toString();

        String expectedTestType1 = """
            class LegalEntity(BaseDataClass):
                _ALLOWED_METADATA = {'@key', '@key:external'}
                \"""
                A class to specify a legal entity, with a required name and an optional entity identifier (such as the LEI).
                \"""
                entityId: Annotated[Optional[list[StrWithMeta | None]], StrWithMeta.serializer(), StrWithMeta.validator(('@scheme', ))] = Field(None, description='A legal entity identifier (e.g. RED entity code).')
                \"""
                A legal entity identifier (e.g. RED entity code).
                \"""
                name: Annotated[StrWithMeta, StrWithMeta.serializer(), StrWithMeta.validator(('@scheme', ))] = Field(..., description='The legal entity name.')
                \"""
                The legal entity name.
                \"""
            """;
        String expectedTestType2 = """
            class TelephoneNumber(BaseDataClass):
                \"""
                A class to specify a telephone number as a type of phone number (e.g. work, personal, ...) alongside with the actual number.
                \"""
                telephoneNumberType: Optional[com.rosetta.test.model.TelephoneTypeEnum.TelephoneTypeEnum] = Field(None, description='The type of telephone number, e.g. work, mobile.')
                \"""
                The type of telephone number, e.g. work, mobile.
                \"""
                number: str = Field(..., description='The actual telephone number.')
                \"""
                The actual telephone number.
                \"""
            """;
        String expectedTestType3 = """
            class AncillaryEntity(BaseDataClass):
                \"""
                Holds an identifier for an ancillary entity, either identified directly via its ancillary role or directly as a legal entity.
                \"""
                ancillaryParty: Optional[com.rosetta.test.model.AncillaryRoleEnum.AncillaryRoleEnum] = Field(None, description='Identifies a party via its ancillary role on a transaction (e.g. CCP or DCO through which the trade test be cleared.)')
                \"""
                Identifies a party via its ancillary role on a transaction (e.g. CCP or DCO through which the trade test be cleared.)
                \"""
                legalEntity: Optional[LegalEntity] = Field(None, description='')

                @rune_condition
                def condition_0_(self):
                    item = self
                    return rune_check_one_of(self, 'ancillaryParty', 'legalEntity', necessity=True)
            """;

        String expectedTestType4 = """
            class AncillaryRoleEnum(rune.runtime.metadata.EnumWithMetaMixin, Enum):
                \"""
                Defines the enumerated values to specify the ancillary roles to the transaction. The product is agnostic to the actual parties involved in the transaction, with the party references abstracted away from the product definition and replaced by the AncillaryRoleEnum. The AncillaryRoleEnum can then be positioned in the product and the AncillaryParty type, which is positioned outside of the product definition, allows the AncillaryRoleEnum to be associated with an actual party reference.
                \"""
                DISRUPTION_EVENTS_DETERMINING_PARTY = "DisruptionEventsDeterminingParty"
                \"""
                Specifies the party which determines additional disruption events.
                \"""
                EXTRAORDINARY_DIVIDENDS_PARTY = "ExtraordinaryDividendsParty"
                \"""
                Specifies the party which determines if dividends are extraordinary in relation to normal levels.
                \"""
            """;
        String expectedTestType5 = """
            class TelephoneTypeEnum(rune.runtime.metadata.EnumWithMetaMixin, Enum):
                \"""
                The enumerated values to specify the type of telephone number, e.g. work vs. mobile.
                \"""
                MOBILE = "Mobile"
                \"""
                A number on a mobile telephone that is often or usually used for work-related calls. This type of number can be used for urgent work related business when a work number is not sufficient to contact the person or firm.
                \"""
                WORK = "Work"
                \"""
                A number used primarily for work-related calls. Includes home office numbers used primarily for work purposes.
                \"""
            """;
        testUtils.assertGeneratedContainsExpectedString(pythonString, expectedTestType1);
        testUtils.assertGeneratedContainsExpectedString(pythonString, expectedTestType2);
        testUtils.assertGeneratedContainsExpectedString(pythonString, expectedTestType3);
        testUtils.assertGeneratedContainsExpectedString(pythonString, expectedTestType4);
        testUtils.assertGeneratedContainsExpectedString(pythonString, expectedTestType5);
    }

    /**
     * Test case for enum with metadata and conditions.
     */
    @Test
    public void testEnumWithMetadataAndConditions() {
        String pythonString = testUtils.generatePythonFromString(
            """
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
            """)
            .toString();

        String expectedTestType = """
            class UnitType(BaseDataClass):
                \"""
                Defines the unit to be used for price, quantity, or other purposes
                \"""
                capacityUnit: Optional[com.rosetta.test.model.CapacityUnitEnum.CapacityUnitEnum] = Field(None, description='Provides an enumerated value for a capacity unit, generally used in the context of defining quantities for commodities.')
                \"""
                Provides an enumerated value for a capacity unit, generally used in the context of defining quantities for commodities.
                \"""
                weatherUnit: Annotated[Optional[com.rosetta.test.model.WeatherUnitEnum.WeatherUnitEnum], com.rosetta.test.model.WeatherUnitEnum.WeatherUnitEnum.serializer(), com.rosetta.test.model.WeatherUnitEnum.WeatherUnitEnum.validator(('@scheme', ))] = Field(None, description='Provides an enumerated values for a weather unit, generally used in the context of defining quantities for commodities.')
                \"""
                Provides an enumerated values for a weather unit, generally used in the context of defining quantities for commodities.
                \"""

                @rune_condition
                def condition_0_UnitType(self):
                    \"""
                    Requires that a unit type must be set.
                    \"""
                    item = self
                    return rune_check_one_of(self, 'capacityUnit', 'weatherUnit', necessity=True)
            """;
        String expectedTestType3 = """
            class WeatherUnitEnum(rune.runtime.metadata.EnumWithMetaMixin, Enum):
                \"""
                Provides enumerated values for weather units, generally used in the context of defining quantities for commodities.
                \"""
                CDD = "CDD"
                \"""
                Denotes Cooling Degree Days as a standard unit.
                \"""
                CPD = "CPD"
                \"""
            """;
        String expectedTestType4 = """
            class CapacityUnitEnum(rune.runtime.metadata.EnumWithMetaMixin, Enum):
                \"""
                Provides enumerated values for capacity units, generally used in the context of defining quantities for commodities.
                \"""
                ALW = "ALW"
                \"""
                Denotes Allowances as standard unit.
                \"""
                BBL = "BBL"
                \"""
                Denotes a Barrel as a standard unit.
                \"""
            """;

        testUtils.assertGeneratedContainsExpectedString(pythonString, expectedTestType);
        testUtils.assertGeneratedContainsExpectedString(pythonString, expectedTestType3);
        testUtils.assertGeneratedContainsExpectedString(pythonString, expectedTestType4);
    }

    /**
     * Test case for enum with metadata id.
     * CashTransfer is acyclic — standalone. The class is in CashTransfer.py.
     * Enums use module-style imports so the reference remains fully qualified.
     */
    @Test
    public void testEnumWithMetadataId() {
        Map<String, CharSequence> gf = testUtils.generatePythonFromString(
                """
                namespace test.metadata : <"test">

                enum CurrencyEnum:
                    USD
                    EUR
                    GBP

                type CashTransfer:
                    amount number (1..1)
                    currency CurrencyEnum (1..1)
                        [metadata id]
                """);

        // CashTransfer is standalone — class is in its own file
        String generatedPython = gf.get("src/test/metadata/CashTransfer.py").toString();

        // Assert that the 'currency' field in CashTransfer is using a metadata
        // wrapper with the correct @key tags. Enums use module-style import so
        // the reference stays fully qualified as namespace.EnumName.EnumName.
        testUtils.assertGeneratedContainsExpectedString(generatedPython,
                "currency: Annotated[test.metadata.CurrencyEnum.CurrencyEnum, test.metadata.CurrencyEnum.CurrencyEnum.serializer(), test.metadata.CurrencyEnum.CurrencyEnum.validator(('@key', '@key:external'))]");

        // Also verify the key-ref constraints
        testUtils.assertGeneratedContainsExpectedString(generatedPython,
                "'currency': {'@key', '@key:external'}");
    }

    /**
     * Test case for enum without metadata.
     */
    @Test
    public void testEnumWithoutMetadata() {
        Map<String, CharSequence> gf = testUtils.generatePythonFromString(
                """
                        namespace test.metadata : <"test">

                        enum CurrencyEnum:
                            USD
                            EUR
                            GBP

                        type CashTransfer:
                            amount number (1..1)
                            currency CurrencyEnum (1..1)
                                [metadata id]
                        """);

        // CashTransfer is standalone — class is in its own file
        String generatedPython = gf.get("src/test/metadata/CashTransfer.py").toString();

        // Enums should be consistently wrapped in Annotated to support metadata
        // during deserialization even if not explicitly required in Rosetta.
        testUtils.assertGeneratedContainsExpectedString(generatedPython,
                "currency: Annotated[test.metadata.CurrencyEnum.CurrencyEnum, test.metadata.CurrencyEnum.CurrencyEnum.serializer(), test.metadata.CurrencyEnum.CurrencyEnum.validator(('@key', '@key:external'))] = Field(..., description='')");
    }
}
