"""Null handling unit tests"""

from rosetta_dsl.test.semantic.robustness.null_handling.IsAbsentTest import (
    IsAbsentTest,
)
from rosetta_dsl.test.semantic.robustness.null_handling.IsAbsentListTest import (
    IsAbsentListTest,
)


def test_is_absent():
    """Test 'is absent' check on scalar value."""
    IsAbsentTest(val=None, target=True).validate_model()
    IsAbsentTest(val="foo", target=False).validate_model()


def test_is_absent_list():
    """Test 'is absent' check on list of values."""
    # Renamed field from 'list' to 'items' to avoid name collision with built-in list type
    IsAbsentListTest(items=[], target=True).validate_model()
    # If list is explicit None?
    IsAbsentListTest(items=None, target=True).validate_model()
    IsAbsentListTest(items=[1], target=False).validate_model()
