#
# Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
# SPDX-License-Identifier: Apache-2.0
#

"""Null handling unit tests"""

from rosetta_dsl.test.robustness.null_handling.functions.IsAbsent import (
    IsAbsent,
)
from rosetta_dsl.test.robustness.null_handling.functions.IsAbsentList import (
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
