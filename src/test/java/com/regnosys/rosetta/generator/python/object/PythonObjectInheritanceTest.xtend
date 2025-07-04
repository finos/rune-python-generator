package com.regnosys.rosetta.generator.python.object

import jakarta.inject.Inject
import com.regnosys.rosetta.tests.RosettaInjectorProvider
import com.regnosys.rosetta.generator.python.PythonGeneratorTestUtils
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.extensions.InjectionExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.^extension.ExtendWith

@ExtendWith(InjectionExtension)
@InjectWith(RosettaInjectorProvider)

class PythonObjectInheritanceTest {

    @Inject PythonGeneratorTestUtils testUtils

    @Test
    def void testPythonClassGenerationWithMultipleParents() {
        val pythonString = testUtils.generatePythonFromString (
            '''
            type D extends C:
                dd string (0..1)
            type B extends A:
                bb string (0..1)
            type C extends B:
                cc string (0..1)
            type A:
                aa string (0..1)
            ''').toString()

        val expectedA =
        '''
        class com_rosetta_test_model_A(BaseDataClass):
            _FQRTN = 'com.rosetta.test.model.A'
            aa: Optional[str] = Field(None, description='')
        '''
        val expectedB =
        '''
        class com_rosetta_test_model_B(com_rosetta_test_model_A):
            _FQRTN = 'com.rosetta.test.model.B'
            bb: Optional[str] = Field(None, description='')
        '''
        val expectedC =
        '''
        class com_rosetta_test_model_C(com_rosetta_test_model_B):
            _FQRTN = 'com.rosetta.test.model.C'
            cc: Optional[str] = Field(None, description='')
        '''
        val expectedD =
        '''
        class com_rosetta_test_model_D(com_rosetta_test_model_C):
            _FQRTN = 'com.rosetta.test.model.D'
            dd: Optional[str] = Field(None, description='')
        '''
        testUtils.assertGeneratedContainsExpectedString(pythonString, expectedA)
        testUtils.assertGeneratedContainsExpectedString(pythonString, expectedB)
        testUtils.assertGeneratedContainsExpectedString(pythonString, expectedC)
        testUtils.assertGeneratedContainsExpectedString(pythonString, expectedD)
    }
    @Test
    def testSuperClasses() {
        val pythonString = testUtils.generatePythonFromString (
            '''
            namespace test

            type Foo extends Bar:

            type Bar extends Baz:

            type Baz:
            
        ''').toString()

        val expectedBaz =
        '''
        class test_Baz(BaseDataClass):
            _FQRTN = 'test.Baz'
            pass
        '''

        val expectedBar =
        '''
        class test_Bar(test_Baz):
            _FQRTN = 'test.Bar'
            pass
        '''

        val expectedFoo =
        '''
        class test_Foo(test_Bar):
            _FQRTN = 'test.Foo'
            pass
        '''

        testUtils.assertGeneratedContainsExpectedString(pythonString, expectedBaz)
        testUtils.assertGeneratedContainsExpectedString(pythonString, expectedBar)
        testUtils.assertGeneratedContainsExpectedString(pythonString, expectedFoo)
    }

    @Test
    def testEnumValue() {
        val pythonString = testUtils.generatePythonFromString (
        '''
            namespace test
            version "1.2.3"

            enum Foo:
                foo0 foo1

            enum Bar extends Foo:
                bar

            enum Baz extends Bar:
                baz
        ''').toString()

        val expectedBar=
        '''
        class Bar(rune.runtime.metadata.EnumWithMetaMixin, Enum):
            BAR = "bar"
            FOO_0 = "foo0"
            FOO_1 = "foo1"
        '''

        val expectedBaz=
        '''
        class Baz(rune.runtime.metadata.EnumWithMetaMixin, Enum):
            BAR = "bar"
            BAZ = "baz"
            FOO_0 = "foo0"
            FOO_1 = "foo1"
        '''

        val expectedFoo=
        '''
        class Foo(rune.runtime.metadata.EnumWithMetaMixin, Enum):
            FOO_0 = "foo0"
            FOO_1 = "foo1"
        '''

        testUtils.assertGeneratedContainsExpectedString(pythonString, expectedBar)
        testUtils.assertGeneratedContainsExpectedString(pythonString, expectedBaz)
        testUtils.assertGeneratedContainsExpectedString(pythonString, expectedFoo)
    }    
}