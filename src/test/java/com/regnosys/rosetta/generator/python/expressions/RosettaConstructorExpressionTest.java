package com.regnosys.rosetta.generator.python.expressions;

import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.regnosys.rosetta.generator.python.PythonGeneratorTestUtils;
import com.regnosys.rosetta.tests.RosettaInjectorProvider;

import jakarta.inject.Inject;

@ExtendWith(InjectionExtension.class)
@InjectWith(RosettaInjectorProvider.class)
public class RosettaConstructorExpressionTest {

    /**
     * Test utils for generating Python.
     */
    @Inject
    private PythonGeneratorTestUtils testUtils;

    /**
     * Test case for constructor expression.
     */
    @Test
    public void testConstructorExpression() {
        String generatedPython = testUtils.generatePythonFromString("""
                type Foo:
                    a int (1..1)
                    b int (1..1)

                type TestConst:
                    f Foo (1..1)
                    condition ConstCheck:
                        f = Foo { a: 1, b: 2 }
                """).toString();

        testUtils.assertGeneratedContainsExpectedString(generatedPython,
                """
                        class com_rosetta_test_model_TestConst(BaseDataClass):
                            _FQRTN = 'com.rosetta.test.model.TestConst'
                            f: com_rosetta_test_model_Foo = Field(..., description='')

                            @rune_condition
                            def condition_0_ConstCheck(self):
                                item = self
                                return rune_all_elements(rune_resolve_attr(self, "f"), "=", com_rosetta_test_model_Foo(a=1, b=2))

                        # Phase 2: Delayed Annotation Updates
                        com_rosetta_test_model_TestConst.__annotations__["f"] = Annotated[com_rosetta_test_model_Foo, com_rosetta_test_model_Foo.serializer(), com_rosetta_test_model_Foo.validator()]

                        # Phase 3: Rebuild
                        com_rosetta_test_model_TestConst.model_rebuild()""");
    }
}
