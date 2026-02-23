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

    @Inject
    private PythonGeneratorTestUtils testUtils;

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

        testUtils.assertGeneratedContainsExpectedString(generatedPython,
                """
                        class com_rosetta_test_model_TestMap(BaseDataClass):
                            _FQRTN = 'com.rosetta.test.model.TestMap'
                            items: Optional[list[Annotated[com_rosetta_test_model_Item, com_rosetta_test_model_Item.serializer(), com_rosetta_test_model_Item.validator()]]] = Field(None, description='')

                            @rune_condition
                            def condition_0_MapCheck(self):
                                item = self
                                return rune_all_elements((lambda item: rune_count(item))(list(map(lambda item: rune_resolve_attr(item, "val"), rune_resolve_attr(self, "items")))), "=", 0)""");
    }
}
