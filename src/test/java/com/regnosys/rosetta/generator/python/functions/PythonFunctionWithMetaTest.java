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
@SuppressWarnings("LineLength")
public class PythonFunctionWithMetaTest {

    /**
     * Test utils for generating Python code.
     */
    @Inject
    private PythonGeneratorTestUtils testUtils;

    /**
     * Test case for function with meta.
     */
    @Test
    public void testFunctionWithMeta() {
        Map<String, CharSequence> gf = testUtils.generatePythonFromString(
                """
                        namespace com.test

                        type Foo:
                            [metadata scheme]
                            val string (1..1)

                        func TestWithMeta:
                            inputs:
                                f Foo (1..1)
                            output:
                                res Foo (1..1)
                            set res:
                                f with-meta { scheme: "myScheme" }
                        """);
        String generated = gf.get("src/com/_bundle.py").toString();
        testUtils.assertGeneratedContainsExpectedString(generated,
                "res = rune_with_meta(rune_resolve_attr(self, \"f\"), {'@scheme': \"myScheme\"})");
    }

    /**
     * Test case for function with meta enum dependency.
     */
    @Test
    public void testFunctionWithMetaEnumDependency() {
        Map<String, CharSequence> gf = testUtils.generatePythonFromString(
                """
                        namespace com.test

                        enum MyEnum:
                            Value1

                        type Foo:
                            [metadata scheme]
                            val string (1..1)

                        func TestWithMetaEnum:
                            inputs:
                                f Foo (1..1)
                            output:
                                res Foo (1..1)
                            set res:
                                f with-meta { scheme: (MyEnum -> Value1) to-string }
                        """);
        String generated = gf.get("src/com/_bundle.py").toString();
        testUtils.assertGeneratedContainsExpectedString(generated,
                "res = rune_with_meta(rune_resolve_attr(self, \"f\"), {'@scheme': rune_str(com.test.MyEnum.MyEnum.VALUE_1)})");
        testUtils.assertGeneratedContainsExpectedString(generated, "import com.test.MyEnum");
    }
}
