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
 * A field whose type has {@code [metadata key]} gets the runtime's serializer and a validator
 * that allows the type's keys, so that {@code @key} and {@code @key:external} are written. The
 * runtime writes an object's keys only through the field's serializer.
 */
@ExtendWith(InjectionExtension.class)
@InjectWith(RosettaInjectorProvider.class)
public class PythonTypeLevelKeyTest {

    private static final String HOOKS = "Party.serializer(), Party.validator(('@key', '@key:external'))";

    // SingleParty and PartyList are standalone. BundledSingle and Loop refer to each other, so
    // they are bundled.
    private static final String MODEL = """
            type Party:
                [metadata key]
                name string (0..1)

            type SingleParty:
                party Party (0..1)

            type PartyList:
                parties Party (0..*)

            type BundledSingle:
                party Party (0..1)
                loop Loop (0..1)

            type Loop:
                back BundledSingle (0..1)
            """;

    @Inject
    private PythonGeneratorTestUtils testUtils;

    private String generated(String file) {
        Map<String, CharSequence> python = testUtils.generatePythonFromString(MODEL);
        return python.get("src/com/" + file).toString();
    }

    @Test
    public void testStandaloneFieldOfKeyedTypeGetsHooks() {
        testUtils.assertGeneratedContainsExpectedString(generated("rosetta/test/model/SingleParty.py"),
                "party: Annotated[Optional[Party], " + HOOKS + "] = Field(None, description='')");
    }

    @Test
    public void testStandaloneListOfKeyedTypeGetsHooks() {
        testUtils.assertGeneratedContainsExpectedString(generated("rosetta/test/model/PartyList.py"),
                "parties: Annotated[Optional[list[Party | None]], " + HOOKS + "] = Field(None, description='')");
    }

    @Test
    public void testBundledFieldOfKeyedTypeAllowsTheTypesKeys() {
        String bundle = generated("_bundle.py");
        testUtils.assertGeneratedContainsExpectedString(bundle,
                "com_rosetta_test_model_BundledSingle.__annotations__[\"party\"] = Annotated[Optional[Party], "
                        + HOOKS + "]");
        testUtils.assertGeneratedContainsExpectedString(bundle,
                "com_rosetta_test_model_BundledSingle.model_fields[\"party\"].metadata += [" + HOOKS + "]");
    }
}
