package com.regnosys.rosetta.generator.python.functions;

import com.regnosys.rosetta.generator.python.PythonGeneratorTestUtils;
import com.regnosys.rosetta.tests.RosettaInjectorProvider;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import jakarta.inject.Inject;
import java.util.Map;

@ExtendWith(InjectionExtension.class)
@InjectWith(RosettaInjectorProvider.class)
@SuppressWarnings("LineLength")
public class PythonFunctionOverloadingTest {

    /**
     * Test utils for generating Python code.
     */
    @Inject
    private PythonGeneratorTestUtils testUtils;

    /**
     * Test case for function with meta.
     */
    @Test
    public void testFunctionDispatcher() {
        Map<String, CharSequence> gf = testUtils.generatePythonFromString(
                """
                namespace com.test

                enum DayCountFractionEnum:
                    ACT_360
                    ACT_365

                func DayCountBasis: <"Return the day count basis (the denominator of the day count fraction) for the day count fraction.">
                    [codeImplementation]
                    [calculation]
                    inputs:
                        dcf DayCountFractionEnum (1..1) <"Day count fraction.">
                    output:
                        basis int (1..1) <"The corresponding basis, typically 360 or 365.">

                func DayCountBasis(dcf: DayCountFractionEnum -> ACT_360):
                    set basis: 360

                func DayCountBasis(dcf: DayCountFractionEnum -> ACT_365):
                    set basis: 365
                """);
        String generated = gf.get("src/com/_bundle.py").toString();
        
        // Verify the dispatcher structure and correct pairing
        testUtils.assertGeneratedContainsExpectedString(generated, "match dcf:");
        testUtils.assertGeneratedContainsExpectedString(generated, 
                """
                case com.test.DayCountFractionEnum.DayCountFractionEnum.ACT_360:
                    return _com_test_DayCountBasis_ACT_360(dcf)
                """.strip().indent(8));
        testUtils.assertGeneratedContainsExpectedString(generated, 
                """
                case com.test.DayCountFractionEnum.DayCountFractionEnum.ACT_365:
                    return _com_test_DayCountBasis_ACT_365(dcf)
                """.strip().indent(8));
        
        // Verify the default native fallback
        testUtils.assertGeneratedContainsExpectedString(generated, 
                """
                case _:
                    basis = rune_execute_native('com.test.DayCountBasis', dcf)
                """.strip().indent(8));

        // Verify specialized helper functions are present and have correct distinct logic
        testUtils.assertGeneratedContainsExpectedString(generated, 
            "def _com_test_DayCountBasis_ACT_360(dcf: com.test.DayCountFractionEnum.DayCountFractionEnum) -> int:");
        testUtils.assertGeneratedContainsExpectedString(generated, "    basis = 360");

        testUtils.assertGeneratedContainsExpectedString(generated, 
            "def _com_test_DayCountBasis_ACT_365(dcf: com.test.DayCountFractionEnum.DayCountFractionEnum) -> int:");
        testUtils.assertGeneratedContainsExpectedString(generated, "    basis = 365");
    }
}
