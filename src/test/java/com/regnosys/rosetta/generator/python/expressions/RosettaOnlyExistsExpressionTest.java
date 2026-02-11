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
public class RosettaOnlyExistsExpressionTest {

    /**
     * Test utils for generating Python.
     */
    @Inject
    private PythonGeneratorTestUtils testUtils;

    /**
     * Test case for only-exists single path.
     */
    @Test
    public void testOnlyExistsSinglePath() {
        testUtils.assertBundleContainsExpectedString("""
                type A:
                    field1 number (0..1)

                type Test:
                    aValue A (1..1)

                    condition TestCond:
                        if aValue -> field1 exists
                            then aValue -> field1 only exists
                """,
                "return rune_check_one_of(self, rune_resolve_attr(rune_resolve_attr(self, \"aValue\"), \"field1\"))");
    }

    /**
     * Test case for only-exists multiple paths.
     */
    @Test
    public void testOnlyExistsMultiplePaths() {
        testUtils.assertBundleContainsExpectedString("""
                type Bar:
                    before number (0..1)
                    after number (0..1)

                func OnlyExistsMultiplePaths:
                    inputs: bar Bar (1..1)
                    output: result boolean (1..1)
                    set result:
                        ( bar -> before, bar -> after ) only exists
                """,
                "result = rune_check_one_of(self, rune_resolve_attr(rune_resolve_attr(self, \"bar\"), \"before\"), rune_resolve_attr(rune_resolve_attr(self, \"bar\"), \"after\"))");
    }

    /**
     * Test case for only-exists with metadata.
     */
    @Test
    public void testOnlyExistsWithMetadata() {
        testUtils.assertBundleContainsExpectedString("""
                type Bar:
                    before number (0..1)
                        [metadata scheme]

                func OnlyExistsWithMetadata:
                    inputs: bar Bar (1..1)
                    output: result boolean (1..1)
                    set result:
                        bar -> before only exists
                """,
                "result = rune_check_one_of(self, rune_resolve_attr(rune_resolve_attr(self, \"bar\"), \"before\"))");
    }

    /**
     * Test case for only-exists three paths.
     */
    @Test
    public void testOnlyExistsThreePaths() {
        testUtils.assertBundleContainsExpectedString("""
                type Bar:
                    a number (0..1)
                    b number (0..1)
                    c number (0..1)

                func OnlyExistsThree:
                    inputs: bar Bar (1..1)
                    output: result boolean (1..1)
                    set result:
                        ( bar -> a, bar -> b, bar -> c ) only exists
                """,
                "result = rune_check_one_of(self, rune_resolve_attr(rune_resolve_attr(self, \"bar\"), \"a\"), rune_resolve_attr(rune_resolve_attr(self, \"bar\"), \"b\"), rune_resolve_attr(rune_resolve_attr(self, \"bar\"), \"c\"))");
    }
}
