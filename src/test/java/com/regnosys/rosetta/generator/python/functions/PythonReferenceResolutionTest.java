/*
 * Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
 * SPDX-License-Identifier: Apache-2.0
 */
package com.regnosys.rosetta.generator.python.functions;

import java.util.Map;

import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.regnosys.rosetta.generator.python.PythonGeneratorTestUtils;
import com.regnosys.rosetta.tests.RosettaInjectorProvider;

import jakarta.inject.Inject;

/**
 * Generated code must import every function and enum it refers to, and refer to an enum value
 * through the enum written in the model.
 */
@ExtendWith(InjectionExtension.class)
@InjectWith(RosettaInjectorProvider.class)
@SuppressWarnings("LineLength")
public class PythonReferenceResolutionTest {

    /** The Rune model the tests generate Python from. */
    private static final String MODEL = """
            enum Kind:
                Plain
                Fancy

            enum Mode:
                Start
                End

            type Settings:
                mode Mode (0..1)

            enum Base:
                Low
                High

            enum Extended extends Base:
                Middle

            type Qualified:
                level Extended (0..1)

                condition KnownLevel:
                    if level exists
                    then level = Extended -> Middle or level = Extended -> High

            type Unqualified:
                level Extended (0..1)

                condition KnownLevel:
                    if level exists
                    then level = Middle or level = High

            type Switched:
                level Extended (0..1)

                condition KnownLevel:
                    level switch
                        High then True,
                        Middle then True,
                        default False

            func Helper:
                inputs:
                    value string (0..1)
                output:
                    result string (0..1)
                set result:
                    value

            func MakeKey:
                inputs:
                    value string (0..1)
                output:
                    result string (0..1)
                set result:
                    value

            func CallInSwitch:
                inputs:
                    value string (0..1)
                output:
                    result string (0..1)
                set result:
                    value switch
                        "a" then Helper(value),
                        default empty

            func CallInWithMeta:
                inputs:
                    value string (0..1)
                output:
                    result string (0..1)
                        [metadata scheme]
                set result:
                    value with-meta { scheme: MakeKey(value) }

            func Dispatch:
                [codeImplementation]
                inputs:
                    kind Kind (1..1)
                    settings Settings (0..1)
                output:
                    result boolean (0..1)

            func Dispatch(kind: Kind -> Fancy):
                alias isStart: settings -> mode = Mode -> Start
                set result: isStart

            func Dispatch(kind: Kind -> Plain):
                set result: False
            """;

    /** Injected test utilities. */
    @Inject
    private PythonGeneratorTestUtils testUtils;

    private String generated(String path) {
        Map<String, CharSequence> python = testUtils.generatePythonFromString(MODEL);
        return python.get("src/com/rosetta/test/model/" + path).toString();
    }

    @Test
    public void testFunctionCalledInSwitchCaseIsImported() {
        testUtils.assertGeneratedContainsExpectedString(generated("functions/CallInSwitch.py"),
                "from com.rosetta.test.model.functions.Helper import Helper");
    }

    @Test
    public void testFunctionCalledInWithMetaEntryIsImported() {
        testUtils.assertGeneratedContainsExpectedString(generated("functions/CallInWithMeta.py"),
                "from com.rosetta.test.model.functions.MakeKey import MakeKey");
    }

    @Test
    public void testEnumUsedOnlyInFirstDispatchImplementationIsImported() {
        testUtils.assertGeneratedContainsExpectedString(generated("functions/Dispatch.py"),
                "import com.rosetta.test.model.Mode\n");
    }

    @Test
    public void testInheritedEnumValueUsesEnumWrittenInModel() {
        String python = generated("Qualified.py");
        testUtils.assertGeneratedContainsExpectedString(python, "com.rosetta.test.model.Extended.Extended.HIGH");
        testUtils.assertGeneratedDoesNotContain(python, "com.rosetta.test.model.Base.Base.HIGH");
    }

    @Test
    public void testUnqualifiedInheritedEnumValueUsesExpressionEnum() {
        String python = generated("Unqualified.py");
        testUtils.assertGeneratedContainsExpectedString(python, "com.rosetta.test.model.Extended.Extended.HIGH");
        testUtils.assertGeneratedDoesNotContain(python, "com.rosetta.test.model.Base.Base.HIGH");
    }

    @Test
    public void testSwitchGuardComparesWithEnumMember() {
        String python = generated("Switched.py");
        testUtils.assertGeneratedContainsExpectedString(python,
                "if switchAttribute == com.rosetta.test.model.Extended.Extended.HIGH:");
        testUtils.assertGeneratedContainsExpectedString(python,
                "elif switchAttribute == com.rosetta.test.model.Extended.Extended.MIDDLE:");
    }
}
