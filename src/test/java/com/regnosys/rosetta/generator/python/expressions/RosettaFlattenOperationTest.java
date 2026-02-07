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

        String expectedFoo = """
                class com_rosetta_test_model_Foo(BaseDataClass):
                    \"""
                    Test flatten operation condition
                    \"""
                    _FQRTN = 'com.rosetta.test.model.Foo'
                    bars: Optional[list[Annotated[com_rosetta_test_model_Bar, com_rosetta_test_model_Bar.serializer(), com_rosetta_test_model_Bar.validator()]]] = Field(None, description='test bar')
                    \"""
                    test bar
                    \"""

                    @rune_condition
                    def condition_0_TestCondition(self):
                        \"""
                        Test Condition
                        \"""
                        item = self
                        return rune_all_elements([1, 2, 3], "=", (lambda item: rune_flatten_list(item))(list(map(lambda item: rune_resolve_attr(item, "numbers"), rune_resolve_attr(self, "bars")))))""";

        testUtils.assertGeneratedContainsExpectedString(generatedPython, expectedFoo);
    }
}
