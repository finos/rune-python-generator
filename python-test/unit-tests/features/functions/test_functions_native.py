#
# Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
# SPDX-License-Identifier: Apache-2.0
#

"""
Unit tests for the RoundToNearest function.
"""

from decimal import Decimal
from rosetta_dsl.test.functions.native.functions.RoundUp import RoundUp
from rosetta_dsl.test.functions.native.functions.RoundDown import RoundDown


def test_round_to_nearest_down():
    """Test rounding down to 2 decimal places."""
    value = Decimal("1.2345")
    digits = Decimal("2")

    result = RoundDown(value, digits)
    assert result == Decimal("1.23")


def test_round_to_nearest_up():
    """Test rounding down to 2 decimal places."""
    value = Decimal("1.2345")
    digits = Decimal("2")

    result = RoundUp(value, digits)
    assert result == Decimal("1.24")
