package com.regnosys.rosetta.generator.python.functions;

import java.util.Map;

import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.regnosys.rosetta.generator.python.PythonGeneratorTestUtils;
import com.regnosys.rosetta.tests.RosettaInjectorProvider;

import jakarta.inject.Inject;

/**
 * Merge of: PythonFunctionNativeTest + PythonFunctionWithMetaTest
 */
@ExtendWith(InjectionExtension.class)
@InjectWith(RosettaInjectorProvider.class)
@SuppressWarnings("LineLength")
public class PythonFunctionExtensionsTest {

    /**
     * Test utils for generating Python code.
     */
    @Inject
    private PythonGeneratorTestUtils testUtils;

    // -------------------------------------------------------------------------
    // From PythonFunctionNativeTest
    // -------------------------------------------------------------------------

    /**
     * Test case for native function support.
     */
    @Test
    public void testNativeFunctionSupport() {

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
        // The function body lives in the standalone function file.
        // Note: rosetta_dsl.test.functions.RoundToNearest is expected because
        // functions live in a 'functions' directory relative to their namespace.
        String funcPython = gf.get("src/rosetta_dsl/test/functions/functions/RoundToNearest.py").toString();
        String expected = """
                self = inspect.currentframe()

                value = rune_cow(value)
                nearest = rune_cow(nearest)
                roundingMode = rune_cow(roundingMode)

                _pre_registry = {}
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
        testUtils.assertGeneratedContainsExpectedString(funcPython, expected);

        // Registration block lives in __init__.py (no bundle is generated for standalone-only native functions).
        String initPython = gf.get("src/rosetta_dsl/__init__.py").toString();
        String registrationExpected = """
            rune_attempt_register_native_functions(
                function_names=[
                    'rosetta_dsl.test.functions.functions.RoundToNearest',
                ]
            )
            """;
        testUtils.assertGeneratedContainsExpectedString(initPython, registrationExpected);
    }

    // -------------------------------------------------------------------------
    // From PythonFunctionWithMetaTest
    // -------------------------------------------------------------------------

    /**
     * Test case for function with meta.
     */
    @Test
    public void testFunctionWithMeta() {
        Map<String, CharSequence> gf = testUtils.generatePythonFromString(
                """
                namespace com.test

                type Foo:
                    [metadata scheme]
                    val string (1..1)

                func TestWithMeta:
                    inputs:
                        f Foo (1..1)
                    output:
                        res Foo (1..1)
                    set res:
                        f with-meta { scheme: "myScheme" }
                """);
        String generatedPython = gf.get("src/com/test/functions/TestWithMeta.py").toString();
        testUtils.assertGeneratedContainsExpectedString(generatedPython,
                "res = (lambda _wm: (_wm.set_meta(check_allowed=False, scheme=\"myScheme\"), _wm)[-1])(rune_resolve_attr(self, \"f\"))");
    }

    /**
     * Test case for function with meta enum dependency.
     */
    @Test
    public void testFunctionWithMetaEnumDependency() {
        Map<String, CharSequence> gf = testUtils.generatePythonFromString(
                """
                namespace com.test

                enum MyEnum:
                    Value1

                type Foo:
                    [metadata scheme]
                    val string (1..1)

                func TestWithMetaEnum:
                    inputs:
                        f Foo (1..1)
                    output:
                        res Foo (1..1)
                    set res:
                        f with-meta { scheme: (MyEnum -> Value1) to-string }
                """);
        String funcPython = gf.get("src/com/test/functions/TestWithMetaEnum.py").toString();
        testUtils.assertGeneratedContainsExpectedString(funcPython,
                "res = (lambda _wm: (_wm.set_meta(check_allowed=False, scheme=rune_str(com.test.MyEnum.MyEnum.VALUE_1)), _wm)[-1])(rune_resolve_attr(self, \"f\"))");
        // Enum module imports are written to the standalone function file.
        testUtils.assertGeneratedContainsExpectedString(funcPython, "import com.test.MyEnum");
    }
}
