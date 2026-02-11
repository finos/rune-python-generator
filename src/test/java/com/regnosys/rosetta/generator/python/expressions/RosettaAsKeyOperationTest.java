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
public class RosettaAsKeyOperationTest {

    /**
     * Test utils for generating Python.
     */
    @Inject
    private PythonGeneratorTestUtils testUtils;

    /**
     * Test case for as-key operation.
     */
    @Test
    public void testAsKeyOperation() {
        testUtils.assertBundleContainsExpectedString("""
                type Bar:
                    field string (0..1)
                        [metadata reference]

                func TestAsKey:
                    inputs: val string (1..1)
                    output: bar Bar (1..1)
                    set bar -> field:
                        val as-key
                """,
                """
                        @replaceable
                        @validate_call
                        def com_rosetta_test_model_functions_TestAsKey(val: str) -> com_rosetta_test_model_Bar:
                            \"\"\"

                            Parameters
                            ----------
                            val : str

                            Returns
                            -------
                            bar : com.rosetta.test.model.Bar

                            \"\"\"
                            self = inspect.currentframe()


                            bar = _get_rune_object('com_rosetta_test_model_Bar', 'field', {rune_resolve_attr(self, "val"): True})


                            return bar""");
    }
}
