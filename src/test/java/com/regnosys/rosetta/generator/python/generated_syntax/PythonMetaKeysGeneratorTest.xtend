package com.regnosys.rosetta.generator.python.generated_syntax

import com.google.inject.Inject
import com.regnosys.rosetta.tests.RosettaInjectorProvider
import com.regnosys.rosetta.generator.python.PythonGeneratorTestUtils
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.extensions.InjectionExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.^extension.ExtendWith

@ExtendWith(InjectionExtension)
@InjectWith(RosettaInjectorProvider)

class PythonMetaKeysGeneratorTest {

    @Inject PythonGeneratorTestUtils testUtils

    @Test
    def void testGeneration() {
        val python = testUtils.generatePythonFromString (
            '''
            namespace test.generated_syntax.meta_key_ref : <"generate Python unit tests from Rosetta.">

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
            ''')
        // check proxies
        testUtils.assertStringInString(
            python.get("src/test/generated_syntax/meta_key_ref/A.py").toString(),
            '''
            # pylint: disable=unused-import
            from test._bundle import test_generated_syntax_meta_key_ref_A as A

            # EOF''')
        testUtils.assertStringInString(
            python.get("src/test/generated_syntax/meta_key_ref/NodeRef.py").toString(),
            '''
            # pylint: disable=unused-import
            from test._bundle import test_generated_syntax_meta_key_ref_NodeRef as NodeRef

            # EOF''')
        testUtils.assertStringInString(
            python.get("src/test/generated_syntax/meta_key_ref/AttributeRef.py").toString(),
            '''
            # pylint: disable=unused-import
            from test._bundle import test_generated_syntax_meta_key_ref_AttributeRef as AttributeRef

            # EOF''')
        testUtils.assertStringInString(
            python.get("src/test/generated_syntax/meta_key_ref/Root.py").toString(),
            '''
            # pylint: disable=unused-import
            from test._bundle import test_generated_syntax_meta_key_ref_Root as Root

            # EOF''')
        val generatedBundle = python.get("src/test/_bundle.py").toString()
        val expectedA = 
        '''
        class test_generated_syntax_meta_key_ref_A(BaseDataClass):
            _FQRTN = 'test.generated_syntax.meta_key_ref.A'
            fieldA: str = Field(..., description='')
        '''
        testUtils.assertStringInString(generatedBundle, expectedA)
        val expectedAttributeRef = 
        '''
        class test_generated_syntax_meta_key_ref_AttributeRef(BaseDataClass):
            _FQRTN = 'test.generated_syntax.meta_key_ref.AttributeRef'
            dateField: Optional[Annotated[DateWithMeta, DateWithMeta.serializer(), DateWithMeta.validator(('@key', '@key:external'))]] = Field(None, description='')
            dateReference: Optional[Annotated[DateWithMeta, DateWithMeta.serializer(), DateWithMeta.validator(('@ref', '@ref:external'))]] = Field(None, description='')
            
            _KEY_REF_CONSTRAINTS = {
                'dateField': {'@key', '@key:external'},
                'dateReference': {'@ref', '@ref:external'}
            }
        '''
        testUtils.assertStringInString(generatedBundle, expectedAttributeRef)
        val expectedNodeRef = 
        '''
        class test_generated_syntax_meta_key_ref_NodeRef(BaseDataClass):
            _FQRTN = 'test.generated_syntax.meta_key_ref.NodeRef'
            typeA: Optional[Annotated[test_generated_syntax_meta_key_ref_A, test_generated_syntax_meta_key_ref_A.serializer(), test_generated_syntax_meta_key_ref_A.validator(('@key', '@key:external'))]] = Field(None, description='')
            aReference: Optional[Annotated[test_generated_syntax_meta_key_ref_A, test_generated_syntax_meta_key_ref_A.serializer(), test_generated_syntax_meta_key_ref_A.validator(('@key', '@key:external', '@ref', '@ref:external'))]] = Field(None, description='')
            
            _KEY_REF_CONSTRAINTS = {
                'aReference': {'@key', '@key:external', '@ref', '@ref:external'},
                'typeA': {'@key', '@key:external'}
            }
        '''
        testUtils.assertStringInString(generatedBundle, expectedNodeRef)
        val expectedRoot = 
        '''
        class test_generated_syntax_meta_key_ref_Root(BaseDataClass):
            _FQRTN = 'test.generated_syntax.meta_key_ref.Root'
            nodeRef: Optional[Annotated[test_generated_syntax_meta_key_ref_NodeRef, test_generated_syntax_meta_key_ref_NodeRef.serializer(), test_generated_syntax_meta_key_ref_NodeRef.validator()]] = Field(None, description='')
            attributeRef: Optional[Annotated[test_generated_syntax_meta_key_ref_AttributeRef, test_generated_syntax_meta_key_ref_AttributeRef.serializer(), test_generated_syntax_meta_key_ref_AttributeRef.validator()]] = Field(None, description='')
        '''
        testUtils.assertStringInString(generatedBundle, expectedRoot)
    }
}