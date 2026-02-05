"""test functions incomplete object return"""

import pytest
from pydantic import ValidationError

from rosetta_dsl.test.functions.functions.TestIncompleteObjectReturn import (
    TestIncompleteObjectReturn,
)


def test_incomplete_object_return():
    """Test incomplete object return.
    The Rosetta function returns an IncompleteObject with a missing required field (value2),
    so this is expected to raise a validation exception.
    """
    with pytest.raises(ValidationError):
        TestIncompleteObjectReturn(value1=5)
