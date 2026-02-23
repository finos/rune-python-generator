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
public class RosettaDistinctOperationTest {

    @Inject
    private PythonGeneratorTestUtils testUtils;

    @Test
    public void testGenerateDistinctCondition() {
        String generatedPython = testUtils.generatePythonFromString("""
                type A: <"Test type">
                    field1 int (1..*) <"Test int field 1">
                    field2 int (1..*) <"Test int field 2">

                type Test: <"Test distinct operation condition">
                    aValue A (1..*) <"Test A type aValue">
                    field3 number (1..1)<"Test number field 3">
                    condition TestCond: <"Test condition">
                        if aValue -> field1 distinct count = 1
                            then field3=0
                        else field3=1
                """).toString();

        testUtils.assertGeneratedContainsExpectedString(
                generatedPython,
                """
                        class com_rosetta_test_model_Test(BaseDataClass):
                            \"""
                            Test distinct operation condition
                            \"""
                            _FQRTN = 'com.rosetta.test.model.Test'
                            aValue: list[Annotated[com_rosetta_test_model_A, com_rosetta_test_model_A.serializer(), com_rosetta_test_model_A.validator()]] = Field(..., description='Test A type aValue', min_length=1)
                            \"""
                            Test A type aValue
                            \"""
                            field3: Decimal = Field(..., description='Test number field 3')
                            \"""
                            Test number field 3
                            \"""

                            @rune_condition
                            def condition_0_TestCond(self):
                                \"""
                                Test condition
                                \"""
                                item = self
                                def _then_fn0():
                                    return rune_all_elements(rune_resolve_attr(self, "field3"), "=", 0)

                                def _else_fn0():
                                    return rune_all_elements(rune_resolve_attr(self, "field3"), "=", 1)

                                return if_cond_fn(rune_all_elements(rune_count(set(rune_resolve_attr(rune_resolve_attr(self, "aValue"), "field1"))), "=", 1), _then_fn0, _else_fn0)""");

        testUtils.assertGeneratedContainsExpectedString(
                generatedPython,
                """
                        class com_rosetta_test_model_A(BaseDataClass):
                            \"""
                            Test type
                            \"""
                            _FQRTN = 'com.rosetta.test.model.A'
                            field1: list[int] = Field(..., description='Test int field 1', min_length=1)
                            \"""
                            Test int field 1
                            \"""
                            field2: list[int] = Field(..., description='Test int field 2', min_length=1)
                            \"""
                            Test int field 2
                            \"""
                        """);
    }
}
