package com.regnosys.rosetta.generator.python.object;

import jakarta.inject.Inject;
import com.regnosys.rosetta.tests.RosettaInjectorProvider;
import com.regnosys.rosetta.generator.python.PythonGeneratorTestUtils;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Disabled;

/**
 * This is an Anchor test.
 * Every element of this test needs to check the entire generated Python.
 */
@ExtendWith(InjectionExtension.class)
@InjectWith(RosettaInjectorProvider.class)
public class PythonCircularDependencyTest {

    @Inject
    private PythonGeneratorTestUtils testUtils;

    @Test
    public void testAttributeCircularDependency() {
        // This test demonstrates a circular dependency via attributes.
        // Pydantic works with this if 'from __future__ import annotations' is used,
        // but the generated bundle order must still be legal.
        // Currently, the DAG ignores cycles, so the order is non-deterministic.
        Map<String, CharSequence> gf = testUtils.generatePythonFromString(
                """
                        type CircularA:
                            b CircularB (1..1)

                        type CircularB:
                            a CircularA (1..1)
                        """);

        String bundle = gf.get("src/com/_bundle.py").toString();

        testUtils.assertGeneratedContainsExpectedString(
                bundle,
                """
                        class com_rosetta_test_model_CircularB(BaseDataClass):
                            _FQRTN = 'com.rosetta.test.model.CircularB'
                            a: com_rosetta_test_model_CircularA = Field(..., description='')


                        class com_rosetta_test_model_CircularA(BaseDataClass):
                            _FQRTN = 'com.rosetta.test.model.CircularA'
                            b: com_rosetta_test_model_CircularB = Field(..., description='')

                        # Phase 2: Delayed Annotation Updates
                        com_rosetta_test_model_CircularB.__annotations__["a"] = Annotated[com_rosetta_test_model_CircularA, com_rosetta_test_model_CircularA.serializer(), com_rosetta_test_model_CircularA.validator()]

                        # Phase 3: Rebuild
                        com_rosetta_test_model_CircularB.model_rebuild()


                        # Phase 2: Delayed Annotation Updates
                        com_rosetta_test_model_CircularA.__annotations__["b"] = Annotated[com_rosetta_test_model_CircularB, com_rosetta_test_model_CircularB.serializer(), com_rosetta_test_model_CircularB.validator()]

                        # Phase 3: Rebuild
                        com_rosetta_test_model_CircularA.model_rebuild()
                        """);
    }

    @Test
    @Disabled("This currently generates an invalid order (Child before Parent) because the DAG ignores the cycle.")
    public void testInheritanceCircularDependency() {
        // Parent depends on Child via attribute
        // Child depends on Parent via inheritance
        // This creates a cycle that MUST be broken by making 'Child' a string forward
        // reference in Parent.
        Map<String, CharSequence> gf = testUtils.generatePythonFromString(
                """
                        type Parent:
                            child Child (0..1)

                        type Child extends Parent:
                            val int (1..1)
                        """);

        String bundle = gf.get("src/com/_bundle.py").toString();

        int parentIndex = bundle.indexOf("class com_rosetta_test_model_Parent");
        int childIndex = bundle.indexOf("class com_rosetta_test_model_Child(com_rosetta_test_model_Parent)");

        // This assertion will likely FAIL or be inconsistent with the current
        // generator.
        assertTrue(parentIndex < childIndex, "Parent must be defined before Child for inheritance to work");
    }
}
