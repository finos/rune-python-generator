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
public class RosettaSwitchExpressionTest {

    @Inject
    private PythonGeneratorTestUtils testUtils;

    @Test
    public void testGenerateSwitch() {
        testUtils.assertBundleContainsExpectedString("""
                type FooTest:
                    a int (1..1) <"Test field a">
                    condition Test:
                        a switch
                            1 then True,
                            2 then True,
                            default False
                """,
                """
                        class com_rosetta_test_model_FooTest(BaseDataClass):
                            _FQRTN = 'com.rosetta.test.model.FooTest'
                            a: int = Field(..., description='Test field a')
                            \"""
                            Test field a
                            \"""

                            @rune_condition
                            def condition_0_Test(self):
                                item = self
                                def _then_1():
                                    return True
                                def _then_2():
                                    return True
                                def _then_default():
                                    return False
                                switchAttribute = rune_resolve_attr(self, "a")
                                if switchAttribute == 1:
                                    return _then_1()
                                elif switchAttribute == 2:
                                    return _then_2()
                                else:
                                    return _then_default()
                        """);
    }
}
