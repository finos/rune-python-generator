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
public class RosettaDistinctOperationTest {

    /**
     * Test utils for generating Python.
     */
    @Inject
    private PythonGeneratorTestUtils testUtils;

    /**
     * Test case for distinct operation condition.
     */
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

        // Targeted assertions for Test class (standalone — no Phase 2/3)
        testUtils.assertGeneratedContainsExpectedString(generatedPython,
                "class Test(BaseDataClass):");
        testUtils.assertGeneratedContainsExpectedString(generatedPython,
                "aValue: list[A | None] = Field(..., description='Test A type aValue', min_length=1)");
        testUtils.assertGeneratedContainsExpectedString(generatedPython, "def condition_0_TestCond(self):");

        // Targeted assertions for A class
        testUtils.assertGeneratedContainsExpectedString(generatedPython,
                "class A(BaseDataClass):");
        testUtils.assertGeneratedContainsExpectedString(generatedPython,
                "field1: list[int | None] = Field(..., description='Test int field 1', min_length=1)");
    }
}
