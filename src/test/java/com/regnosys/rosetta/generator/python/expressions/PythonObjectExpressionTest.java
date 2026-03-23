package com.regnosys.rosetta.generator.python.expressions;

import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.regnosys.rosetta.generator.python.PythonGeneratorTestUtils;
import com.regnosys.rosetta.tests.RosettaInjectorProvider;

import jakarta.inject.Inject;
import java.util.Map;

/**
 * Every element of this test needs to check the entire generated Python.
 * This class focuses on Rosetta constructor expressions, as-key operations,
 * and alias/shortcut logic.
 */
@ExtendWith(InjectionExtension.class)
@InjectWith(RosettaInjectorProvider.class)
@SuppressWarnings("LineLength")
public class PythonObjectExpressionTest {

    /**
     * Test utils for generating Python.
     */
    @Inject
    private PythonGeneratorTestUtils testUtils;

    // -------------------------------------------------------------------------
    // From RosettaConstructorExpressionTest
    // -------------------------------------------------------------------------

    /**
     * Test case for constructor expression.
     */
    @Test
    public void testConstructorExpression() {
        String generatedPython = testUtils.generatePythonFromString("""
                type Foo:
                    a int (1..1)
                    b int (1..1)

                type TestConst:
                    f Foo (1..1)
                    condition ConstCheck:
                        f = Foo { a: 1, b: 2 }
                """).toString();

        // TestConst.py uses short name for the field type but flattened name in condition expressions
        testUtils.assertGeneratedContainsExpectedString(generatedPython,
                "class TestConst(BaseDataClass):");
        testUtils.assertGeneratedContainsExpectedString(generatedPython,
                "f: Foo = Field(..., description='')");
        testUtils.assertGeneratedContainsExpectedString(generatedPython,
                "return rune_all_elements(rune_resolve_attr(self, \"f\"), \"=\", com_rosetta_test_model_Foo(a=1, b=2))");
    }

    // -------------------------------------------------------------------------
    // From RosettaAsKeyOperationTest
    // -------------------------------------------------------------------------

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
        String generatedPython = gf.get("src/com/rosetta/test/model/functions/TestAsKey.py").toString();
        testUtils.assertGeneratedContainsExpectedString(generatedPython, "bar = ObjectBuilder(Bar)");
        testUtils.assertGeneratedContainsExpectedString(generatedPython,
                "bar.field = Reference(rune_resolve_attr(self, \"val\"))");
    }

    // -------------------------------------------------------------------------
    // From RosettaShortcutTest
    // -------------------------------------------------------------------------

    /**
     * Test case for function alias.
     */
    @Test
    public void testFunctionAlias() {
        testUtils.assertBundleContainsExpectedString("""
                func UseShortcut:
                    inputs: val int (1..1)
                    output: result int (1..1)
                    alias MyShortcut: val + 5
                    set result: MyShortcut * 2
                """,
                """
                @replaceable
                @validate_call
                def UseShortcut(val: int) -> int:
                    \"\"\"

                    Parameters
                    ----------
                    val : int

                    Returns
                    -------
                    result : int

                    \"\"\"
                    self = inspect.currentframe()

                    val = rune_cow(val)


                    MyShortcut = (rune_resolve_attr(self, "val") + 5)
                    result = (rune_resolve_attr(self, "MyShortcut") * 2)


                    return result""");
    }
}
