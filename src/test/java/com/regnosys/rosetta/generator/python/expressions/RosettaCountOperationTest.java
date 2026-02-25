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
public class RosettaCountOperationTest {

    /**
     * Test utils for generating Python.
     */
    @Inject
    private PythonGeneratorTestUtils testUtils;

    /**
     * Test case for count operation in condition.
     */
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

        // Targeted assertions for Test class (Phase 1, 2, 3)
        testUtils.assertGeneratedContainsExpectedString(generatedPython,
                "class com_rosetta_test_model_Test(BaseDataClass):");
        testUtils.assertGeneratedContainsExpectedString(generatedPython,
                "aValue: list[com_rosetta_test_model_A] = Field(..., description='Test A type aValue', min_length=1)");
        testUtils.assertGeneratedContainsExpectedString(generatedPython, "def condition_0_TestCond(self):");
        testUtils.assertGeneratedContainsExpectedString(generatedPython,
                "com_rosetta_test_model_Test.__annotations__[\"aValue\"] = Annotated[list[com_rosetta_test_model_A], com_rosetta_test_model_A.serializer(), com_rosetta_test_model_A.validator()]");
        testUtils.assertGeneratedContainsExpectedString(generatedPython,
                "com_rosetta_test_model_Test.model_rebuild()");

        // Targeted assertions for A class (Native list fields, no Phase 2/3 needed)
        testUtils.assertGeneratedContainsExpectedString(generatedPython,
                "class com_rosetta_test_model_A(BaseDataClass):");
        testUtils.assertGeneratedContainsExpectedString(generatedPython,
                "field1: Optional[list[int]] = Field(None, description='Test int field 1')");
        testUtils.assertGeneratedContainsExpectedString(generatedPython,
                "field2: list[int] = Field(..., description='Test int field 2', min_length=1)");
    }
}
