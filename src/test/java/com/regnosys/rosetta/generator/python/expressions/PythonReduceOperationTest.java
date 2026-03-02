package com.regnosys.rosetta.generator.python.expressions;

import jakarta.inject.Inject;
import com.regnosys.rosetta.tests.RosettaInjectorProvider;
import com.regnosys.rosetta.generator.python.PythonGeneratorTestUtils;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import java.util.Map;

@ExtendWith(InjectionExtension.class)
@InjectWith(RosettaInjectorProvider.class)
public class PythonReduceOperationTest {

    @Inject
    private PythonGeneratorTestUtils testUtils;

    @Test
    public void testReduceOperation() {
        Map<String, CharSequence> gf = testUtils.generatePythonFromString(
                """
                namespace com.test
                
                func SumList:
                    inputs:
                        items int (0..*)
                    output:
                        result int (1..1)
                    set result:
                        items
                        reduce a, b [ a + b ]
                """);

        String bundle = gf.get("src/com/_bundle.py").toString();
        
        testUtils.assertGeneratedContainsExpectedString(bundle, "functools.reduce(lambda a, b: (a + b), rune_resolve_attr(self, \"items\"))");
    }
}
