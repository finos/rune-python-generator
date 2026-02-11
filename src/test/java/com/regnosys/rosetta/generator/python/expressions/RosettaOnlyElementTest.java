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
public class RosettaOnlyElementTest {

    /**
     * Test utils for generating Python.
     */
    @Inject
    private PythonGeneratorTestUtils testUtils;

    /**
     * Test case for only-element condition.
     */
    @Test
    public void testGenerateOnlyElementCondition() {
        String generatedPython = testUtils.generatePythonFromString("""
                enum TestEnum: <"Enum to test">
                TestEnumValue1 <"Test enum value 1">
                TestEnumValue2 <"Test enum value 2">
                type Test1: <"Test only-element condition.">
                    field1 TestEnum (0..1) <"Test enum field 1">
                    field2 number (0..1) <"Test number field 2">
                    condition TestCond: <"Test condition">
                        if field1 only-element= TestEnum->TestEnumValue1
                            then field2=0
                """).toString();

        String expectedTestEnum = """
                class TestEnum(rune.runtime.metadata.EnumWithMetaMixin, Enum):
                    \"""
                    Enum to test
                    \"""
                    TEST_ENUM_VALUE_1 = "TestEnumValue1"
                    \"""
                    Test enum value 1
                    \"""
                    TEST_ENUM_VALUE_2 = "TestEnumValue2"
                    \"""
                    Test enum value 2
                    \"""";

        String expectedTest1 = """
                class com_rosetta_test_model_Test1(BaseDataClass):
                    \"""
                    Test only-element condition.
                    \"""
                    _FQRTN = 'com.rosetta.test.model.Test1'
                    field1: Optional[com.rosetta.test.model.TestEnum.TestEnum] = Field(None, description='Test enum field 1')
                    \"""
                    Test enum field 1
                    \"""
                    field2: Optional[Decimal] = Field(None, description='Test number field 2')
                    \"""
                    Test number field 2
                    \"""

                    @rune_condition
                    def condition_0_TestCond(self):
                        \"""
                        Test condition
                        \"""
                        item = self
                        def _then_fn0():
                            return rune_all_elements(rune_resolve_attr(self, "field2"), "=", 0)

                        def _else_fn0():
                            return True

                        return if_cond_fn(rune_all_elements(rune_get_only_element(rune_resolve_attr(self, "field1")), "=", com.rosetta.test.model.TestEnum.TestEnum.TEST_ENUM_VALUE_1), _then_fn0, _else_fn0)""";

        testUtils.assertGeneratedContainsExpectedString(generatedPython, expectedTestEnum);
        testUtils.assertGeneratedContainsExpectedString(generatedPython, expectedTest1);
    }
}
