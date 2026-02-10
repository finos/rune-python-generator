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
public class RosettaFlattenOperationTest {

    @Inject
    private PythonGeneratorTestUtils testUtils;

    @Test
    public void testGenerateFlattenCondition() {
        String generatedPython = testUtils.generatePythonFromString("""
                type Bar:
                    numbers int (0..*)
                type Foo: <"Test flatten operation condition">
                    bars Bar (0..*) <"test bar">
                    condition TestCondition: <"Test Condition">
                        [1, 2, 3] =
                        (bars
                            extract numbers
                            then flatten)
                """).toString();

        // Targeted assertions for Foo class (Phase 1, 2, 3)
        testUtils.assertGeneratedContainsExpectedString(generatedPython,
                "class com_rosetta_test_model_Foo(BaseDataClass):");
        testUtils.assertGeneratedContainsExpectedString(generatedPython,
                "bars: Optional[list[com_rosetta_test_model_Bar]] = Field(None, description='test bar')");
        testUtils.assertGeneratedContainsExpectedString(generatedPython,
                "com_rosetta_test_model_Foo.__annotations__[\"bars\"] = Optional[list[Annotated[com_rosetta_test_model_Bar, com_rosetta_test_model_Bar.serializer(), com_rosetta_test_model_Bar.validator()]]]");
        testUtils.assertGeneratedContainsExpectedString(generatedPython, "com_rosetta_test_model_Foo.model_rebuild()");
        testUtils.assertGeneratedContainsExpectedString(generatedPython,
                "return rune_all_elements([1, 2, 3], \"=\", (lambda item: rune_flatten_list(item))(list(map(lambda item: rune_resolve_attr(item, \"numbers\"), rune_resolve_attr(self, \"bars\")))))");
    }
}
