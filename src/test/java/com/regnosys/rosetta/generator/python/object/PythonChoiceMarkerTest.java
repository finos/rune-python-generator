/*
 * Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
 * SPDX-License-Identifier: Apache-2.0
 */
package com.regnosys.rosetta.generator.python.object;

import java.util.Map;

import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.regnosys.rosetta.generator.python.PythonGeneratorTestUtils;
import com.regnosys.rosetta.tests.RosettaInjectorProvider;

import jakarta.inject.Inject;

/**
 * A class generated from a {@code choice} is marked as one, because the runtime reads and writes
 * it in a different JSON format. A type with optional attributes and a {@code one-of} condition
 * generates the same fields and condition, so the marker is the only thing that tells them apart.
 */
@ExtendWith(InjectionExtension.class)
@InjectWith(RosettaInjectorProvider.class)
public class PythonChoiceMarkerTest {

    private static final String MARKER = "_RUNE_CHOICE: ClassVar[bool] = True";

    // LoopOption and LoopHolder refer to each other through LoopChoice, so all three are bundled.
    private static final String MODEL = """
            type A:
                fieldA string (1..1)

            type B:
                fieldB string (1..1)

            choice ChoiceData:
                A
                B

            choice ChoiceNested:
                ChoiceData
                string

            type ViaOneOf:
                A A (0..1)
                B B (0..1)

                condition Choice:
                    one-of

            type LoopOption:
                back LoopHolder (0..1)

            choice LoopChoice:
                LoopOption
                B

            type LoopHolder:
                option LoopChoice (0..1)
            """;

    @Inject
    private PythonGeneratorTestUtils testUtils;

    private String generated(String file) {
        Map<String, CharSequence> python = testUtils.generatePythonFromString(MODEL);
        return python.get("src/com/" + file).toString();
    }

    @Test
    public void testChoiceIsMarked() {
        testUtils.assertAppearsAfter(generated("rosetta/test/model/ChoiceData.py"),
                "class ChoiceData(BaseDataClass):", "    " + MARKER);
    }

    @Test
    public void testNestedChoiceIsMarked() {
        testUtils.assertAppearsAfter(generated("rosetta/test/model/ChoiceNested.py"),
                "class ChoiceNested(BaseDataClass):", "    " + MARKER);
    }

    @Test
    public void testBundledChoiceIsMarked() {
        testUtils.assertAppearsAfter(generated("_bundle.py"),
                "class com_rosetta_test_model_LoopChoice(BaseDataClass):", "    " + MARKER);
    }

    @Test
    public void testTypeWithOneOfConditionIsNotMarked() {
        testUtils.assertGeneratedDoesNotContain(generated("rosetta/test/model/ViaOneOf.py"), "_RUNE_CHOICE");
    }

    @Test
    public void testOrdinaryTypeIsNotMarked() {
        testUtils.assertGeneratedDoesNotContain(generated("rosetta/test/model/A.py"), "_RUNE_CHOICE");
    }
}
