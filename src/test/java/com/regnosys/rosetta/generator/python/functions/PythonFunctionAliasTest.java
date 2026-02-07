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
public class PythonFunctionAliasTest {

    @Inject
    private PythonGeneratorTestUtils testUtils;

    @Test
    public void testAliasSimple() {

        Map<String, CharSequence> gf = testUtils.generatePythonFromString(
                """
                        func TestAlias:
                            inputs:
                                inp1 number(1..1)
                                inp2 number(1..1)
                            output:
                                result number(1..1)
                            alias Alias:
                                if inp1 < 0 then inp1 else inp2

                            set result:
                                Alias
                        """);

        String expectedBundle = """
                @replaceable
                @validate_call
                def com_rosetta_test_model_functions_TestAlias(inp1: Decimal, inp2: Decimal) -> Decimal:
                    \"\"\"

                    Parameters
                    ----------
                    inp1 : Decimal

                    inp2 : Decimal

                    Returns
                    -------
                    result : Decimal

                    \"\"\"
                    self = inspect.currentframe()


                    def _then_fn0():
                        return rune_resolve_attr(self, "inp1")

                    def _else_fn0():
                        return rune_resolve_attr(self, "inp2")

                    Alias = if_cond_fn(rune_all_elements(rune_resolve_attr(self, "inp1"), "<", 0), _then_fn0, _else_fn0)
                    result = rune_resolve_attr(self, "Alias")


                    return result
                """;
        testUtils.assertGeneratedContainsExpectedString(gf.get("src/com/_bundle.py").toString(), expectedBundle);

    }

    @Test
    public void testAliasWithTypeOutput() {

        Map<String, CharSequence> gf = testUtils.generatePythonFromString(
                """
                        type A:
                            valueA number(1..1)

                        type B:
                            valueB number(1..1)

                        type C:
                            valueC number(1..1)

                        func TestAliasWithTypeOutput:
                            inputs:
                                a A (1..1)
                                b B (1..1)
                            output:
                                c C (1..1)
                            alias Alias1:
                                a->valueA
                            alias Alias2:
                                b->valueB
                            set c->valueC:
                                Alias1*Alias2
                        """);

        String expectedBundle = """
                @replaceable
                @validate_call
                def com_rosetta_test_model_functions_TestAliasWithTypeOutput(a: com_rosetta_test_model_A, b: com_rosetta_test_model_B) -> com_rosetta_test_model_C:
                    \"\"\"

                    Parameters
                    ----------
                    a : com.rosetta.test.model.A

                    b : com.rosetta.test.model.B

                    Returns
                    -------
                    c : com.rosetta.test.model.C

                    \"\"\"
                    self = inspect.currentframe()


                    Alias1 = rune_resolve_attr(rune_resolve_attr(self, "a"), "valueA")
                    Alias2 = rune_resolve_attr(rune_resolve_attr(self, "b"), "valueB")
                    c = _get_rune_object('com_rosetta_test_model_C', 'valueC', (rune_resolve_attr(self, "Alias1") * rune_resolve_attr(self, "Alias2")))


                    return c
                """;

        testUtils.assertGeneratedContainsExpectedString(gf.get("src/com/_bundle.py").toString(), expectedBundle);

    }
}
