package com.regnosys.rosetta.generator.python.functions;

import com.regnosys.rosetta.generator.python.PythonGeneratorTestUtils;
import com.regnosys.rosetta.tests.RosettaInjectorProvider;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import jakarta.inject.Inject;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Disabled;

@Disabled("Functions are being phased out in tests.")
@ExtendWith(InjectionExtension.class)
@InjectWith(RosettaInjectorProvider.class)
public class PythonFunctionOrderTest {

    @Inject
    private PythonGeneratorTestUtils testUtils;

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

        String bundle = gf.get("src/com/_bundle.py").toString();

        // Check ordering in the bundle.
        // We expect ClassA to be defined before ClassB (because B depends on A)
        // and both to be defined before MyFunc (because MyFunc depends on B and A).

        int classAIndex = bundle.indexOf("class com_rosetta_test_model_ClassA");
        int classBIndex = bundle.indexOf("class com_rosetta_test_model_ClassB");
        int funcIndex = bundle.indexOf("def com_rosetta_test_model_functions_MyFunc");

        assertTrue(classAIndex < classBIndex, "ClassA should be defined before ClassB");
        assertTrue(classBIndex < funcIndex, "ClassB should be defined before MyFunc");
    }

    @Test
    @Disabled("Circular dependencies are currently not supported and will cause a topological sort error or runtime NameError")
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

        String bundle = gf.get("src/com/_bundle.py").toString();

        int classAIndex = bundle.indexOf("class com_rosetta_test_model_ClassA");
        int classBIndex = bundle.indexOf("class com_rosetta_test_model_ClassB");

        // Ideally, one should be defined, and the other use a forward reference or
        // string type hint.
        // For now, we just assert that generation succeeds (which it might not if DAG
        // cyclic error occurs).
        assertTrue(classAIndex != -1 && classBIndex != -1, "Both classes should be generated");
    }
}
