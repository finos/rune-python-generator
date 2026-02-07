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
public class RosettaConversionTest {

    @Inject
    private PythonGeneratorTestUtils testUtils;

    @Test
    public void testConversions() {
        String generatedPython = testUtils.generatePythonFromString("""
                type TestConv:
                    val int (1..1)
                    s string (1..1)
                    condition ConvCheck:
                        val to-string = "1" and
                        s to-int = 1
                """).toString();

        testUtils.assertGeneratedContainsExpectedString(generatedPython,
                """
                        class com_rosetta_test_model_TestConv(BaseDataClass):
                            _FQRTN = 'com.rosetta.test.model.TestConv'
                            val: int = Field(..., description='')
                            s: str = Field(..., description='')

                            @rune_condition
                            def condition_0_ConvCheck(self):
                                item = self
                                return (rune_all_elements(rune_str(rune_resolve_attr(self, "val")), "=", "1") and rune_all_elements(int(rune_resolve_attr(self, "s")), "=", 1))""");
    }
}
