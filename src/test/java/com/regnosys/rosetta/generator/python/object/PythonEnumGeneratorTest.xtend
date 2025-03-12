package com.regnosys.rosetta.generator.python.object
import com.google.inject.Inject
import com.regnosys.rosetta.tests.RosettaInjectorProvider
import com.regnosys.rosetta.generator.python.PythonGeneratorTestUtils
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.extensions.InjectionExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.^extension.ExtendWith
import com.regnosys.rosetta.generator.java.enums.EnumHelper
import static org.hamcrest.CoreMatchers.*
import static org.hamcrest.MatcherAssert.*
import org.junit.jupiter.api.Disabled

@ExtendWith(InjectionExtension)
@InjectWith(RosettaInjectorProvider)

class PythonEnumGeneratorTest {
    
    @Inject PythonGeneratorTestUtils testUtils
   
    @Test
    def void testEnumGeneration() {
        val pythonString = testUtils.generatePythonFromString(
            '''
            enum TestEnum: <"Test enum description.">
                TestEnumValue1 <"Test enum value 1">
                TestEnumValue2 <"Test enum value 2">
                TestEnumValue3 <"Test enum value 3">
                _1 displayName "1" <"Rolls on the 1st day of the month.">
            ''').toString()

        val expected = 
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
            """
            TEST_ENUM_VALUE_3 = "TestEnumValue3"
            """
            Test enum value 3
            """
            _1 = "1"
            """
            Rolls on the 1st day of the month.
            """'''
        testUtils.assertStringInString (pythonString, expected)
    }

    @Test
    def void testEnumGenerationWithUppercaseUnderscoreFormattedNames() {
        assertThat(EnumHelper.formatEnumName("ISDA1993Commodity"), is("ISDA_1993_COMMODITY"))
        assertThat(EnumHelper.formatEnumName("ISDA1998FX"), is("ISDA1998FX"))
        assertThat(EnumHelper.formatEnumName("iTraxxEuropeDealer"), is("I_TRAXX_EUROPE_DEALER"))
        assertThat(EnumHelper.formatEnumName("StandardLCDS"), is("STANDARD_LCDS"))
        assertThat(EnumHelper.formatEnumName("_1_1"), is("_1_1"))
        assertThat(EnumHelper.formatEnumName("_30E_360_ISDA"), is("_30E_360_ISDA"))
        assertThat(EnumHelper.formatEnumName("ACT_365L"), is("ACT_365L"))
        assertThat(EnumHelper.formatEnumName("OSPPrice"), is("OSP_PRICE"))
        assertThat(EnumHelper.formatEnumName("FRAYield"), is("FRA_YIELD"))
        assertThat(EnumHelper.formatEnumName("AED-EBOR-Reuters"), is("AED_EBOR_REUTERS"))
        assertThat(EnumHelper.formatEnumName("EUR-EURIBOR-Reuters"), is("EUR_EURIBOR_REUTERS"))
        assertThat(EnumHelper.formatEnumName("DJ.iTraxx.Europe"), is("DJ_I_TRAXX_EUROPE"))
        assertThat(EnumHelper.formatEnumName("IVS1OpenMarkets"), is("IVS_1_OPEN_MARKETS"))
        assertThat(EnumHelper.formatEnumName("D"), is("D"))
        assertThat(EnumHelper.formatEnumName("_1"), is("_1"))
        assertThat(EnumHelper.formatEnumName("DJ.CDX.NA"), is("DJ_CDX_NA"))
        assertThat(EnumHelper.formatEnumName("novation"), is("NOVATION"))
        assertThat(EnumHelper.formatEnumName("partialNovation"), is("PARTIAL_NOVATION"))
        assertThat(EnumHelper.formatEnumName("ALUMINIUM_ALLOY_LME_15_MONTH"), is("ALUMINIUM_ALLOY_LME_15_MONTH"))
        assertThat(EnumHelper.formatEnumName("AggregateClient"), is("AGGREGATE_CLIENT"))
        assertThat(EnumHelper.formatEnumName("Currency1PerCurrency2"), is("CURRENCY_1_PER_CURRENCY_2"))
    }

    @Test
    @Disabled
    def void testEnumGenerationWithDisplayName() {
        /*
        val pythonString = testUtils.generatePythonFromString(
            '''
            synonym source FpML
            enum TestEnumWithDisplay:
                one displayName "uno" <"Some description"> [synonym FpML value "oneSynonym"]
                two <"Some other description"> [synonym FpML value "twoSynonym"]
                three displayName "tria" <"Some description"> [synonym FpML value "threeSynonym"]
                four  displayName "tessera" <"Some description"> [synonym FpML value "fourSynonym"]
            ''').toString()
       
        assertThat(testEnumCode,
            allOf(containsString('''TestEnumWithDisplay()'''),
                containsString('''TestEnumWithDisplay(String displayName)'''),
                containsString('''public String toString()''')))

        code.compileToClasses
        */
        
    }

    @Test
    @Disabled
    def void testEnumGenerationWithDeprecatedAnnotation() {
        /*
        val pythonString = testUtils.generatePythonFromString(
            '''
                enum TestEnumDeprecated:
                    [deprecated]
                    one
                    two
            ''').toString()
        */
    }
    @Test //not developed at the moment
    @Disabled
    def void testGenerationAnnotationForEnumSynonyms() {
        /*
        val code = testUtils.generatePythonFromString(
            '''
            synonym source FpML
            enum TestEnum:
                one <"Some description"> [synonym FpML value "oneSynonym"]
                two <"Some other description"> [synonym FpML value "twoSynonym"]
            ''').toString()

        val testEnumCode = code.get(rootPackage.name + ".TestEnum")
        assertThat(testEnumCode, containsString('''RosettaSynonym(value = "oneSynonym", source = "FpML")'''))

        code.compileToClasses
        */
    }
}
