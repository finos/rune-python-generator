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
        testUtils.assertBundleContainsExpectedString("""
                type TestAgg:
                    items int (0..*)
                    condition AggCheck:
                        items sum = 10 and
                        items max = 5 and
                        items min = 1
                """,
                "return ((rune_all_elements(sum(rune_resolve_attr(self, \"items\")), \"=\", 10) and rune_all_elements(max(rune_resolve_attr(self, \"items\")), \"=\", 5)) and rune_all_elements(min(rune_resolve_attr(self, \"items\")), \"=\", 1))");
    }

    @Test
    public void testAccessors() {
        testUtils.assertBundleContainsExpectedString("""
                type TestAccess:
                    items int (0..*)
                    condition AccessCheck:
                        items first = 1 and
                        items last = 5
                """,
                "return (rune_all_elements(rune_resolve_attr(self, \"items\")[0], \"=\", 1) and rune_all_elements(rune_resolve_attr(self, \"items\")[-1], \"=\", 5))");
    }

    @Test
    public void testSortOperation() {
        testUtils.assertBundleContainsExpectedString("""
                type TestSort:
                    items int (0..*)
                    condition SortCheck:
                        items sort = [1]
                """,
                "return rune_all_elements(sorted(rune_resolve_attr(self, \"items\")), \"=\", [1])");
    }

    @Test
    public void testListComparison() {
        testUtils.assertBundleContainsExpectedString("""
                type TestListComp:
                    list1 int (0..*)
                    list2 int (0..*)
                    condition CompCheck:
                        list1 = list2
                """,
                "return rune_all_elements(rune_resolve_attr(self, \"list1\"), \"=\", rune_resolve_attr(self, \"list2\"))");
    }

    @Test
    public void testCollectionLiteral() {
        testUtils.assertBundleContainsExpectedString("""
                func TestLiteral:
                    output: result int (0..*)
                    set result:
                        [1, 2, 3]
                """,
                "result = [1, 2, 3]");
    }
}
