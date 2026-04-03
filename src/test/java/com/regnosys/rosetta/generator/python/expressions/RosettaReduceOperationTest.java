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
public class RosettaReduceOperationTest {

    @Inject
    private PythonGeneratorTestUtils testUtils;

    @Test
    public void testReduceIntSum() {
        testUtils.assertBundleContainsExpectedString("""
                func SumInts:
                    inputs: values int (0..*)
                    output: result int (1..1)
                    set result:
                        values reduce a, b [ a + b ]
                """,
                "functools.reduce(lambda a, b: (a + b), rune_resolve_attr(self, \"values\"))");
    }

    @Test
    public void testReduceIntSubtract() {
        testUtils.assertBundleContainsExpectedString("""
                func SubtractInts:
                    inputs: values int (0..*)
                    output: result int (1..1)
                    set result:
                        values reduce a, b [ a - b ]
                """,
                "functools.reduce(lambda a, b: (a - b), rune_resolve_attr(self, \"values\"))");
    }

    @Test
    public void testReduceNumberSum() {
        testUtils.assertBundleContainsExpectedString("""
                func SumNumbers:
                    inputs: values number (0..*)
                    output: result number (1..1)
                    set result:
                        values reduce acc, cur [ acc + cur ]
                """,
                "functools.reduce(lambda acc, cur: (acc + cur), rune_resolve_attr(self, \"values\"))");
    }

    @Test
    public void testReduceInCondition() {
        testUtils.assertBundleContainsExpectedString("""
                type Scores:
                    vals int (0..*)
                    condition TotalPositive:
                        (vals reduce a, b [ a + b ]) > 0
                """,
                "functools.reduce(lambda a, b: (a + b), rune_resolve_attr(self, \"vals\"))");
    }
}
