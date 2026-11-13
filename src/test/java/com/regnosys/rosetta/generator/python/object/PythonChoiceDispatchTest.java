/*
 * Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
 * SPDX-License-Identifier: Apache-2.0
 */
package com.regnosys.rosetta.generator.python.object;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.regnosys.rosetta.generator.python.PythonGeneratorTestUtils;
import com.regnosys.rosetta.tests.RosettaInjectorProvider;

import jakarta.inject.Inject;

/**
 * Every choice class gets a {@code _CHOICE_DISPATCH} table mapping each {@code @type} it can be
 * read with to the attribute name of the option that holds it (#333). A data type is keyed by its
 * Rune FQN, a basic type, enum or type alias of either by the option name, and a nested choice
 * contributes its leaves. An intermediate choice also contributes its own FQN when the option
 * has metadata, because Java writes that choice's metadata/reference wrapper with its own type.
 */
@ExtendWith(InjectionExtension.class)
@InjectWith(RosettaInjectorProvider.class)
@SuppressWarnings("checkstyle:LineLength")
public class PythonChoiceDispatchTest {

    private static final String MARKER = "    _RUNE_CHOICE: ClassVar[bool] = True\n";

    private static final String NESTED_MODEL = """
            type A:
                value string (1..1)
            type B:
                value string (1..1)
            type C:
                value string (1..1)
            type D:
                value string (1..1)
            type E1:
                value string (1..1)
            type F:
                value string (1..1)
            type G:
                value string (1..1)
            type H:
                value string (1..1)

            choice ChoiceAB:
                A
                B
            choice ChoiceCD:
                C
                D
            choice ChoiceEF:
                E1
                F
            choice ChoiceGH:
                G
                H

            choice ChoiceABCD:
                ChoiceAB
                ChoiceCD
            choice ChoiceEFGH:
                ChoiceEF
                ChoiceGH

            choice OuterChoice:
                ChoiceABCD
                ChoiceEFGH

            type Root:
                payload OuterChoice (0..1)
            """;

    @Inject
    private PythonGeneratorTestUtils testUtils;

    private String generated(String model, String file) {
        Map<String, CharSequence> python = testUtils.generatePythonFromString(model);
        return python.get("src/com/" + file).toString();
    }

    @Test
    public void testChoiceWithDirectOptions() {
        testUtils.assertGeneratedContainsExpectedString(
                generated(NESTED_MODEL, "rosetta/test/model/ChoiceAB.py"),
                MARKER + """
                    _CHOICE_DISPATCH: ClassVar[dict[str, str]] = {
                        "com.rosetta.test.model.A": "A",
                        "com.rosetta.test.model.B": "B",
                    }
                """);
    }

    @Test
    public void testChoiceOfChoices() {
        testUtils.assertGeneratedContainsExpectedString(
                generated(NESTED_MODEL, "rosetta/test/model/ChoiceABCD.py"),
                MARKER + """
                    _CHOICE_DISPATCH: ClassVar[dict[str, str]] = {
                        "com.rosetta.test.model.A": "ChoiceAB",
                        "com.rosetta.test.model.B": "ChoiceAB",
                        "com.rosetta.test.model.C": "ChoiceCD",
                        "com.rosetta.test.model.D": "ChoiceCD",
                    }
                """);
    }

    @Test
    public void testThreeLevelChoice() {
        testUtils.assertGeneratedContainsExpectedString(
                generated(NESTED_MODEL, "rosetta/test/model/OuterChoice.py"),
                MARKER + """
                    _CHOICE_DISPATCH: ClassVar[dict[str, str]] = {
                        "com.rosetta.test.model.A": "ChoiceABCD",
                        "com.rosetta.test.model.B": "ChoiceABCD",
                        "com.rosetta.test.model.C": "ChoiceABCD",
                        "com.rosetta.test.model.D": "ChoiceABCD",
                        "com.rosetta.test.model.E1": "ChoiceEFGH",
                        "com.rosetta.test.model.F": "ChoiceEFGH",
                        "com.rosetta.test.model.G": "ChoiceEFGH",
                        "com.rosetta.test.model.H": "ChoiceEFGH",
                    }
                """);
    }

    @Test
    public void testBasicTypeAndEnumOptionsAreKeyedByOptionName() {
        String model = """
                enum CurrencyEnum:
                    USD
                    EUR

                type A:
                    value string (1..1)

                choice Pick:
                    A
                    CurrencyEnum
                    string
                """;
        testUtils.assertGeneratedContainsExpectedString(
                generated(model, "rosetta/test/model/Pick.py"),
                MARKER + """
                    _CHOICE_DISPATCH: ClassVar[dict[str, str]] = {
                        "CurrencyEnum": "CurrencyEnum",
                        "com.rosetta.test.model.A": "A",
                        "string": "string",
                    }
                """);
    }

    @Test
    public void testPlainNestedChoicesDoNotExposeIntermediateDiscriminators() {
        String outer = generated(NESTED_MODEL, "rosetta/test/model/OuterChoice.py");
        for (String intermediate : new String[] {
                "ChoiceAB", "ChoiceCD", "ChoiceEF", "ChoiceGH", "ChoiceABCD", "ChoiceEFGH"
        }) {
            testUtils.assertGeneratedDoesNotContain(outer,
                    "\"com.rosetta.test.model." + intermediate + "\":");
        }
    }

    @Test
    public void testLocatedChoiceOptionKeepsItsOwnDiscriminator() {
        String model = """
                type A:
                    value string (1..1)
                type B:
                    value string (1..1)
                choice Inner:
                    A
                    B
                choice Located:
                    Inner
                        [metadata location]
                    int
                """;
        testUtils.assertGeneratedContainsExpectedString(
                generated(model, "rosetta/test/model/Located.py"),
                MARKER + """
                    _CHOICE_DISPATCH: ClassVar[dict[str, str]] = {
                        "com.rosetta.test.model.A": "Inner",
                        "com.rosetta.test.model.B": "Inner",
                        "com.rosetta.test.model.Inner": "Inner",
                        "int": "int",
                    }
                """);
    }

    @Test
    public void testWrappedDescendantDiscriminatorSurvivesPlainAncestor() {
        String model = """
                type A:
                    value string (1..1)
                type B:
                    value string (1..1)
                type C:
                    value string (1..1)
                choice Inner:
                    A
                    B
                choice Located:
                    Inner
                        [metadata location]
                    string
                choice Ancestor:
                    Located
                    C
                """;
        String ancestor = generated(model, "rosetta/test/model/Ancestor.py");
        testUtils.assertGeneratedContainsExpectedString(ancestor,
                MARKER + """
                    _CHOICE_DISPATCH: ClassVar[dict[str, str]] = {
                        "com.rosetta.test.model.A": "Located",
                        "com.rosetta.test.model.B": "Located",
                        "com.rosetta.test.model.C": "C",
                        "com.rosetta.test.model.Inner": "Located",
                        "string": "Located",
                    }
                """);
        testUtils.assertGeneratedDoesNotContain(ancestor, "\"com.rosetta.test.model.Located\":");
    }

    @Test
    public void testReferenceChoiceOptionKeepsItsOwnDiscriminator() {
        String model = """
                type A:
                    value string (1..1)
                type B:
                    value string (1..1)
                choice Inner:
                    A
                    B
                choice Referenced:
                    Inner
                        [metadata address "pointsTo"=Root->located]
                    string
                type Root:
                    located Inner (0..1)
                        [metadata location]
                    payload Referenced (0..1)
                """;
        testUtils.assertGeneratedContainsExpectedString(
                generated(model, "rosetta/test/model/Referenced.py"),
                MARKER + """
                    _CHOICE_DISPATCH: ClassVar[dict[str, str]] = {
                        "com.rosetta.test.model.A": "Inner",
                        "com.rosetta.test.model.B": "Inner",
                        "com.rosetta.test.model.Inner": "Inner",
                        "string": "string",
                    }
                """);
    }

    @Test
    public void testAliasOfPlainChoiceOnlyContributesLeafDiscriminators() {
        String model = """
                type A:
                    value string (1..1)
                type B:
                    value string (1..1)
                type C:
                    value string (1..1)
                choice Inner:
                    A
                    B
                typeAlias AliasOfInner: Inner
                choice Pick:
                    AliasOfInner
                    C
                """;
        testUtils.assertGeneratedContainsExpectedString(
                generated(model, "rosetta/test/model/Pick.py"),
                MARKER + """
                    _CHOICE_DISPATCH: ClassVar[dict[str, str]] = {
                        "com.rosetta.test.model.A": "AliasOfInner",
                        "com.rosetta.test.model.B": "AliasOfInner",
                        "com.rosetta.test.model.C": "C",
                    }
                """);
    }

    @Test
    public void testTypeAliasOptions() {
        String model = """
                typeAlias Text: string

                type A:
                    value string (1..1)

                typeAlias AliasOfA: A

                choice WithAliases:
                    Text
                    AliasOfA
                """;
        testUtils.assertGeneratedContainsExpectedString(
                generated(model, "rosetta/test/model/WithAliases.py"),
                MARKER + """
                    _CHOICE_DISPATCH: ClassVar[dict[str, str]] = {
                        "Text": "Text",
                        "com.rosetta.test.model.A": "AliasOfA",
                    }
                """);
    }

    @Test
    public void testBundledChoice() {
        // LoopOption and LoopHolder refer to each other through LoopChoice, so all three are bundled.
        String model = """
                type B:
                    value string (1..1)

                type LoopOption:
                    back LoopHolder (0..1)

                choice LoopChoice:
                    LoopOption
                    B

                type LoopHolder:
                    option LoopChoice (0..1)
                """;
        testUtils.assertGeneratedContainsExpectedString(
                generated(model, "_bundle.py"),
                "class com_rosetta_test_model_LoopChoice(BaseDataClass):\n"
                        + "    model_config = ConfigDict(defer_build=True)\n"
                        + MARKER + """
                    _CHOICE_DISPATCH: ClassVar[dict[str, str]] = {
                        "com.rosetta.test.model.B": "B",
                        "com.rosetta.test.model.LoopOption": "LoopOption",
                    }
                """);
    }

    @Test
    public void testNamespacePrefixIsNotPartOfKeys() {
        Map<String, CharSequence> python = testUtils.generatePythonFromString(NESTED_MODEL, "finos", null);
        String all = python.values().stream().map(CharSequence::toString).collect(Collectors.joining("\n"));
        testUtils.assertGeneratedContainsExpectedString(all, """
                    _CHOICE_DISPATCH: ClassVar[dict[str, str]] = {
                        "com.rosetta.test.model.A": "A",
                        "com.rosetta.test.model.B": "B",
                    }
                """);
        testUtils.assertGeneratedDoesNotContain(all, "\"finos.com.rosetta.test.model.");
    }

    @Test
    public void testTypesThatAreNotChoicesHaveNoTable() {
        String model = """
                type A:
                    value string (1..1)

                type B:
                    value string (1..1)

                type ViaOneOf:
                    A A (0..1)
                    B B (0..1)

                    condition Choice:
                        one-of
                """;
        testUtils.assertGeneratedDoesNotContain(generated(model, "rosetta/test/model/ViaOneOf.py"), "_CHOICE_DISPATCH");
        testUtils.assertGeneratedDoesNotContain(generated(model, "rosetta/test/model/A.py"), "_CHOICE_DISPATCH");
    }

    @Test
    public void testSameLeafThroughTwoOptionsFailsGeneration() {
        String model = """
                type A:
                    value string (1..1)
                type B:
                    value string (1..1)
                type C:
                    value string (1..1)

                choice ChoiceAB:
                    A
                    B
                choice ChoiceAC:
                    A
                    C

                choice Ambiguous:
                    ChoiceAB
                    ChoiceAC
                """;
        RuntimeException e = assertThrows(RuntimeException.class, () -> testUtils.generatePythonFromString(model));
        String messages = causeMessages(e);
        assertTrue(messages.contains("com.rosetta.test.model.Ambiguous")
                && messages.contains("'com.rosetta.test.model.A'")
                && messages.contains("'ChoiceAB'")
                && messages.contains("'ChoiceAC'"),
                "Expected the choice, the key and both options in the error, but was: " + messages);
    }

    private static String causeMessages(Throwable t) {
        StringBuilder messages = new StringBuilder();
        for (Throwable c = t; c != null; c = c.getCause()) {
            messages.append(c.getMessage()).append('\n');
        }
        return messages.toString();
    }
}
