package com.regnosys.rosetta.generator.python.functions;

import com.regnosys.rosetta.generator.python.PythonGeneratorTestUtils;
import com.regnosys.rosetta.tests.RosettaInjectorProvider;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import jakarta.inject.Inject;
import java.util.Map;

@Disabled("Functions are being phased out in tests.")
@ExtendWith(InjectionExtension.class)
@InjectWith(RosettaInjectorProvider.class)
public class PythonFunctionControlFlowTest {

    @Inject
    private PythonGeneratorTestUtils testUtils;

    @Test
    public void testGeneratedFunctionAbs() {
        Map<String, CharSequence> gf = testUtils.generatePythonFromString(
                """
                        func Abs: <"Returns the absolute value of a number. If the argument is not negative, the argument is returned. If the argument is negative, the negation of the argument is returned.">
                            inputs:
                                arg number (1..1)
                            output:
                                result number (1..1)
                            set result:
                                if arg < 0 then -1 * arg else arg
                        """);

        String expectedBundle = """
                @replaceable
                @validate_call
                def com_rosetta_test_model_functions_Abs(arg: Decimal) -> Decimal:
                    \"\"\"
                    Returns the absolute value of a number. If the argument is not negative, the argument is returned. If the argument is negative, the negation of the argument is returned.

                    Parameters
                    ----------
                    arg : Decimal

                    Returns
                    -------
                    result : Decimal

                    \"\"\"
                    self = inspect.currentframe()


                    def _then_fn0():
                        return (-1 * rune_resolve_attr(self, "arg"))

                    def _else_fn0():
                        return rune_resolve_attr(self, "arg")

                    result = if_cond_fn(rune_all_elements(rune_resolve_attr(self, "arg"), "<", 0), _then_fn0, _else_fn0)


                    return result
                """;

        testUtils.assertGeneratedContainsExpectedString(gf.get("src/com/_bundle.py").toString(), expectedBundle);
    }
}
