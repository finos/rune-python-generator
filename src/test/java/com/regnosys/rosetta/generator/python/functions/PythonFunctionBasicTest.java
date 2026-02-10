package com.regnosys.rosetta.generator.python.functions;

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
 * This class focuses on basic function generation logic.
 */
@ExtendWith(InjectionExtension.class)
@InjectWith(RosettaInjectorProvider.class)
public class PythonFunctionBasicTest {

    @Inject
    private PythonGeneratorTestUtils testUtils;

    @Test
    public void testGeneratedFunctionWithAddingNumbers() {
        Map<String, CharSequence> gf = testUtils.generatePythonFromString(
                """
                        func AddTwoNumbers: <\"Add two numbers together.\">
                            inputs:
                                number1 number (1..1) <\"The first number to add.\">
                                number2 number (1..1) <\"The second number to add.\">
                            output:
                                result number (1..1)
                            set result:
                                number1 + number2
                            """);
        String expectedBundle = """
                @replaceable
                @validate_call
                def com_rosetta_test_model_functions_AddTwoNumbers(number1: Decimal, number2: Decimal) -> Decimal:
                    \"\"\"
                    Add two numbers together.

                    Parameters
                    ----------
                    number1 : Decimal
                    The first number to add.

                    number2 : Decimal
                    The second number to add.

                    Returns
                    -------
                    result : Decimal

                    \"\"\"
                    self = inspect.currentframe()


                    result = (rune_resolve_attr(self, \"number1\") + rune_resolve_attr(self, \"number2\"))


                    return result
                """;
        testUtils.assertGeneratedContainsExpectedString(gf.get("src/com/_bundle.py").toString(), expectedBundle);
    }

    @Test
    public void testFunctionWithFunctionCallingFunction() {
        Map<String, CharSequence> gf = testUtils.generatePythonFromString(
                """
                        func BaseFunction:
                            inputs:
                                value number (1..1)
                            output:
                                result number (1..1)
                            set result:
                                value * 2
                        func MainFunction:
                            inputs:
                                value number (1..1)
                            output:
                                result number (1..1)
                            set result:
                                BaseFunction(value)
                            """);

        String expectedBundleBaseFunction = """
                @replaceable
                @validate_call
                def com_rosetta_test_model_functions_BaseFunction(value: Decimal) -> Decimal:
                    \"\"\"

                    Parameters
                    ----------
                    value : Decimal

                    Returns
                    -------
                    result : Decimal

                    \"\"\"
                    self = inspect.currentframe()


                    result = (rune_resolve_attr(self, "value") * 2)


                    return result
                """;
        String expectedBundleMainFunction = """
                @replaceable
                @validate_call
                def com_rosetta_test_model_functions_MainFunction(value: Decimal) -> Decimal:
                    \"\"\"

                    Parameters
                    ----------
                    value : Decimal

                    Returns
                    -------
                    result : Decimal

                    \"\"\"
                    self = inspect.currentframe()


                    result = com_rosetta_test_model_functions_BaseFunction(rune_resolve_attr(self, "value"))


                    return result
                """;

        String expectedBundleString = gf.get("src/com/_bundle.py").toString();
        testUtils.assertGeneratedContainsExpectedString(expectedBundleString, expectedBundleBaseFunction);
        testUtils.assertGeneratedContainsExpectedString(expectedBundleString, expectedBundleMainFunction);
    }
}
