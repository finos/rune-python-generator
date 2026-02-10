package com.regnosys.rosetta.generator.python.generated_syntax;

import jakarta.inject.Inject;
import com.regnosys.rosetta.tests.RosettaInjectorProvider;
import com.regnosys.rosetta.generator.python.PythonGeneratorTestUtils;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Map;

@ExtendWith(InjectionExtension.class)
@InjectWith(RosettaInjectorProvider.class)
public class PythonMetaDataGeneratorTest {

    @Inject
    private PythonGeneratorTestUtils testUtils;

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
                            """);
        }
        return python;
    }

    @Test
    public void testAProxy() {
        Map<String, CharSequence> python = getPython();
        testUtils.assertGeneratedContainsExpectedString(
                python.get("src/test/generated_syntax/metadata/A.py").toString(),
                """
                        # pylint: disable=unused-import
                        from test._bundle import test_generated_syntax_metadata_A as A

                        # EOF
                        """);
    }

    @Test
    public void testNodeRefProxy() {
        Map<String, CharSequence> python = getPython();
        testUtils.assertGeneratedContainsExpectedString(
                python.get("src/test/generated_syntax/metadata/NodeRef.py").toString(),
                """
                        # pylint: disable=unused-import
                        from test._bundle import test_generated_syntax_metadata_NodeRef as NodeRef

                        # EOF
                        """);
    }

    @Test
    public void testAttributeRefProxy() {
        Map<String, CharSequence> python = getPython();
        testUtils.assertGeneratedContainsExpectedString(
                python.get("src/test/generated_syntax/metadata/AttributeRef.py").toString(),
                """
                        # pylint: disable=unused-import
                        from test._bundle import test_generated_syntax_metadata_AttributeRef as AttributeRef

                        # EOF
                        """);
    }

    @Test
    public void testRootProxy() {
        Map<String, CharSequence> python = getPython();
        testUtils.assertGeneratedContainsExpectedString(
                python.get("src/test/generated_syntax/metadata/Root.py").toString(),
                """
                        # pylint: disable=unused-import
                        from test._bundle import test_generated_syntax_metadata_Root as Root

                        # EOF
                        """);
    }

    @Test
    public void testBundleExists() {
        Map<String, CharSequence> python = getPython();
        assertTrue(python.containsKey("src/test/_bundle.py"), "The bundle should be in the generated Python");
    }

    @Test
    public void testExpectedBundleA() {
        Map<String, CharSequence> python = getPython();
        String bundle = python.get("src/test/_bundle.py").toString();

        // Native types are not delayed
        assertTrue(bundle.contains("class test_generated_syntax_metadata_A(BaseDataClass):"), "Class A body");
        assertTrue(bundle.contains("_ALLOWED_METADATA = {'@key', '@key:external'}"), "Class A metadata");
        assertTrue(bundle.contains("fieldA: str = Field(..., description='')"), "Class A field");
    }

    @Test
    public void testExpectedBundleAttributeRef() {
        Map<String, CharSequence> python = getPython();
        String bundle = python.get("src/test/_bundle.py").toString();

        // Date is a basic type, so DateWithMeta is currently not delayed
        assertTrue(bundle.contains("class test_generated_syntax_metadata_AttributeRef(BaseDataClass):"),
                "Class AttributeRef body");
        assertTrue(bundle.contains(
                "dateField: Optional[Annotated[DateWithMeta, DateWithMeta.serializer(), DateWithMeta.validator(('@key', '@key:external'))]] = Field(None, description='')"),
                "Class AttributeRef field 1");
        assertTrue(bundle.contains(
                "dateReference: Optional[Annotated[DateWithMeta, DateWithMeta.serializer(), DateWithMeta.validator(('@ref', '@ref:external'))]] = Field(None, description='')"),
                "Class AttributeRef field 2");
    }

    @Test
    public void testExpectedBundleNodeRef() {
        Map<String, CharSequence> python = getPython();
        String bundle = python.get("src/test/_bundle.py").toString();

        // Phase 1: Clean Body
        assertTrue(bundle.contains("class test_generated_syntax_metadata_NodeRef(BaseDataClass):"),
                "Class NodeRef body");
        assertTrue(bundle.contains("typeA: Optional[test_generated_syntax_metadata_A] = Field(None, description='')"),
                "Class NodeRef field A (clean)");
        assertTrue(
                bundle.contains("aReference: Optional[test_generated_syntax_metadata_A] = Field(None, description='')"),
                "Class NodeRef field ref (clean)");

        // Phase 2: Delayed Update
        assertTrue(bundle.contains(
                "test_generated_syntax_metadata_NodeRef.__annotations__[\"typeA\"] = Optional[Annotated[test_generated_syntax_metadata_A, test_generated_syntax_metadata_A.serializer(), test_generated_syntax_metadata_A.validator()]]"),
                "NodeRef typeA delayed update");
        assertTrue(bundle.contains(
                "test_generated_syntax_metadata_NodeRef.__annotations__[\"aReference\"] = Optional[Annotated[test_generated_syntax_metadata_A, test_generated_syntax_metadata_A.serializer(), test_generated_syntax_metadata_A.validator(('@key', '@key:external', '@ref', '@ref:external'))]]"),
                "NodeRef aReference delayed update");

        // Phase 3: Rebuild
        assertTrue(bundle.contains("test_generated_syntax_metadata_NodeRef.model_rebuild()"), "NodeRef rebuild");
    }

    @Test
    public void testExpectedBundleRoot() {
        Map<String, CharSequence> python = getPython();
        String bundle = python.get("src/test/_bundle.py").toString();

        // Phase 1: Clean Body
        assertTrue(bundle.contains("class test_generated_syntax_metadata_Root(BaseDataClass):"), "Class Root body");
        assertTrue(
                bundle.contains(
                        "nodeRef: Optional[test_generated_syntax_metadata_NodeRef] = Field(None, description='')"),
                "Class Root field nodeRef (clean)");
        assertTrue(bundle.contains(
                "attributeRef: Optional[test_generated_syntax_metadata_AttributeRef] = Field(None, description='')"),
                "Class Root field attributeRef (clean)");

        // Phase 2: Delayed Update
        assertTrue(bundle.contains(
                "test_generated_syntax_metadata_Root.__annotations__[\"nodeRef\"] = Optional[Annotated[test_generated_syntax_metadata_NodeRef, test_generated_syntax_metadata_NodeRef.serializer(), test_generated_syntax_metadata_NodeRef.validator()]]"),
                "Root nodeRef delayed update");
        assertTrue(bundle.contains(
                "test_generated_syntax_metadata_Root.__annotations__[\"attributeRef\"] = Optional[Annotated[test_generated_syntax_metadata_AttributeRef, test_generated_syntax_metadata_AttributeRef.serializer(), test_generated_syntax_metadata_AttributeRef.validator()]]"),
                "Root attributeRef delayed update");

        // Phase 3: Rebuild
        assertTrue(bundle.contains("test_generated_syntax_metadata_Root.model_rebuild()"), "Root rebuild");
    }

    @Test
    public void testExpectedBundleScheme() {
        Map<String, CharSequence> python = getPython();
        String bundle = python.get("src/test/_bundle.py").toString();

        assertTrue(bundle.contains("class test_generated_syntax_metadata_SchemeTest(BaseDataClass):"),
                "Class SchemeTest body");
        assertTrue(bundle.contains("_ALLOWED_METADATA = {'@scheme'}"), "Class SchemeTest metadata");
        assertTrue(bundle.contains("a: str = Field(..., description='')"), "Class SchemeTest field");
    }
}
