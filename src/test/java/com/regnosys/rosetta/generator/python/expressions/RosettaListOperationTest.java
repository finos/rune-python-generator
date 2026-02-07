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
public class RosettaListOperationTest {

    @Inject
    private PythonGeneratorTestUtils testUtils;

    @Test
    public void testAggregations() {
        String generatedPython = testUtils.generatePythonFromString("""
                type TestAgg:
                    items int (0..*)
                    condition AggCheck:
                        items sum = 10 and
                        items max = 5 and
                        items min = 1
                """).toString();

        testUtils.assertGeneratedContainsExpectedString(generatedPython,
                """
                        class com_rosetta_test_model_TestAgg(BaseDataClass):
                            _FQRTN = 'com.rosetta.test.model.TestAgg'
                            items: Optional[list[int]] = Field(None, description='')

                            @rune_condition
                            def condition_0_AggCheck(self):
                                item = self
                                return ((rune_all_elements(sum(rune_resolve_attr(self, "items")), "=", 10) and rune_all_elements(max(rune_resolve_attr(self, "items")), "=", 5)) and rune_all_elements(min(rune_resolve_attr(self, "items")), "=", 1))""");
    }

    @Test
    public void testAccessors() {
        String generatedPython = testUtils.generatePythonFromString("""
                type TestAccess:
                    items int (0..*)
                    condition AccessCheck:
                        items first = 1 and
                        items last = 5
                """).toString();

        testUtils.assertGeneratedContainsExpectedString(generatedPython,
                """
                        class com_rosetta_test_model_TestAccess(BaseDataClass):
                            _FQRTN = 'com.rosetta.test.model.TestAccess'
                            items: Optional[list[int]] = Field(None, description='')

                            @rune_condition
                            def condition_0_AccessCheck(self):
                                item = self
                                return (rune_all_elements(rune_resolve_attr(self, "items")[0], "=", 1) and rune_all_elements(rune_resolve_attr(self, "items")[-1], "=", 5))""");
    }

    @Test
    public void testSortOperation() {
        String generatedPython = testUtils.generatePythonFromString("""
                type TestSort:
                    items int (0..*)
                    condition SortCheck:
                        items sort = [1]
                """).toString();

        testUtils.assertGeneratedContainsExpectedString(generatedPython,
                """
                        class com_rosetta_test_model_TestSort(BaseDataClass):
                            _FQRTN = 'com.rosetta.test.model.TestSort'
                            items: Optional[list[int]] = Field(None, description='')

                            @rune_condition
                            def condition_0_SortCheck(self):
                                item = self
                                return rune_all_elements(sorted(rune_resolve_attr(self, "items")), "=", [1])""");
    }
}
