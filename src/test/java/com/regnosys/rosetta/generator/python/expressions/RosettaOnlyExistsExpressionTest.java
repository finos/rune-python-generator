package com.regnosys.rosetta.generator.python.expressions;

import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.regnosys.rosetta.generator.python.PythonGeneratorTestUtils;
import com.regnosys.rosetta.tests.RosettaInjectorProvider;

import jakarta.inject.Inject;

@Disabled("Functions are being phased out in tests.")
@ExtendWith(InjectionExtension.class)
@InjectWith(RosettaInjectorProvider.class)
public class RosettaOnlyExistsExpressionTest {

    @Inject
    private PythonGeneratorTestUtils testUtils;

    @Test
    @Disabled("Functions are being phased out in tests.")
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
