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
        String generatedBundle = python.get("src/test/_bundle.py").toString();
        String expectedA = """
                class test_generated_syntax_metadata_A(BaseDataClass):
                    _ALLOWED_METADATA = {'@key', '@key:external'}
                    _FQRTN = 'test.generated_syntax.metadata.A'
                    fieldA: str = Field(..., description='')
                """;
        testUtils.assertGeneratedContainsExpectedString(generatedBundle, expectedA);
    }

    @Test
    public void testExpectedBundleAttributeRef() {
        Map<String, CharSequence> python = getPython();
        String generatedBundle = python.get("src/test/_bundle.py").toString();
        String expectedAttributeRef = """
                class test_generated_syntax_metadata_AttributeRef(BaseDataClass):
                    _FQRTN = 'test.generated_syntax.metadata.AttributeRef'
                    dateField: Optional[Annotated[DateWithMeta, DateWithMeta.serializer(), DateWithMeta.validator(('@key', '@key:external'))]] = Field(None, description='')
                    dateReference: Optional[Annotated[DateWithMeta, DateWithMeta.serializer(), DateWithMeta.validator(('@ref', '@ref:external'))]] = Field(None, description='')

                    _KEY_REF_CONSTRAINTS = {
                        'dateField': {'@key', '@key:external'},
                        'dateReference': {'@ref', '@ref:external'}
                    }
                """;
        testUtils.assertGeneratedContainsExpectedString(generatedBundle, expectedAttributeRef);
    }

    @Test
    public void testExpectedBundleNodeRef() {
        Map<String, CharSequence> python = getPython();
        String generatedBundle = python.get("src/test/_bundle.py").toString();
        String expectedNodeRef = """
                class test_generated_syntax_metadata_NodeRef(BaseDataClass):
                    _FQRTN = 'test.generated_syntax.metadata.NodeRef'
                    typeA: Optional[Annotated[test_generated_syntax_metadata_A, test_generated_syntax_metadata_A.serializer(), test_generated_syntax_metadata_A.validator()]] = Field(None, description='')
                    aReference: Optional[Annotated[test_generated_syntax_metadata_A, test_generated_syntax_metadata_A.serializer(), test_generated_syntax_metadata_A.validator(('@key', '@key:external', '@ref', '@ref:external'))]] = Field(None, description='')

                    _KEY_REF_CONSTRAINTS = {
                        'aReference': {'@key', '@key:external', '@ref', '@ref:external'}
                    }
                """;
        testUtils.assertGeneratedContainsExpectedString(generatedBundle, expectedNodeRef);
    }

    @Test
    public void testExpectedBundleRoot() {
        Map<String, CharSequence> python = getPython();
        String generatedBundle = python.get("src/test/_bundle.py").toString();
        String expectedRoot = """
                class test_generated_syntax_metadata_Root(BaseDataClass):
                    _FQRTN = 'test.generated_syntax.metadata.Root'
                    nodeRef: Optional[Annotated[test_generated_syntax_metadata_NodeRef, test_generated_syntax_metadata_NodeRef.serializer(), test_generated_syntax_metadata_NodeRef.validator()]] = Field(None, description='')
                    attributeRef: Optional[Annotated[test_generated_syntax_metadata_AttributeRef, test_generated_syntax_metadata_AttributeRef.serializer(), test_generated_syntax_metadata_AttributeRef.validator()]] = Field(None, description='')
                """;
        testUtils.assertGeneratedContainsExpectedString(generatedBundle, expectedRoot);
    }

    @Test
    public void testExpectedBundleScheme() {
        Map<String, CharSequence> python = getPython();
        String generatedBundle = python.get("src/test/_bundle.py").toString();
        String expectedScheme = """
                class test_generated_syntax_metadata_SchemeTest(BaseDataClass):
                    _ALLOWED_METADATA = {'@scheme'}
                    _FQRTN = 'test.generated_syntax.metadata.SchemeTest'
                    a: str = Field(..., description='')
                """;
        testUtils.assertGeneratedContainsExpectedString(generatedBundle, expectedScheme);
    }
}
