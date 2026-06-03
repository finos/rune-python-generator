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
 * Tests for the {@code symbol instanceof RosettaMetaType} branch of
 * {@code PythonExpressionGenerator#generateSymbolReference}.
 *
 * <p>This branch fires when a meta-type symbol (declared with {@code metaType})
 * appears as a bare symbol reference inside a filter or map lambda — with no
 * preceding field navigation.  The scope receiver supplies the implicit object:
 * <pre>
 *   metaType maps to "ref..." -&gt; scope.receiver() + ".resolve_ref_key(metaKey)"
 *   metaType maps to other   -&gt; scope.receiver() + ".get_meta(metaKey)"
 * </pre>
 *
 * <p>For {@code extract [metadata reference] field then filter reference exists},
 * the normal {@code then} path applies: the extract step produces field values, and
 * {@code filter reference exists} calls {@code item.resolve_ref_key("ref")} on each
 * extracted value (item-side check on the @ref metadata slot):
 * <pre>
 *   extract field then filter reference exists
 *     -&gt;  (lambda item: rune_filter(item, lambda item: item.resolve_ref_key("ref")))(extractedValues)
 * </pre>
 *
 * <p>Contrast with {@code generateFeatureCall}'s {@code RosettaMetaType} branch,
 * which handles {@code field -&gt; metaType} navigation and wraps the receiver in a
 * null-safe lambda.
 */
@ExtendWith(InjectionExtension.class)
@InjectWith(RosettaInjectorProvider.class)
@SuppressWarnings("LineLength")
public class PythonSymbolReferenceMetaTypeTest {

    @Inject
    private PythonGeneratorTestUtils testUtils;

    // -------------------------------------------------------------------------
    // resolve_ref_key path: extract [metadata reference] field then filter reference exists
    //
    // tryGenerateReferenceFilterChain detects this pattern and rewrites it:
    //   - the raw parent collection is kept; the extract step is suppressed
    //   - filter uses item.resolve_ref_key("fieldName") on each parent
    //   - after filtering, the field is extracted from each matched parent
    // -------------------------------------------------------------------------

    /**
     * {@code extract partyReference then filter reference exists} is rewritten so
     * the raw counterparties are filtered using {@code item.resolve_ref_key("partyReference")}
     * (the field name), then {@code partyReference} is extracted from each match.
     */
    @Test
    public void testMetaTypeReferenceEmitsResolveRefWithFieldName() {
        String generated = testUtils.generatePythonAndExtractBundle("""
                metaType reference string

                type Party:
                    [metadata key]
                    name string (0..1)

                type Counterparty:
                    partyReference Party (1..1)
                        [metadata reference]

                func FilterCounterpartiesByReference:
                    inputs:
                        counterparties Counterparty (0..*)
                    output:
                        result Party (0..1)
                    set result:
                        counterparties
                            extract partyReference
                            then filter reference exists
                            then first
                """);

        // filter uses the field name, not the meta-key "ref"
        testUtils.assertGeneratedContainsExpectedString(generated,
                "rune_filter(item, lambda item: rune_attr_exists(item.resolve_ref_key(\"partyReference\")))");
        // raw counterparties fed to the rewritten filter — no post-filter extract step
        testUtils.assertGeneratedContainsExpectedString(generated,
                "[x for x in rune_resolve_attr(self, \"counterparties\") or [] if x is not None]");
        testUtils.assertGeneratedDoesNotContain(generated, "resolve_ref_key(\"ref\")");
        testUtils.assertGeneratedDoesNotContain(generated,
                "map(lambda _p: rune_resolve_attr(_p, \"partyReference\")");
    }

    /**
     * Verifies the rewrite uses the DSL field name, not the meta-key.
     * With field "tagged", the filter uses {@code resolve_ref_key("tagged")}.
     */
    @Test
    public void testMetaTypeReferenceUsesFieldNameNotMetaKey() {
        String generated = testUtils.generatePythonAndExtractBundle("""
                metaType reference string

                type Tagged:
                    [metadata key]
                    value int (0..1)

                type Holder:
                    tagged Tagged (0..1)
                        [metadata reference]

                func FilterHoldersByReference:
                    inputs:
                        holders Holder (0..*)
                    output:
                        result Tagged (0..1)
                    set result:
                        holders
                            extract tagged
                            then filter reference exists
                            then first
                """);

        // filter uses the field name "tagged", not the meta-key "ref"
        testUtils.assertGeneratedContainsExpectedString(generated,
                "lambda item: rune_attr_exists(item.resolve_ref_key(\"tagged\"))");
        // no post-filter extract step
        testUtils.assertGeneratedDoesNotContain(generated, "resolve_ref_key(\"ref\")");
        testUtils.assertGeneratedDoesNotContain(generated,
                "map(lambda _p: rune_resolve_attr(_p, \"tagged\")");
    }

    // -------------------------------------------------------------------------
    // get_meta path: metaType maps to a name that does NOT start with "ref"
    // PythonCodeGeneratorUtil.mapMetaTypeToMetaDataName("scheme") == "scheme"
    // -------------------------------------------------------------------------

    /**
     * {@code filter scheme exists} after extracting a {@code [metadata scheme]}
     * field emits {@code item.get_meta("scheme")} via generateSymbolReference.
     */
    @Test
    public void testMetaTypeSchemeEmitsGetMeta() {
        testUtils.assertBundleContainsExpectedString("""
                metaType scheme string

                type Trade:
                    identifier string (0..1)
                        [metadata scheme]

                func FilterIdentifiersByScheme:
                    inputs:
                        trades Trade (0..*)
                    output:
                        result string (0..1)
                            [metadata scheme]
                    set result:
                        trades
                            extract identifier
                            then filter scheme exists
                            then first
                """,
                "rune_filter(item, lambda item: rune_attr_exists(item.get_meta(\"scheme\")))");
    }

    /**
     * Confirms that the two paths produce structurally identical filter expressions
     * except for the method name: {@code resolve_ref_key} vs {@code get_meta}.
     */
    @Test
    public void testMetaTypeSchemeUsesGetMetaNotResolveRef() {
        String generated = testUtils.generatePythonAndExtractBundle("""
                metaType scheme string

                type Document:
                    category string (0..1)
                        [metadata scheme]

                func FilterDocumentsByScheme:
                    inputs:
                        docs Document (0..*)
                    output:
                        result string (0..1)
                            [metadata scheme]
                    set result:
                        docs
                            extract category
                            then filter scheme exists
                            then first
                """);

        testUtils.assertGeneratedContainsExpectedString(generated,
                "lambda item: rune_attr_exists(item.get_meta(\"scheme\"))");
        testUtils.assertGeneratedDoesNotContain(generated,
                "resolve_ref_key");
    }
}
