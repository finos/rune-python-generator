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
public class RosettaJoinOperationTest {

    @Inject
    private PythonGeneratorTestUtils testUtils;

    @Test
    public void testJoinOperation() {
        String generatedPython = testUtils.generatePythonFromString("""
                type TestJoin:
                    field1 string (1..*)
                    delimiter string (1..1)
                    condition JoinCheck:
                        field1 join delimiter = "A,B"
                """).toString();

        testUtils.assertGeneratedContainsExpectedString(generatedPython,
                """
                        class com_rosetta_test_model_TestJoin(BaseDataClass):
                            _FQRTN = 'com.rosetta.test.model.TestJoin'
                            field1: list[str] = Field(..., description='', min_length=1)
                            delimiter: str = Field(..., description='')

                            @rune_condition
                            def condition_0_JoinCheck(self):
                                item = self
                                return rune_all_elements(rune_resolve_attr(self, "delimiter").join(rune_resolve_attr(self, "field1")), "=", "A,B")""");
    }
}
