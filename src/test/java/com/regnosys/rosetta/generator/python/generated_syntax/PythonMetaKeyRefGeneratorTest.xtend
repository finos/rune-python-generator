package com.regnosys.rosetta.generator.python.generated_syntax

import jakarta.inject.Inject
import com.regnosys.rosetta.tests.RosettaInjectorProvider
import com.regnosys.rosetta.generator.python.PythonGeneratorTestUtils
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.extensions.InjectionExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.^extension.ExtendWith
import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(InjectionExtension)
@InjectWith(RosettaInjectorProvider)

class PythonMetaKeyRefGeneratorTest {

    @Inject PythonGeneratorTestUtils testUtils

    @Test
    def void testGeneration() {
        val python = testUtils.generatePythonFromString (
            '''
            namespace test.generated_syntax.meta_key_ref : <"generate Python unit tests from Rosetta.">

            type KeyRef:
                fieldA string (1..1)
                    [metadata id]
                    [metadata reference]

            type ScopedKeyRef:
                fieldA string (1..1)
                    [metadata location]
                    [metadata address]
            ''')
        // check proxies
        val proxyKeyRef = python.get("src/test/generated_syntax/meta_key_ref/KeyRef.py")
        if (proxyKeyRef === null) {
            fail ('src/test/generated_syntax/meta_key_ref/KeyRef.py was not found')
        }
        testUtils.assertGeneratedContainsExpectedString(
            proxyKeyRef.toString(),
            '''
            # pylint: disable=unused-import
            from test._bundle import test_generated_syntax_meta_key_ref_KeyRef as KeyRef

            # EOF''')
        val proxyScopedKeyRef = python.get("src/test/generated_syntax/meta_key_ref/ScopedKeyRef.py")
        if (proxyScopedKeyRef === null) {
            fail ('src/test/generated_syntax/meta_key_ref/proxyScopedKeyRef.py was not found')
        }
        testUtils.assertGeneratedContainsExpectedString(
            python.get("src/test/generated_syntax/meta_key_ref/ScopedKeyRef.py").toString(),
            '''
            # pylint: disable=unused-import
            from test._bundle import test_generated_syntax_meta_key_ref_ScopedKeyRef as ScopedKeyRef

            # EOF''')
        val generatedBundle = python.get("src/test/_bundle.py").toString()
        if (generatedBundle === null) {
            fail ('src/test/_bundle.py was not found')
        }
        val expectedKeyRef = 
        '''
        class test_generated_syntax_meta_key_ref_KeyRef(BaseDataClass):
            _FQRTN = 'test.generated_syntax.meta_key_ref.KeyRef'
            fieldA: Annotated[StrWithMeta, StrWithMeta.serializer(), StrWithMeta.validator(('@ref', '@ref:external', '@key', '@key:external'))] = Field(..., description='')
            
            _KEY_REF_CONSTRAINTS = {
                'fieldA': {'@ref', '@ref:external', '@key', '@key:external'}
            }
        '''
        testUtils.assertGeneratedContainsExpectedString(generatedBundle, expectedKeyRef)
        val expectedScopedKeyRef = 
        '''
        class test_generated_syntax_meta_key_ref_ScopedKeyRef(BaseDataClass):
            _FQRTN = 'test.generated_syntax.meta_key_ref.ScopedKeyRef'
            fieldA: Annotated[StrWithMeta, StrWithMeta.serializer(), StrWithMeta.validator(('@key:scoped', '@ref:scoped'))] = Field(..., description='')
            
            _KEY_REF_CONSTRAINTS = {
                'fieldA': {'@key:scoped', '@ref:scoped'}
            }
        '''
        testUtils.assertGeneratedContainsExpectedString(generatedBundle, expectedScopedKeyRef)
    }
}