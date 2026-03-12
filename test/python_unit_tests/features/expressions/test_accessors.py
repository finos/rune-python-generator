"""Accessors expression unit tests"""

from rosetta_dsl._bundle import (
    rosetta_dsl_test_semantic_expressions_accessors_TestFirst as TestFirst,
    rosetta_dsl_test_semantic_expressions_accessors_TestLast as TestLast,
    rosetta_dsl_test_semantic_expressions_accessors_TestFirstFiltered as TestFirstFiltered,
    rosetta_dsl_test_semantic_expressions_accessors_TestLastFiltered as TestLastFiltered,
    rosetta_dsl_test_semantic_expressions_accessors_Item as Item,
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
    assert TestLastFiltered(items=items) == 2 # wait, the test says items=[1, 2, None] -> last is 2. 
    # Oops, my last test said 3 but I passed 2. Corrected to 2.


def test_first_with_all_none_list():
    """Test first operation with list of all Nones. Expected: None."""
    items = [Item(val=None), Item(val=None)]
    assert TestFirstFiltered(items=items) is None
