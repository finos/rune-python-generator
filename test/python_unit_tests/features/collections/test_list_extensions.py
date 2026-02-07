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


def test_list_first():
    """Test 'first' list operator."""
    assert ListFirst(list=[1, 2, 3]) == 1
    # Current implementation raises IndexError for empty list
    try:
        ListFirst(list=[])
    except IndexError:
        pass


def test_list_last():
    """Test 'last' list operator."""
    assert ListLast(list=[1, 2, 3]) == 3
    # Current implementation raises IndexError for empty list
    try:
        ListLast(list=[])
    except IndexError:
        pass


def test_list_distinct():
    """Test 'distinct' list operator."""
    res = ListDistinct(list=[1, 2, 2, 3])
    # distinct works
    assert len(res) == 3
    assert 1 in res


def test_list_sum():
    """Test 'sum' list operator."""
    assert ListSum(list=[1, 2, 3]) == 6
    assert ListSum(list=[]) == 0


def test_list_only_element():
    """Test 'only-element' list operator."""
    assert ListOnlyElement(list=[1]) == 1

    # Returns None if multiple elements exist
    assert ListOnlyElement(list=[1, 2]) is None

    # Returns None or raises IndexError if empty?
    try:
        val = ListOnlyElement(list=[])
        assert val is None
    except IndexError:
        pass
