/*
 * Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
 * SPDX-License-Identifier: Apache-2.0
 */
package com.regnosys.rosetta.generator.python.expressions;

import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.regnosys.rosetta.generator.python.PythonGeneratorTestUtils;
import com.regnosys.rosetta.tests.RosettaInjectorProvider;

import jakarta.inject.Inject;

/**
 * Tests documenting how type aliases are handled in generated Python.
 *
 * <p>In the Java generator, a {@code typeAlias} with a {@code condition} block
 * generates a full validator that is invoked whenever the alias type is used as
 * an attribute.  The Python generator instead strips type aliases to their
 * underlying primitive (e.g. {@code Foo: string} → Python {@code str}) and does
 * <em>not</em> generate any condition enforcement for the alias.
 *
 * <p>This class documents:
 * <ol>
 *   <li>Current behaviour: the alias maps to the correct Python primitive in
 *       generated attribute declarations.</li>
 *   <li>Known gap: conditions defined on the alias are not generated in Python
 *       ({@link #testTypeAliasConditionNotGenerated}).</li>
 * </ol>
 *
 * <p>Mirrors {@code TypeAliasConditionTest} in the rune-dsl Java generator tests.
 */
@ExtendWith(InjectionExtension.class)
@InjectWith(RosettaInjectorProvider.class)
@SuppressWarnings("LineLength")
public class PythonTypeAliasConditionTest {

    @Inject
    private PythonGeneratorTestUtils testUtils;

    /**
     * A {@code typeAlias} over {@code string} generates {@code str} for any
     * attribute that uses it — the alias name is not preserved in Python.
     */
    @Test
    public void testStringTypeAliasReducesToStr() {
        testUtils.assertBundleContainsExpectedString("""
                typeAlias Foo:
                    string

                type Bar:
                    foo Foo (1..1)
                """,
                "foo: str");
    }

    /**
     * A {@code typeAlias} over {@code int} generates {@code int} for attributes.
     */
    @Test
    public void testIntTypeAliasReducesToInt() {
        testUtils.assertBundleContainsExpectedString("""
                typeAlias PosInt:
                    int

                type Wrapper:
                    value PosInt (1..1)
                """,
                "value: int");
    }

    /**
     * A chain of type aliases collapses to the base primitive.
     * {@code Bar: Foo}, {@code Foo: string} → Python {@code str}.
     */
    @Test
    public void testChainedTypeAliasReducesToBasePrimitive() {
        testUtils.assertBundleContainsExpectedString("""
                typeAlias Foo:
                    string

                typeAlias Bar:
                    Foo

                type Holder:
                    val Bar (1..1)
                """,
                "val: str");
    }

    /**
     * A function whose parameter type is an alias generates the underlying
     * Python primitive in the function signature.
     */
    @Test
    public void testTypeAliasParameterReducesToPrimitive() {
        testUtils.assertBundleContainsExpectedString("""
                typeAlias MyStr:
                    string

                func UseAlias:
                    inputs: val MyStr (1..1)
                    output: result string (1..1)
                    set result: val
                """,
                "def UseAlias(val: str) -> str:");
    }

    /**
     * Known gap: conditions defined on a {@code typeAlias} are not generated
     * in Python.  In the Java generator these produce a named validator
     * (e.g. {@code FooC}) that fires on every attribute of that alias type.
     * The Python generator strips the alias to its primitive and emits no
     * condition logic.
     *
     * <p>This test is disabled to document the gap without failing the build.
     * When/if Python condition generation for type aliases is implemented,
     * remove {@code @Disabled} and adjust the assertion.
     */
    @Disabled("Known gap: type alias conditions are not generated in Python — "
            + "aliases are stripped to primitives; no condition validator is emitted")
    @Test
    public void testTypeAliasConditionNotGenerated() {
        testUtils.assertBundleContainsExpectedString("""
                typeAlias Foo:
                    string

                    condition C:
                        item <> "forbidden"
                """,
                // In Java this condition becomes a validator named "FooC".
                // In Python nothing is generated for it — this assertion fails.
                "condition_0_C");
    }
}
