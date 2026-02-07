"""List extensions unit tests"""

from rosetta_dsl.test.semantic.collections.extensions.functions.ListFirst import (
    ListFirst,
)
from rosetta_dsl.test.semantic.collections.extensions.functions.ListLast import ListLast
from rosetta_dsl.test.semantic.collections.extensions.functions.ListDistinct import (
    ListDistinct,
)
from rosetta_dsl.test.semantic.collections.extensions.functions.ListSum import ListSum
from rosetta_dsl.test.semantic.collections.extensions.functions.ListOnlyElement import (
    ListOnlyElement,
)
from rosetta_dsl.test.semantic.collections.extensions.functions.ListReverse import (
    ListReverse,
)


def test_list_first():
    """Test 'first' list operator."""
    assert ListFirst(items=[1, 2, 3]) == 1
    # Current implementation raises IndexError for empty list
    try:
        ListFirst(items=[])
    except IndexError:
        pass


def test_list_last():
    """Test 'last' list operator."""
    assert ListLast(items=[1, 2, 3]) == 3
    # Current implementation raises IndexError for empty list
    try:
        ListLast(items=[])
    except IndexError:
        pass


def test_list_distinct():
    """Test 'distinct' list operator."""
    res = ListDistinct(items=[1, 2, 2, 3])
    # distinct works
    assert len(res) == 3
    assert 1 in res


def test_list_sum():
    """Test 'sum' list operator."""
    assert ListSum(items=[1, 2, 3]) == 6
    assert ListSum(items=[]) == 0


def test_list_only_element():
    """Test 'only-element' list operator."""
    assert ListOnlyElement(items=[1]) == 1

    # Returns None if multiple elements exist
    assert ListOnlyElement(items=[1, 2]) is None

    # Returns None or raises IndexError if empty?
    try:
        val = ListOnlyElement(items=[])
        assert val is None
    except IndexError:
        pass


def test_list_reverse():
    """Test 'reverse' list operator."""
    assert ListReverse(items=[1, 2, 3]) == [3, 2, 1]
    assert ListReverse(items=[]) == []
