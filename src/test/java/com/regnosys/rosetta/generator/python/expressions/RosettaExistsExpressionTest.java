package com.regnosys.rosetta.generator.python.expressions;

import com.regnosys.rosetta.generator.python.PythonGeneratorTestUtils;
import com.regnosys.rosetta.tests.RosettaInjectorProvider;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import jakarta.inject.Inject;

@ExtendWith(InjectionExtension.class)
@InjectWith(RosettaInjectorProvider.class)
public class RosettaExistsExpressionTest {

    @Inject
    private PythonGeneratorTestUtils testUtils;

    @Test
    public void testExistsBasic() {
        testUtils.assertBundleContainsExpectedString(
                """
                        type Bar:
                            field number (0..1)

                        func ExistsBasic:
                            inputs: bar Bar (1..1)
                            output: result boolean (1..1)
                            set result:
                                bar -> field exists
                        """,
                """
                        @replaceable
                        @validate_call
                        def com_rosetta_test_model_functions_ExistsBasic(bar: com_rosetta_test_model_Bar) -> bool:
                            \"\"\"

                            Parameters
                            ----------
                            bar : com.rosetta.test.model.Bar

                            Returns
                            -------
                            result : bool

                            \"\"\"
                            self = inspect.currentframe()


                            result = rune_attr_exists(rune_resolve_attr(rune_resolve_attr(self, "bar"), "field"))


                            return result""");
    }

    @Test
    public void testSingleExists() {
        // UPDATED: Expecting "single" argument
        //
        testUtils.assertBundleContainsExpectedString(
                """
                        type Bar:
                            field number (0..1)

                        func SingleExists:
                            inputs: bar Bar (1..1)
                            output: result boolean (1..1)
                            set result:
                                bar -> field single exists
                        """,
                """
                        @replaceable
                        @validate_call
                        def com_rosetta_test_model_functions_SingleExists(bar: com_rosetta_test_model_Bar) -> bool:
                            \"\"\"

                            Parameters
                            ----------
                            bar : com.rosetta.test.model.Bar

                            Returns
                            -------
                            result : bool

                            \"\"\"
                            self = inspect.currentframe()


                            result = rune_attr_exists(rune_resolve_attr(rune_resolve_attr(self, "bar"), "field"), "single")


                            return result""");
    }

    @Test
    public void testMultipleExists() {
        // UPDATED: Expecting "multiple" argument
        //
        testUtils.assertBundleContainsExpectedString(
                """
                        type Bar:
                            fieldList number (0..*)

                        func MultipleExists:
                            inputs: bar Bar (1..1)
                            output: result boolean (1..1)
                            set result:
                                bar -> fieldList multiple exists
                        """,
                """
                        @replaceable
                        @validate_call
                        def com_rosetta_test_model_functions_MultipleExists(bar: com_rosetta_test_model_Bar) -> bool:
                            \"\"\"

                            Parameters
                            ----------
                            bar : com.rosetta.test.model.Bar

                            Returns
                            -------
                            result : bool

                            \"\"\"
                            self = inspect.currentframe()


                            result = rune_attr_exists(rune_resolve_attr(rune_resolve_attr(self, "bar"), "fieldList"), "multiple")


                            return result""");
    }

    @Test
    public void testExistsWithMetadata() {
        testUtils.assertBundleContainsExpectedString(
                """
                        type Bar:
                            field number (0..1)
                                [metadata scheme]

                        func ExistsWithMetadata:
                            inputs: bar Bar (1..1)
                            output: result boolean (1..1)
                            set result:
                                bar -> field exists
                        """,
                """
                        @replaceable
                        @validate_call
                        def com_rosetta_test_model_functions_ExistsWithMetadata(bar: com_rosetta_test_model_Bar) -> bool:
                            \"\"\"

                            Parameters
                            ----------
                            bar : com.rosetta.test.model.Bar

                            Returns
                            -------
                            result : bool

                            \"\"\"
                            self = inspect.currentframe()


                            result = rune_attr_exists(rune_resolve_attr(rune_resolve_attr(self, "bar"), "field"))


                            return result""");
    }

    @Test
    public void testExistsWithLogicalOperators() {
        testUtils.assertBundleContainsExpectedString(
                """
                        type Bar:
                            field1 number (0..1)
                            field2 number (0..1)

                        func ExistsWithLogical:
                            inputs: bar Bar (1..1)
                            output: result boolean (1..1)
                            set result:
                                bar -> field1 exists and bar -> field2 exists
                        """,
                """
                        @replaceable
                        @validate_call
                        def com_rosetta_test_model_functions_ExistsWithLogical(bar: com_rosetta_test_model_Bar) -> bool:
                            \"\"\"

                            Parameters
                            ----------
                            bar : com.rosetta.test.model.Bar

                            Returns
                            -------
                            result : bool

                            \"\"\"
                            self = inspect.currentframe()


                            result = (rune_attr_exists(rune_resolve_attr(rune_resolve_attr(self, "bar"), "field1")) and rune_attr_exists(rune_resolve_attr(rune_resolve_attr(self, "bar"), "field2")))


                            return result""");
    }

    @Test
    public void testDeepPathSingleExists() {
        testUtils.assertBundleContainsExpectedString(
                """
                        type Sub:
                            field number (0..1)
                        type Bar:
                            sub Sub (0..1)

                        func DeepExists:
                            inputs: bar Bar (1..1)
                            output: result boolean (1..1)
                            set result:
                                bar -> sub -> field single exists
                        """,
                """
                        result = rune_attr_exists(rune_resolve_attr(rune_resolve_attr(rune_resolve_attr(self, "bar"), "sub"), "field"), "single")""");
    }

    @Test
    public void testExistsInFunctionArguments() {
        testUtils.assertBundleContainsExpectedString(
                """
                        func ExistsArg:
                            inputs:
                                arg1 number (0..1)
                                arg2 number (0..1)
                            output:
                                result boolean (1..1)
                            set result:
                                arg1 exists or arg2 exists
                        """,
                """
                        result = (rune_attr_exists(rune_resolve_attr(self, "arg1")) or rune_attr_exists(rune_resolve_attr(self, "arg2")))""");
    }
}
