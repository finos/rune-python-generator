package com.regnosys.rosetta.generator.python.object;

import java.util.Map;

import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

        String generatedPython = gf.get("src/test/_bundle.py").toString();

        // 1. Check class is defined
        testUtils.assertGeneratedContainsExpectedString(
                generatedPython,
                "class test_collision_CollidingName(BaseDataClass):");

        // 2. Check function is defined
        testUtils.assertGeneratedContainsExpectedString(
                generatedPython,
                "def test_collision_functions_CollidingName(inParam: int) -> test_collision_CollidingName:");

        // 3. Verify ordering: Class should be independent but function should follow it in the bundle (it doesn't strictly matter now but we keep the check)
        int classIndex = generatedPython.indexOf("class test_collision_CollidingName");
        int funcIndex = generatedPython.indexOf("def test_collision_functions_CollidingName");
        
        assertTrue(classIndex < funcIndex, "Class must be defined before function");
        
        // 4. Verify Phase 2 is between them (or at least after class)
        int phase2Index = generatedPython.indexOf("# Phase 2: Delayed Annotation Updates");
        assertTrue(classIndex < phase2Index, "Class must be defined before Phase 2");
        assertTrue(phase2Index < funcIndex, "Phase 2 must be before function");
    }
}
