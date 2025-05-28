package com.regnosys.rosetta.generator.python.generated_syntax

import jakarta.inject.Inject
import com.regnosys.rosetta.tests.RosettaInjectorProvider
import com.regnosys.rosetta.generator.python.PythonGeneratorTestUtils
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.extensions.InjectionExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.^extension.ExtendWith

@ExtendWith(InjectionExtension)
@InjectWith(RosettaInjectorProvider)

class PythonChoiceGeneratorTest {

    @Inject PythonGeneratorTestUtils testUtils

    @Test
    def void testGeneration () {
        val python = testUtils.generatePythonFromString (
            '''
            namespace test.generated_syntax.semantic : <"generate Python unit tests from Rosetta.">

            type Choice:
                intType int (0..1)
                stringType string (0..1)
                condition Choice: one-of
            ''')
        // check proxies
        testUtils.assertGeneratedContainsExpectedString(
            python.get("src/test/generated_syntax/semantic/Choice.py").toString(),
            '''
            # pylint: disable=unused-import
            from test._bundle import test_generated_syntax_semantic_Choice as Choice

            # EOF''')
        val generatedBundle = python.get("src/test/_bundle.py").toString()
        val expectedChoice = 
        '''
        class test_generated_syntax_semantic_Choice(BaseDataClass):
            _FQRTN = 'test.generated_syntax.semantic.Choice'
            intType: Optional[int] = Field(None, description='')
            stringType: Optional[str] = Field(None, description='')
            
            @rune_condition
            def condition_0_Choice(self):
                item = self
                return rune_check_one_of(self, 'intType', 'stringType', necessity=True)
        '''
        testUtils.assertGeneratedContainsExpectedString(generatedBundle, expectedChoice)
    }
}