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
public class RosettaListOperationTest {

    @Inject
    private PythonGeneratorTestUtils testUtils;

    @Test
    public void testAggregations() {
        testUtils.assertBundleContainsExpectedString("""
                func TestAggregations:
                    inputs: items int (0..*)
                    output: result boolean (1..1)
                    set result:
                        items sum = 10 and
                        items max = 5 and
                        items min = 1
                """,
                """
                        @replaceable
                        @validate_call
                        def com_rosetta_test_model_functions_TestAggregations(items: list[int] | None) -> bool:
                            \"\"\"

                            Parameters
                            ----------
                            items : list[int]

                            Returns
                            -------
                            result : bool

                            \"\"\"
                            self = inspect.currentframe()


                            result = ((rune_all_elements(sum(rune_resolve_attr(self, \"items\")), \"=\", 10) and rune_all_elements(max(rune_resolve_attr(self, \"items\")), \"=\", 5)) and rune_all_elements(min(rune_resolve_attr(self, \"items\")), \"=\", 1))


                            return result
                        """);
    }

    @Test
    public void testAccessors() {
        testUtils.assertBundleContainsExpectedString("""
                func TestAccessors:
                    inputs: items int (0..*)
                    output: result boolean (1..1)
                    set result:
                        items first = 1 and
                        items last = 5
                """,
                """
                        @replaceable
                        @validate_call
                        def com_rosetta_test_model_functions_TestAccessors(items: list[int] | None) -> bool:
                            \"\"\"

                            Parameters
                            ----------
                            items : list[int]

                            Returns
                            -------
                            result : bool

                            \"\"\"
                            self = inspect.currentframe()


                            result = (rune_all_elements(rune_resolve_attr(self, \"items\")[0], \"=\", 1) and rune_all_elements(rune_resolve_attr(self, \"items\")[-1], \"=\", 5))


                            return result
                        """);
    }

    @Test
    public void testSortOperation() {
        testUtils.assertBundleContainsExpectedString("""
                func TestSort:
                    inputs: items int (0..*)
                    output: result int (0..*)
                    set result:
                        items sort
                """,
                """
                        @replaceable
                        @validate_call
                        def com_rosetta_test_model_functions_TestSort(items: list[int] | None) -> list[int]:
                            \"\"\"

                            Parameters
                            ----------
                            items : list[int]

                            Returns
                            -------
                            result : list[int]

                            \"\"\"
                            self = inspect.currentframe()


                            result = sorted(rune_resolve_attr(self, \"items\"))


                            return result
                        """);
    }

    @Test
    public void testListComparison() {
        testUtils.assertBundleContainsExpectedString("""
                func TestListComparison:
                    inputs:
                        list1 int (0..*)
                        list2 int (0..*)
                    output: result boolean (1..1)
                    set result:
                        list1 = list2
                """,
                """
                        @replaceable
                        @validate_call
                        def com_rosetta_test_model_functions_TestListComparison(list1: list[int] | None, list2: list[int] | None) -> bool:
                            \"\"\"

                            Parameters
                            ----------
                            list1 : list[int]

                            list2 : list[int]

                            Returns
                            -------
                            result : bool

                            \"\"\"
                            self = inspect.currentframe()


                            result = rune_all_elements(rune_resolve_attr(self, \"list1\"), \"=\", rune_resolve_attr(self, \"list2\"))


                            return result
                        """);
    }

    @Test
    public void testCollectionLiteral() {
        testUtils.assertBundleContainsExpectedString("""
                func TestLiteral:
                    output: result int (0..*)
                    set result:
                        [1, 2, 3]
                """,
                """
                        @replaceable
                        @validate_call
                        def com_rosetta_test_model_functions_TestLiteral() -> list[int]:
                            \"\"\"

                            Parameters
                            ----------
                            Returns
                            -------
                            result : list[int]

                            \"\"\"
                            self = inspect.currentframe()


                            result = [1, 2, 3]


                            return result
                        """);
    }

    @Test
    public void testReverseOperation() {
        testUtils.assertBundleContainsExpectedString("""
                func TestReverse:
                    inputs: items int (0..*)
                    output: result int (0..*)
                    set result:
                        items reverse
                """,
                """
                        @replaceable
                        @validate_call
                        def com_rosetta_test_model_functions_TestReverse(items: list[int] | None) -> list[int]:
                            \"\"\"

                            Parameters
                            ----------
                            items : list[int]

                            Returns
                            -------
                            result : list[int]

                            \"\"\"
                            self = inspect.currentframe()


                            result = list(reversed(rune_resolve_attr(self, \"items\")))


                            return result
                        """);
    }
}
