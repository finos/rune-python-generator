#
# Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
# SPDX-License-Identifier: Apache-2.0
#

"""Sort Closure expression unit tests"""

from rosetta_dsl.test.expressions.sort_closure.SortItem import SortItem
from rosetta_dsl.test.expressions.sort_closure.functions.SortItems import SortItems


def test_sort_closure():
    """Test sort closure expression."""
    items = [SortItem(val1=5, val2=10), SortItem(val1=1, val2=100)]

    expected = [SortItem(val1=1, val2=100), SortItem(val1=5, val2=10)]
    assert SortItems(items=items) == expected
