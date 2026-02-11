package com.regnosys.rosetta.generator.python.expressions;

import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.regnosys.rosetta.generator.python.PythonGeneratorTestUtils;
import com.regnosys.rosetta.tests.RosettaInjectorProvider;

import jakarta.inject.Inject;

@ExtendWith(InjectionExtension.class)
@InjectWith(RosettaInjectorProvider.class)
public class RosettaMapOperationTest {

        /**
         * Test utils for generating Python.
         */
        @Inject
        private PythonGeneratorTestUtils testUtils;

        /**
         * Test case for map operation.
         */
        @Test
        public void testMapOperation() {
                String generatedPython = testUtils.generatePythonFromString("""
                                type Item:
                                    val int (1..1)
                                type TestMap:
                                    items Item (0..*)
                                    condition MapCheck:
                                        (items extract val then count) = 0
                                """).toString();

                // Targeted assertions for TestMap class (Phase 1, 2, 3)
                testUtils.assertGeneratedContainsExpectedString(generatedPython,
                                "class com_rosetta_test_model_TestMap(BaseDataClass):");
                testUtils.assertGeneratedContainsExpectedString(generatedPython,
                                "items: Optional[list[com_rosetta_test_model_Item]] = Field(None, description='')");
                testUtils.assertGeneratedContainsExpectedString(generatedPython,
                                "com_rosetta_test_model_TestMap.__annotations__[\"items\"] = Optional[list[Annotated[com_rosetta_test_model_Item, com_rosetta_test_model_Item.serializer(), com_rosetta_test_model_Item.validator()]]]");
                testUtils.assertGeneratedContainsExpectedString(generatedPython,
                                "com_rosetta_test_model_TestMap.model_rebuild()");
                testUtils.assertGeneratedContainsExpectedString(generatedPython,
                                "return rune_all_elements((lambda item: rune_count(item))(list(map(lambda item: rune_resolve_attr(item, \"val\"), rune_resolve_attr(self, \"items\")))), \"=\", 0)");
        }
}
