"""Sort Closure expression unit tests"""

from rosetta_dsl._bundle import (
    rosetta_dsl_test_semantic_expressions_sort_closure_SortItem as SortItem,
    rosetta_dsl_test_semantic_expressions_sort_closure_SortItems as SortItems,
)


def test_sort_closure():
    """Test sort closure expression."""
    items = [SortItem(val1=5, val2=10), SortItem(val1=1, val2=100)]

    expected = [SortItem(val1=1, val2=100), SortItem(val1=5, val2=10)]
    assert SortItems(items=items) == expected
