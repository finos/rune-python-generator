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
@SuppressWarnings("LineLength")
public class RosettaMapOperationTest {

    /**
     * Test utils for generating Python.
     */
    @Inject
    private PythonGeneratorTestUtils testUtils;

    /**
     * Test case for map operation.
     */
    @Test
    public void testMapOperation() {
        String generatedPython = testUtils.generatePythonFromString("""
                type Item:
                    val int (1..1)
                type TestMap:
                    items Item (0..*)
                    condition MapCheck:
                        (items extract val then count) = 0
                """).toString();

        // Targeted assertions for TestMap class (standalone — no Phase 2/3)
        testUtils.assertGeneratedContainsExpectedString(generatedPython,
                "class TestMap(BaseDataClass):");
        testUtils.assertGeneratedContainsExpectedString(generatedPython,
                "items: Optional[list[Item | None]] = Field(None, description='')");
        testUtils.assertGeneratedContainsExpectedString(generatedPython,
                "return rune_all_elements((lambda item: (lambda items: sum(1 for x in (items if (hasattr(items, '__iter__') and not isinstance(items, (str, dict, bytes, bytearray))) else ([items] if items is not None else [])) if x is not None))(item))([x for x in map(lambda item: rune_resolve_attr(item, \"val\"), rune_resolve_attr(self, \"items\") or []) if x is not None]), \"=\", 0)");
    }
}
