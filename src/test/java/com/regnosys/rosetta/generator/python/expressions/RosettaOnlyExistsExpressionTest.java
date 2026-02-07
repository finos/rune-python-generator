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
public class RosettaOnlyExistsExpressionTest {

    @Inject
    private PythonGeneratorTestUtils testUtils;

    @Test
    public void testGenerateOnlyExistsCondition() {
        String generatedPython = testUtils.generatePythonFromString("""
                type A: <"Test type">
                field1 number (0..1) <"Test number field 1">

                type Test: <"Test only exists condition">
                    aValue A (1..1) <"Test A type aValue">

                    condition TestCond: <"Test condition">
                        if aValue -> field1 exists
                            then aValue -> field1 only exists
                """).toString();

        testUtils.assertGeneratedContainsExpectedString(
                generatedPython,
                """
                        class com_rosetta_test_model_Test(BaseDataClass):
                            \"""
                            Test only exists condition
                            \"""
                            _FQRTN = 'com.rosetta.test.model.Test'
                            aValue: Annotated[com_rosetta_test_model_A, com_rosetta_test_model_A.serializer(), com_rosetta_test_model_A.validator()] = Field(..., description='Test A type aValue')
                            \"""
                            Test A type aValue
                            \"""

                            @rune_condition
                            def condition_0_TestCond(self):
                                \"""
                                Test condition
                                \"""
                                item = self
                                def _then_fn0():
                                    return rune_check_one_of(self, rune_resolve_attr(rune_resolve_attr(self, "aValue"), "field1"))

                                def _else_fn0():
                                    return True

                                return if_cond_fn(rune_attr_exists(rune_resolve_attr(rune_resolve_attr(self, "aValue"), "field1")), _then_fn0, _else_fn0)""");

        testUtils.assertGeneratedContainsExpectedString(
                generatedPython,
                """
                        class com_rosetta_test_model_A(BaseDataClass):
                            \"""
                            Test type
                            \"""
                            _FQRTN = 'com.rosetta.test.model.A'
                            field1: Optional[Decimal] = Field(None, description='Test number field 1')
                            \"""
                            Test number field 1
                            \"""
                        """);
    }
}
