/*
 * Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
 * SPDX-License-Identifier: Apache-2.0
 */
package com.regnosys.rosetta.generator.python.object;

import jakarta.inject.Inject;
import com.regnosys.rosetta.tests.RosettaInjectorProvider;
import com.regnosys.rosetta.generator.python.PythonGeneratorTestUtils;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.util.Map;

/**
 * Tests for Python metadata code generation, covering key/reference metadata,
 * attribute metadata, and meta key-ref generation.
 */
@ExtendWith(InjectionExtension.class)
@InjectWith(RosettaInjectorProvider.class)
@SuppressWarnings("checkstyle:LineLength")
public class PythonMetadataTest {

    /**
     * Test utils for generating Python.
     */
    @Inject
    private PythonGeneratorTestUtils testUtils;

    /**
     * Generated Python (lazy-initialised once per test instance).
     */
    private Map<String, CharSequence> python = null;

    private Map<String, CharSequence> getPython() {
        if (python == null) {
            python = testUtils.generatePythonFromString(
                """
                namespace test.generated_syntax.metadata : <"generate Python unit tests from Rosetta.">

                type A:
                    [metadata key]
                    fieldA string (1..1)

                type NodeRef:
                    typeA A (0..1)
                    aReference A (0..1)
                        [metadata reference]

                type AttributeRef:
                    dateField date (0..1)
                        [metadata id]
                    dateReference date (0..1)
                        [metadata reference]

                type Root:
                    [rootType]
                    nodeRef NodeRef (0..1)
                    attributeRef AttributeRef (0..1)

                type SchemeTest:
                    [metadata scheme]
                    a string (1..1)

                func TestMetaPath:
                    inputs:
                        inpRoot Root (1..1)
                    output:
                        out NodeRef (1..1)

                    set out: inpRoot -> nodeRef
                """);
        }
        return python;
    }

    // -----------------------------------------------------------------------
    // Methods from PythonMetaDataGeneratorTest (renamed)
    // -----------------------------------------------------------------------

    /**
     * A is acyclic — standalone. File contains the class directly.
     */
    @Test
    public void testKeyMetadata() {
        String aPython = getPython().get("src/test/generated_syntax/metadata/A.py").toString();
        testUtils.assertGeneratedContainsExpectedString(aPython, "class A(BaseDataClass):");
        testUtils.assertGeneratedContainsExpectedString(aPython, "_ALLOWED_METADATA = {'@key', '@key:external'}");
        testUtils.assertGeneratedContainsExpectedString(aPython, "fieldA: str = Field(..., description='')");
    }

    /**
     * NodeRef is acyclic — standalone. aReference has [metadata reference] so
     * its annotation is inline; typeA has no metadata so it is Optional[A] directly.
     */
    @Test
    public void testReferenceMetadata() {
        String nodeRefPython = getPython().get("src/test/generated_syntax/metadata/NodeRef.py").toString();
        testUtils.assertGeneratedContainsExpectedString(nodeRefPython, "class NodeRef(BaseDataClass):");
        testUtils.assertGeneratedContainsExpectedString(nodeRefPython,
            "typeA: Optional[A] = Field(None, description='')");
        testUtils.assertGeneratedContainsExpectedString(nodeRefPython,
            "aReference: Annotated[Optional[A | BaseReference], A.serializer(), A.validator(('@key', '@key:external', '@ref', '@ref:external'))] = Field(None, description='')");
    }

    /**
     * AttributeRef is acyclic — standalone. Date-type attributes with metadata
     * are always inline (DateWithMeta is a basic type).
     */
    @Test
    public void testAttributeWithDateMetadata() {
        String attrRefPython = getPython().get("src/test/generated_syntax/metadata/AttributeRef.py").toString();
        testUtils.assertGeneratedContainsExpectedString(attrRefPython, "class AttributeRef(BaseDataClass):");
        testUtils.assertGeneratedContainsExpectedString(attrRefPython,
            "dateField: Annotated[Optional[DateWithMeta], DateWithMeta.serializer(), DateWithMeta.validator(('@key', '@key:external'))]");
        testUtils.assertGeneratedContainsExpectedString(attrRefPython,
            "dateReference: Annotated[Optional[DateWithMeta | BaseReference], DateWithMeta.serializer(), DateWithMeta.validator(('@ref', '@ref:external'))]");
    }

    /**
     * Root is acyclic — standalone. Attributes reference standalone NodeRef and
     * AttributeRef directly; no Phase 2/3 needed.
     */
    @Test
    public void testRootWithMetadataDependency() {
        String rootPython = getPython().get("src/test/generated_syntax/metadata/Root.py").toString();
        testUtils.assertGeneratedContainsExpectedString(rootPython, "class Root(BaseDataClass):");
        testUtils.assertGeneratedContainsExpectedString(rootPython,
            "nodeRef: Optional[NodeRef] = Field(None, description='')");
        testUtils.assertGeneratedContainsExpectedString(rootPython,
            "attributeRef: Optional[AttributeRef] = Field(None, description='')");
    }

    /**
     * SchemeTest is acyclic — standalone.
     */
    @Test
    public void testSchemeMetadata() {
        String schemePython = getPython().get("src/test/generated_syntax/metadata/SchemeTest.py").toString();
        testUtils.assertGeneratedContainsExpectedString(schemePython, "class SchemeTest(BaseDataClass):");
        testUtils.assertGeneratedContainsExpectedString(schemePython, "_ALLOWED_METADATA = {'@scheme'}");
        testUtils.assertGeneratedContainsExpectedString(schemePython, "a: str = Field(..., description='')");
    }

    /**
     * The bundle is still generated (even when mostly empty), because functions
     * require the module guardian infrastructure.
     */
    @Test
    public void testBundleExists() {
        assertFalse(getPython().containsKey("src/test/_bundle.py"),
            "No bundle should be generated for a standalone-only model");
    }

    /**
     * The function TestMetaPath is acyclic — standalone. The function uses its
     * simple name and short names for standalone input/output types.
     */
    @Test
    public void testFunctionWithMetaPath() {
        String functionPython = getPython()
            .get("src/test/generated_syntax/metadata/functions/TestMetaPath.py").toString();
        testUtils.assertGeneratedContainsExpectedString(functionPython,
            "def TestMetaPath(inpRoot: Root) -> NodeRef:");
        testUtils.assertGeneratedContainsExpectedString(functionPython,
            "out = rune_resolve_attr(rune_resolve_attr(self, \"inpRoot\"), \"nodeRef\")");
    }

    // -----------------------------------------------------------------------
    // Method from PythonKeyRefTest (unchanged name)
    // -----------------------------------------------------------------------

    /**
     * Test case for generating key ref.
     * KeyEntity and RefEntity are acyclic — both standalone. RefEntity.py
     * holds the class directly with inline annotation; no Phase 2/3 needed.
     */
    @Test
    public void testKeyRef() {
        Map<String, CharSequence> gf = testUtils.generatePythonFromString(
            """
            type KeyEntity:
                [metadata key]
                value int (1..1)

            type RefEntity:
                ke KeyEntity (1..1)
                    [metadata reference]
            """);

        // RefEntity is standalone — class is in its own file, not the bundle
        String refEntityPython = gf.get("src/com/rosetta/test/model/RefEntity.py").toString();

        // Class declaration uses short name (no _FQRTN for standalone)
        testUtils.assertGeneratedContainsExpectedString(refEntityPython,
            "class RefEntity(BaseDataClass):");

        // Annotation is inline in the class body (no Phase 2 __annotations__ update)
        testUtils.assertGeneratedContainsExpectedString(refEntityPython,
            "ke: Annotated[KeyEntity | BaseReference, KeyEntity.serializer(), KeyEntity.validator(('@key', '@key:external', '@ref', '@ref:external'))] = Field(..., description='')");
    }

    // -----------------------------------------------------------------------
    // Method from PythonMetaKeyRefGeneratorTest (renamed)
    // -----------------------------------------------------------------------

    /**
     * Test case for generating meta key ref (id/reference location metadata).
     * KeyRef and ScopedKeyRef are acyclic — both standalone. Classes are
     * written directly to their FQ-path files; metadata annotations are
     * inline in the class body (no Phase 2/3 needed).
     */
    @Test
    public void testIdReferenceLocationMetadata() {
        Map<String, CharSequence> python = testUtils.generatePythonFromString(
            """
            namespace test.generated_syntax.meta_key_ref : <"generate Python unit tests from Rosetta.">

            type KeyRef:
                fieldA string (1..1)
                [metadata id]
                [metadata reference]

            type ScopedKeyRef:
                fieldA string (1..1)
                [metadata location]
                [metadata address]
            """);

        // Standalone files contain classes directly (not proxy stubs)
        String keyRefPython = python.get("src/test/generated_syntax/meta_key_ref/KeyRef.py").toString();
        assertNotNull(keyRefPython, "KeyRef.py was not found");
        testUtils.assertGeneratedContainsExpectedString(keyRefPython,
            "class KeyRef(BaseDataClass):");

        String scopedKeyRefPython = python.get("src/test/generated_syntax/meta_key_ref/ScopedKeyRef.py").toString();
        assertNotNull(scopedKeyRefPython, "ScopedKeyRef.py was not found");
        testUtils.assertGeneratedContainsExpectedString(scopedKeyRefPython,
            "class ScopedKeyRef(BaseDataClass):");

        // KeyRef checks — annotations are inline in the standalone file
        testUtils.assertGeneratedContainsExpectedString(keyRefPython,
            "'fieldA': {'@ref', '@ref:external', '@key', '@key:external'}");
        // StrWithMeta is a basic type — annotation is always inline
        testUtils.assertGeneratedContainsExpectedString(keyRefPython,
            "fieldA: Annotated[StrWithMeta | BaseReference, StrWithMeta.serializer(), StrWithMeta.validator(('@ref', '@ref:external', '@key', '@key:external'))] = Field(..., description='')");

        // ScopedKeyRef checks
        testUtils.assertGeneratedContainsExpectedString(scopedKeyRefPython,
            "'fieldA': {'@key:scoped', '@ref:scoped'}");
        testUtils.assertGeneratedContainsExpectedString(scopedKeyRefPython,
            "fieldA: Annotated[StrWithMeta | BaseReference, StrWithMeta.serializer(), StrWithMeta.validator(('@key:scoped', '@ref:scoped'))] = Field(..., description='')");
    }
}
