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
public class RosettaContainsOperationTest {

    /**
     * Test utils for generating Python.
     */
    @Inject
    private PythonGeneratorTestUtils testUtils;

    /**
     * Test case for binary contains operation.
     */
    @Test
    public void testGenerateBinContainsCondition() {
        String generatedPython = testUtils.generatePythonFromString("""
                enum C: <"Test type C">
                    field4 <"Test enum field 4">
                    field5 <"Test enum field 5">
                type A: <"Test type">
                    field1 int (1..*) <"Test int field 1">
                    cValue C (1..*) <"Test C type cValue">
                type B: <"Test type B">
                    field2 int (1..*) <"Test int field 2">
                    aValue A (1..*) <"Test A type aValue">
                type Test: <"Test filter operation condition">
                    bValue B (1..*) <"Test B type bValue">
                    field3 boolean (0..1) <"Test bool type field3">
                    condition TestCond: <"Test condition">
                        if field3=True
                        then bValue->aValue->cValue contains C->field4
                """).toString();

        String expectedC = """
                class C(rune.runtime.metadata.EnumWithMetaMixin, Enum):
                    \"""
                    Test type C
                    \"""
                    FIELD_4 = "field4"
                    \"""
                    Test enum field 4
                    \"""
                    FIELD_5 = "field5"
                    \"""
                    Test enum field 5
                    \"""
                """;

        String expectedA = """
                class com_rosetta_test_model_A(BaseDataClass):
                    \"""
                    Test type
                    \"""
                    _FQRTN = 'com.rosetta.test.model.A'
                    field1: list[int] = Field(..., description='Test int field 1', min_length=1)
                    \"""
                    Test int field 1
                    \"""
                    cValue: list[com.rosetta.test.model.C.C] = Field(..., description='Test C type cValue', min_length=1)
                    \"""
                    Test C type cValue
                    \"""
                """;

        String expectedBPhase1 = """
                class com_rosetta_test_model_B(BaseDataClass):
                    \"""
                    Test type B
                    \"""
                    _FQRTN = 'com.rosetta.test.model.B'
                    field2: list[int] = Field(..., description='Test int field 2', min_length=1)
                    \"""
                    Test int field 2
                    \"""
                    aValue: list[com_rosetta_test_model_A] = Field(..., description='Test A type aValue', min_length=1)
                    \"""
                    Test A type aValue
                    \"""
                """;

        String expectedBPhase23 = """
                # Phase 2: Delayed Annotation Updates
                com_rosetta_test_model_B.__annotations__["aValue"] = Annotated[list[com_rosetta_test_model_A], com_rosetta_test_model_A.serializer(), com_rosetta_test_model_A.validator()]

                # Phase 3: Rebuild
                com_rosetta_test_model_B.model_rebuild()
                """;

        String expectedTestPhase1 = """
                class com_rosetta_test_model_Test(BaseDataClass):
                    \"""
                    Test filter operation condition
                    \"""
                    _FQRTN = 'com.rosetta.test.model.Test'
                    bValue: list[com_rosetta_test_model_B] = Field(..., description='Test B type bValue', min_length=1)
                    \"""
                    Test B type bValue
                    \"""
                    field3: Optional[bool] = Field(None, description='Test bool type field3')
                    \"""
                    Test bool type field3
                    \"""

                    @rune_condition
                    def condition_0_TestCond(self):
                        \"""
                        Test condition
                        \"""
                        item = self
                        def _then_fn0():
                            return rune_contains(rune_resolve_attr(rune_resolve_attr(rune_resolve_attr(self, "bValue"), "aValue"), "cValue"), com.rosetta.test.model.C.C.FIELD_4)

                        def _else_fn0():
                            return True

                        return if_cond_fn(rune_all_elements(rune_resolve_attr(self, "field3"), "=", True), _then_fn0, _else_fn0)
                """;

        String expectedTestPhase23 = """
                # Phase 2: Delayed Annotation Updates
                com_rosetta_test_model_Test.__annotations__["bValue"] = Annotated[list[com_rosetta_test_model_B], com_rosetta_test_model_B.serializer(), com_rosetta_test_model_B.validator()]

                # Phase 3: Rebuild
                com_rosetta_test_model_Test.model_rebuild()""";

        testUtils.assertGeneratedContainsExpectedString(generatedPython, expectedC);
        testUtils.assertGeneratedContainsExpectedString(generatedPython, expectedA);
        testUtils.assertGeneratedContainsExpectedString(generatedPython, expectedBPhase1);
        testUtils.assertGeneratedContainsExpectedString(generatedPython, expectedBPhase23);
        testUtils.assertGeneratedContainsExpectedString(generatedPython, expectedTestPhase1);
        testUtils.assertGeneratedContainsExpectedString(generatedPython, expectedTestPhase23);
    }
}
