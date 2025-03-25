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
    def void testEnumWithConditions () {
        val python = testUtils.generatePythonFromString(
            '''
            enum PeriodExtendedEnum /*extends PeriodEnum*/ : <"The enumerated values to specify a time period containing the additional value of Term.">
                H <"Hour">
                D <"Day">
                W <"Week">
                M <"Month">
                Y <"Year">
                T <"Term. The period commencing on the effective date and ending on the termination date. The T period always appears in association with periodMultiplier = 1, and the notation is intended for use in contexts where the interval thus qualified (e.g. accrual period, payment period, reset period, ...) spans the entire term of the trade.">
                C <"CalculationPeriod - the period corresponds to the calculation period   For example, used in the Commodity Markets to indicate that a reference contract is the one that corresponds to the period of the calculation period.">

            type Frequency: <"A class for defining a date frequency, e.g. one day, three months, through the combination of an integer value and a standardized period value that is specified as part of an enumeration.">
                [metadata key]

                periodMultiplier int (1..1) <"A time period multiplier, e.g. 1, 2, or 3. If the period value is T (Term) then period multiplier must contain the value 1.">
                period PeriodExtendedEnum (1..1) <"A time period, e.g. a day, week, month, year or term of the stream.">

                condition TermPeriod: <"FpML specifies that if period value is T (Term) then periodMultiplier must contain the value 1.">
                    if period = PeriodExtendedEnum -> T then periodMultiplier = 1

                condition PositivePeriodMultiplier: <"FpML specifies periodMultiplier as a positive integer.">
                    periodMultiplier > 0
            ''')
        val generatedBundle = python.get("src/com/_bundle.py").toString()
        val expected = 
        '''
        class com_rosetta_test_model_Frequency(BaseDataClass):
            """
            A class for defining a date frequency, e.g. one day, three months, through the combination of an integer value and a standardized period value that is specified as part of an enumeration.
            """
            _FQRTN = 'com.rosetta.test.model.Frequency'
            periodMultiplier: int = Field(..., description='A time period multiplier, e.g. 1, 2, or 3. If the period value is T (Term) then period multiplier must contain the value 1.')
            """
            A time period multiplier, e.g. 1, 2, or 3. If the period value is T (Term) then period multiplier must contain the value 1.
            """
            period: com.rosetta.test.model.PeriodExtendedEnum.PeriodExtendedEnum = Field(..., description='A time period, e.g. a day, week, month, year or term of the stream.')
            """
            A time period, e.g. a day, week, month, year or term of the stream.
            """
            
            @rune_condition
            def condition_0_TermPeriod(self):
                """
                FpML specifies that if period value is T (Term) then periodMultiplier must contain the value 1.
                """
                item = self
                def _then_fn0():
                    return rune_all_elements(rune_resolve_attr(self, "periodMultiplier"), "=", 1)
                
                def _else_fn0():
                    return True
                
                return if_cond_fn(rune_all_elements(rune_resolve_attr(self, "period"), "=", com.rosetta.test.model.PeriodExtendedEnum.PeriodExtendedEnum.T), _then_fn0, _else_fn0)
            
            @rune_condition
            def condition_1_PositivePeriodMultiplier(self):
                """
                FpML specifies periodMultiplier as a positive integer.
                """
                item = self
                return rune_all_elements(rune_resolve_attr(self, "periodMultiplier"), ">", 0)
            '''        
        testUtils.assertStringInString (generatedBundle, expected)

    }
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
            """
        '''
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
