package com.regnosys.rosetta.generator.python.functions;

import java.util.Map;

import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.regnosys.rosetta.generator.python.PythonGeneratorTestUtils;
import com.regnosys.rosetta.tests.RosettaInjectorProvider;

import jakarta.inject.Inject;

/**
 * Every element of this test needs to check the entire generated Python.
 * This class focuses on logic within function bodies.
 */
@ExtendWith(InjectionExtension.class)
@InjectWith(RosettaInjectorProvider.class)
public class PythonFunctionAccumulationTest {

    /**
     * Test utils for generating Python code.
     */
    @Inject
    private PythonGeneratorTestUtils testUtils;

    /**
     * Test case for function with append to list.
     */
    @Test
    public void testGenerateFunctionWithAppendToList() {
        Map<String, CharSequence> gf = testUtils.generatePythonFromString(
                """
                        func AppendToList: <"Append a single value to a list of numbers.">
                            inputs:
                                list number (0..*) <"Input list.">
                                value number (1..1) <"Value to add to a list.">
                            output:
                                result number (0..*) <"Resulting list.">

                            add result: list
                            add result: value
                        """);

        String generated = gf.get("src/com/_bundle.py").toString();
        // Check core logic separately to maintain robustness
        String expected = """
                    result = []

                    rune_add_to_list(result, rune_resolve_attr(self, \"list\"))

                    rune_add_to_list(result, rune_resolve_attr(self, \"value\"))
                """;

        testUtils.assertGeneratedContainsExpectedString(generated, expected);
    }

    /**
     * Test case for function with add operation.
     */
    @Test
    public void testGenerateFunctionWithAddOperation() {
        Map<String, CharSequence> gf = testUtils.generatePythonFromString(
                """
                        type Quantity:
                            value number (0..1)
                            unit UnitType (0..1)
                        type UnitType:
                            value int (1..1)
                        func FilterQuantity:
                            inputs:
                                quantities Quantity (0..*)
                                unit UnitType (1..1)
                            output:
                                filteredQuantities Quantity (0..*)
                            add filteredQuantities:
                                quantities
                                    filter quantities -> unit all = unit
                        """);

        String generated = gf.get("src/com/_bundle.py").toString();
        // Check core logic separately to maintain robustness
        testUtils.assertGeneratedContainsExpectedString(generated, "filteredQuantities = []");
        testUtils.assertGeneratedContainsExpectedString(generated,
                "rune_add_to_list(filteredQuantities, rune_filter(rune_resolve_attr(self, \"quantities\"), lambda item: rune_all_elements(rune_resolve_attr(item, \"unit\"), \"=\", rune_resolve_attr(self, \"unit\"))");
        testUtils.assertGeneratedContainsExpectedString(generated, "return filteredQuantities");
    }
}
