package com.regnosys.rosetta.generator.python.expressions;

import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.regnosys.rosetta.generator.python.PythonGeneratorTestUtils;
import com.regnosys.rosetta.tests.RosettaInjectorProvider;

import jakarta.inject.Inject;

@ExtendWith(InjectionExtension.class)
@InjectWith(RosettaInjectorProvider.class)
public class RosettaAnyOperationTest {

    /**
     * Test utils for generating Python.
     */
    @Inject
    private PythonGeneratorTestUtils testUtils;

    /**
     * Test case for any condition.
     */
    @Test
    public void testGenerateAnyCondition() {
        testUtils.assertBundleContainsExpectedString("""
                type Test: <"Test any operation condition">
                    field1 string (1..1) <"Test string field1">
                    field2 string (1..1) <"Test boolean field2">
                    condition TestCond: <"Test condition">
                        if field1="A"
                        then ["B", "C", "D"] any = field2
                """,
                """
                        class com_rosetta_test_model_Test(BaseDataClass):
                            \"""
                            Test any operation condition
                            \"""
                            _FQRTN = 'com.rosetta.test.model.Test'
                            field1: str = Field(..., description='Test string field1')
                            \"""
                            Test string field1
                            \"""
                            field2: str = Field(..., description='Test boolean field2')
                            \"""
                            Test boolean field2
                            \"""

                            @rune_condition
                            def condition_0_TestCond(self):
                                \"""
                                Test condition
                                \"""
                                item = self
                                def _then_fn0():
                                    return rune_all_elements(["B", "C", "D"], "=", rune_resolve_attr(self, "field2"))

                                def _else_fn0():
                                    return True

                                return if_cond_fn(rune_all_elements(rune_resolve_attr(self, "field1"), "=", "A"), _then_fn0, _else_fn0)""");
    }
}
