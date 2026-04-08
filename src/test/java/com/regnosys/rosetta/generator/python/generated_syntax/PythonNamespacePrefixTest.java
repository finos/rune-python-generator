package com.regnosys.rosetta.generator.python.generated_syntax;

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
 * <p>No-prefix behaviour is already thoroughly covered by
 * {@link PythonBasicGeneratorTest} (namespace {@code test.generated_syntax.basic})
 * and the other tests in the {@code generated_syntax} package.  This class also
 * includes explicit no-prefix assertions for the same model to make the
 * before/after contrast immediately visible.
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
        return testUtils.generatePythonFromString(MODEL, PREFIX);
    }

    private Map<String, CharSequence> withoutPrefix() {
        return testUtils.generatePythonFromString(MODEL);
    }

    // -------------------------------------------------------------------------
    // Bundle file path
    // -------------------------------------------------------------------------

    @Test
    void testBundleFilePathWithPrefix() {
        assertTrue(withPrefix().containsKey("src/finos/_bundle.py"),
                "Bundle should be at src/finos/_bundle.py when prefix is 'finos'");
    }

    @Test
    void testBundleFilePathWithoutPrefix() {
        Map<String, CharSequence> gen = withoutPrefix();
        assertTrue(gen.containsKey("src/cdm/_bundle.py"),
                "Bundle should be at src/cdm/_bundle.py when no prefix is set");
        assertFalse(gen.containsKey("src/finos/_bundle.py"),
                "src/finos/_bundle.py must not appear when no prefix is set");
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
    // Type stub file path
    // -------------------------------------------------------------------------

    @Test
    void testTypeStubFilePathWithPrefix() {
        assertTrue(withPrefix().containsKey("src/finos/cdm/event/common/BusinessEvent.py"),
                "Stub file should be at src/finos/cdm/event/common/BusinessEvent.py");
    }

    @Test
    void testTypeStubFilePathWithoutPrefix() {
        Map<String, CharSequence> gen = withoutPrefix();
        assertTrue(gen.containsKey("src/cdm/event/common/BusinessEvent.py"),
                "Stub file should be at src/cdm/event/common/BusinessEvent.py when no prefix");
        assertFalse(gen.containsKey("src/finos/cdm/event/common/BusinessEvent.py"),
                "Prefixed stub path must not appear when no prefix is set");
    }

    // -------------------------------------------------------------------------
    // Stub import statement  (standalone file → bundle)
    // -------------------------------------------------------------------------

    @Test
    void testStubImportWithPrefix() {
        String stub = withPrefix().get("src/finos/cdm/event/common/BusinessEvent.py").toString();
        assertTrue(stub.contains("from finos._bundle import finos_cdm_event_common_BusinessEvent as BusinessEvent"),
                "Stub must import from finos._bundle using the prefixed class name");
    }

    @Test
    void testStubImportWithoutPrefix() {
        String stub = withoutPrefix().get("src/cdm/event/common/BusinessEvent.py").toString();
        assertTrue(stub.contains("from cdm._bundle import cdm_event_common_BusinessEvent as BusinessEvent"),
                "Stub must import from cdm._bundle using the un-prefixed class name");
        assertFalse(stub.contains("finos"),
                "No prefix token must appear in the stub when no prefix is set");
    }

    // -------------------------------------------------------------------------
    // Bundle class name  (class definition line)
    // -------------------------------------------------------------------------

    @Test
    void testBundleClassNameWithPrefix() {
        String bundle = withPrefix().get("src/finos/_bundle.py").toString();
        assertTrue(bundle.contains("class finos_cdm_event_common_BusinessEvent(BaseDataClass):"),
                "Bundle class must use the prefixed name finos_cdm_event_common_BusinessEvent");
    }

    @Test
    void testBundleClassNameWithoutPrefix() {
        String bundle = withoutPrefix().get("src/cdm/_bundle.py").toString();
        assertTrue(bundle.contains("class cdm_event_common_BusinessEvent(BaseDataClass):"),
                "Bundle class must use cdm_event_common_BusinessEvent when no prefix");
        assertFalse(bundle.contains("finos_"),
                "No prefix token must appear in bundle class names when no prefix is set");
    }

    // -------------------------------------------------------------------------
    // _FQRTN — must remain the unmodified Rune type identity in both cases
    // -------------------------------------------------------------------------

    @Test
    void testFQRTNUnchangedWithPrefix() {
        String bundle = withPrefix().get("src/finos/_bundle.py").toString();
        assertTrue(bundle.contains("_FQRTN = 'cdm.event.common.BusinessEvent'"),
                "_FQRTN must reflect the original Rune namespace, not the prefix");
        assertFalse(bundle.contains("_FQRTN = 'finos.cdm"),
                "_FQRTN must never include the namespace prefix");
    }

    @Test
    void testFQRTNUnchangedWithoutPrefix() {
        String bundle = withoutPrefix().get("src/cdm/_bundle.py").toString();
        assertTrue(bundle.contains("_FQRTN = 'cdm.event.common.BusinessEvent'"),
                "_FQRTN must be the Rune qualified name when no prefix is set");
    }

    // -------------------------------------------------------------------------
    // Cross-type reference in bundle  (ContractDetails.businessEvent attribute)
    // -------------------------------------------------------------------------

    @Test
    void testTypeReferenceInBundleWithPrefix() {
        String bundle = withPrefix().get("src/finos/_bundle.py").toString();
        assertTrue(bundle.contains("finos_cdm_event_common_BusinessEvent"),
                "Attribute type reference in ContractDetails must use the prefixed class name");
    }

    @Test
    void testTypeReferenceInBundleWithoutPrefix() {
        String bundle = withoutPrefix().get("src/cdm/_bundle.py").toString();
        assertTrue(bundle.contains("cdm_event_common_BusinessEvent"),
                "Attribute type reference must use un-prefixed class name when no prefix");
        assertFalse(bundle.contains("finos_cdm"),
                "No prefixed type references must appear when no prefix is set");
    }

    // -------------------------------------------------------------------------
    // Enum import in bundle header
    // -------------------------------------------------------------------------

    @Test
    void testEnumImportInBundleWithPrefix() {
        String bundle = withPrefix().get("src/finos/_bundle.py").toString();
        assertTrue(bundle.contains("import finos.cdm.event.common.EventTypeEnum"),
                "Enum import in bundle must be prefixed with 'finos.'");
    }

    @Test
    void testEnumImportInBundleWithoutPrefix() {
        String bundle = withoutPrefix().get("src/cdm/_bundle.py").toString();
        assertTrue(bundle.contains("import cdm.event.common.EventTypeEnum"),
                "Enum import must use the un-prefixed namespace when no prefix is set");
        assertFalse(bundle.contains("import finos."),
                "No prefixed enum import must appear when no prefix is set");
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
    // Function stub file path
    // -------------------------------------------------------------------------

    @Test
    void testFunctionStubFilePathWithPrefix() {
        assertTrue(withPrefix().containsKey("src/finos/cdm/event/common/functions/GetYear.py"),
                "Function stub must be at src/finos/cdm/event/common/functions/GetYear.py when prefix is 'finos'");
    }

    @Test
    void testFunctionStubFilePathWithoutPrefix() {
        Map<String, CharSequence> gen = withoutPrefix();
        assertTrue(gen.containsKey("src/cdm/event/common/functions/GetYear.py"),
                "Function stub must be at src/cdm/event/common/functions/GetYear.py when no prefix");
        assertFalse(gen.containsKey("src/finos/cdm/event/common/functions/GetYear.py"),
                "Prefixed function stub path must not appear when no prefix is set");
    }

    // -------------------------------------------------------------------------
    // Function stub import statement
    // -------------------------------------------------------------------------

    @Test
    void testFunctionStubImportWithPrefix() {
        String stub = withPrefix().get("src/finos/cdm/event/common/functions/GetYear.py").toString();
        assertTrue(stub.contains("from finos._bundle import finos_cdm_event_common_functions_GetYear as GetYear"),
                "Function stub must import from finos._bundle using the prefixed function name");
    }

    @Test
    void testFunctionStubImportWithoutPrefix() {
        String stub = withoutPrefix().get("src/cdm/event/common/functions/GetYear.py").toString();
        assertTrue(stub.contains("from cdm._bundle import cdm_event_common_functions_GetYear as GetYear"),
                "Function stub must import from cdm._bundle using the un-prefixed function name");
        assertFalse(stub.contains("finos"),
                "No prefix token must appear in the function stub when no prefix is set");
    }

    // -------------------------------------------------------------------------
    // Bundle function def name
    // -------------------------------------------------------------------------

    @Test
    void testBundleFunctionDefWithPrefix() {
        String bundle = withPrefix().get("src/finos/_bundle.py").toString();
        assertTrue(bundle.contains("def finos_cdm_event_common_functions_GetYear("),
                "Bundle must define function as finos_cdm_event_common_functions_GetYear when prefix is 'finos'");
    }

    @Test
    void testBundleFunctionDefWithoutPrefix() {
        String bundle = withoutPrefix().get("src/cdm/_bundle.py").toString();
        assertTrue(bundle.contains("def cdm_event_common_functions_GetYear("),
                "Bundle must define function as cdm_event_common_functions_GetYear when no prefix");
        assertFalse(bundle.contains("def finos_"),
                "No prefixed function def must appear in bundle when no prefix is set");
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
