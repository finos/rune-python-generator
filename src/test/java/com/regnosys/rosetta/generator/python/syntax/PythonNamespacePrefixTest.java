/*
 * Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
 * SPDX-License-Identifier: Apache-2.0
 */
package com.regnosys.rosetta.generator.python.syntax;

import jakarta.inject.Inject;
import com.regnosys.rosetta.tests.RosettaInjectorProvider;
import com.regnosys.rosetta.generator.python.PythonGeneratorTestUtils;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that the -x / --namespace-prefix option correctly prepends a prefix
 * segment to every namespace throughout all generated Python artefacts.
 *
 * <p>The model uses namespace {@code cdm.event.common} and prefix {@code finos},
 * so the effective namespace becomes {@code finos.cdm.event.common}.
 *
 * <p>In the feature branch all types in this model are acyclic, so they are
 * generated as standalone files (no {@code _bundle.py}).  Assertions are written
 * against the standalone output structure:
 * <ul>
 *   <li>Class definitions live directly in their own {@code .py} file.</li>
 *   <li>Cross-type imports use {@code from <fqn> import <Name>}.</li>
 *   <li>{@code _FQRTN} is absent (it only appears in bundled classes).</li>
 *   <li>No {@code _bundle.py} is generated.</li>
 * </ul>
 *
 * <p>No-prefix behaviour is validated for every assertion so the before/after
 * contrast is immediately visible.
 */
@ExtendWith(InjectionExtension.class)
@InjectWith(RosettaInjectorProvider.class)
public class PythonNamespacePrefixTest {

    @Inject
    private PythonGeneratorTestUtils testUtils;

    private static final String PREFIX = "finos";

    /**
     * Rosetta model with a type, a type that references it, and an enum —
     * covering every category of generated artefact.
     */
    private static final String MODEL = """
            namespace cdm.event.common

            type BusinessEvent:
                eventType EventTypeEnum (0..1)
                tradeDate date (0..1)

            type ContractDetails:
                businessEvent BusinessEvent (0..1)

            enum EventTypeEnum:
                Exercise
                Novation

            func GetYear: <"Get the year.">
                inputs:
                    n number (1..1)
                output:
                    result number (1..1)
                set result:
                    n
            """;

    private Map<String, CharSequence> withPrefix() {
        return testUtils.generatePythonFromString(MODEL, PREFIX, null);
    }

    private Map<String, CharSequence> withoutPrefix() {
        return testUtils.generatePythonFromString(MODEL);
    }

    // -------------------------------------------------------------------------
    // No bundle generated (all types are acyclic → standalone)
    // -------------------------------------------------------------------------

    @Test
    void testNoBundleWithPrefix() {
        assertFalse(withPrefix().containsKey("src/finos/_bundle.py"),
                "No _bundle.py should be generated for an acyclic model even with prefix");
    }

    @Test
    void testNoBundleWithoutPrefix() {
        assertFalse(withoutPrefix().containsKey("src/cdm/_bundle.py"),
                "No _bundle.py should be generated for an acyclic model");
    }

    // -------------------------------------------------------------------------
    // Standalone type file path
    // -------------------------------------------------------------------------

    @Test
    void testTypeFilePathWithPrefix() {
        assertTrue(withPrefix().containsKey("src/finos/cdm/event/common/BusinessEvent.py"),
                "Standalone file should be at src/finos/cdm/event/common/BusinessEvent.py when prefix is 'finos'");
    }

    @Test
    void testTypeFilePathWithoutPrefix() {
        Map<String, CharSequence> gen = withoutPrefix();
        assertTrue(gen.containsKey("src/cdm/event/common/BusinessEvent.py"),
                "Standalone file should be at src/cdm/event/common/BusinessEvent.py when no prefix is set");
        assertFalse(gen.containsKey("src/finos/cdm/event/common/BusinessEvent.py"),
                "Prefixed standalone path must not appear when no prefix is set");
    }

    // -------------------------------------------------------------------------
    // Enum standalone file path
    // -------------------------------------------------------------------------

    @Test
    void testEnumFilePathWithPrefix() {
        assertTrue(withPrefix().containsKey("src/finos/cdm/event/common/EventTypeEnum.py"),
                "Enum file should be under src/finos/cdm/event/common/ when prefix is 'finos'");
    }

    @Test
    void testEnumFilePathWithoutPrefix() {
        Map<String, CharSequence> gen = withoutPrefix();
        assertTrue(gen.containsKey("src/cdm/event/common/EventTypeEnum.py"),
                "Enum file should be under src/cdm/event/common/ when no prefix is set");
        assertFalse(gen.containsKey("src/finos/cdm/event/common/EventTypeEnum.py"),
                "Prefixed enum path must not appear when no prefix is set");
    }

    // -------------------------------------------------------------------------
    // Standalone class definition  (short name, not flattened)
    // -------------------------------------------------------------------------

    @Test
    void testClassDefinitionWithPrefix() {
        String file = withPrefix().get("src/finos/cdm/event/common/BusinessEvent.py").toString();
        assertTrue(file.contains("class BusinessEvent(BaseDataClass):"),
                "Standalone class must use the short name 'BusinessEvent', not a flattened form");
        assertFalse(file.contains("finos_cdm_event_common_BusinessEvent"),
                "Flattened class name must not appear in a standalone file");
    }

    @Test
    void testClassDefinitionWithoutPrefix() {
        String file = withoutPrefix().get("src/cdm/event/common/BusinessEvent.py").toString();
        assertTrue(file.contains("class BusinessEvent(BaseDataClass):"),
                "Standalone class must use the short name 'BusinessEvent' when no prefix");
        assertFalse(file.contains("finos"),
                "No prefix token must appear in the standalone file when no prefix is set");
    }

    // -------------------------------------------------------------------------
    // _FQRTN — must be ABSENT from standalone files in both cases
    // -------------------------------------------------------------------------

    @Test
    void testFQRTNAbsentWithPrefix() {
        String file = withPrefix().get("src/finos/cdm/event/common/BusinessEvent.py").toString();
        assertFalse(file.contains("_FQRTN"),
                "_FQRTN must not appear in a standalone class file (only in bundled classes)");
    }

    @Test
    void testFQRTNAbsentWithoutPrefix() {
        String file = withoutPrefix().get("src/cdm/event/common/BusinessEvent.py").toString();
        assertFalse(file.contains("_FQRTN"),
                "_FQRTN must not appear in a standalone class file when no prefix is set");
    }

    // -------------------------------------------------------------------------
    // Cross-type reference  (ContractDetails.businessEvent imports BusinessEvent)
    // -------------------------------------------------------------------------

    @Test
    void testTypeReferenceImportWithPrefix() {
        String file = withPrefix().get("src/finos/cdm/event/common/ContractDetails.py").toString();
        assertTrue(file.contains("from finos.cdm.event.common.BusinessEvent import BusinessEvent"),
                "ContractDetails must import BusinessEvent from the prefixed path");
    }

    @Test
    void testTypeReferenceImportWithoutPrefix() {
        String file = withoutPrefix().get("src/cdm/event/common/ContractDetails.py").toString();
        assertTrue(file.contains("from cdm.event.common.BusinessEvent import BusinessEvent"),
                "ContractDetails must import BusinessEvent from the un-prefixed path when no prefix");
        assertFalse(file.contains("finos"),
                "No prefix token must appear in ContractDetails when no prefix is set");
    }

    // -------------------------------------------------------------------------
    // Enum import in standalone type file  (BusinessEvent.eventType is EventTypeEnum)
    // -------------------------------------------------------------------------

    @Test
    void testEnumImportInTypeFileWithPrefix() {
        String file = withPrefix().get("src/finos/cdm/event/common/BusinessEvent.py").toString();
        assertTrue(file.contains("finos.cdm.event.common.EventTypeEnum"),
                "BusinessEvent must import EventTypeEnum from the prefixed namespace");
    }

    @Test
    void testEnumImportInTypeFileWithoutPrefix() {
        String file = withoutPrefix().get("src/cdm/event/common/BusinessEvent.py").toString();
        assertTrue(file.contains("cdm.event.common.EventTypeEnum"),
                "BusinessEvent must import EventTypeEnum from the un-prefixed namespace when no prefix");
        assertFalse(file.contains("finos"),
                "No prefix token must appear in BusinessEvent when no prefix is set");
    }

    // -------------------------------------------------------------------------
    // __init__.py files  (workspace and sub-package layers)
    // -------------------------------------------------------------------------

    @Test
    void testInitFilesWithPrefix() {
        Map<String, CharSequence> gen = withPrefix();
        assertTrue(gen.containsKey("src/finos/__init__.py"),
                "Top-level __init__.py must be created under src/finos/");
        assertTrue(gen.containsKey("src/finos/cdm/__init__.py"),
                "__init__.py must be created for the cdm sub-package");
        assertTrue(gen.containsKey("src/finos/cdm/event/__init__.py"),
                "__init__.py must be created for the cdm.event sub-package");
        assertTrue(gen.containsKey("src/finos/cdm/event/common/__init__.py"),
                "__init__.py must be created for the cdm.event.common sub-package");
    }

    @Test
    void testInitFilesWithoutPrefix() {
        Map<String, CharSequence> gen = withoutPrefix();
        assertTrue(gen.containsKey("src/cdm/__init__.py"),
                "Top-level __init__.py must be at src/cdm/ when no prefix");
        assertTrue(gen.containsKey("src/cdm/event/__init__.py"),
                "__init__.py must be created for the cdm.event sub-package when no prefix");
        assertTrue(gen.containsKey("src/cdm/event/common/__init__.py"),
                "__init__.py must be created for cdm.event.common when no prefix");
        assertFalse(gen.containsKey("src/finos/__init__.py"),
                "src/finos/__init__.py must not be created when no prefix is set");
    }

    // -------------------------------------------------------------------------
    // Top-level __init__.py content
    // -------------------------------------------------------------------------

    @Test
    void testInitFileContainsNamespacePrefixWithPrefix() {
        String init = withPrefix().get("src/finos/__init__.py").toString();
        assertTrue(init.contains("rune_namespace_prefix='finos'"),
                "Top-level __init__.py must set rune_namespace_prefix to the configured prefix value");
        assertFalse(init.contains("rune_namespace_prefix=None"),
                "rune_namespace_prefix must not be None when a prefix is set");
    }

    @Test
    void testInitFileContainsNamespacePrefixNoneWithoutPrefix() {
        String init = withoutPrefix().get("src/cdm/__init__.py").toString();
        assertTrue(init.contains("rune_namespace_prefix=None"),
                "Top-level __init__.py must set rune_namespace_prefix=None when no prefix is configured");
        assertFalse(init.contains("rune_namespace_prefix='"),
                "rune_namespace_prefix must not have a string value when no prefix is set");
    }

    @Test
    void testInitFileContainsRuneDeserializeWithPrefix() {
        String init = withPrefix().get("src/finos/__init__.py").toString();
        assertTrue(init.contains("from rune.runtime.base_data_class import BaseDataClass"),
                "Top-level __init__.py must import BaseDataClass");
        assertTrue(init.contains("def rune_deserialize("),
                "Top-level __init__.py must define the rune_deserialize helper function");
    }

    @Test
    void testInitFileContainsRuneDeserializeWithoutPrefix() {
        String init = withoutPrefix().get("src/cdm/__init__.py").toString();
        assertTrue(init.contains("from rune.runtime.base_data_class import BaseDataClass"),
                "Top-level __init__.py must import BaseDataClass when no prefix");
        assertTrue(init.contains("def rune_deserialize("),
                "Top-level __init__.py must define the rune_deserialize helper function when no prefix");
    }

    // -------------------------------------------------------------------------
    // Top-level __init__.py — native function registration with prefix
    // -------------------------------------------------------------------------

    private static final String NATIVE_FUNC_MODEL = """
            namespace cdm.event.common

            func NativeFunc:
                [codeImplementation]
                inputs:
                    n number (1..1)
                output:
                    result number (1..1)
            """;

    @Test
    void testInitFileNativeFunctionRegistrationWithPrefix() {
        Map<String, CharSequence> gen = testUtils.generatePythonFromString(NATIVE_FUNC_MODEL, PREFIX, null);
        String init = gen.get("src/finos/__init__.py").toString();
        assertTrue(init.contains("from rune.runtime.native_registry import rune_register_native as _rune_register_native"),
                "Top-level __init__.py must import rune_register_native when native functions are present");
        assertTrue(init.contains("from finos.cdm.event.common.rune.native.NativeFunc import NativeFunc"),
                "Native function must be imported from the prefixed rune/native path");
        assertTrue(init.contains("_rune_register_native('finos.cdm.event.common.functions.NativeFunc',"),
                "Native function must be registered under the prefixed FQN");
    }

    @Test
    void testInitFileNativeFunctionRegistrationWithoutPrefix() {
        Map<String, CharSequence> gen = testUtils.generatePythonFromString(NATIVE_FUNC_MODEL);
        String init = gen.get("src/cdm/__init__.py").toString();
        assertTrue(init.contains("from rune.runtime.native_registry import rune_register_native as _rune_register_native"),
                "Top-level __init__.py must import rune_register_native when no prefix");
        assertTrue(init.contains("from cdm.event.common.rune.native.NativeFunc import NativeFunc"),
                "Native function must be imported from the unprefixed rune/native path");
        assertTrue(init.contains("_rune_register_native('cdm.event.common.functions.NativeFunc',"),
                "Native function must be registered under the unprefixed FQN");
    }

    // -------------------------------------------------------------------------
    // _FQRTN value in bundled classes — prefix must NOT appear
    // -------------------------------------------------------------------------

    private static final String CYCLIC_MODEL = """
            namespace cdm.event.common

            type CycleA:
                b CycleB (0..1)

            type CycleB:
                a CycleA (0..1)
            """;

    @Test
    void testFQRTNExcludesPrefixInBundledClass() {
        Map<String, CharSequence> gen = testUtils.generatePythonFromString(CYCLIC_MODEL, PREFIX, null);
        String bundle = gen.get("src/finos/_bundle.py").toString();
        assertTrue(bundle.contains("_FQRTN: ClassVar[str] = 'cdm.event.common.CycleA'"),
                "_FQRTN must use the unprefixed FQN even when a namespace prefix is set");
        assertFalse(bundle.contains("'finos.cdm.event.common.CycleA'"),
                "_FQRTN must not include the namespace prefix");
    }

    @Test
    void testFQRTNWithoutPrefixInBundledClass() {
        Map<String, CharSequence> gen = testUtils.generatePythonFromString(CYCLIC_MODEL);
        String bundle = gen.get("src/cdm/_bundle.py").toString();
        assertTrue(bundle.contains("_FQRTN: ClassVar[str] = 'cdm.event.common.CycleA'"),
                "_FQRTN must contain the full FQN when no prefix is set");
    }

    // -------------------------------------------------------------------------
    // Function stub file path
    // -------------------------------------------------------------------------

    @Test
    void testFunctionFilePathWithPrefix() {
        assertTrue(withPrefix().containsKey("src/finos/cdm/event/common/functions/GetYear.py"),
                "Function file must be at src/finos/cdm/event/common/functions/GetYear.py when prefix is 'finos'");
    }

    @Test
    void testFunctionFilePathWithoutPrefix() {
        Map<String, CharSequence> gen = withoutPrefix();
        assertTrue(gen.containsKey("src/cdm/event/common/functions/GetYear.py"),
                "Function file must be at src/cdm/event/common/functions/GetYear.py when no prefix");
        assertFalse(gen.containsKey("src/finos/cdm/event/common/functions/GetYear.py"),
                "Prefixed function path must not appear when no prefix is set");
    }

    // -------------------------------------------------------------------------
    // Function file content  (standalone function, not a stub)
    // -------------------------------------------------------------------------

    @Test
    void testFunctionDefinitionWithPrefix() {
        String file = withPrefix().get("src/finos/cdm/event/common/functions/GetYear.py").toString();
        assertTrue(file.contains("def GetYear("),
                "Function file must contain the actual function definition 'def GetYear('");
        assertFalse(file.contains("from finos._bundle import"),
                "Standalone function file must not contain a bundle stub import");
    }

    @Test
    void testFunctionDefinitionWithoutPrefix() {
        String file = withoutPrefix().get("src/cdm/event/common/functions/GetYear.py").toString();
        assertTrue(file.contains("def GetYear("),
                "Function file must contain the actual function definition 'def GetYear(' when no prefix");
        assertFalse(file.contains("finos"),
                "No prefix token must appear in the function file when no prefix is set");
    }

    // -------------------------------------------------------------------------
    // functions/__init__.py
    // -------------------------------------------------------------------------

    @Test
    void testFunctionInitFileWithPrefix() {
        assertTrue(withPrefix().containsKey("src/finos/cdm/event/common/functions/__init__.py"),
                "__init__.py must be created for the functions sub-package under src/finos/cdm/event/common/");
    }

    @Test
    void testFunctionInitFileWithoutPrefix() {
        Map<String, CharSequence> gen = withoutPrefix();
        assertTrue(gen.containsKey("src/cdm/event/common/functions/__init__.py"),
                "__init__.py must be created for the functions sub-package under src/cdm/event/common/");
        assertFalse(gen.containsKey("src/finos/cdm/event/common/functions/__init__.py"),
                "Prefixed functions __init__.py must not appear when no prefix is set");
    }
}
