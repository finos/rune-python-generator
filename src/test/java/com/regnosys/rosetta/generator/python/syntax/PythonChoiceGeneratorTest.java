package com.regnosys.rosetta.generator.python.syntax;

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
@SuppressWarnings("LineLength")
public class PythonChoiceGeneratorTest {

    /**
     * Test utils for generating Python.
     */
    @Inject
    private PythonGeneratorTestUtils testUtils;

    /**
     * Test case for generating choice.
     * Choice is acyclic — standalone. The class is written directly to
     * Choice.py; there is no proxy stub and no bundle entry.
     */
    @Test
    public void testGeneration() {
        Map<String, CharSequence> python = testUtils.generatePythonFromString(
            """
            namespace test.generated_syntax.semantic : <"generate Python unit tests from Rosetta.">

            type Choice:
                intType int (0..1)
                stringType string (0..1)
                condition Choice: one-of
            """);

        // Standalone file contains the class directly (not a proxy stub)
        String choicePython = python.get("src/test/generated_syntax/semantic/Choice.py").toString();
        String expectedChoice = """
            class Choice(BaseDataClass):
                intType: Optional[int] = Field(None, description='')
                stringType: Optional[str] = Field(None, description='')

                @rune_condition
                def condition_0_Choice(self):
                    item = self
                    return rune_check_one_of(self, 'intType', 'stringType', necessity=True)
            """;
        testUtils.assertGeneratedContainsExpectedString(choicePython, expectedChoice);
    }
}
