package com.regnosys.rosetta.generator.python.expressions;

import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.regnosys.rosetta.generator.python.PythonGeneratorTestUtils;
import com.regnosys.rosetta.tests.RosettaInjectorProvider;

import jakarta.inject.Inject;
import java.util.Map;

@ExtendWith(InjectionExtension.class)
@InjectWith(RosettaInjectorProvider.class)
@SuppressWarnings("LineLength")
public class PythonCollectionExpressionTest {

    /**
     * Test utils for generating Python.
     */
    @Inject
    private PythonGeneratorTestUtils testUtils;

    // -------------------------------------------------------------------------
    // From RosettaListOperationTest
    // -------------------------------------------------------------------------

    /**
     * Test case for aggregations.
     */
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
                        def TestAggregations(items: list[int | None] | None) -> bool:
                            \"\"\"

                            Parameters
                            ----------
                            items : list[int | None]

                            Returns
                            -------
                            result : bool

                            \"\"\"
                            self = inspect.currentframe()

                            items = rune_cow(items)


                            result = ((rune_all_elements((lambda items: sum(x for x in (items or []) if x is not None) if items is not None else None)(rune_resolve_attr(self, \"items\")), \"=\", 10) and rune_all_elements((lambda items: max((x for x in (items or []) if x is not None), default=None) if items is not None else None)(rune_resolve_attr(self, \"items\")), \"=\", 5)) and rune_all_elements((lambda items: min((x for x in (items or []) if x is not None), default=None) if items is not None else None)(rune_resolve_attr(self, \"items\")), \"=\", 1))


                            return result
                        """);
    }

    /**
     * Test case for accessors.
     */
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
                        def TestAccessors(items: list[int | None] | None) -> bool:
                            \"\"\"

                            Parameters
                            ----------
                            items : list[int | None]

                            Returns
                            -------
                            result : bool

                            \"\"\"
                            self = inspect.currentframe()

                            items = rune_cow(items)


                            result = (rune_all_elements(next((x for x in (rune_resolve_attr(self, \"items\") or []) if x is not None), None), \"=\", 1) and rune_all_elements(next((x for x in reversed(rune_resolve_attr(self, \"items\") or []) if x is not None), None), \"=\", 5))


                            return result
                        """);
    }

    /**
     * Test case for sort operation.
     */
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
                        def TestSort(items: list[int | None] | None) -> list[int | None]:
                            \"\"\"

                            Parameters
                            ----------
                            items : list[int | None]

                            Returns
                            -------
                            result : list[int | None]

                            \"\"\"
                            self = inspect.currentframe()

                            items = rune_cow(items)


                            result = (lambda items: sorted(x for x in (items or []) if x is not None) if items is not None else None)(rune_resolve_attr(self, \"items\"))


                            return result
                        """);
    }

    /**
     * Test case for list comparison.
     */
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
                        def TestListComparison(list1: list[int | None] | None, list2: list[int | None] | None) -> bool:
                            \"\"\"

                            Parameters
                            ----------
                            list1 : list[int | None]

                            list2 : list[int | None]

                            Returns
                            -------
                            result : bool

                            \"\"\"
                            self = inspect.currentframe()

                            list1 = rune_cow(list1)
                            list2 = rune_cow(list2)


                            result = rune_all_elements(rune_resolve_attr(self, \"list1\"), \"=\", rune_resolve_attr(self, \"list2\"))


                            return result
                        """);
    }

    /**
     * Test case for collection literal.
     */
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
                        def TestLiteral() -> list[int | None]:
                            \"\"\"

                            Parameters
                            ----------
                            Returns
                            -------
                            result : list[int | None]

                            \"\"\"
                            self = inspect.currentframe()


                            result = [1, 2, 3]


                            return result
                        """);
    }

    /**
     * Test case for reverse operation.
     */
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
                        def TestReverse(items: list[int | None] | None) -> list[int | None]:
                            \"\"\"

                            Parameters
                            ----------
                            items : list[int | None]

                            Returns
                            -------
                            result : list[int | None]

                            \"\"\"
                            self = inspect.currentframe()

                            items = rune_cow(items)


                            result = (lambda items: list(reversed([x for x in (items or []) if x is not None])) if items is not None else None)(rune_resolve_attr(self, \"items\"))


                            return result
                        """);
    }

    // -------------------------------------------------------------------------
    // From RosettaFilterOperationTest
    // -------------------------------------------------------------------------

    /**
     * Test case for filter operation.
     */
    @Test
    public void testFilterOperation() {
        String generatedPython = testUtils.generatePythonAndExtractBundle("""
                type Item:
                    val int (1..1)
                type TestFilter:
                    items Item (0..*)
                    condition FilterCheck:
                        (items filter [ val > 5 ] then count) = 0
                """);

        testUtils.assertGeneratedContainsExpectedString(generatedPython,
                "class TestFilter(BaseDataClass):");
        testUtils.assertGeneratedContainsExpectedString(generatedPython,
                "items: Optional[list[Item | None]] = Field(None, description='')");
        testUtils.assertGeneratedContainsExpectedString(generatedPython,
                "return rune_all_elements((lambda item: (lambda items: sum(1 for x in (items if (hasattr(items, '__iter__') and not isinstance(items, (str, dict, bytes, bytearray))) else ([items] if items is not None else [])) if x is not None))(item))(rune_filter(rune_resolve_attr(self, \"items\"), lambda item: rune_all_elements(rune_resolve_attr(item, \"val\"), \">\", 5))), \"=\", 0)");
    }

    /**
     * Test case for nested filter map count.
     */
    @Test
    public void testNestedFilterMapCount() {
        String generatedPython = testUtils.generatePythonAndExtractBundle("""
                type Item:
                    val int (1..1)
                func TestNestedNested:
                    inputs: items Item (0..*)
                    output: result int (1..1)
                    set result:
                        items filter [ val > 5 ] then count
                """);

        testUtils.assertGeneratedContainsExpectedString(generatedPython,
                "def TestNestedNested(items: list[Item | None] | None) -> int:");
        testUtils.assertGeneratedContainsExpectedString(generatedPython,
                "result = (lambda item: (lambda items: sum(1 for x in (items if (hasattr(items, '__iter__') and not isinstance(items, (str, dict, bytes, bytearray))) else ([items] if items is not None else [])) if x is not None))(item))(rune_filter(rune_resolve_attr(self, \"items\"), lambda item: rune_all_elements(rune_resolve_attr(item, \"val\"), \">\", 5)))");
    }

    // -------------------------------------------------------------------------
    // From RosettaMapOperationTest
    // -------------------------------------------------------------------------

    /**
     * Test case for map operation.
     */
    @Test
    public void testMapOperation() {
        String generatedPython = testUtils.generatePythonFromString("""
                type Item:
                    val int (1..1)
                type TestMap:
                    items Item (0..*)
                    condition MapCheck:
                        (items extract val then count) = 0
                """).toString();

        // Targeted assertions for TestMap class (standalone — no Phase 2/3)
        testUtils.assertGeneratedContainsExpectedString(generatedPython,
                "class TestMap(BaseDataClass):");
        testUtils.assertGeneratedContainsExpectedString(generatedPython,
                "items: Optional[list[Item | None]] = Field(None, description='')");
        testUtils.assertGeneratedContainsExpectedString(generatedPython,
                "return rune_all_elements((lambda item: (lambda items: sum(1 for x in (items if (hasattr(items, '__iter__') and not isinstance(items, (str, dict, bytes, bytearray))) else ([items] if items is not None else [])) if x is not None))(item))([x for x in map(lambda item: rune_resolve_attr(item, \"val\"), rune_resolve_attr(self, \"items\") or []) if x is not None]), \"=\", 0)");
    }

    // -------------------------------------------------------------------------
    // From RosettaFlattenOperationTest
    // -------------------------------------------------------------------------

    /**
     * Test case for flatten operation.
     */
    @Test
    public void testGenerateFlattenCondition() {
        String generatedPython = testUtils.generatePythonFromString("""
                type Bar:
                    numbers int (0..*)
                type Foo: <"Test flatten operation condition">
                    bars Bar (0..*) <"test bar">
                    condition TestCondition: <"Test Condition">
                        [1, 2, 3] =
                        (bars
                            extract numbers
                            then flatten)
                """).toString();

        // Targeted assertions for Foo class (standalone — no Phase 2/3)
        testUtils.assertGeneratedContainsExpectedString(generatedPython,
                "class Foo(BaseDataClass):");
        testUtils.assertGeneratedContainsExpectedString(generatedPython,
                "bars: Optional[list[Bar | None]] = Field(None, description='test bar')");
        testUtils.assertGeneratedContainsExpectedString(generatedPython,
                "return rune_all_elements([1, 2, 3], \"=\", (lambda item: (lambda nested: [x for sub in (nested or []) if sub is not None for x in (sub if (hasattr(sub, '__iter__') and not isinstance(sub, (str, dict, bytes, bytearray))) else [sub]) if x is not None] if nested is not None else None)(item))([x for x in map(lambda item: rune_resolve_attr(item, \"numbers\"), rune_resolve_attr(self, \"bars\") or []) if x is not None]))");
    }

    // -------------------------------------------------------------------------
    // From RosettaDistinctOperationTest
    // -------------------------------------------------------------------------

    /**
     * Test case for distinct operation condition.
     */
    @Test
    public void testGenerateDistinctCondition() {
        String generatedPython = testUtils.generatePythonFromString("""
                type A: <"Test type">
                    field1 int (1..*) <"Test int field 1">
                    field2 int (1..*) <"Test int field 2">

                type Test: <"Test distinct operation condition">
                    aValue A (1..*) <"Test A type aValue">
                    field3 number (1..1)<"Test number field 3">
                    condition TestCond: <"Test condition">
                        if aValue -> field1 distinct count = 1
                            then field3=0
                        else field3=1
                """).toString();

        // Targeted assertions for Test class (standalone — no Phase 2/3)
        testUtils.assertGeneratedContainsExpectedString(generatedPython,
                "class Test(BaseDataClass):");
        testUtils.assertGeneratedContainsExpectedString(generatedPython,
                "aValue: list[A | None] = Field(..., description='Test A type aValue', min_length=1)");
        testUtils.assertGeneratedContainsExpectedString(generatedPython, "def condition_0_TestCond(self):");

        // Targeted assertions for A class
        testUtils.assertGeneratedContainsExpectedString(generatedPython,
                "class A(BaseDataClass):");
        testUtils.assertGeneratedContainsExpectedString(generatedPython,
                "field1: list[int | None] = Field(..., description='Test int field 1', min_length=1)");
    }

    // -------------------------------------------------------------------------
    // From RosettaCountOperationTest
    // -------------------------------------------------------------------------

    /**
     * Test case for count operation in condition.
     */
    @Test
    public void testGenerateCountCondition() {
        String generatedPython = testUtils.generatePythonFromString("""
                type A: <"Test type">
                    field1 int (0..*) <"Test int field 1">
                    field2 int (1..*) <"Test int field 2">
                    field3 int (1..3) <"Test int field 3">
                    field4 int (0..3) <"Test int field 4">

                type Test: <"Test count operation condition">
                    aValue A (1..*) <"Test A type aValue">

                    condition TestCond: <"Test condition">
                        if aValue -> field1 count <> aValue -> field2 count
                            then True
                        else False
                """).toString();

        // Targeted assertions for Test class (standalone — no Phase 2/3)
        testUtils.assertGeneratedContainsExpectedString(generatedPython,
                "class Test(BaseDataClass):");
        testUtils.assertGeneratedContainsExpectedString(generatedPython,
                "aValue: list[A | None] = Field(..., description='Test A type aValue', min_length=1)");
        testUtils.assertGeneratedContainsExpectedString(generatedPython, "def condition_0_TestCond(self):");

        // Targeted assertions for A class
        testUtils.assertGeneratedContainsExpectedString(generatedPython,
                "class A(BaseDataClass):");
        testUtils.assertGeneratedContainsExpectedString(generatedPython,
                "field1: Optional[list[int | None]] = Field(None, description='Test int field 1')");
        testUtils.assertGeneratedContainsExpectedString(generatedPython,
                "field2: list[int | None] = Field(..., description='Test int field 2', min_length=1)");
    }

    // -------------------------------------------------------------------------
    // From RosettaAnyOperationTest
    // -------------------------------------------------------------------------

    /**
     * Test case for any condition.
     */
    @Test
    public void testGenerateAnyCondition() {
        testUtils.assertBundleContainsExpectedString("""
                type Test: <"Test any operation condition">
                    field1 string (1..1) <"Test string field1">
                    field2 string (1..1) <"Test boolean field2">
                    condition TestCond: <"Test condition">
                        if field1="A"
                        then ["B", "C", "D"] any = field2
                """,
                """
                class Test(BaseDataClass):
                    \"""
                    Test any operation condition
                    \"""
                    field1: str = Field(..., description='Test string field1')
                    \"""
                    Test string field1
                    \"""
                    field2: str = Field(..., description='Test boolean field2')
                    \"""
                    Test boolean field2
                    \"""

                    @rune_condition
                    def condition_0_TestCond(self):
                        \"""
                        Test condition
                        \"""
                        item = self
                        def _then_fn0():
                            return rune_any_elements(["B", "C", "D"], "=", rune_resolve_attr(self, "field2"))

                        def _else_fn0():
                            return True

                        return if_cond_fn(rune_all_elements(rune_resolve_attr(self, "field1"), "=", "A"), _then_fn0, _else_fn0)""");
    }

    /**
     * Test case for any <> condition.
     */
    @Test
    public void testGenerateAnyNotEqualsCondition() {
        testUtils.assertBundleContainsExpectedString("""
                type Test: <"Test any <> condition">
                    field1 string (1..1) <"Test string field1">
                    field2 string (1..1) <"Test string field2">
                    condition TestCond: <"Test condition">
                        if field1="A"
                        then ["B", "C", "D"] any <> field2
                """,
                """
                    @rune_condition
                    def condition_0_TestCond(self):
                        \"""
                        Test condition
                        \"""
                        item = self
                        def _then_fn0():
                            return rune_any_elements(["B", "C", "D"], "<>", rune_resolve_attr(self, "field2"))

                        def _else_fn0():
                            return True

                        return if_cond_fn(rune_all_elements(rune_resolve_attr(self, "field1"), "=", "A"), _then_fn0, _else_fn0)
                """);
    }

    /**
     * Test case for all <> condition.
     */
    @Test
    public void testGenerateAllNotEqualsCondition() {
        testUtils.assertBundleContainsExpectedString("""
                type Test: <"Test all <> condition">
                    field1 string (1..1) <"Test string field1">
                    field2 string (1..1) <"Test string field2">
                    condition TestCond: <"Test condition">
                        if field1="A"
                        then ["B", "C", "D"] all <> field2
                """,
                """
                    @rune_condition
                    def condition_0_TestCond(self):
                        \"""
                        Test condition
                        \"""
                        item = self
                        def _then_fn0():
                            return (not rune_any_elements(["B", "C", "D"], "=", rune_resolve_attr(self, "field2")))

                        def _else_fn0():
                            return True

                        return if_cond_fn(rune_all_elements(rune_resolve_attr(self, "field1"), "=", "A"), _then_fn0, _else_fn0)
                """);
    }

    // -------------------------------------------------------------------------
    // From RosettaContainsOperationTest
    // -------------------------------------------------------------------------

    /**
     * Test case for binary contains operation.
     */
    @Test
    public void testGenerateBinContainsCondition() {
        String generatedPython = testUtils.generatePythonFromString("""
                enum C: <"Test type C">
                    field4 <"Test enum field 4">
                    field5 <"Test enum field 5">
                type A: <"Test type">
                    field1 int (1..*) <"Test int field 1">
                    cValue C (1..*) <"Test C type cValue">
                type B: <"Test type B">
                    field2 int (1..*) <"Test int field 2">
                    aValue A (1..*) <"Test A type aValue">
                type Test: <"Test filter operation condition">
                    bValue B (1..*) <"Test B type bValue">
                    field3 boolean (0..1) <"Test bool type field3">
                    condition TestCond: <"Test condition">
                        if field3=True
                        then bValue->aValue->cValue contains C->field4
                """).toString();

        String expectedC = """
                class C(rune.runtime.metadata.EnumWithMetaMixin, Enum):
                    \"""
                    Test type C
                    \"""
                    FIELD_4 = "field4"
                    \"""
                    Test enum field 4
                    \"""
                    FIELD_5 = "field5"
                    \"""
                    Test enum field 5
                    \"""
                """;

        String expectedA = """
                class A(BaseDataClass):
                    \"""
                    Test type
                    \"""
                    field1: list[int | None] = Field(..., description='Test int field 1', min_length=1)
                    \"""
                    Test int field 1
                    \"""
                    cValue: list[com.rosetta.test.model.C.C | None] = Field(..., description='Test C type cValue', min_length=1)
                    \"""
                    Test C type cValue
                    \"""
                """;

        String expectedBPhase1 = """
                class B(BaseDataClass):
                    \"""
                    Test type B
                    \"""
                    field2: list[int | None] = Field(..., description='Test int field 2', min_length=1)
                    \"""
                    Test int field 2
                    \"""
                    aValue: list[A | None] = Field(..., description='Test A type aValue', min_length=1)
                    \"""
                    Test A type aValue
                    \"""
                """;

        String expectedTestPhase1 = """
                class Test(BaseDataClass):
                    \"""
                    Test filter operation condition
                    \"""
                    bValue: list[B | None] = Field(..., description='Test B type bValue', min_length=1)
                    \"""
                    Test B type bValue
                    \"""
                    field3: Optional[bool] = Field(None, description='Test bool type field3')
                    \"""
                    Test bool type field3
                    \"""

                    @rune_condition
                    def condition_0_TestCond(self):
                        \"""
                        Test condition
                        \"""
                        item = self
                        def _then_fn0():
                            return rune_contains(rune_resolve_attr(rune_resolve_attr(rune_resolve_attr(self, "bValue"), "aValue"), "cValue"), com.rosetta.test.model.C.C.FIELD_4)

                        def _else_fn0():
                            return True

                        return if_cond_fn(rune_all_elements(rune_resolve_attr(self, "field3"), "=", True), _then_fn0, _else_fn0)
                """;

        testUtils.assertGeneratedContainsExpectedString(generatedPython, expectedC);
        testUtils.assertGeneratedContainsExpectedString(generatedPython, expectedA);
        testUtils.assertGeneratedContainsExpectedString(generatedPython, expectedBPhase1);
        testUtils.assertGeneratedContainsExpectedString(generatedPython, expectedTestPhase1);
    }

    // -------------------------------------------------------------------------
    // From RosettaDisjointOperationTest
    // -------------------------------------------------------------------------

    /**
     * Test case for disjoint binary expression condition.
     */
    @Test
    public void testGenerateBinDisjointCondition() {
        testUtils.assertBundleContainsExpectedString("""
                type Test: <"Test disjoint binary expression condition">
                    field1 string (1..1) <"Test string field1">
                    field2 string (1..1) <"Test string field2">
                    field3 boolean (1..1) <"Test boolean field3">
                    condition TestCond: <"Test condition">
                        if field3=False
                        then if ["B", "C", "D"] any = field2 and ["A"] disjoint field1
                        then field3=True
                """,
                """
                        class Test(BaseDataClass):
                            \"""
                            Test disjoint binary expression condition
                            \"""
                            field1: str = Field(..., description='Test string field1')
                            \"""
                            Test string field1
                            \"""
                            field2: str = Field(..., description='Test string field2')
                            \"""
                            Test string field2
                            \"""
                            field3: bool = Field(..., description='Test boolean field3')
                            \"""
                            Test boolean field3
                            \"""

                            @rune_condition
                            def condition_0_TestCond(self):
                                \"""
                                Test condition
                                \"""
                                item = self
                                def _then_fn1():
                                    return rune_all_elements(rune_resolve_attr(self, "field3"), "=", True)

                                def _else_fn1():
                                    return True

                                def _then_fn0():
                                    return if_cond_fn((rune_any_elements(["B", "C", "D"], "=", rune_resolve_attr(self, "field2")) and rune_disjoint(["A"], rune_resolve_attr(self, "field1"))), _then_fn1, _else_fn1)

                                def _else_fn0():
                                    return True

                                return if_cond_fn(rune_all_elements(rune_resolve_attr(self, "field3"), "=", False), _then_fn0, _else_fn0)""");
    }

    // -------------------------------------------------------------------------
    // From RosettaJoinOperationTest
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // From RosettaOnlyElementTest
    // -------------------------------------------------------------------------

    /**
     * Test case for only-element condition.
     */
    @Test
    public void testGenerateOnlyElementCondition() {
        String generatedPython = testUtils.generatePythonFromString("""
                enum TestEnum: <"Enum to test">
                TestEnumValue1 <"Test enum value 1">
                TestEnumValue2 <"Test enum value 2">
                type Test1: <"Test only-element condition.">
                    field1 TestEnum (0..1) <"Test enum field 1">
                    field2 number (0..1) <"Test number field 2">
                    condition TestCond: <"Test condition">
                        if field1 only-element= TestEnum->TestEnumValue1
                            then field2=0
                """).toString();

        String expectedTestEnum = """
                class TestEnum(rune.runtime.metadata.EnumWithMetaMixin, Enum):
                    \"""
                    Enum to test
                    \"""
                    TEST_ENUM_VALUE_1 = "TestEnumValue1"
                    \"""
                    Test enum value 1
                    \"""
                    TEST_ENUM_VALUE_2 = "TestEnumValue2"
                    \"""
                    Test enum value 2
                    \"""";

        String expectedTest1 = """
                class Test1(BaseDataClass):
                    \"""
                    Test only-element condition.
                    \"""
                    field1: Optional[com.rosetta.test.model.TestEnum.TestEnum] = Field(None, description='Test enum field 1')
                    \"""
                    Test enum field 1
                    \"""
                    field2: Optional[Decimal] = Field(None, description='Test number field 2')
                    \"""
                    Test number field 2
                    \"""

                    @rune_condition
                    def condition_0_TestCond(self):
                        \"""
                        Test condition
                        \"""
                        item = self
                        def _then_fn0():
                            return rune_all_elements(rune_resolve_attr(self, "field2"), "=", 0)

                        def _else_fn0():
                            return True

                        return if_cond_fn(rune_all_elements(rune_get_only_element([x for x in (rune_resolve_attr(self, \"field1\") or []) if x is not None]), \"=\", com.rosetta.test.model.TestEnum.TestEnum.TEST_ENUM_VALUE_1), _then_fn0, _else_fn0)""";

        testUtils.assertGeneratedContainsExpectedString(generatedPython, expectedTestEnum);
        testUtils.assertGeneratedContainsExpectedString(generatedPython, expectedTest1);
    }

    // -------------------------------------------------------------------------
    // From PythonReduceOperationTest
    // -------------------------------------------------------------------------

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

        String generatedPython = gf.get("src/com/test/functions/SumList.py").toString();

        testUtils.assertGeneratedContainsExpectedString(generatedPython, "functools.reduce(lambda a, b: (a + b), rune_resolve_attr(self, \"items\"))");
    }
}
