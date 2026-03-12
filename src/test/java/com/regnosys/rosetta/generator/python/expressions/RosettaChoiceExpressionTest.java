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
public class RosettaChoiceExpressionTest {

    @Inject
    private PythonGeneratorTestUtils testUtils;

    @Test
    public void testGenerateChoiceCondition() {
        testUtils.assertBundleContainsExpectedString("""
                type Test1:<"Test choice condition.">
                field1 string (0..1) <"Test string field 1">
                field2 string (0..1) <"Test string field 2">
                field3 string (0..1) <"Test string field 3">
                condition TestChoice: optional choice field1, field2, field3
                """,
                """
                        class com_rosetta_test_model_Test1(BaseDataClass):
                            \"""
                            Test choice condition.
                            \"""
                            _FQRTN = 'com.rosetta.test.model.Test1'
                            field1: Optional[str] = Field(None, description='Test string field 1')
                            \"""
                            Test string field 1
                            \"""
                            field2: Optional[str] = Field(None, description='Test string field 2')
                            \"""
                            Test string field 2
                            \"""
                            field3: Optional[str] = Field(None, description='Test string field 3')
                            \"""
                            Test string field 3
                            \"""

                            @rune_condition
                            def condition_0_TestChoice(self):
                                item = self
                                return rune_check_one_of(self, 'field1', 'field2', 'field3', necessity=False)""");
    }

    @Test
    public void testGenerateOneOfCondition() {
        testUtils.assertBundleContainsExpectedString("""
                type Test1:<"Test one-of condition.">
                    field1 string (0..1) <"Test string field 1">
                    condition OneOf: one-of
                """,
                """
                        class com_rosetta_test_model_Test1(BaseDataClass):
                            _CHOICE_ALIAS_MAP ={"field1":[]}
                            \"""
                            Test one-of condition.
                            \"""
                            _FQRTN = 'com.rosetta.test.model.Test1'
                            field1: Optional[str] = Field(None, description='Test string field 1')
                            \"""
                            Test string field 1
                            \"""

                            @rune_condition
                            def condition_0_OneOf(self):
                                item = self
                                return rune_check_one_of(self, 'field1', necessity=True)""");
    }
}
