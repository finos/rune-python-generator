package com.regnosys.rosetta.generator.python.object;

import java.util.Map;

import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.regnosys.rosetta.generator.python.PythonGeneratorTestUtils;
import com.regnosys.rosetta.tests.RosettaInjectorProvider;

import jakarta.inject.Inject;

/**
 * This is an Anchor test.
 * Every element of this test needs to check the entire generated Python.
 */
@ExtendWith(InjectionExtension.class)
@InjectWith(RosettaInjectorProvider.class)
@SuppressWarnings("LineLength")
public class PythonCircularDependencyTest {

    /**
     * PythonGeneratorTestUtils is used to generate Python code from Rosetta models.
     */
    @Inject
    private PythonGeneratorTestUtils testUtils;

    @Test
    public void testLazyConstruction() {
        Map<String, CharSequence> gf = testUtils.generatePythonFromString(
                """
                        namespace rosetta_dsl.test.semantic.object_construction
                        type C:
                            p1 int(1..1)
                            p2 int(1..1)


                        func TestLazyConstruction:
                            output:
                                result C(1..1)
                            set result->p1: 1
                            set result->p2: 2
                        """);
        String bundle = gf.get("src/rosetta_dsl/_bundle.py").toString();
        testUtils.assertGeneratedContainsExpectedString(
                bundle, "from rune.runtime.object_builder import ObjectBuilder");
        testUtils.assertGeneratedContainsExpectedString(
                bundle,
                """
                            result = ObjectBuilder(rosetta_dsl_test_semantic_object_construction_C)
                            result.p1 = 1
                            result.p2 = 2
                            result = result.to_model()


                            return result
                        """);
    }

    @Test
    public void testForCircularDependency() {
        Map<String, CharSequence> gf = testUtils.generatePythonFromString(
                """
                namespace rosetta_dsl.test.model.circular_dependency

                type Bar1: <"Test Circular Dependency">
                    number1 int(1..1)
                    bar2 Bar2(0..1)
                type Bar2:
                    number2 int(1..1)
                    bar1 Bar1(0..1)
                    condition Test:
                        if bar1 exists
                            then bar1->number1 > 0
                """);

        String bundle = gf.toString();

        testUtils.assertGeneratedContainsExpectedString(
                bundle,
                """
                # Phase 2: Delayed Annotation Updates
                rosetta_dsl_test_model_circular_dependency_Bar1.__annotations__["bar2"] = Annotated[Optional[rosetta_dsl_test_model_circular_dependency_Bar2], rosetta_dsl_test_model_circular_dependency_Bar2.serializer(), rosetta_dsl_test_model_circular_dependency_Bar2.validator()]
                rosetta_dsl_test_model_circular_dependency_Bar2.__annotations__["bar1"] = Annotated[Optional[rosetta_dsl_test_model_circular_dependency_Bar1], rosetta_dsl_test_model_circular_dependency_Bar1.serializer(), rosetta_dsl_test_model_circular_dependency_Bar1.validator()]
                """);

        testUtils.assertGeneratedContainsExpectedString(
                bundle,
                """


                # Phase 3: Rebuild
                rosetta_dsl_test_model_circular_dependency_Bar1.model_rebuild()
                rosetta_dsl_test_model_circular_dependency_Bar2.model_rebuild()
                """);
    }

    /**
     * Test case for attribute circular dependency.
     */
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
                class com_rosetta_test_model_CircularA(BaseDataClass):
                    _FQRTN = 'com.rosetta.test.model.CircularA'
                    b: com_rosetta_test_model_CircularB = Field(..., description='')


                class com_rosetta_test_model_CircularB(BaseDataClass):
                    _FQRTN = 'com.rosetta.test.model.CircularB'
                    a: com_rosetta_test_model_CircularA = Field(..., description='')


                # Phase 2: Delayed Annotation Updates
                com_rosetta_test_model_CircularA.__annotations__["b"] = Annotated[com_rosetta_test_model_CircularB, com_rosetta_test_model_CircularB.serializer(), com_rosetta_test_model_CircularB.validator()]
                com_rosetta_test_model_CircularB.__annotations__["a"] = Annotated[com_rosetta_test_model_CircularA, com_rosetta_test_model_CircularA.serializer(), com_rosetta_test_model_CircularA.validator()]


                # Phase 3: Rebuild
                com_rosetta_test_model_CircularA.model_rebuild()
                com_rosetta_test_model_CircularB.model_rebuild()
                """);
    }

    /**
     * Test case for inheritance circular dependency.
     * This test demonstrates a circular dependency via inheritance.
     */
    @Test
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
