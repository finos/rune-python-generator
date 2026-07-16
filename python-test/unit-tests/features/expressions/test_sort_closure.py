#
# Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
# SPDX-License-Identifier: Apache-2.0
#

"""Sort closure expression unit tests."""

from rosetta_dsl.test.expressions.sort_closure.SortItem import SortItem
from rosetta_dsl.test.expressions.sort_closure.functions.SortItems import SortItems
from rosetta_dsl.test.expressions.sort_closure.functions.SortItemsBySecondField import (
    SortItemsBySecondField,
)
from rosetta_dsl.test.expressions.sort_closure.functions.SortIntegers import SortIntegers


def test_sort_closure():
    """Sort complex objects by key field val1."""
    items = [SortItem(val1=5, val2=10), SortItem(val1=1, val2=100)]
    expected = [SortItem(val1=1, val2=100), SortItem(val1=5, val2=10)]
    assert SortItems(items=items) == expected


def test_sort_by_second_field():
    """Sort complex objects by key field val2."""
    items = [SortItem(val1=5, val2=100), SortItem(val1=1, val2=10)]
    result = SortItemsBySecondField(items=items)
    expected = [SortItem(val1=1, val2=10), SortItem(val1=5, val2=100)]
    assert result == expected


def test_sort_integers_no_key():
    """Plain sort without a key expression sorts a list of integers."""
    items = [3, 1, 4, 1, 5, 9, 2]
    result = SortIntegers(items=items)
    assert result == [1, 1, 2, 3, 4, 5, 9]


def test_sort_preserves_all_non_null_items():
    """All non-null items survive the sort; order is ascending by key."""
    items = [SortItem(val1=10, val2=1), SortItem(val1=2, val2=5)]
    result = SortItems(items=items)
    assert len(result) == 2
    assert result[0].val1 == 2
    assert result[1].val1 == 10
