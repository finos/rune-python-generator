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

    /**
     * Test utils for generating Python.
     */
    @Inject
    private PythonGeneratorTestUtils testUtils;

    /**
     * Test case for filter operation.
     */
    @Test
    public void testFilterOperation() {
        String bundle = testUtils.generatePythonAndExtractBundle("""
                type Item:
                    val int (1..1)
                type TestFilter:
                    items Item (0..*)
                    condition FilterCheck:
                        (items filter [ val > 5 ] then count) = 0
                """);

        testUtils.assertGeneratedContainsExpectedString(bundle,
                "class com_rosetta_test_model_TestFilter(BaseDataClass):");
        testUtils.assertGeneratedContainsExpectedString(bundle,
                "items: Optional[list[com_rosetta_test_model_Item]] = Field(None, description='')");
        testUtils.assertGeneratedContainsExpectedString(bundle,
                "com_rosetta_test_model_TestFilter.__annotations__[\"items\"] = Annotated[Optional[list[com_rosetta_test_model_Item]], com_rosetta_test_model_Item.serializer(), com_rosetta_test_model_Item.validator()]");
        testUtils.assertGeneratedContainsExpectedString(bundle,
                "com_rosetta_test_model_TestFilter.model_rebuild()");
        testUtils.assertGeneratedContainsExpectedString(bundle,
                "return rune_all_elements((lambda item: rune_count(item))(rune_filter(rune_resolve_attr(self, \"items\"), lambda item: rune_all_elements(rune_resolve_attr(item, \"val\"), \">\", 5))), \"=\", 0)");
    }

    /**
     * Test case for nested filter map count.
     */
    @Test
    public void testNestedFilterMapCount() {
        String bundle = testUtils.generatePythonAndExtractBundle("""
                type Item:
                    val int (1..1)
                func TestNestedNested:
                    inputs: items Item (0..*)
                    output: result int (1..1)
                    set result:
                        items filter [ val > 5 ] then count
                """);

        testUtils.assertGeneratedContainsExpectedString(bundle,
                "def com_rosetta_test_model_functions_TestNestedNested(items: list[com_rosetta_test_model_Item] | None) -> int:");
        testUtils.assertGeneratedContainsExpectedString(bundle,
                "result = (lambda item: rune_count(item))(rune_filter(rune_resolve_attr(self, \"items\"), lambda item: rune_all_elements(rune_resolve_attr(item, \"val\"), \">\", 5)))");
    }
}
