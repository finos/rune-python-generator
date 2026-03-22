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
public class RosettaFlattenOperationTest {

    /**
     * Test utils for generating Python.
     */
    @Inject
    private PythonGeneratorTestUtils testUtils;

    /**
     * Test case for flatten operation.
     */
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

        // Targeted assertions for Foo class (standalone — no Phase 2/3)
        testUtils.assertGeneratedContainsExpectedString(generatedPython,
                "class Foo(BaseDataClass):");
        testUtils.assertGeneratedContainsExpectedString(generatedPython,
                "bars: Optional[list[Bar | None]] = Field(None, description='test bar')");
        testUtils.assertGeneratedContainsExpectedString(generatedPython,
                "return rune_all_elements([1, 2, 3], \"=\", (lambda item: (lambda nested: [x for sub in (nested or []) if sub is not None for x in (sub if (hasattr(sub, '__iter__') and not isinstance(sub, (str, dict, bytes, bytearray))) else [sub]) if x is not None] if nested is not None else None)(item))([x for x in map(lambda item: rune_resolve_attr(item, \"numbers\"), rune_resolve_attr(self, \"bars\") or []) if x is not None]))");
    }
}
