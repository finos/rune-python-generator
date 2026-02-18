package com.regnosys.rosetta.generator.python.expressions;

import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.regnosys.rosetta.generator.python.PythonGeneratorTestUtils;
import com.regnosys.rosetta.tests.RosettaInjectorProvider;

import jakarta.inject.Inject;
import java.util.Map;

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
        Map<String, CharSequence> gf = testUtils.generatePythonFromString("""
                type Bar:
                    field string (0..1)
                        [metadata reference]

                func TestAsKey:
                    inputs: val string (1..1)
                    output: bar Bar (1..1)
                    set bar -> field:
                        val as-key
                """);
        String generated = gf.get("src/com/_bundle.py").toString();
        testUtils.assertGeneratedContainsExpectedString(generated, "bar = Draft(com_rosetta_test_model_Bar)");
        testUtils.assertGeneratedContainsExpectedString(generated,
                "bar.field = {rune_resolve_attr(self, \"val\"): True}");
        testUtils.assertGeneratedContainsExpectedString(generated, "bar = bar.to_model()");
    }
}
