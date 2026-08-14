/*
 * Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
 * SPDX-License-Identifier: Apache-2.0
 */
package com.regnosys.rosetta.generator.python.object;

import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.regnosys.rosetta.generator.python.PythonGeneratorTestUtils;
import com.regnosys.rosetta.tests.RosettaInjectorProvider;

import jakarta.inject.Inject;

/**
 * Verifies that bundled classes use {@code defer_build=True} combined with Phase 3
 * {@code model_rebuild(force=True)} calls to achieve efficient, correct schema compilation.
 *
 * <p>Background: bundled classes use Phase 2 to update field annotations after class
 * definition (because circular references cannot be resolved at class-definition time).
 * Phase 3 {@code model_rebuild(force=True)} then forces Pydantic to re-read those
 * annotations and compile the schema with the correct types.
 *
 * <p>{@code model_config = ConfigDict(defer_build=True)} on every bundled class defers
 * the initial schema compilation from class-definition time. Because all cyclic types are
 * fully defined by the time Phase 3 runs (the whole bundle has been executed), Pydantic
 * can build schemas more efficiently — each rebuild takes ~4× less time and memory than
 * without {@code defer_build=True}. CDM result: ~1.8 GB / ~5s vs ~7.9 GB / ~17s.
 *
 * <p>Phase 3 is still required for correctness: without it, Pydantic uses the {@code None}-typed
 * placeholder schema (which is trivially built even with {@code defer_build=True}) and
 * deserialization fails with "Input should be None" errors.
 */
@ExtendWith(InjectionExtension.class)
@InjectWith(RosettaInjectorProvider.class)
@SuppressWarnings("LineLength")
public class PythonBundleRebuildOrderTest {

    @Inject
    private PythonGeneratorTestUtils testUtils;

    /**
     * Bundled classes must carry {@code model_config = ConfigDict(defer_build=True)} AND the
     * bundle must emit Phase 3 {@code model_rebuild(force=True)} calls after Phase 2.
     *
     * <p>Model: Parent ↔ Child (both bundled due to cycle). Child must be rebuilt before Parent
     * because Parent holds a deferred field of type Child (Child's schema must exist before
     * Parent's schema validates it).
     */
    @Test
    public void testBundledClassesUseDeferBuildWithPhase3Rebuilds() {
        String model = """
                type Parent:
                    child Child (0..1)

                type Child extends Parent:
                    extra Child (0..1)
                """;

        String bundle = testUtils.generatePythonAndExtractBundle(model);

        // Every bundled class must carry defer_build=True
        testUtils.assertGeneratedContainsExpectedString(bundle, "model_config = ConfigDict(defer_build=True)");

        // Phase 3 model_rebuild calls must be present (needed for correct deserialization)
        testUtils.assertGeneratedContainsExpectedString(bundle, "model_rebuild(force=True)");

        // Phase 2 annotation updates must precede Phase 3
        testUtils.assertGeneratedContainsExpectedString(bundle, "# Phase 2: Delayed Annotation Updates");
        testUtils.assertAppearsAfter(bundle,
                "# Phase 2: Delayed Annotation Updates",
                "model_rebuild(force=True)");
    }
}
