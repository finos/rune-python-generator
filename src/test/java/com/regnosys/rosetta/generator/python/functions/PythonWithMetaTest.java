package com.regnosys.rosetta.generator.python.functions;

import com.regnosys.rosetta.generator.python.PythonGeneratorTestUtils;
import com.regnosys.rosetta.tests.RosettaInjectorProvider;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import jakarta.inject.Inject;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Map;

@ExtendWith(InjectionExtension.class)
@InjectWith(RosettaInjectorProvider.class)
public class PythonWithMetaTest {

    @Inject
    private PythonGeneratorTestUtils testUtils;

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
        assertTrue(
                generated.contains("res = rune_with_meta(rune_resolve_attr(self, \"f\"), {'@scheme': \"myScheme\"})"),
                "rune_with_meta logic");
    }

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
        assertTrue(generated.contains(
                "res = rune_with_meta(rune_resolve_attr(self, \"f\"), {'@scheme': rune_str(com.test.MyEnum.MyEnum.VALUE_1)})"),
                "rune_with_meta with expr");
        assertTrue(generated.contains("import com.test.MyEnum"), "Enum import");
    }
}
