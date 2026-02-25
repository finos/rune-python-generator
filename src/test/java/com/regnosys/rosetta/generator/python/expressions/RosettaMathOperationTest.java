package com.regnosys.rosetta.generator.python.expressions;

import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.regnosys.rosetta.generator.python.PythonGeneratorTestUtils;
import com.regnosys.rosetta.tests.RosettaInjectorProvider;

import jakarta.inject.Inject;

/**
 * Every element of this test needs to check the entire generated Python.
 * This class focuses on expression-to-python logic for math operations.
 */
@ExtendWith(InjectionExtension.class)
@InjectWith(RosettaInjectorProvider.class)
@SuppressWarnings("checkstyle:LineLength")
public class RosettaMathOperationTest {

    /**
     * Test utils for generating Python.
     */
    @Inject
    private PythonGeneratorTestUtils testUtils;

    /**
     * Test case for math operations.
     */
    @Test
    public void testMathOperations() {
        String generatedPython = testUtils.generatePythonFromString("""
                type TestMath:
                    a int (1..1)
                    b int (1..1)
                    condition MathCheck:
                        if a * b = 10 and a - b = 3 and a / b = 2
                        then True
                """).toString();

        testUtils.assertGeneratedContainsExpectedString(generatedPython,
                """
                        class com_rosetta_test_model_TestMath(BaseDataClass):
                            _FQRTN = 'com.rosetta.test.model.TestMath'
                            a: int = Field(..., description='')
                            b: int = Field(..., description='')

                            @rune_condition
                            def condition_0_MathCheck(self):
                                item = self
                                def _then_fn0():
                                    return True

                                def _else_fn0():
                                    return True

                                return if_cond_fn(((rune_all_elements((rune_resolve_attr(self, "a") * rune_resolve_attr(self, "b")), "=", 10) and rune_all_elements((rune_resolve_attr(self, "a") - rune_resolve_attr(self, "b")), "=", 3)) and rune_all_elements((rune_resolve_attr(self, "a") / rune_resolve_attr(self, "b")), "=", 2)), _then_fn0, _else_fn0)""");
    }

    /**
     * Test case for arithmetic operator.
     */
    @Test
    public void testArithmeticOperator() {
        // This was already full output style
        String generatedPython = testUtils.generatePythonFromString("""
                type ArithmeticTest:
                    a int (1..1)
                    b int (1..1)
                    condition Test:
                        if a + b = 3 then True
                        else False
                """).toString();

        testUtils.assertGeneratedContainsExpectedString(
                generatedPython,
                """
                        class com_rosetta_test_model_ArithmeticTest(BaseDataClass):
                            _FQRTN = 'com.rosetta.test.model.ArithmeticTest'
                            a: int = Field(..., description='')
                            b: int = Field(..., description='')

                            @rune_condition
                            def condition_0_Test(self):
                                item = self
                                def _then_fn0():
                                    return True

                                def _else_fn0():
                                    return False

                                return if_cond_fn(rune_all_elements((rune_resolve_attr(self, "a") + rune_resolve_attr(self, "b")), "=", 3), _then_fn0, _else_fn0)""");
    }
}
