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
 * An attribute whose Python name is a name its own annotation uses ({@code int}, {@code str},
 * {@code bool}, {@code datetime}, {@code Decimal}) must not shadow that name (#331). Generated
 * modules use {@code from __future__ import annotations}, so pydantic resolves annotations with
 * the class's own names in scope, where the attribute's name is its {@code FieldInfo} default.
 * The annotation is qualified through an alias that can't be an attribute name instead.
 */
@ExtendWith(InjectionExtension.class)
@InjectWith(RosettaInjectorProvider.class)
public class PythonBuiltinNameClashTest {

    /**
     * The Rune model the tests generate Python from.
     * BundledChoice and Node refer to each other, so both are bundled. The other types are standalone.
     */
    private static final String MODEL = """
            choice ChoiceBasic:
                string
                int

            type Clash:
                str string (0..1)
                bool boolean (0..1)

            type DateClash:
                datetime date (0..1)

            type TimeClash:
                datetime time (0..1)

            type DecimalClash:
                Decimal number (0..1)

            type NoClash:
                quantity int (0..1)
                name string (0..1)
                tradeDate date (0..1)
                amount number (0..1)

            type WithMeta:
                str string (0..1)
                    [metadata scheme]

            choice BundledChoice:
                int
                Node

            type Node:
                next BundledChoice (0..1)
                str string (0..1)
            """;

    /** Injected test utilities. */
    @Inject
    private PythonGeneratorTestUtils testUtils;

    private String generated(String file) {
        Map<String, CharSequence> python = testUtils.generatePythonFromString(MODEL);
        return python.get("src/com/" + file).toString();
    }

    @Test
    public void testIntOptionOfChoiceIsQualified() {
        testUtils.assertGeneratedContainsExpectedString(generated("rosetta/test/model/ChoiceBasic.py"),
                "    int: Optional[_builtins.int] = Field(None, description='')");
    }

    @Test
    public void testStrAndBoolAttributesAreQualified() {
        String clash = generated("rosetta/test/model/Clash.py");
        testUtils.assertGeneratedContainsExpectedString(clash,
                "    str: Optional[_builtins.str] = Field(None, description='')");
        testUtils.assertGeneratedContainsExpectedString(clash,
                "    bool: Optional[_builtins.bool] = Field(None, description='')");
    }

    @Test
    public void testDatetimeAttributeIsQualified() {
        testUtils.assertGeneratedContainsExpectedString(generated("rosetta/test/model/DateClash.py"),
                "    datetime: Optional[_datetime.date] = Field(None, description='')");
        testUtils.assertGeneratedContainsExpectedString(generated("rosetta/test/model/TimeClash.py"),
                "    datetime: Optional[_datetime.time] = Field(None, description='')");
    }

    @Test
    public void testDecimalAttributeIsQualified() {
        testUtils.assertGeneratedContainsExpectedString(generated("rosetta/test/model/DecimalClash.py"),
                "    Decimal: Optional[_decimal.Decimal] = Field(None, description='')");
    }

    @Test
    public void testBundledClassesAreQualified() {
        String bundle = generated("_bundle.py");
        testUtils.assertGeneratedContainsExpectedString(bundle,
                "    int: Optional[_builtins.int] = Field(None, description='')");
        testUtils.assertGeneratedContainsExpectedString(bundle,
                "    str: Optional[_builtins.str] = Field(None, description='')");
    }

    @Test
    public void testAliasesAreImported() {
        for (String file : new String[] {"rosetta/test/model/ChoiceBasic.py", "_bundle.py"}) {
            String python = generated(file);
            testUtils.assertGeneratedContainsExpectedString(python, "import builtins as _builtins\n");
            testUtils.assertGeneratedContainsExpectedString(python, "import datetime as _datetime\n");
            testUtils.assertGeneratedContainsExpectedString(python, "import decimal as _decimal\n");
        }
    }

    @Test
    public void testAttributesWithoutAClashAreUnchanged() {
        String noClash = generated("rosetta/test/model/NoClash.py");
        testUtils.assertGeneratedContainsExpectedString(noClash,
                "    quantity: Optional[int] = Field(None, description='')");
        testUtils.assertGeneratedContainsExpectedString(noClash,
                "    name: Optional[str] = Field(None, description='')");
        testUtils.assertGeneratedContainsExpectedString(noClash,
                "    tradeDate: Optional[datetime.date] = Field(None, description='')");
        testUtils.assertGeneratedContainsExpectedString(noClash,
                "    amount: Optional[Decimal] = Field(None, description='')");
        testUtils.assertGeneratedContainsExpectedString(generated("rosetta/test/model/ChoiceBasic.py"),
                "    string: Optional[str] = Field(None, description='')");
    }

    @Test
    public void testAttributeWithMetadataIsUnchanged() {
        // The type is StrWithMeta, so the attribute's name doesn't clash with it
        testUtils.assertGeneratedContainsExpectedString(generated("rosetta/test/model/WithMeta.py"),
                "    str: Annotated[Optional[StrWithMeta], StrWithMeta.serializer(), "
                        + "StrWithMeta.validator(('@scheme', ))] = Field(None, description='')");
    }
}
