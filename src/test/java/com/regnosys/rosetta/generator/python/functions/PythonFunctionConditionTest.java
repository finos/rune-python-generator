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
public class PythonFunctionConditionTest {

    /**
     * Test utils for generating Python code.
     */
    @Inject
    private PythonGeneratorTestUtils testUtils;

    /**
     * Test case for simple condition.
     */
    @Test
    public void testSimpleCondition() {
        Map<String, CharSequence> gf = testUtils.generatePythonFromString(
                """
                        func MinMaxWithSimpleCondition:
                            inputs:
                                in1 number (1..1)
                                in2 number (1..1)
                                direction string (1..1)
                            output:
                                result number (1..1)
                            condition Directiom:
                                direction = "min" or direction = "max"
                            set result:
                                if direction = "min" then
                                    [in1, in2] min
                                else if direction = "max" then
                                    [in1, in2] max
                        """);

        String expectedBundle = """
                @replaceable
                @validate_call
                def com_rosetta_test_model_MinMaxWithSimpleCondition(in1: Decimal, in2: Decimal, direction: str) -> Decimal:
                    \"\"\"

                    Parameters
                    ----------
                    in1 : Decimal

                    in2 : Decimal

                    direction : str

                    Returns
                    -------
                    result : Decimal

                    \"\"\"
                    self = inspect.currentframe()

                    in1 = rune_cow(in1)
                    in2 = rune_cow(in2)
                    direction = rune_cow(direction)

                    _pre_registry = {}
                    # conditions

                    @rune_local_condition(_pre_registry)
                    def condition_0_Directiom():
                        item = self
                        return (rune_all_elements(rune_resolve_attr(self, \"direction\"), "=", \"min\") or rune_all_elements(rune_resolve_attr(self, \"direction\"), "=", \"max\"))
                    # Execute all registered conditions
                    rune_execute_local_conditions(_pre_registry, 'Pre-condition')

                    def _then_fn1():
                        return (lambda items: max((x for x in (items or []) if x is not None), default=None) if items is not None else None)([rune_resolve_attr(self, \"in1\"), rune_resolve_attr(self, \"in2\")])

                    def _else_fn1():
                        return True

                    def _then_fn0():
                        return (lambda items: min((x for x in (items or []) if x is not None), default=None) if items is not None else None)([rune_resolve_attr(self, "in1"), rune_resolve_attr(self, "in2")])

                    def _else_fn0():
                        return if_cond_fn(rune_all_elements(rune_resolve_attr(self, "direction"), "=", "max"), _then_fn1, _else_fn1)

                    result = if_cond_fn(rune_all_elements(rune_resolve_attr(self, "direction"), "=", "min"), _then_fn0, _else_fn0)


                    return rune_unwrap(result)
                """;
        testUtils.assertGeneratedContainsExpectedString(gf.get("src/com/_bundle.py").toString(), expectedBundle);
    }

    /**
     * Test case for post condition.
     */
    @Test
    public void testPostCondition() {
        Map<String, CharSequence> gf = testUtils.generatePythonFromString(
                """
                        func MinMaxWithPostCondition:
                            inputs:
                                in1 number (1..1)
                                in2 number (1..1)
                                direction string (1..1)
                            output:
                                result number (1..1)
                            set result:
                                if direction = "min" then
                                    [in1, in2] min
                                else if direction = "max" then
                                    [in1, in2] max
                            post-condition Directiom:
                                direction = "min" or direction = "max"                        """);
        String expectedBundle = """
                @replaceable
                @validate_call
                def com_rosetta_test_model_MinMaxWithPostCondition(in1: Decimal, in2: Decimal, direction: str) -> Decimal:
                    \"\"\"

                    Parameters
                    ----------
                    in1 : Decimal

                    in2 : Decimal

                    direction : str

                    Returns
                    -------
                    result : Decimal

                    \"\"\"
                    self = inspect.currentframe()

                    in1 = rune_cow(in1)
                    in2 = rune_cow(in2)
                    direction = rune_cow(direction)

                    _post_registry = {}

                    def _then_fn1():
                        return (lambda items: max((x for x in (items or []) if x is not None), default=None) if items is not None else None)([rune_resolve_attr(self, \"in1\"), rune_resolve_attr(self, \"in2\")])

                    def _else_fn1():
                        return True

                    def _then_fn0():
                        return (lambda items: min((x for x in (items or []) if x is not None), default=None) if items is not None else None)([rune_resolve_attr(self, \"in1\"), rune_resolve_attr(self, \"in2\")])

                    def _else_fn0():
                        return if_cond_fn(rune_all_elements(rune_resolve_attr(self, \"direction\"), "=", \"max\"), _then_fn1, _else_fn1)

                    result = if_cond_fn(rune_all_elements(rune_resolve_attr(self, \"direction\"), "=", \"min\"), _then_fn0, _else_fn0)

                    # post-conditions

                    @rune_local_condition(_post_registry)
                    def condition_0_Directiom():
                        item = self
                        return (rune_all_elements(rune_resolve_attr(self, \"direction\"), "=", \"min\") or rune_all_elements(rune_resolve_attr(self, \"direction\"), "=", \"max\"))
                    # Execute all registered post-conditions
                    rune_execute_local_conditions(_post_registry, 'Post-condition')

                    return rune_unwrap(result)
                """;
        testUtils.assertGeneratedContainsExpectedString(gf.get("src/com/_bundle.py").toString(), expectedBundle);
    }

    /**
     * Test case for multiple conditions.
     */
    @Test
    public void testMultipleConditions() {
        Map<String, CharSequence> gf = testUtils.generatePythonFromString(
                """
                        func MinMaxWithMPositiveNumbersAndMultipleCondition:
                            inputs:
                                in1 number (1..1)
                                in2 number (1..1)
                                direction string (1..1)

                            output:
                                result number (1..1)
                            condition Directiom:
                                direction = "min" or direction = "max"
                            condition PositiveNumbers:
                                in1 > 0 and in2 > 0
                            set result:
                                if direction = "min" then
                                    [in1, in2] min
                                else if direction = "max" then
                                    [in1, in2] max
                        """);
        String expectedBundle = """
                @replaceable
                @validate_call
                def com_rosetta_test_model_MinMaxWithMPositiveNumbersAndMultipleCondition(in1: Decimal, in2: Decimal, direction: str) -> Decimal:
                    \"\"\"

                    Parameters
                    ----------
                    in1 : Decimal

                    in2 : Decimal

                    direction : str

                    Returns
                    -------
                    result : Decimal

                    \"\"\"
                    self = inspect.currentframe()

                    in1 = rune_cow(in1)
                    in2 = rune_cow(in2)
                    direction = rune_cow(direction)

                    _pre_registry = {}
                    # conditions

                    @rune_local_condition(_pre_registry)
                    def condition_0_Directiom():
                        item = self
                        return (rune_all_elements(rune_resolve_attr(self, "direction"), "=", "min") or rune_all_elements(rune_resolve_attr(self, "direction"), "=", "max"))

                    @rune_local_condition(_pre_registry)
                    def condition_1_PositiveNumbers():
                        item = self
                        return (rune_all_elements(rune_resolve_attr(self, "in1"), ">", 0) and rune_all_elements(rune_resolve_attr(self, "in2"), ">", 0))
                    # Execute all registered conditions
                    rune_execute_local_conditions(_pre_registry, 'Pre-condition')

                    def _then_fn1():
                        return (lambda items: max((x for x in (items or []) if x is not None), default=None) if items is not None else None)([rune_resolve_attr(self, \"in1\"), rune_resolve_attr(self, \"in2\")])

                    def _else_fn1():
                        return True

                    def _then_fn0():
                        return (lambda items: min((x for x in (items or []) if x is not None), default=None) if items is not None else None)([rune_resolve_attr(self, "in1"), rune_resolve_attr(self, "in2")])

                    def _else_fn0():
                        return if_cond_fn(rune_all_elements(rune_resolve_attr(self, "direction"), "=", "max"), _then_fn1, _else_fn1)

                    result = if_cond_fn(rune_all_elements(rune_resolve_attr(self, "direction"), "=", "min"), _then_fn0, _else_fn0)


                    return rune_unwrap(result)
                """;
        testUtils.assertGeneratedContainsExpectedString(gf.get("src/com/_bundle.py").toString(), expectedBundle);
    }
}
