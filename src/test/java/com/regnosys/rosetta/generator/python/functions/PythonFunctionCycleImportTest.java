/*
 * Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
 * SPDX-License-Identifier: Apache-2.0
 */
package com.regnosys.rosetta.generator.python.functions;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Map;

import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.regnosys.rosetta.generator.python.PythonGeneratorTestUtils;
import com.regnosys.rosetta.tests.RosettaInjectorProvider;

import jakarta.inject.Inject;

/**
 * Functions that call each other import each other inside the calling function, so that either
 * module can be imported first. Every other call keeps its import at the top of the file,
 * including calls made by a function in a cycle to a function outside it, calls into a cycle
 * from outside it, and a function calling itself.
 */
@ExtendWith(InjectionExtension.class)
@InjectWith(RosettaInjectorProvider.class)
public class PythonFunctionCycleImportTest {

    private static final String PACKAGE = "com.rosetta.test.model.functions.";

    // IsEven and IsOdd call each other. IsEven also calls Helper, which is outside the cycle.
    // UsesIsEven calls into the cycle from outside it. UsesHelper has no cycle near it.
    // Countdown calls itself.
    private static final String MODEL = """
            func IsEven:
                inputs:
                    n int (1..1)
                output:
                    result boolean (1..1)
                set result:
                    if n = 0 then True else IsOdd(Helper(n) - 2)

            func IsOdd:
                inputs:
                    n int (1..1)
                output:
                    result boolean (1..1)
                set result:
                    if n = 0 then False else IsEven(n - 1)

            func Helper:
                inputs:
                    n int (1..1)
                output:
                    result int (1..1)
                set result:
                    n + 1

            func UsesHelper:
                inputs:
                    n int (1..1)
                output:
                    result int (1..1)
                set result:
                    Helper(n)

            func UsesIsEven:
                inputs:
                    n int (1..1)
                output:
                    result boolean (1..1)
                set result:
                    IsEven(n)

            func Countdown:
                inputs:
                    n int (1..1)
                output:
                    result int (1..1)
                set result:
                    if n = 0 then 0 else Countdown(n - 1)
            """;

    @Inject
    private PythonGeneratorTestUtils testUtils;

    private String generated(String functionName) {
        Map<String, CharSequence> python = testUtils.generatePythonFromString(MODEL);
        return python.get("src/com/rosetta/test/model/functions/" + functionName + ".py").toString();
    }

    private static String importOf(String functionName) {
        return "from " + PACKAGE + functionName + " import " + functionName;
    }

    /** Asserts that the function's import is at the top of the file and not inside the function. */
    private void assertTopLevelImport(String generated, String functionName) {
        testUtils.assertGeneratedContainsExpectedString(generated, "\n" + importOf(functionName));
        testUtils.assertGeneratedDoesNotContain(generated, "    " + importOf(functionName));
    }

    /** Asserts that nothing is imported inside the generated function. */
    private static void assertNoImportInsideFunction(String generated, String functionName) {
        String body = generated.substring(generated.indexOf("def " + functionName + "("));
        assertFalse(body.contains("\n    from ") || body.contains("\n    import "),
                functionName + " should not import anything inside the function:\n" + body);
    }

    @Test
    public void testFunctionInCycleIsImportedInsideTheCaller() {
        String isEven = generated("IsEven");
        testUtils.assertAppearsAfter(isEven, "def IsEven(", "    " + importOf("IsOdd"));
        testUtils.assertGeneratedDoesNotContain(isEven, "\n" + importOf("IsOdd"));
    }

    @Test
    public void testOtherFunctionInCycleIsImportedInsideTheCaller() {
        String isOdd = generated("IsOdd");
        testUtils.assertAppearsAfter(isOdd, "def IsOdd(", "    " + importOf("IsEven"));
        testUtils.assertGeneratedDoesNotContain(isOdd, "\n" + importOf("IsEven"));
    }

    @Test
    public void testFunctionInCycleKeepsTopLevelImportForCallOutsideCycle() {
        assertTopLevelImport(generated("IsEven"), "Helper");
    }

    @Test
    public void testCallIntoCycleFromOutsideKeepsTopLevelImport() {
        String usesIsEven = generated("UsesIsEven");
        assertTopLevelImport(usesIsEven, "IsEven");
        assertNoImportInsideFunction(usesIsEven, "UsesIsEven");
    }

    @Test
    public void testFunctionOutsideCycleKeepsTopLevelImport() {
        String usesHelper = generated("UsesHelper");
        assertTopLevelImport(usesHelper, "Helper");
        assertNoImportInsideFunction(usesHelper, "UsesHelper");
    }

    @Test
    public void testRecursiveFunctionDoesNotImportItself() {
        String countdown = generated("Countdown");
        testUtils.assertGeneratedDoesNotContain(countdown, importOf("Countdown"));
        assertNoImportInsideFunction(countdown, "Countdown");
    }
}
