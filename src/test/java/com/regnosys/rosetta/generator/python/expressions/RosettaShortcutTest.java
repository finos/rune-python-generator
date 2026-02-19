package com.regnosys.rosetta.generator.python.expressions;

import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.regnosys.rosetta.generator.python.PythonGeneratorTestUtils;
import com.regnosys.rosetta.tests.RosettaInjectorProvider;

import jakarta.inject.Inject;

@Disabled("Functions are being phased out in tests.")
@ExtendWith(InjectionExtension.class)
@InjectWith(RosettaInjectorProvider.class)
public class RosettaShortcutTest {

    @Inject
    private PythonGeneratorTestUtils testUtils;

    @Test
    public void testFunctionAlias() {
        testUtils.assertBundleContainsExpectedString("""
                func UseShortcut:
                    inputs: val int (1..1)
                    output: res int (1..1)
                    alias MyShortcut: val + 5
                    set res: MyShortcut * 2
                """,
                """
                        @replaceable
                        @validate_call
                        def com_rosetta_test_model_functions_UseShortcut(val: int) -> int:
                            \"\"\"

                            Parameters
                            ----------
                            val : int

                            Returns
                            -------
                            res : int

                            \"\"\"
                            self = inspect.currentframe()


                            MyShortcut = (rune_resolve_attr(self, "val") + 5)
                            res = (rune_resolve_attr(self, "MyShortcut") * 2)


                            return res""");
    }
}
