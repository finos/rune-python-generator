package com.regnosys.rosetta.generator.python.expressions;

import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.regnosys.rosetta.generator.python.PythonGeneratorTestUtils;
import com.regnosys.rosetta.tests.RosettaInjectorProvider;

import jakarta.inject.Inject;

/**
 * Every element of this test needs to check the entire generated Python.
 * This class focuses on Rosetta alias/shortcut logic.
 */
@ExtendWith(InjectionExtension.class)
@InjectWith(RosettaInjectorProvider.class)
public class RosettaShortcutTest {

    /**
     * Test utils for generating Python.
     */
    @Inject
    private PythonGeneratorTestUtils testUtils;

    /**
     * Test case for function alias.
     */
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
                        def com_rosetta_test_model_UseShortcut(val: int) -> int:
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
