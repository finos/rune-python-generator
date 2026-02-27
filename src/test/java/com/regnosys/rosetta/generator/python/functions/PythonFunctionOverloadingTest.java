package com.regnosys.rosetta.generator.python.functions;

import com.regnosys.rosetta.generator.python.PythonGeneratorTestUtils;
import com.regnosys.rosetta.tests.RosettaInjectorProvider;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import jakarta.inject.Inject;
import java.util.Map;

@ExtendWith(InjectionExtension.class)
@InjectWith(RosettaInjectorProvider.class)
@SuppressWarnings("LineLength")
public class PythonFunctionOverloadingTest {

    /**
     * Test utils for generating Python code.
     */
    @Inject
    private PythonGeneratorTestUtils testUtils;

    /**
     * Test case for function with meta.
     */
    @Disabled
    @Test
    public void testFunctionWithMeta() {
        Map<String, CharSequence> gf = testUtils.generatePythonFromString(
                """
                namespace com.test

                enum DayCountFractionEnum:
                    ACT_360
                    _30_360

                func DayCountBasis: <"Return the day count basis (the denominator of the day count fraction) for the day count fraction.">
                    [codeImplementation]
                    [calculation]
                    inputs:
                        dcf DayCountFractionEnum (1..1) <"Day count fraction.">
                    output:
                        basis int (1..1) <"The corresponding basis, typically 360 or 365.">

                func DayCountBasis(dcf: DayCountFractionEnum -> ACT_360):
                    set basis: 360

                func DayCountBasis(dcf: DayCountFractionEnum -> _30_360):
                    set basis: 360
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
