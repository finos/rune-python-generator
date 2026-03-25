"""Collection operators unit tests demonstrating current failures (null handling gaps)"""

from rosetta_dsl.test.expressions.collections.functions.TestSum import TestSum
from rosetta_dsl.test.expressions.collections.functions.TestDistinct import TestDistinct
from rosetta_dsl.test.expressions.collections.functions.TestReverse import TestReverse
from rosetta_dsl.test.expressions.collections.functions.TestFlatten import TestFlatten
from rosetta_dsl.test.expressions.collections.functions.TestMax import TestMax
from rosetta_dsl.test.expressions.collections.functions.TestMin import TestMin
from rosetta_dsl.test.expressions.collections.functions.TestSort import TestSort
from rosetta_dsl.test.expressions.collections.functions.TestCount import TestCount
from rosetta_dsl.test.expressions.collections.functions.TestOnlyElement import (
    TestOnlyElement,
)
from rosetta_dsl.test.expressions.collections.Item import Item
from rosetta_dsl.test.expressions.collections.Nested import Nested


def test_sum_with_nulls():
    """Test sum operation with nulls. Expected: 3 (1+None+2)"""
    items = [Item(val=1), Item(val=None), Item(val=2)]
    assert TestSum(items=items) == 3


def test_distinct_with_nulls():
    """Test distinct operation with nulls. Expected: [1, 2]"""
    items = [Item(val=1), Item(val=None), Item(val=2), Item(val=1)]
    result = TestDistinct(items=items)
    assert sorted([x for x in result if x is not None]) == [1, 2]
    assert None not in result


def test_reverse_with_nulls():
    """Test reverse operation with nulls. Expected: [2, 1]"""
    items = [Item(val=1), Item(val=2), Item(val=None)]
    result = TestReverse(items=items)
    assert result == [2, 1]


def test_flatten_with_nulls():
    """Test flatten operation with nulls. Expected: [1, 2]"""
    nested = [Nested(items=[Item(val=1), Item(val=None)]), Nested(items=[Item(val=2)])]
    result = TestFlatten(nested=nested)
    assert result == [1, 2]


def test_max_with_nulls():
    """Test max operation with nulls. Expected: 10"""
    items = [Item(val=1), Item(val=None), Item(val=10)]
    assert TestMax(items=items) == 10


def test_min_with_nulls():
    """Test min operation with nulls. Expected: 1"""
    items = [Item(val=1), Item(val=None), Item(val=10)]
    assert TestMin(items=items) == 1


def test_sort_with_nulls():
    """Test sort operation with nulls. Expected: [1, 10]"""
    items = [Item(val=10), Item(val=None), Item(val=1)]
    result = TestSort(items=items)
    assert result == [1, 10]


def test_count_with_nulls():
    """Test count operation with nulls. Expected: 2"""
    items = [Item(val=1), Item(val=None), Item(val=10)]
    assert TestCount(items=items) == 2


def test_only_element_with_nulls():
    """Test only-element with nulls. Expected: 1"""
    items = [Item(val=1), Item(val=None)]
    assert TestOnlyElement(items=items) == 1
