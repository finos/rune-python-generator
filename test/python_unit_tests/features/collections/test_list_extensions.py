"""List extensions unit tests"""

import pytest
from rosetta_dsl.test.semantic.collections.extensions.ListFirstTest import (
    ListFirstTest,
)
from rosetta_dsl.test.semantic.collections.extensions.ListLastTest import ListLastTest
from rosetta_dsl.test.semantic.collections.extensions.ListDistinctTest import (
    ListDistinctTest,
)
from rosetta_dsl.test.semantic.collections.extensions.ListSumTest import ListSumTest
from rosetta_dsl.test.semantic.collections.extensions.ListOnlyElementTest import (
    ListOnlyElementTest,
)
from rosetta_dsl.test.semantic.collections.extensions.ListReverseTest import (
    ListReverseTest,
)


def test_list_first():
    """Test 'first' list operator."""
    ListFirstTest(items=[1, 2, 3], target=1).validate_model()
    # Current implementation raises IndexError for empty list
    with pytest.raises(Exception):
        ListFirstTest(items=[], target=None).validate_model()


def test_list_last():
    """Test 'last' list operator."""
    ListLastTest(items=[1, 2, 3], target=3).validate_model()
    # Current implementation raises IndexError for empty list
    with pytest.raises(Exception):
        ListLastTest(items=[], target=None).validate_model()


def test_list_distinct():
    """Test 'distinct' list operator."""
    ListDistinctTest(items=[1, 2, 2, 3], target=[1, 2, 3]).validate_model()


def test_list_sum():
    """Test 'sum' list operator."""
    ListSumTest(items=[1, 2, 3], target=6).validate_model()
    ListSumTest(items=[], target=0).validate_model()


def test_list_only_element():
    """Test 'only-element' list operator."""
    ListOnlyElementTest(items=[1], target=1).validate_model()

    # Returns None if multiple elements exist
    ListOnlyElementTest(items=[1, 2], target=None).validate_model()

    # Returns None if empty
    ListOnlyElementTest(items=[], target=None).validate_model()


def test_list_reverse():
    """Test 'reverse' list operator."""
    ListReverseTest(items=[1, 2, 3], target=[3, 2, 1]).validate_model()
    ListReverseTest(items=[], target=[]).validate_model()
