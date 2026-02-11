package com.regnosys.rosetta.generator.python.object;

import jakarta.inject.Inject;
import com.regnosys.rosetta.tests.RosettaInjectorProvider;
import com.regnosys.rosetta.generator.python.PythonGeneratorTestUtils;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(InjectionExtension.class)
@InjectWith(RosettaInjectorProvider.class)
public class PythonObjectInheritanceTest {

    /**
     * Test utils for generating Python.
     */
    @Inject
    private PythonGeneratorTestUtils testUtils;

    /**
     * Test case for multiple parents.
     */
    @Test
    public void testPythonClassGenerationWithMultipleParents() {
        String pythonString = testUtils.generatePythonFromString(
                """
                        type D extends C:
                            dd string (0..1)
                        type B extends A:
                            bb string (0..1)
                        type C extends B:
                            cc string (0..1)
                        type A:
                            aa string (0..1)
                        """).toString();

        String expectedA = """
                class com_rosetta_test_model_A(BaseDataClass):
                    _FQRTN = 'com.rosetta.test.model.A'
                    aa: Optional[str] = Field(None, description='')
                """;
        String expectedB = """
                class com_rosetta_test_model_B(com_rosetta_test_model_A):
                    _FQRTN = 'com.rosetta.test.model.B'
                    bb: Optional[str] = Field(None, description='')
                """;
        String expectedC = """
                class com_rosetta_test_model_C(com_rosetta_test_model_B):
                    _FQRTN = 'com.rosetta.test.model.C'
                    cc: Optional[str] = Field(None, description='')
                """;
        String expectedD = """
                class com_rosetta_test_model_D(com_rosetta_test_model_C):
                    _FQRTN = 'com.rosetta.test.model.D'
                    dd: Optional[str] = Field(None, description='')
                """;
        testUtils.assertGeneratedContainsExpectedString(pythonString, expectedA);
        testUtils.assertGeneratedContainsExpectedString(pythonString, expectedB);
        testUtils.assertGeneratedContainsExpectedString(pythonString, expectedC);
        testUtils.assertGeneratedContainsExpectedString(pythonString, expectedD);
    }

    /**
     * Test case for super classes.
     */
    @Test
    public void testSuperClasses() {
        String pythonString = testUtils.generatePythonFromString(
                """
                        namespace test

                        type Foo extends Bar:

                        type Bar extends Baz:

                        type Baz:

                        """).toString();

        String expectedBaz = """
                class test_Baz(BaseDataClass):
                    _FQRTN = 'test.Baz'
                    pass
                """;

        String expectedBar = """
                class test_Bar(test_Baz):
                    _FQRTN = 'test.Bar'
                    pass
                """;

        String expectedFoo = """
                class test_Foo(test_Bar):
                    _FQRTN = 'test.Foo'
                    pass
                """;

        testUtils.assertGeneratedContainsExpectedString(pythonString, expectedBaz);
        testUtils.assertGeneratedContainsExpectedString(pythonString, expectedBar);
        testUtils.assertGeneratedContainsExpectedString(pythonString, expectedFoo);
    }

    /**
     * Test case for enum value.
     */
    @Test
    public void testEnumValue() {
        String pythonString = testUtils.generatePythonFromString(
                """
                        namespace test
                        version "1.2.3"

                        enum Foo:
                            foo0 foo1

                        enum Bar extends Foo:
                            bar

                        enum Baz extends Bar:
                            baz
                        """).toString();

        String expectedBar = """
                class Bar(rune.runtime.metadata.EnumWithMetaMixin, Enum):
                    BAR = "bar"
                    FOO_0 = "foo0"
                    FOO_1 = "foo1"
                """;

        String expectedBaz = """
                class Baz(rune.runtime.metadata.EnumWithMetaMixin, Enum):
                    BAR = "bar"
                    BAZ = "baz"
                    FOO_0 = "foo0"
                    FOO_1 = "foo1"
                """;

        String expectedFoo = """
                class Foo(rune.runtime.metadata.EnumWithMetaMixin, Enum):
                    FOO_0 = "foo0"
                    FOO_1 = "foo1"
                """;

        testUtils.assertGeneratedContainsExpectedString(pythonString, expectedBar);
        testUtils.assertGeneratedContainsExpectedString(pythonString, expectedBaz);
        testUtils.assertGeneratedContainsExpectedString(pythonString, expectedFoo);
    }
}
