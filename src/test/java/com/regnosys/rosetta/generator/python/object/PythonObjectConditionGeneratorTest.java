package com.regnosys.rosetta.generator.python.object;

import jakarta.inject.Inject;
import com.regnosys.rosetta.tests.RosettaInjectorProvider;
import com.regnosys.rosetta.generator.python.PythonGeneratorTestUtils;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(InjectionExtension.class)
@InjectWith(RosettaInjectorProvider.class)
public class PythonObjectConditionGeneratorTest {

    @Inject
    private PythonGeneratorTestUtils testUtils;

    @Test
    public void testConditions1() {
        testUtils.assertBundleContainsExpectedString(
                """
                        type A:
                            a0 string (0..1)
                            a1 string (0..1)

                            condition C1:
                                a0 exists or a1 exists
                        """,
                """
                        class com_rosetta_test_model_A(BaseDataClass):
                            _FQRTN = 'com.rosetta.test.model.A'
                            a0: Optional[str] = Field(None, description='')
                            a1: Optional[str] = Field(None, description='')

                            @rune_condition
                            def condition_0_C1(self):
                                item = self
                                return (rune_attr_exists(rune_resolve_attr(self, "a0")) or rune_attr_exists(rune_resolve_attr(self, "a1")))""");
    }

    @Test
    public void testGenerateTypesChoiceCondition() {
        String pythonString = testUtils.generatePythonFromString(
                """
                        type TestType: <"Test type description.">
                            testTypeValue1 string (0..1) <"Test string">
                            testTypeValue2 string (0..1) <"Test optional string">

                            condition TestChoice: <"Test choice description.">
                                optional choice testTypeValue1, testTypeValue2
                        """)
                .toString();
        testUtils.assertGeneratedContainsExpectedString(
                pythonString,
                """
                        class com_rosetta_test_model_TestType(BaseDataClass):
                            \"""
                            Test type description.
                            \"""
                            _FQRTN = 'com.rosetta.test.model.TestType'
                            testTypeValue1: Optional[str] = Field(None, description='Test string')
                            \"""
                            Test string
                            \"""
                            testTypeValue2: Optional[str] = Field(None, description='Test optional string')
                            \"""
                            Test optional string
                            \"""

                            @rune_condition
                            def condition_0_TestChoice(self):
                                \"""
                                Test choice description.
                                \"""
                                item = self
                                return rune_check_one_of(self, 'testTypeValue1', 'testTypeValue2', necessity=False)
                        """);
    }

    @Test
    public void testGenerateIfThenCondition() {
        testUtils.assertBundleContainsExpectedString(
                """
                        type AttributeIfThenTest:
                            attr1 string (0..1)
                            attr2 string (0..1)

                            condition TestIfThen:
                                if attr1 exists
                                then attr2 exists
                        """,
                """
                        class com_rosetta_test_model_AttributeIfThenTest(BaseDataClass):
                            _FQRTN = 'com.rosetta.test.model.AttributeIfThenTest'
                            attr1: Optional[str] = Field(None, description='')
                            attr2: Optional[str] = Field(None, description='')

                            @rune_condition
                            def condition_0_TestIfThen(self):
                                item = self
                                def _then_fn0():
                                    return rune_attr_exists(rune_resolve_attr(self, "attr2"))

                                def _else_fn0():
                                    return True

                                return if_cond_fn(rune_attr_exists(rune_resolve_attr(self, "attr1")), _then_fn0, _else_fn0)""");
    }

    @Test
    public void testConditionsGeneration() {
        String pythonString = testUtils.generatePythonFromString(
                """
                        type A:
                            a0 int (0..1)
                            a1 int (0..1)
                            condition: one-of
                        type B:
                            intValue1 int (0..1)
                            intValue2 int (0..1)
                            aValue A (1..1)
                            condition Rule:
                                intValue1 < 100
                            condition OneOrTwo: <"Choice rule to represent an FpML choice construct.">
                                optional choice intValue1, intValue2
                            condition ReqOneOrTwo: <"Choice rule to represent an FpML choice construct.">
                                required choice intValue1, intValue2
                            condition SecondOneOrTwo: <"FpML specifies a choice between adjustedDate and [unadjustedDate (required), dateAdjutsments (required), adjustedDate (optional)].">
                                aValue->a0 exists
                                    or (intValue2 exists and intValue1 exists and intValue1 exists)
                                    or (intValue2 exists and intValue1 exists and intValue1 is absent)
                        """)
                .toString();
        testUtils.assertGeneratedContainsExpectedString(
                pythonString,
                """
                        class com_rosetta_test_model_A(BaseDataClass):
                            _FQRTN = 'com.rosetta.test.model.A'
                            a0: Optional[int] = Field(None, description='')
                            a1: Optional[int] = Field(None, description='')

                            @rune_condition
                            def condition_0_(self):
                                item = self
                                return rune_check_one_of(self, 'a0', 'a1', necessity=True)
                        """);
        testUtils.assertGeneratedContainsExpectedString(
                pythonString,
                """
                        class com_rosetta_test_model_B(BaseDataClass):
                            _FQRTN = 'com.rosetta.test.model.B'
                            intValue1: Optional[int] = Field(None, description='')
                            intValue2: Optional[int] = Field(None, description='')
                            aValue: Annotated[com_rosetta_test_model_A, com_rosetta_test_model_A.serializer(), com_rosetta_test_model_A.validator()] = Field(..., description='')

                            @rune_condition
                            def condition_0_Rule(self):
                                item = self
                                return rune_all_elements(rune_resolve_attr(self, "intValue1"), "<", 100)

                            @rune_condition
                            def condition_1_OneOrTwo(self):
                                \"""
                                Choice rule to represent an FpML choice construct.
                                \"""
                                item = self
                                return rune_check_one_of(self, 'intValue1', 'intValue2', necessity=False)

                            @rune_condition
                            def condition_2_ReqOneOrTwo(self):
                                \"""
                                Choice rule to represent an FpML choice construct.
                                \"""
                                item = self
                                return rune_check_one_of(self, 'intValue1', 'intValue2', necessity=True)

                            @rune_condition
                            def condition_3_SecondOneOrTwo(self):
                                \"""
                                FpML specifies a choice between adjustedDate and [unadjustedDate (required), dateAdjutsments (required), adjustedDate (optional)].
                                \"""
                                item = self
                                return ((rune_attr_exists(rune_resolve_attr(rune_resolve_attr(self, "aValue"), "a0")) or ((rune_attr_exists(rune_resolve_attr(self, "intValue2")) and rune_attr_exists(rune_resolve_attr(self, "intValue1"))) and rune_attr_exists(rune_resolve_attr(self, "intValue1")))) or ((rune_attr_exists(rune_resolve_attr(self, "intValue2")) and rune_attr_exists(rune_resolve_attr(self, "intValue1"))) and (not rune_attr_exists(rune_resolve_attr(self, "intValue1")))))""");
    }

    @Test
    public void testGenerateIfThenElseCondition() {
        testUtils.assertBundleContainsExpectedString(
                """
                        type AttributeIfThenElseTest:
                            attr1 string (0..1)
                            attr2 string (0..1)
                            attr3 string (0..1)

                            condition TestIfThenElse:
                                if attr1 exists
                                then attr2 exists
                                else attr3 exists
                        """,
                """
                        class com_rosetta_test_model_AttributeIfThenElseTest(BaseDataClass):
                            _FQRTN = 'com.rosetta.test.model.AttributeIfThenElseTest'
                            attr1: Optional[str] = Field(None, description='')
                            attr2: Optional[str] = Field(None, description='')
                            attr3: Optional[str] = Field(None, description='')

                            @rune_condition
                            def condition_0_TestIfThenElse(self):
                                item = self
                                def _then_fn0():
                                    return rune_attr_exists(rune_resolve_attr(self, "attr2"))

                                def _else_fn0():
                                    return rune_attr_exists(rune_resolve_attr(self, "attr3"))

                                return if_cond_fn(rune_attr_exists(rune_resolve_attr(self, "attr1")), _then_fn0, _else_fn0)""");
    }

    @Test
    public void testConditionLessOrEqual() {
        testUtils.assertBundleContainsExpectedString(
                """
                        type Foo:
                            a number (0..1)
                            b number (0..1)

                            condition:
                                a <= b
                        """,
                """
                        class com_rosetta_test_model_Foo(BaseDataClass):
                            _FQRTN = 'com.rosetta.test.model.Foo'
                            a: Optional[Decimal] = Field(None, description='')
                            b: Optional[Decimal] = Field(None, description='')

                            @rune_condition
                            def condition_0_(self):
                                item = self
                                return rune_all_elements(rune_resolve_attr(self, "a"), "<=", rune_resolve_attr(self, "b"))""");
    }
}
