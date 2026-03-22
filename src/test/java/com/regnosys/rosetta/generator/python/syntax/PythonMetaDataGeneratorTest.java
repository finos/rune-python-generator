package com.regnosys.rosetta.generator.python.syntax;

import jakarta.inject.Inject;
import com.regnosys.rosetta.tests.RosettaInjectorProvider;
import com.regnosys.rosetta.generator.python.PythonGeneratorTestUtils;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.assertFalse;
import java.util.Map;

@ExtendWith(InjectionExtension.class)
@InjectWith(RosettaInjectorProvider.class)
@SuppressWarnings("checkstyle:LineLength")
public class PythonMetaDataGeneratorTest {

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

    /**
     * A is acyclic — standalone. File contains the class directly.
     */
    @Test
    public void testAStandalone() {
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
    public void testNodeRefStandalone() {
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
    public void testAttributeRefStandalone() {
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
    public void testRootStandalone() {
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
    public void testSchemeTestStandalone() {
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
}
