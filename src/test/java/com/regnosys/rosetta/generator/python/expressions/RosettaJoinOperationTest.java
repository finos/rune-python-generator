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
@SuppressWarnings("LineLength")
public class RosettaJoinOperationTest {

    /**
     * Test utils for generating Python.
     */
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
                        class TestJoin(BaseDataClass):
                            field1: list[str | None] = Field(..., description='', min_length=1)
                            delimiter: str = Field(..., description='')

                            @rune_condition
                            def condition_0_JoinCheck(self):
                                item = self
                                return rune_all_elements((lambda items, sep: (sep or "").join(x for x in (items or []) if x is not None) if items is not None else None)(rune_resolve_attr(self, "field1"), rune_resolve_attr(self, "delimiter")), "=", "A,B")""");
    }
}
