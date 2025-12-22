package com.regnosys.rosetta.generator.python.generated_syntax

import jakarta.inject.Inject
import com.regnosys.rosetta.tests.RosettaInjectorProvider
import com.regnosys.rosetta.generator.python.PythonGeneratorTestUtils
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.extensions.InjectionExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.^extension.ExtendWith
import static org.junit.Assert.assertTrue;
import java.util.Map

@ExtendWith(InjectionExtension)
@InjectWith(RosettaInjectorProvider)
class PythonMetaDataGeneratorTest {

    @Inject PythonGeneratorTestUtils testUtils

    var Map<String, CharSequence> python = null

    def Map<String, CharSequence> getPython() {
        if (python === null) {
            python = testUtils.generatePythonFromString(
                '''
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
                ''')
        }
        return python
    }

    @Test
    def void testAProxy() {
        val python = getPython()
        testUtils.assertGeneratedContainsExpectedString(
            python.get("src/test/generated_syntax/metadata/A.py").toString(),
            '''
            # pylint: disable=unused-import
            from test._bundle import test_generated_syntax_metadata_A as A

            # EOF''')
    }

    @Test
    def void testNodeRefProxy() {
        val python = getPython()
        testUtils.assertGeneratedContainsExpectedString(
            python.get("src/test/generated_syntax/metadata/NodeRef.py").toString(),
            '''
            # pylint: disable=unused-import
            from test._bundle import test_generated_syntax_metadata_NodeRef as NodeRef

            # EOF''')
    }

    @Test
    def void testAttributeRefProxy() {
        val python = getPython()
        testUtils.assertGeneratedContainsExpectedString(
            python.get("src/test/generated_syntax/metadata/AttributeRef.py").toString(),
            '''
            # pylint: disable=unused-import
            from test._bundle import test_generated_syntax_metadata_AttributeRef as AttributeRef

            # EOF''')
    }

    @Test
    def void testRootProxy() {
        val python = getPython()
        testUtils.assertGeneratedContainsExpectedString(
            python.get("src/test/generated_syntax/metadata/Root.py").toString(),
            '''
            # pylint: disable=unused-import
            from test._bundle import test_generated_syntax_metadata_Root as Root

            # EOF''')
    }

    @Test
    def void testBundleExists() {
        val python = getPython()
        assertTrue("The bundle should be in the generated Python", python.containsKey("src/test/_bundle.py"))
    }

    @Test
    def void testExpectedBundleA() {
        val python = getPython()
        val generatedBundle = python.get("src/test/_bundle.py").toString()
        val expectedA = 
        '''
        class test_generated_syntax_metadata_A(BaseDataClass):
            _ALLOWED_METADATA = {'@key', '@key:external'}
            _FQRTN = 'test.generated_syntax.metadata.A'
            fieldA: str = Field(..., description='')
        '''
        testUtils.assertGeneratedContainsExpectedString(generatedBundle, expectedA)
    }

    @Test
    def void testExpectedBundleAttributeRef() {
        val python = getPython()
        val generatedBundle = python.get("src/test/_bundle.py").toString()
        val expectedAttributeRef = 
        '''
        class test_generated_syntax_metadata_AttributeRef(BaseDataClass):
            _FQRTN = 'test.generated_syntax.metadata.AttributeRef'
            dateField: Optional[Annotated[DateWithMeta, DateWithMeta.serializer(), DateWithMeta.validator(('@key', '@key:external'))]] = Field(None, description='')
            dateReference: Optional[Annotated[DateWithMeta, DateWithMeta.serializer(), DateWithMeta.validator(('@ref', '@ref:external'))]] = Field(None, description='')
            
            _KEY_REF_CONSTRAINTS = {
                'dateField': {'@key', '@key:external'},
                'dateReference': {'@ref', '@ref:external'}
            }
        '''
        testUtils.assertGeneratedContainsExpectedString(generatedBundle, expectedAttributeRef)
    }

    @Test
    def void testExpectedBundleNodeRef() {
        val python = getPython()
        val generatedBundle = python.get("src/test/_bundle.py").toString()
        val expectedNodeRef = 
        '''
        class test_generated_syntax_metadata_NodeRef(BaseDataClass):
            _FQRTN = 'test.generated_syntax.metadata.NodeRef'
            typeA: Optional[Annotated[test_generated_syntax_metadata_A, test_generated_syntax_metadata_A.serializer(), test_generated_syntax_metadata_A.validator()]] = Field(None, description='')
            aReference: Optional[Annotated[test_generated_syntax_metadata_A, test_generated_syntax_metadata_A.serializer(), test_generated_syntax_metadata_A.validator(('@key', '@key:external', '@ref', '@ref:external'))]] = Field(None, description='')
            
            _KEY_REF_CONSTRAINTS = {
                'aReference': {'@key', '@key:external', '@ref', '@ref:external'}
            }
        '''
        testUtils.assertGeneratedContainsExpectedString(generatedBundle, expectedNodeRef)
    }

    @Test
    def void testExpectedBundleRoot() {
        val python = getPython()
        val generatedBundle = python.get("src/test/_bundle.py").toString()
        val expectedRoot = 
        '''
        class test_generated_syntax_metadata_Root(BaseDataClass):
            _FQRTN = 'test.generated_syntax.metadata.Root'
            nodeRef: Optional[Annotated[test_generated_syntax_metadata_NodeRef, test_generated_syntax_metadata_NodeRef.serializer(), test_generated_syntax_metadata_NodeRef.validator()]] = Field(None, description='')
            attributeRef: Optional[Annotated[test_generated_syntax_metadata_AttributeRef, test_generated_syntax_metadata_AttributeRef.serializer(), test_generated_syntax_metadata_AttributeRef.validator()]] = Field(None, description='')
        '''
        testUtils.assertGeneratedContainsExpectedString(generatedBundle, expectedRoot)
    }

    @Test
    def void testExpectedBundleScheme() {
        val python = getPython()
        val generatedBundle = python.get("src/test/_bundle.py").toString()
        val expectedScheme = 
        '''
        class test_generated_syntax_metadata_SchemeTest(BaseDataClass):
            _ALLOWED_METADATA = {'@scheme'}
            _FQRTN = 'test.generated_syntax.metadata.SchemeTest'
            a: str = Field(..., description='')
        '''
        testUtils.assertGeneratedContainsExpectedString(generatedBundle, expectedScheme)
    }
}