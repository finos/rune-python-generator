"""Type conversion unit tests"""

import pytest
from rosetta_dsl.test.semantic.expressions.type_conversion.StringToInt import (
    StringToInt,
)
from rosetta_dsl.test.semantic.expressions.type_conversion.IntToString import (
    IntToString,
)


def test_string_to_int():
    """Test string to integer conversion."""
    assert StringToInt(s="123") == 123
    with pytest.raises(Exception):  # ValueError or similar
        StringToInt(s="abc")


def test_int_to_string():
    """Test integer to string conversion."""
    assert IntToString(i=456) == "456"


if __name__ == "__main__":
    test_string_to_int()
    test_int_to_string()
