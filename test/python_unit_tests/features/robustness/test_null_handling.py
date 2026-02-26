"""Null handling unit tests"""

from rosetta_dsl.test.semantic.robustness.null_handling.IsAbsent import (
    IsAbsent,
)
from rosetta_dsl.test.semantic.robustness.null_handling.IsAbsentList import (
    IsAbsentList,
)


def test_is_absent():
    """Test 'is absent' check on scalar value."""
    assert IsAbsent(val=None) is True
    assert IsAbsent(val="foo") is False


def test_is_absent_list():
    """Test 'is absent' check on list of values."""
    assert IsAbsentList(list=[]) is True
    # If list is explicit None?
    assert IsAbsentList(list=None) is True
    assert IsAbsentList(list=[1]) is False
