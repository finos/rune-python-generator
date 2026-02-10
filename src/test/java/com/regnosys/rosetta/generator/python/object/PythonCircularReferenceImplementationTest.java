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

/**
 * This is an Anchor test.
 * Every element of this test needs to check the entire generated Python
 * to ensure the full Phase 1-2-3 structure is preserved.
 */
@ExtendWith(InjectionExtension.class)
@InjectWith(RosettaInjectorProvider.class)
public class PythonCircularReferenceImplementationTest {

        @Inject
        private PythonGeneratorTestUtils testUtils;

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

                String bundle = gf.get("src/rosetta_dsl/_bundle.py").toString();

                // 1. Verify Clean Definitions in Phase 1
                assertTrue(bundle.contains("class rosetta_dsl_test_language_CircularDependency_A(BaseDataClass):"),
                                "Class A should be defined");
                assertTrue(bundle.contains(
                                "b: rosetta_dsl_test_language_CircularDependency_B = Field(..., description='')"),
                                "Field A.b should have a clean type hint");

                assertTrue(bundle.contains("class rosetta_dsl_test_language_CircularDependency_B(BaseDataClass):"),
                                "Class B should be defined");
                assertTrue(
                                bundle.contains(
                                                "a: Optional[rosetta_dsl_test_language_CircularDependency_A] = Field(None, description='')"),
                                "Field B.a should have a clean type hint");

                // 2. Verify Delayed Annotation Updates in Phase 2
                assertTrue(bundle.contains(
                                "rosetta_dsl_test_language_CircularDependency_A.__annotations__[\"b\"] = Annotated[rosetta_dsl_test_language_CircularDependency_B, rosetta_dsl_test_language_CircularDependency_B.serializer(), rosetta_dsl_test_language_CircularDependency_B.validator()]"));
                assertTrue(bundle.contains(
                                "rosetta_dsl_test_language_CircularDependency_B.__annotations__[\"a\"] = Optional[Annotated[rosetta_dsl_test_language_CircularDependency_A, rosetta_dsl_test_language_CircularDependency_A.serializer(), rosetta_dsl_test_language_CircularDependency_A.validator()]]"));

                // 3. Verify Model Rebuilds in Phase 3
                assertTrue(bundle.contains("rosetta_dsl_test_language_CircularDependency_A.model_rebuild()"));
                assertTrue(bundle.contains("rosetta_dsl_test_language_CircularDependency_B.model_rebuild()"));
        }
}
