/*
 * Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
 * SPDX-License-Identifier: Apache-2.0
 */
package com.regnosys.rosetta.generator.python.object;

import java.util.Map;

import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.regnosys.rosetta.generator.python.PythonGeneratorTestUtils;
import com.regnosys.rosetta.tests.RosettaInjectorProvider;

import jakarta.inject.Inject;

@ExtendWith(InjectionExtension.class)
@InjectWith(RosettaInjectorProvider.class)
public class PythonTypeAliasTest {

    @Inject
    private PythonGeneratorTestUtils testUtils;

    @Test
    public void testBasicTypeAlias() {
        // Test that typeAlias is correctly stripped to its underlying basic type (Java approach).
        Map<String, CharSequence> gf = testUtils.generatePythonFromString(
                """
                namespace test_alias
                
                typeAlias MyNumber: number
                
                type Foo:
                    val MyNumber (1..1)
                """);

        // Foo is acyclic — standalone file contains the class directly
        String generatedPython = gf.get("src/test_alias/Foo.py").toString();

        // 1. Check field usage (uses the underlying basic type: Decimal)
        testUtils.assertGeneratedContainsExpectedString(
                generatedPython,
                "val: Decimal = Field(..., description='')");

        // 2. Check that the alias assignment is NOT present
        Assertions.assertFalse(generatedPython.contains("test_alias_MyNumber = Decimal"),
            "Aliased types should be stripped to their underlying types (Java approach)");
    }

    @Test
    public void testComplexTypeAlias() {
        // Test that typeAlias is correctly stripped to its underlying object type.
        Map<String, CharSequence> gf = testUtils.generatePythonFromString(
                """
                namespace test_alias
                
                type Origin:
                    attr int (1..1)
                
                typeAlias AliasToOrigin: Origin
                
                type Target:
                    val AliasToOrigin (1..1)
                """);

        // Target is acyclic — standalone file contains the class directly
        String generatedPython = gf.get("src/test_alias/Target.py").toString();

        // 1. Check field usage (uses the underlying object type with short name: Origin)
        testUtils.assertGeneratedContainsExpectedString(
                generatedPython,
                "val: Origin = Field(..., description='')");

        // 2. Check that the alias assignment is NOT present
        Assertions.assertFalse(generatedPython.contains("AliasToOrigin"));
    }

    @Test
    public void testFunctionWithAliasDependency() {
        // Test that typeAlias is correctly stripped within a function signature.
        Map<String, CharSequence> gf = testUtils.generatePythonFromString(
                """
                typeAlias AliasedNum: number
                
                func MyTestFunc:
                    inputs:
                        inParam AliasedNum(1..1)
                    output:
                        outParam string(1..1)
                    set outParam:
                        "1"
                """);

        // MyTestFunc is acyclic — standalone function file
        String funcPython = gf.get("src/com/rosetta/test/model/functions/MyTestFunc.py").toString();

        // 1. Check function signature (stripped to Decimal; standalone function uses simple name)
        testUtils.assertGeneratedContainsExpectedString(
                funcPython,
                "def MyTestFunc(inParam: Decimal) -> str:");

        // 2. Check that it DOES NOT mention the alias name
        Assertions.assertFalse(funcPython.contains("AliasedNum"), "Function output should use stripped type");
    }
}
