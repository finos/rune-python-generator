package com.regnosys.rosetta.generator.python.functions;

import com.regnosys.rosetta.generator.python.PythonGeneratorTestUtils;
import com.regnosys.rosetta.tests.RosettaInjectorProvider;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import jakarta.inject.Inject;
import java.util.Map;

@ExtendWith(InjectionExtension.class)
@InjectWith(RosettaInjectorProvider.class)
public class PythonFunctionListTest {

    /**
     * Test utils for generating Python code.
     */
    @Inject
    private PythonGeneratorTestUtils testUtils;

    /**
     * Test case for function dependency order.
     */
    @Test
    public void testFunctionList() {
        String rosetta = """
                func TestListFunction:
                    inputs:
                        list1 number (1..*)
                        multiplier number (1..1)
                    output:
                        result number (1..*)
                    set result:
                        list1 extract [ item * multiplier ]
                """;
        Map<String, CharSequence> gf = testUtils.generatePythonFromString(rosetta);

        String bundle = gf.get("src/com/_bundle.py").toString();

        testUtils.assertGeneratedContainsExpectedString(bundle, "item * rune_resolve_attr(self, \"multiplier\")");
    }
}
