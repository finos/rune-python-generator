package com.regnosys.rosetta.generator.python.functions;

import java.util.Map;

import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.regnosys.rosetta.generator.python.PythonGeneratorTestUtils;
import com.regnosys.rosetta.tests.RosettaInjectorProvider;

import jakarta.inject.Inject;

@ExtendWith(InjectionExtension.class)
@InjectWith(RosettaInjectorProvider.class)
@SuppressWarnings("LineLength")
public class PythonFunctionExternalTest {
    /**
     * Test utils for generating Python code.
     */
    @Inject
    private PythonGeneratorTestUtils testUtils;

    /**
     * Test case for external function support.
     */
    @Test
    public void testExternalFunctionSupport() {

        Map<String, CharSequence> gf = testUtils.generatePythonFromString(
                """
                namespace rosetta_dsl.test.functions

                enum RoundingModeEnum:
                    Down
                    Up

                func RoundToNearest: <"Round a number to the supplied nearest, using the supplied rounding mode.">
                    [codeImplementation]
                    inputs:
                        value number (1..1) <"The original (unrounded) number.">
                        nearest number (1..1) <"The nearest number to round to.">
                        roundingMode RoundingModeEnum (1..1) <"The method of rounding (up to nearest/down to nearest.">
                    output:
                        roundedValue number (1..1)
                    condition PositiveNearest:
                        nearest > 0
                """);
        String bundle = gf.get("src/rosetta_dsl/_bundle.py").toString();

        // Note: rosetta_dsl.test.functions.functions.RoundToNearest is expected because
        // functions live in a 'functions' directory relative to their namespace.
        String expected = """
                    _pre_registry = {}
                    self = inspect.currentframe()

                    # conditions

                    @rune_local_condition(_pre_registry)
                    def condition_0_PositiveNearest():
                        item = self
                        return rune_all_elements(rune_resolve_attr(self, \"nearest\"), \">\", 0)
                    # Execute all registered conditions
                    rune_execute_local_conditions(_pre_registry, 'Pre-condition')

                    roundedValue = rune_execute_native('rosetta_dsl.test.functions.functions.RoundToNearest', value, nearest, roundingMode)


                    return roundedValue
                """;
        testUtils.assertGeneratedContainsExpectedString(bundle, expected);
        
        String registrationExpected = """
                rune_attempt_register_native_functions(
                    native_functions=[
                        'rosetta_dsl.test.functions.functions.RoundToNearest',
                    ]
                )
                """;
        testUtils.assertGeneratedContainsExpectedString(bundle, registrationExpected);
    }
}
