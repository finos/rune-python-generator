"""Collection operators unit tests demonstrating current failures (null handling gaps)"""

from rosetta_dsl._bundle import (
    rosetta_dsl_test_semantic_expressions_collections_TestSum as TestSum,
    rosetta_dsl_test_semantic_expressions_collections_TestDistinct as TestDistinct,
    rosetta_dsl_test_semantic_expressions_collections_TestReverse as TestReverse,
    rosetta_dsl_test_semantic_expressions_collections_TestFlatten as TestFlatten,
    rosetta_dsl_test_semantic_expressions_collections_TestMax as TestMax,
    rosetta_dsl_test_semantic_expressions_collections_TestMin as TestMin,
    rosetta_dsl_test_semantic_expressions_collections_TestSort as TestSort,
    rosetta_dsl_test_semantic_expressions_collections_TestCount as TestCount,
    rosetta_dsl_test_semantic_expressions_collections_TestOnlyElement as TestOnlyElement,
    rosetta_dsl_test_semantic_expressions_collections_Item as Item,
    rosetta_dsl_test_semantic_expressions_collections_Nested as Nested,
)


def test_sum_with_nulls():
    """Test sum operation with nulls. Expected: 3 (1+None+2), Current: TypeError."""
    items = [Item(val=1), Item(val=None), Item(val=2)]
    # This is expected to fail with TypeError: unsupported operand type(s) for +: 'int' and 'NoneType'
    assert TestSum(items=items) == 3


def test_distinct_with_nulls():
    """Test distinct operation with nulls. Expected: [1, 2], Current: [1, None, 2]."""
    items = [Item(val=1), Item(val=None), Item(val=2), Item(val=1)]
    result = TestDistinct(items=items)
    # The current implementation set(expr) will include None if present.
    # Result will likely be {1, None, 2} (in some order).
    # We expect nulls to be filtered.
    assert sorted([x for x in result if x is not None]) == [1, 2] # This part passes but doesn't prove it's fixed.
    assert None not in result # This is the part that will fail.


def test_reverse_with_nulls():
    """Test reverse operation with nulls. Expected: [2, 1], Current: [None, 2, 1]."""
    items = [Item(val=1), Item(val=2), Item(val=None)]
    result = TestReverse(items=items)
    # Expected filtered reverse: [2, 1]
    # Current: [None, 2, 1]
    assert result == [2, 1]


def test_flatten_with_nulls():
    """Test flatten operation with nulls. Expected: [1, 2], Current: [1, None, 2]."""
    nested = [
        Nested(items=[Item(val=1), Item(val=None)]),
        Nested(items=[Item(val=2)])
    ]
    result = TestFlatten(nested=nested)
    # Expected: [1, 2]
    assert result == [1, 2]


def test_max_with_nulls():
    """Test max operation with nulls. Expected: 10, Current: TypeError."""
    items = [Item(val=1), Item(val=None), Item(val=10)]
    assert TestMax(items=items) == 10


def test_min_with_nulls():
    """Test min operation with nulls. Expected: 1, Current: TypeError."""
    items = [Item(val=1), Item(val=None), Item(val=10)]
    assert TestMin(items=items) == 1


def test_sort_with_nulls():
    """Test sort operation with nulls. Expected: [1, 10], Current: [1, None, 10] or TypeError."""
    items = [Item(val=10), Item(val=None), Item(val=1)]
    result = TestSort(items=items)
    assert result == [1, 10]


def test_count_with_nulls():
    """Test count operation with nulls. Expected: 2, Current: 3."""
    items = [Item(val=1), Item(val=None), Item(val=10)]
    # Currently it will likely count the None.
    assert TestCount(items=items) == 2


def test_only_element_with_nulls():
    """Test only-element with nulls. Expected: 1, Current: None."""
    items = [Item(val=1), Item(val=None)]
    # If it filters None, it becomes [1], so only-element is 1.
    # Currently it sees [1, None] and returns None.
    assert TestOnlyElement(items=items) == 1
