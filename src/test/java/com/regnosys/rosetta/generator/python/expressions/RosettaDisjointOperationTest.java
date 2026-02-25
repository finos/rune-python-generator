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
@SuppressWarnings("LineLength")
public class RosettaDisjointOperationTest {

    /**
     * Test utils for generating Python.
     */
    @Inject
    private PythonGeneratorTestUtils testUtils;

    /**
     * Test case for disjoint binary expression condition.
     */
    @Test
    public void testGenerateBinDisjointCondition() {
        testUtils.assertBundleContainsExpectedString("""
                type Test: <"Test disjoint binary expression condition">
                    field1 string (1..1) <"Test string field1">
                    field2 string (1..1) <"Test string field2">
                    field3 boolean (1..1) <"Test boolean field3">
                    condition TestCond: <"Test condition">
                        if field3=False
                        then if ["B", "C", "D"] any = field2 and ["A"] disjoint field1
                        then field3=True
                """,
                """
                        class com_rosetta_test_model_Test(BaseDataClass):
                            \"""
                            Test disjoint binary expression condition
                            \"""
                            _FQRTN = 'com.rosetta.test.model.Test'
                            field1: str = Field(..., description='Test string field1')
                            \"""
                            Test string field1
                            \"""
                            field2: str = Field(..., description='Test string field2')
                            \"""
                            Test string field2
                            \"""
                            field3: bool = Field(..., description='Test boolean field3')
                            \"""
                            Test boolean field3
                            \"""

                            @rune_condition
                            def condition_0_TestCond(self):
                                \"""
                                Test condition
                                \"""
                                item = self
                                def _then_fn1():
                                    return rune_all_elements(rune_resolve_attr(self, "field3"), "=", True)

                                def _else_fn1():
                                    return True

                                def _then_fn0():
                                    return if_cond_fn((rune_all_elements(["B", "C", "D"], "=", rune_resolve_attr(self, "field2")) and rune_disjoint(["A"], rune_resolve_attr(self, "field1"))), _then_fn1, _else_fn1)

                                def _else_fn0():
                                    return True

                                return if_cond_fn(rune_all_elements(rune_resolve_attr(self, "field3"), "=", False), _then_fn0, _else_fn0)""");
    }
}
