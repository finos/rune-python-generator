package com.regnosys.rosetta.generator.python.exceptions;

import com.regnosys.rosetta.tests.RosettaInjectorProvider;
import com.regnosys.rosetta.generator.python.PythonGeneratorTestUtils;

import jakarta.inject.Inject;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(InjectionExtension.class)
@InjectWith(RosettaInjectorProvider.class)
public class PythonExceptionsTest {

    /**
     * PythonGeneratorTestUtils is used to generate Python code from Rosetta models.
     */
    @Inject
    private PythonGeneratorTestUtils testUtils;

    /**
     * Test case for a non-existing attribute type.
     */
    @Test
    public void testNonExistentAttributeType() {
        try {
            testUtils.generatePythonFromString(
                    """
                            type B:
                                intValue1 int (0..1)
                                intValue2 int (0..1)
                                aValue A (1..1)
                            """);
        } catch (AssertionError e) {
            assertTrue(e.getMessage().contains("Couldn't resolve reference to RosettaType 'A'"));
        }
    }

    /**
     * Test case for a non-existing attribute used in a condition.
     */
    @Test
    public void testUNonExistentSymbolUsage() {
        try {
            testUtils.generatePythonFromString(
                    """
                            type TestType: <"Test type with one-of condition.">
                                field1 string (0..1) <"Test string field 1">
                                field2 string (0..1) <"Test string field 2">
                                condition BusinessCentersChoice: <"Choice rule to represent an FpML choice construct.">
                                     if field1 exists
                                         then field3 > 0
                            """);
        } catch (AssertionError e) {
            assertTrue(e.getMessage().contains("Couldn't resolve reference to RosettaSymbol 'field3'"));
        }
    }

    /**
     * Test case for a non-existing type used as a super type.
     */
    @Test
    public void testNonExistentTypeSuperType() {
        try {
            testUtils.generatePythonFromString(
                    """
                            type TestType1 extends TestType2:
                            TestType2Value1 number (0..1) <"Test number">
                            TestType2Value2 date (0..*) <"Test date">
                            """);
        } catch (AssertionError e) {
            assertTrue(e.getMessage().contains("Couldn't resolve reference to Data 'TestType2'"));
        }
    }
}
