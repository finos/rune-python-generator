package com.regnosys.rosetta.generator.python.object;

import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.regnosys.rosetta.generator.python.PythonGeneratorTestUtils;
import com.regnosys.rosetta.tests.RosettaInjectorProvider;

import jakarta.inject.Inject;
import java.util.Map;

/**
 * This is an Anchor test for circular dependencies and complex generation patterns.
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
        String model = """
                namespace rosetta_dsl.test.semantic.object_construction
                type C:
                    p1 int(1..1)
                    p2 int(1..1)


                func TestLazyConstruction:
                    output:
                        result C(1..1)
                    set result->p1: 1
                    set result->p2: 2
                """;
        
        testUtils.assertBundleContainsExpectedString(model, "result = ObjectBuilder(C)");
        testUtils.assertBundleContainsExpectedString(model, "result.p1 = 1");
        testUtils.assertBundleContainsExpectedString(model, "result.p2 = 2");
    }

    @Test
    public void testForCircularDependency() {
        String model = """
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
                """;

        testUtils.assertBundleContainsExpectedString(model, "# Phase 2: Delayed Annotation Updates");
        testUtils.assertBundleContainsExpectedString(model,
                "rosetta_dsl_test_model_circular_dependency_Bar1.__annotations__[\"bar2\"] = Annotated[Optional[rosetta_dsl_test_model_circular_dependency_Bar2], rosetta_dsl_test_model_circular_dependency_Bar2.serializer(), rosetta_dsl_test_model_circular_dependency_Bar2.validator()]");
        testUtils.assertBundleContainsExpectedString(model,
                "rosetta_dsl_test_model_circular_dependency_Bar2.__annotations__[\"bar1\"] = Annotated[Optional[rosetta_dsl_test_model_circular_dependency_Bar1], rosetta_dsl_test_model_circular_dependency_Bar1.serializer(), rosetta_dsl_test_model_circular_dependency_Bar1.validator()]");

        testUtils.assertBundleContainsExpectedString(model, "# Phase 3: Rebuild");
        testUtils.assertBundleContainsExpectedString(model, "rosetta_dsl_test_model_circular_dependency_Bar1.model_rebuild()");
        testUtils.assertBundleContainsExpectedString(model, "rosetta_dsl_test_model_circular_dependency_Bar2.model_rebuild()");
    }

    /**
     * Test case for attribute circular dependency.
     */
    @Test
    public void testAttributeCircularDependency() {
        String model = """
                type CircularA:
                    b CircularB (1..1)

                type CircularB:
                    a CircularA (1..1)
                """;

        testUtils.assertBundleContainsExpectedString(model, "class com_rosetta_test_model_CircularA(BaseDataClass):");
        testUtils.assertBundleContainsExpectedString(model, "_FQRTN = 'com.rosetta.test.model.CircularA'");
        testUtils.assertBundleContainsExpectedString(model, "b: com_rosetta_test_model_CircularB = Field(..., description='')");

        testUtils.assertBundleContainsExpectedString(model, "class com_rosetta_test_model_CircularB(BaseDataClass):");
        testUtils.assertBundleContainsExpectedString(model, "_FQRTN = 'com.rosetta.test.model.CircularB'");
        testUtils.assertBundleContainsExpectedString(model, "a: com_rosetta_test_model_CircularA = Field(..., description='')");

        testUtils.assertBundleContainsExpectedString(model, "# Phase 2: Delayed Annotation Updates");
        testUtils.assertBundleContainsExpectedString(model,
                "com_rosetta_test_model_CircularA.__annotations__[\"b\"] = Annotated[com_rosetta_test_model_CircularB, com_rosetta_test_model_CircularB.serializer(), com_rosetta_test_model_CircularB.validator()]");
        testUtils.assertBundleContainsExpectedString(model,
                "com_rosetta_test_model_CircularB.__annotations__[\"a\"] = Annotated[com_rosetta_test_model_CircularA, com_rosetta_test_model_CircularA.serializer(), com_rosetta_test_model_CircularA.validator()]");

        testUtils.assertBundleContainsExpectedString(model, "# Phase 3: Rebuild");
        testUtils.assertBundleContainsExpectedString(model, "com_rosetta_test_model_CircularA.model_rebuild()");
        testUtils.assertBundleContainsExpectedString(model, "com_rosetta_test_model_CircularB.model_rebuild()");
    }

    /**
     * Bundled types must have proxy stubs at their FQ-path files so that
     * external code can import via the fully-qualified name without touching
     * _bundle.py directly.
     */
    @Test
    public void testProxyStubsForBundledTypes() {
        // Attribute cycle: CircularA ↔ CircularB (default namespace → com._bundle)
        Map<String, CharSequence> gfAttr = testUtils.generatePythonFromString(
                """
                type CircularA:
                    b CircularB (1..1)

                type CircularB:
                    a CircularA (1..1)
                """);

        String proxyCircularA = gfAttr.get("src/com/rosetta/test/model/CircularA.py").toString();
        testUtils.assertGeneratedContainsExpectedString(proxyCircularA,
                "from com._bundle import com_rosetta_test_model_CircularA as CircularA");
        testUtils.assertGeneratedContainsExpectedString(proxyCircularA, "# EOF");

        String proxyCircularB = gfAttr.get("src/com/rosetta/test/model/CircularB.py").toString();
        testUtils.assertGeneratedContainsExpectedString(proxyCircularB,
                "from com._bundle import com_rosetta_test_model_CircularB as CircularB");
        testUtils.assertGeneratedContainsExpectedString(proxyCircularB, "# EOF");

        // Inheritance cycle: Parent ↔ Child (default namespace → com._bundle)
        Map<String, CharSequence> gfInh = testUtils.generatePythonFromString(
                """
                type Parent:
                    child Child (0..1)

                type Child extends Parent:
                    val int (1..1)
                """);

        String proxyParent = gfInh.get("src/com/rosetta/test/model/Parent.py").toString();
        testUtils.assertGeneratedContainsExpectedString(proxyParent,
                "from com._bundle import com_rosetta_test_model_Parent as Parent");
        testUtils.assertGeneratedContainsExpectedString(proxyParent, "# EOF");

        String proxyChild = gfInh.get("src/com/rosetta/test/model/Child.py").toString();
        testUtils.assertGeneratedContainsExpectedString(proxyChild,
                "from com._bundle import com_rosetta_test_model_Child as Child");
        testUtils.assertGeneratedContainsExpectedString(proxyChild, "# EOF");
    }

    /**
     * Test case for inheritance circular dependency.
     */
    @Test
    public void testInheritanceCircularDependency() {
        String model = """
                type Parent:
                    child Child (0..1)

                type Child extends Parent:
                    val int (1..1)
                """;

        String generatedPython = testUtils.generatePythonAndExtractBundle(model);

        int parentIndex = generatedPython.indexOf("class com_rosetta_test_model_Parent");
        int childIndex = generatedPython.indexOf("class com_rosetta_test_model_Child(com_rosetta_test_model_Parent)");

        assertTrue(parentIndex != -1, "Parent class definition not found");
        assertTrue(childIndex != -1, "Child class definition not found");
        assertTrue(parentIndex < childIndex, "Parent must be defined before Child for inheritance to work");
    }
}
