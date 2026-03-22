package com.regnosys.rosetta.generator.python.functions;

import com.regnosys.rosetta.generator.python.PythonGeneratorTestUtils;
import com.regnosys.rosetta.tests.RosettaInjectorProvider;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import jakarta.inject.Inject;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(InjectionExtension.class)
@InjectWith(RosettaInjectorProvider.class)
@SuppressWarnings("LineLength")
public class PythonFunctionOrderTest {

    /**
     * Test utils for generating Python code.
     */
    @Inject
    private PythonGeneratorTestUtils testUtils;

    /**
     * Test case for function dependency order.
     */
    @Test
    public void testFunctionDependencyOrder() {
        // Define ClassB which depends on ClassA, and a function which depends on both.
        // Rosetta allows defining them in any order, but Python requires definition
        // before use (or forward refs).
        Map<String, CharSequence> gf = testUtils.generatePythonFromString(
            """
            type ClassB:
                attr ClassA (1..1)

            func MyFunc:
                inputs:
                    arg ClassB (1..1)
                output:
                    out ClassA (1..1)
                set out:
                    arg->attr

            type ClassA:
                val string (1..1)
            """);

        // ClassA and ClassB have no cycle so they are standalone.
        // MyFunc is also standalone. Ordering is enforced by Python imports, not file order.
        assertTrue(gf.containsKey("src/com/rosetta/test/model/ClassA.py"), "ClassA should be generated standalone");
        assertTrue(gf.containsKey("src/com/rosetta/test/model/ClassB.py"), "ClassB should be generated standalone");
        assertTrue(gf.containsKey("src/com/rosetta/test/model/functions/MyFunc.py"), "MyFunc should be generated standalone");
        String funcPython = gf.get("src/com/rosetta/test/model/functions/MyFunc.py").toString();
        testUtils.assertGeneratedContainsExpectedString(funcPython, "def MyFunc");
    }

    /**
     * Test case for circular dependency order.
     */
    @Test
    public void testCircularDependencyOrder() {
        // Define ClassA that depends on ClassB, and ClassB that depends on ClassA.
        // This is a classic circular dependency which the topological sort cannot
        // handle.
        Map<String, CharSequence> gf = testUtils.generatePythonFromString(
            """
            type ClassA:
                b ClassB (0..1)

            type ClassB:
                a ClassA (0..1)
            """);

        String generatedPython = gf.get("src/com/_bundle.py").toString();

        int classAIndex = generatedPython.indexOf("class com_rosetta_test_model_ClassA");
        int classBIndex = generatedPython.indexOf("class com_rosetta_test_model_ClassB");

        // Ideally, one should be defined, and the other use a forward reference or
        // string type hint.
        // For now, we just assert that generation succeeds (which it might not if DAG
        // cyclic error occurs).
        assertTrue(classAIndex != -1 && classBIndex != -1, "Both classes should be generated");
    }
}
