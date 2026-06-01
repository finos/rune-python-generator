/*
 * Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
 * SPDX-License-Identifier: Apache-2.0
 */
package com.regnosys.rosetta.generator.python.expressions;

import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.regnosys.rosetta.generator.python.PythonGeneratorTestUtils;
import com.regnosys.rosetta.tests.RosettaInjectorProvider;

import jakarta.inject.Inject;

/**
 * Tests that WithMetaOperation generates correct Python for basic types,
 * enum types, and complex (derived) types.
 *
 * Expected emission rules:
 *   - Basic type:   StrWithMeta(arg, scheme=val) / NumberWithMeta(arg, id=val) etc.
 *   - Enum type:    EnumType.deserialize({'@data': arg, '@scheme': val}, allowed_meta=set())
 *   - Complex type: (lambda _wm: (_wm.set_meta(check_allowed=False, location=val), _wm)[-1])(arg)
 */
@ExtendWith(InjectionExtension.class)
@InjectWith(RosettaInjectorProvider.class)
@SuppressWarnings("LineLength")
public class PythonWithMetaExpressionTest {

    /** Injected test utilities. */
    @Inject
    private PythonGeneratorTestUtils testUtils;

    // -------------------------------------------------------------------------
    // Basic type: string with scheme
    // -------------------------------------------------------------------------

    @Test
    public void testWithMetaBasicTypeScheme() {
        testUtils.assertBundleContainsExpectedString("""
                func TestWithMetaString:
                    inputs:
                        val string (0..1)
                        schemeVal string (0..1)
                    output:
                        result string (0..1)
                            [metadata scheme]
                    set result:
                        val with-meta { scheme: schemeVal }
                """,
                "StrWithMeta(rune_resolve_attr(self, \"val\"), scheme=rune_resolve_attr(self, \"schemeVal\"))");
    }

    // -------------------------------------------------------------------------
    // Basic type: string with id
    // -------------------------------------------------------------------------

    @Test
    public void testWithMetaBasicTypeId() {
        testUtils.assertBundleContainsExpectedString("""
                func TestWithMetaStringId:
                    inputs:
                        val string (0..1)
                        idVal string (0..1)
                    output:
                        result string (0..1)
                            [metadata id]
                    set result:
                        val with-meta { id: idVal }
                """,
                "StrWithMeta(rune_resolve_attr(self, \"val\"), id=rune_resolve_attr(self, \"idVal\"))");
    }

    // -------------------------------------------------------------------------
    // Basic type: number with scheme
    // -------------------------------------------------------------------------

    @Test
    public void testWithMetaBasicTypeNumber() {
        testUtils.assertBundleContainsExpectedString("""
                func TestWithMetaNumber:
                    inputs:
                        val number (0..1)
                        schemeVal string (0..1)
                    output:
                        result number (0..1)
                            [metadata scheme]
                    set result:
                        val with-meta { scheme: schemeVal }
                """,
                "NumberWithMeta(rune_resolve_attr(self, \"val\"), scheme=rune_resolve_attr(self, \"schemeVal\"))");
    }

    // -------------------------------------------------------------------------
    // Basic type: multiple metadata entries
    // -------------------------------------------------------------------------

    @Test
    public void testWithMetaBasicTypeMultipleEntries() {
        testUtils.assertBundleContainsExpectedString("""
                func TestWithMetaMulti:
                    inputs:
                        val string (0..1)
                        idVal string (0..1)
                        schemeVal string (0..1)
                    output:
                        result string (0..1)
                            [metadata id]
                            [metadata scheme]
                    set result:
                        val with-meta { id: idVal, scheme: schemeVal }
                """,
                "StrWithMeta(rune_resolve_attr(self, \"val\"), id=rune_resolve_attr(self, \"idVal\"), scheme=rune_resolve_attr(self, \"schemeVal\"))");
    }

    // -------------------------------------------------------------------------
    // Enum type: enum with scheme
    // -------------------------------------------------------------------------

    @Test
    public void testWithMetaEnumScheme() {
        testUtils.assertBundleContainsExpectedString("""
                enum MyEnum:
                    VALUE_A
                    VALUE_B

                func TestWithMetaEnum:
                    inputs:
                        val MyEnum (0..1)
                        schemeVal string (0..1)
                    output:
                        result MyEnum (0..1)
                            [metadata scheme]
                    set result:
                        val with-meta { scheme: schemeVal }
                """,
                "com.rosetta.test.model.MyEnum.MyEnum.deserialize({'@data': rune_resolve_attr(self, \"val\"), '@scheme': rune_resolve_attr(self, \"schemeVal\")}, allowed_meta={'@scheme'})");
    }

    // -------------------------------------------------------------------------
    // Complex type: with location (scoped key)
    // -------------------------------------------------------------------------

    @Test
    public void testWithMetaComplexTypeLocation() {
        testUtils.assertBundleContainsExpectedString("""
                type MyData:
                    value string (1..1)

                func TestWithMetaLocation:
                    inputs:
                        val MyData (0..1)
                        keyVal string (0..1)
                    output:
                        result MyData (0..1)
                            [metadata location]
                    set result:
                        val with-meta { location: keyVal }
                """,
                "(lambda _wm: (_wm.set_meta(check_allowed=False, location=rune_resolve_attr(self, \"keyVal\")), _wm)[-1])(rune_resolve_attr(self, \"val\"))");
    }

    // -------------------------------------------------------------------------
    // Complex type: with address (scoped reference)
    // -------------------------------------------------------------------------

    @Test
    public void testWithMetaComplexTypeAddress() {
        testUtils.assertBundleContainsExpectedString("""
                type MyData:
                    value string (1..1)

                func TestWithMetaAddress:
                    inputs:
                        val MyData (0..1)
                        addrVal string (0..1)
                    output:
                        result MyData (0..1)
                            [metadata address]
                    set result:
                        val with-meta { address: addrVal }
                """,
                "(lambda _wm: (_wm.set_meta(check_allowed=False, address=rune_resolve_attr(self, \"addrVal\")), _wm)[-1])(rune_resolve_attr(self, \"val\"))");
    }

    // -------------------------------------------------------------------------
    // Filter on standalone metadata reference existence: filter reference exists
    // -------------------------------------------------------------------------

    @Test
    public void testFilterOnMetadataReferenceExists() {
        testUtils.assertBundleContainsExpectedString("""
                metaType reference string

                type Party:
                    [metadata key]
                    name string (0..1)

                type Leg:
                    partyRef Party (0..1)
                        [metadata reference]

                func GetPartyRef:
                    inputs:
                        leg Leg (0..1)
                    output:
                        result Party (0..1)
                            [metadata reference]
                    set result:
                        leg -> partyRef

                func FilterByReference:
                    inputs:
                        legs Leg (0..*)
                    output:
                        result Party (0..1)
                            [metadata reference]
                    set result:
                        legs
                            extract GetPartyRef( item )
                            then filter reference exists
                            then first
                """,
                "rune_filter(item, lambda item: rune_attr_exists(item.get_meta(\"reference\")))");
    }

    // -------------------------------------------------------------------------
    // Feature call on metadata reference: field -> reference exists
    // -------------------------------------------------------------------------

    @Test
    public void testFeatureCallOnMetadataReference() {
        testUtils.assertBundleContainsExpectedString("""
                metaType reference string

                type Party:
                    [metadata key]
                    name string (0..1)

                type Counterparty:
                    partyReference Party (1..1)
                        [metadata reference]

                func FilterByPartyReference:
                    inputs:
                        counterparties Counterparty (0..*)
                    output:
                        result Counterparty (0..1)
                    set result:
                        counterparties
                            filter partyReference -> reference exists
                            then only-element
                """,
                "rune_filter(rune_resolve_attr(self, \"counterparties\"), lambda item: rune_attr_exists((lambda _r: _r.get_meta(\"reference\") if _r is not None else None)(rune_resolve_attr(item, \"partyReference\"))))");
    }
}
