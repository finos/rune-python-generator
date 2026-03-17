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
public class RosettaCountOperationTest {

    @Inject
    private PythonGeneratorTestUtils testUtils;

    @Test
    public void testGenerateCountCondition() {
        String generatedPython = testUtils.generatePythonFromString("""
                type A: <"Test type">
                    field1 int (0..*) <"Test int field 1">
                    field2 int (1..*) <"Test int field 2">
                    field3 int (1..3) <"Test int field 3">
                    field4 int (0..3) <"Test int field 4">

                type Test: <"Test count operation condition">
                    aValue A (1..*) <"Test A type aValue">

                    condition TestCond: <"Test condition">
                        if aValue -> field1 count <> aValue -> field2 count
                            then True
                        else False
                """).toString();

        testUtils.assertGeneratedContainsExpectedString(
                generatedPython,
                """
                        class com_rosetta_test_model_Test(BaseDataClass):
                            \"""
                            Test count operation condition
                            \"""
                            _FQRTN = 'com.rosetta.test.model.Test'
                            aValue: list[Annotated[com_rosetta_test_model_A, com_rosetta_test_model_A.serializer(), com_rosetta_test_model_A.validator()]] = Field(..., description='Test A type aValue', min_length=1)
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
                                    return True

                                def _else_fn0():
                                    return False

                                return if_cond_fn(rune_any_elements(rune_count(rune_resolve_attr(rune_resolve_attr(self, "aValue"), "field1")), "<>", rune_count(rune_resolve_attr(rune_resolve_attr(self, "aValue"), "field2"))), _then_fn0, _else_fn0)
                        """);

        testUtils.assertGeneratedContainsExpectedString(
                generatedPython,
                """
                        class com_rosetta_test_model_A(BaseDataClass):
                            \"""
                            Test type
                            \"""
                            _FQRTN = 'com.rosetta.test.model.A'
                            field1: Optional[list[int]] = Field(None, description='Test int field 1')
                            \"""
                            Test int field 1
                            \"""
                            field2: list[int] = Field(..., description='Test int field 2', min_length=1)
                            \"""
                            Test int field 2
                            \"""
                            field3: list[int] = Field(..., description='Test int field 3', min_length=1, max_length=3)
                            \"""
                            Test int field 3
                            \"""
                            field4: Optional[list[int]] = Field(None, description='Test int field 4', max_length=3)
                            \"""
                            Test int field 4
                            \"""
                        """);
    }
}
