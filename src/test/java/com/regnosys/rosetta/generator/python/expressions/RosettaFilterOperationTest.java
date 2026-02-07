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
public class RosettaFilterOperationTest {

    @Inject
    private PythonGeneratorTestUtils testUtils;

    @Test
    public void testFilterOperation() {
        testUtils.assertBundleContainsExpectedString("""
                type Item:
                    val int (1..1)
                type TestFilter:
                    items Item (0..*)
                    condition FilterCheck:
                        (items filter [ val > 5 ] then count) = 0
                """,
                "return rune_all_elements((lambda item: rune_count(item))(rune_filter(rune_resolve_attr(self, \"items\"), lambda item: rune_all_elements(rune_resolve_attr(item, \"val\"), \">\", 5))), \"=\", 0)");
    }

    @Test
    public void testNestedFilterMapCount() {
        testUtils.assertBundleContainsExpectedString("""
                type Item:
                    val int (1..1)
                func TestNestedNested:
                    inputs: items Item (0..*)
                    output: result int (1..1)
                    set result:
                        items filter [ val > 5 ] then count
                """,
                "result = (lambda item: rune_count(item))(rune_filter(rune_resolve_attr(self, \"items\"), lambda item: rune_all_elements(rune_resolve_attr(item, \"val\"), \">\", 5)))");
    }
}
