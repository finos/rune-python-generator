/*
 * Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
 * SPDX-License-Identifier: Apache-2.0
 */
package com.regnosys.rosetta.generator.python.object;

import java.util.Map;

import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.regnosys.rosetta.generator.python.PythonGeneratorTestUtils;
import com.regnosys.rosetta.tests.RosettaInjectorProvider;

import jakarta.inject.Inject;

@ExtendWith(InjectionExtension.class)
@InjectWith(RosettaInjectorProvider.class)
public class PythonNameCollisionTest {

    @Inject
    private PythonGeneratorTestUtils testUtils;

    @Test
    public void testDataAndFunctionSameName() {
        // Test where a 'type' and a 'func' have the same name in the same namespace.
        // This is known to happen in CDM for 'CalculationPeriod'.
        Map<String, CharSequence> gf = testUtils.generatePythonFromString(
                """
                namespace test.collision
                
                type CollidingName:
                    attr int(1..1)
                    other CollidingName(0..1)
                        [metadata reference]
                
                func CollidingName:
                    inputs:
                        inParam int(1..1)
                    output:
                        result CollidingName(1..1)
                    set result -> attr: inParam
                """);

        // CollidingName type is standalone (self-ref via metadata reference is handled inline).
        String classPython = gf.get("src/test/collision/CollidingName.py").toString();
        testUtils.assertGeneratedContainsExpectedString(classPython, "class CollidingName(BaseDataClass):");
        testUtils.assertGeneratedContainsExpectedString(classPython,
                "other: Annotated[Optional[CollidingName | BaseReference], CollidingName.serializer(), CollidingName.validator(('@ref', '@ref:external'))] = Field(None, description='')");

        // CollidingName function is also standalone.
        String funcPython = gf.get("src/test/collision/functions/CollidingName.py").toString();
        testUtils.assertGeneratedContainsExpectedString(funcPython,
                "def CollidingName(inParam: int) -> CollidingName:");
    }
}
