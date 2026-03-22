package com.regnosys.rosetta.generator.python.object;

import jakarta.inject.Inject;
import com.regnosys.rosetta.tests.RosettaInjectorProvider;
import com.regnosys.rosetta.generator.python.PythonGeneratorTestUtils;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import java.util.Map;

/**
 * This is an Anchor test.
 * Every element of this test needs to check the entire generated Python
 * to ensure the full Phase 1-2-3 structure is preserved.
 */
@ExtendWith(InjectionExtension.class)
@InjectWith(RosettaInjectorProvider.class)
@SuppressWarnings("LineLength")
public class PythonCircularReferenceImplementationTest {

    /**
     * Injected test utils.
     */
    @Inject
    private PythonGeneratorTestUtils testUtils;

    /**
     * Test case for circular dependency implementation.
     */
    @Test
    public void testCircularDependencyImplementation() {
        Map<String, CharSequence> gf = testUtils.generatePythonFromString(
                """
                namespace rosetta_dsl.test.language.CircularDependency

                type A:
                        b B (1..1)

                type B:
                        a A (0..1)
                """);

        String generatedPython = gf.get("src/rosetta_dsl/_bundle.py").toString();

        // 1. Verify Clean Definitions in Phase 1
        testUtils.assertGeneratedContainsExpectedString(generatedPython,
                "class rosetta_dsl_test_language_CircularDependency_A(BaseDataClass):");
        testUtils.assertGeneratedContainsExpectedString(generatedPython,
                "b: rosetta_dsl_test_language_CircularDependency_B = Field(..., description='')");

        testUtils.assertGeneratedContainsExpectedString(generatedPython,
                "class rosetta_dsl_test_language_CircularDependency_B(BaseDataClass):");
        testUtils.assertGeneratedContainsExpectedString(generatedPython,
                "a: Optional[rosetta_dsl_test_language_CircularDependency_A] = Field(None, description='')");

        // 2. Verify Delayed Annotation Updates in Phase 2
        testUtils.assertGeneratedContainsExpectedString(generatedPython,
                "rosetta_dsl_test_language_CircularDependency_A.__annotations__[\"b\"] = Annotated[rosetta_dsl_test_language_CircularDependency_B, rosetta_dsl_test_language_CircularDependency_B.serializer(), rosetta_dsl_test_language_CircularDependency_B.validator()]");
        testUtils.assertGeneratedContainsExpectedString(generatedPython,
                "rosetta_dsl_test_language_CircularDependency_B.__annotations__[\"a\"] = Annotated[Optional[rosetta_dsl_test_language_CircularDependency_A], rosetta_dsl_test_language_CircularDependency_A.serializer(), rosetta_dsl_test_language_CircularDependency_A.validator()]");

        // 3. Verify Model Rebuilds in Phase 3
        testUtils.assertGeneratedContainsExpectedString(generatedPython,
                "rosetta_dsl_test_language_CircularDependency_A.model_rebuild()");
        testUtils.assertGeneratedContainsExpectedString(generatedPython,
                "rosetta_dsl_test_language_CircularDependency_B.model_rebuild()");

        // 4. Verify Proxy Stubs at FQ paths — external imports must always use
        //    the fully-qualified path, never import from _bundle directly.
        String proxyA = gf.get("src/rosetta_dsl/test/language/CircularDependency/A.py").toString();
        testUtils.assertGeneratedContainsExpectedString(proxyA,
                "# pylint: disable=unused-import");
        testUtils.assertGeneratedContainsExpectedString(proxyA,
                "from rosetta_dsl._bundle import rosetta_dsl_test_language_CircularDependency_A as A");
        testUtils.assertGeneratedContainsExpectedString(proxyA, "# EOF");

        String proxyB = gf.get("src/rosetta_dsl/test/language/CircularDependency/B.py").toString();
        testUtils.assertGeneratedContainsExpectedString(proxyB,
                "# pylint: disable=unused-import");
        testUtils.assertGeneratedContainsExpectedString(proxyB,
                "from rosetta_dsl._bundle import rosetta_dsl_test_language_CircularDependency_B as B");
        testUtils.assertGeneratedContainsExpectedString(proxyB, "# EOF");
    }
}
