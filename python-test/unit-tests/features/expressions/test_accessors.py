"""Accessors expression unit tests"""

from rosetta_dsl.test.semantic.expressions.accessors.TestFirst import TestFirst
from rosetta_dsl.test.semantic.expressions.accessors.TestLast import TestLast
from rosetta_dsl.test.semantic.expressions.accessors.TestFirstFiltered import (
    TestFirstFiltered,
)
from rosetta_dsl.test.semantic.expressions.accessors.TestLastFiltered import (
    TestLastFiltered,
)
from rosetta_dsl.test.semantic.expressions.accessors.Item import Item
from rosetta_dsl.test.semantic.expressions.accessors.TestMin import TestMin
from rosetta_dsl.test.semantic.expressions.accessors.TestMax import TestMax
from rosetta_dsl.test.semantic.expressions.accessors.TestSum import TestSum
from rosetta_dsl.test.semantic.expressions.accessors.TestCount import TestCount
from rosetta_dsl.test.semantic.expressions.accessors.TestJoin import TestJoin
from rosetta_dsl.test.semantic.expressions.accessors.NestedItem import NestedItem
from rosetta_dsl.test.semantic.expressions.accessors.ItemList import ItemList
from rosetta_dsl.test.semantic.expressions.accessors.TestFlatten import TestFlatten
from rosetta_dsl.test.semantic.expressions.accessors.TestDistinct import TestDistinct
from rosetta_dsl.test.semantic.expressions.accessors.TestSort import TestSort
from rosetta_dsl.test.semantic.expressions.accessors.TestFilter import TestFilter
from rosetta_dsl.test.semantic.expressions.accessors.TestOnlyElement import (
    TestOnlyElement,
)
from rosetta_dsl.test.semantic.expressions.accessors.TestExtractDeep import (
    TestExtractDeep,
)


def test_first_with_values():
    """Test first operation with normal list."""
    assert TestFirst(items=[1, 2, 3]) == 1


def test_last_with_values():
    """Test last operation with normal list."""
    assert TestLast(items=[1, 2, 3]) == 3


def test_first_with_empty_list():
    """Test first operation with empty list. Expected: None, Current: IndexError."""
    assert TestFirst(items=[]) is None


def test_last_with_empty_list():
    """Test last operation with empty list. Expected: None, Current: IndexError."""
    assert TestLast(items=[]) is None


def test_first_with_none_list():
    """Test first operation with None list. Expected: None, Current: None attribute access crash."""
    assert TestFirst(items=None) is None


def test_last_with_none_list():
    """Test last operation with None list. Expected: None, Current: None attribute access crash."""
    assert TestLast(items=None) is None


def test_first_with_leading_none():
    """Test first operation with leading None. Expected: 1 (filtered)."""
    items = [Item(val=None), Item(val=1), Item(val=2)]
    assert TestFirstFiltered(items=items) == 1


def test_last_with_trailing_none():
    """Test last operation with trailing None. Expected: 3 (filtered)."""
    items = [Item(val=1), Item(val=2), Item(val=None)]
    assert TestLastFiltered(items=items) == 2


def test_first_with_all_none_list():
    """Test first operation with list of all Nones. Expected: None."""
    items = [Item(val=None), Item(val=None)]
    assert TestFirstFiltered(items=items) is None


def test_min_with_none():
    """Test min operation with sparse list."""
    assert TestMin([1, None, 3]) == 1


def test_min_with_all_none():
    """Test min operation with list of all Nones. Expected: None."""
    assert TestMin([None, None]) is None


def test_max_with_all_none():
    """Test max operation with list of all Nones. Expected: None."""
    assert TestMax(items=[None, None]) is None


def test_sum_with_all_none():
    """Test sum operation with list of all Nones. Expected: 0"""
    # In Rosetta, sum of empty/null list is 0.
    assert TestSum(items=[None, None]) == 0


def test_count_with_all_none():
    """Test count operation with list of all Nones. Expected: 0"""
    assert TestCount(items=[None, None]) == 0


def test_min_empty():
    """Test min operation with empty list. Expected: None."""
    assert TestMin([]) is None


def test_max_empty():
    """Test max operation with empty list. Expected: None."""
    assert TestMax(items=[]) is None


def test_sum_empty():
    """Test sum operation with empty list. Expected: 0"""
    assert TestSum(items=[]) == 0


def test_count_empty():
    """Test count operation with empty list. Expected: 0"""
    assert TestCount(items=[]) == 0


def test_only_element_empty():
    """Test only-element with empty list. Expected: None"""
    assert TestOnlyElement(items=[]) is None


def test_flatten_empty():
    """Test flatten with empty list. Expected: []"""
    assert TestFlatten(groups=[]) == []


def test_distinct_empty():
    """Test distinct with empty list. Expected: []"""
    assert list(TestDistinct(items=[])) == []


def test_sort_empty():
    """Test sort with empty list. Expected: []"""
    assert TestSort(items=[]) == []


def test_join_empty():
    """Test join with empty list. Expected: ''"""
    assert TestJoin(items=[], delimiter=",") == ""


def test_filter_empty():
    """Test filter with empty list. Expected: []"""
    assert TestFilter(items=[]) == []


def test_extract_deep_empty():
    """Test deep extract with empty list. Expected: None (current behavior of rune_resolve_attr)"""
    assert TestExtractDeep(nested=[]) is None


def test_join_with_none():
    """Test join operation with nulls. Expected: 'ac'"""
    assert TestJoin(items=["a", None, "c"], delimiter="") == "ac"


def test_extract_deep_with_none():
    """Test deep extract (dot access) with nulls. Expected: [1, 2]"""
    # nested -> innerItem -> val
    items = [
        NestedItem(innerItem=Item(val=1)),
        NestedItem(innerItem=None),
        NestedItem(innerItem=Item(val=None)),
        NestedItem(innerItem=Item(val=2)),
    ]
    assert TestExtractDeep(nested=items) == [1, 2]


def test_extract_deep_all_none():
    """Test deep extract with all nulls. Expected: None (current behavior of rune_resolve_attr)"""
    items = [NestedItem(innerItem=None), NestedItem(innerItem=Item(val=None))]
    # Note: dot access uses rune_resolve_attr which returns None for empty projected lists
    assert TestExtractDeep(nested=items) is None


def test_last_all_none():
    """Test last with all nulls. Expected: None"""
    assert TestLast(items=[None, None]) is None


def test_flatten_all_none():
    """Test flatten with all nulls. Expected: []"""
    groups = [ItemList(items=[None, None]), ItemList(items=None)]
    assert TestFlatten(groups=groups) == []


def test_distinct_all_none():
    """Test distinct with all nulls. Expected: []"""
    assert list(TestDistinct(items=[None, None])) == []


def test_sort_all_none():
    """Test sort with all nulls. Expected: []"""
    assert TestSort(items=[None, None]) == []


def test_join_all_none():
    """Test join with all nulls. Expected: ''"""
    assert TestJoin(items=[None, None], delimiter=",") == ""


def test_filter_all_none():
    """Test filter with all nulls. Expected: []"""
    assert TestFilter(items=[None, None]) == []
