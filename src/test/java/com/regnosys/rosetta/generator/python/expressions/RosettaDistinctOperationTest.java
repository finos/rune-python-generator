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

        // Targeted assertions for Test class (Phase 1, 2, 3)
        testUtils.assertGeneratedContainsExpectedString(generatedPython,
                "class com_rosetta_test_model_Test(BaseDataClass):");
        testUtils.assertGeneratedContainsExpectedString(generatedPython,
                "aValue: list[com_rosetta_test_model_A] = Field(..., description='Test A type aValue', min_length=1)");
        testUtils.assertGeneratedContainsExpectedString(generatedPython, "def condition_0_TestCond(self):");
        testUtils.assertGeneratedContainsExpectedString(generatedPython,
                "com_rosetta_test_model_Test.__annotations__[\"aValue\"] = list[Annotated[com_rosetta_test_model_A, com_rosetta_test_model_A.serializer(), com_rosetta_test_model_A.validator()]]");
        testUtils.assertGeneratedContainsExpectedString(generatedPython, "com_rosetta_test_model_Test.model_rebuild()");

        // Targeted assertions for A class
        testUtils.assertGeneratedContainsExpectedString(generatedPython,
                "class com_rosetta_test_model_A(BaseDataClass):");
        testUtils.assertGeneratedContainsExpectedString(generatedPython,
                "field1: list[int] = Field(..., description='Test int field 1', min_length=1)");
    }
}
