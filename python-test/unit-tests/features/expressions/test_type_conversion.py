#
# Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
# SPDX-License-Identifier: Apache-2.0
#

"""Type conversion unit tests"""

import pytest
from rosetta_dsl.test.expressions.type_conversion.functions.StringToInt import (
    StringToInt,
)
from rosetta_dsl.test.expressions.type_conversion.functions.IntToString import (
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
