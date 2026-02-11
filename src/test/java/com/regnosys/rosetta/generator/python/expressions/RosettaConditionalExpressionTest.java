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
public class RosettaConditionalExpressionTest {

    /**
     * Test utils for generating Python.
     */
    @Inject
    private PythonGeneratorTestUtils testUtils;

    /**
     * Test case for if-then condition.
     */
    @Test
    public void testGenerateIfThenCondition() {
        testUtils.assertBundleContainsExpectedString("""
                type Test1: <"Test if-then condition.">
                    field1 string (0..1) <"Test string field 1">
                    field2 number (0..1) <"Test number field 2">
                    condition TestCond: <"Test condition">
                        if field1 exists
                            then field2=0
                """,
                """
                        class com_rosetta_test_model_Test1(BaseDataClass):
                            \"""
                            Test if-then condition.
                            \"""
                            _FQRTN = 'com.rosetta.test.model.Test1'
                            field1: Optional[str] = Field(None, description='Test string field 1')
                            \"""
                            Test string field 1
                            \"""
                            field2: Optional[Decimal] = Field(None, description='Test number field 2')
                            \"""
                            Test number field 2
                            \"""

                            @rune_condition
                            def condition_0_TestCond(self):
                                \"""
                                Test condition
                                \"""
                                item = self
                                def _then_fn0():
                                    return rune_all_elements(rune_resolve_attr(self, "field2"), "=", 0)

                                def _else_fn0():
                                    return True

                                return if_cond_fn(rune_attr_exists(rune_resolve_attr(self, "field1")), _then_fn0, _else_fn0)""");
    }

    @Test
    public void testGenerateIfThenElseCondition() {
        testUtils.assertBundleContainsExpectedString("""
                type Test1: <"Test if-then-else condition.">
                field1 string (0..1) <"Test string field 1">
                field2 number (0..1) <"Test number field 2">
                condition TestCond: <"Test condition">
                    if field1 exists
                        then field2=0
                        else field2=1
                """,
                """
                        class com_rosetta_test_model_Test1(BaseDataClass):
                            \"""
                            Test if-then-else condition.
                            \"""
                            _FQRTN = 'com.rosetta.test.model.Test1'
                            field1: Optional[str] = Field(None, description='Test string field 1')
                            \"""
                            Test string field 1
                            \"""
                            field2: Optional[Decimal] = Field(None, description='Test number field 2')
                            \"""
                            Test number field 2
                            \"""

                            @rune_condition
                            def condition_0_TestCond(self):
                                \"""
                                Test condition
                                \"""
                                item = self
                                def _then_fn0():
                                    return rune_all_elements(rune_resolve_attr(self, "field2"), "=", 0)

                                def _else_fn0():
                                    return rune_all_elements(rune_resolve_attr(self, "field2"), "=", 1)

                                return if_cond_fn(rune_attr_exists(rune_resolve_attr(self, "field1")), _then_fn0, _else_fn0)""");
    }

    @Test
    public void testGenerateBooleanCondition() {
        testUtils.assertBundleContainsExpectedString("""
                type Test1: <"Test boolean condition.">
                field1 boolean (1..1) <"Test booelan field 1">
                field2 number (0..1) <"Test number field 2">
                condition TestCond: <"Test condition">
                    if field1= True
                        then field2=0
                        else field2=5
                """,
                """
                        class com_rosetta_test_model_Test1(BaseDataClass):
                            \"""
                            Test boolean condition.
                            \"""
                            _FQRTN = 'com.rosetta.test.model.Test1'
                            field1: bool = Field(..., description='Test booelan field 1')
                            \"""
                            Test booelan field 1
                            \"""
                            field2: Optional[Decimal] = Field(None, description='Test number field 2')
                            \"""
                            Test number field 2
                            \"""

                            @rune_condition
                            def condition_0_TestCond(self):
                                \"""
                                Test condition
                                \"""
                                item = self
                                def _then_fn0():
                                    return rune_all_elements(rune_resolve_attr(self, "field2"), "=", 0)

                                def _else_fn0():
                                    return rune_all_elements(rune_resolve_attr(self, "field2"), "=", 5)

                                return if_cond_fn(rune_all_elements(rune_resolve_attr(self, "field1"), "=", True), _then_fn0, _else_fn0)""");
    }

    @Test
    public void testGenerateAbsentCondition() {
        testUtils.assertBundleContainsExpectedString("""
                type Test1: <"Test absent condition.">
                field1 boolean (1..1) <"Test booelan field 1">
                field2 number (0..1) <"Test number field 2">
                condition TestCond: <"Test condition">
                    if field1= True
                        then field2=0
                        else field2 is absent
                """,
                """
                        class com_rosetta_test_model_Test1(BaseDataClass):
                            \"""
                            Test absent condition.
                            \"""
                            _FQRTN = 'com.rosetta.test.model.Test1'
                            field1: bool = Field(..., description='Test booelan field 1')
                            \"""
                            Test booelan field 1
                            \"""
                            field2: Optional[Decimal] = Field(None, description='Test number field 2')
                            \"""
                            Test number field 2
                            \"""

                            @rune_condition
                            def condition_0_TestCond(self):
                                \"""
                                Test condition
                                \"""
                                item = self
                                def _then_fn0():
                                    return rune_all_elements(rune_resolve_attr(self, "field2"), "=", 0)

                                def _else_fn0():
                                    return (not rune_attr_exists(rune_resolve_attr(self, "field2")))

                                return if_cond_fn(rune_all_elements(rune_resolve_attr(self, "field1"), "=", True), _then_fn0, _else_fn0)""");
    }
}
