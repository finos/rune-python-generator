#
# Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
# SPDX-License-Identifier: Apache-2.0
#

"""Conditional expression unit tests"""

from rosetta_dsl.test.expressions.conditional.functions.ConditionalValue import (
    ConditionalValue,
)
from rosetta_dsl.test.expressions.conditional.functions.ConditionalNested import (
    ConditionalNested,
)


def test_conditional_value():
    """Test simple if-then-else expression."""
    assert ConditionalValue(param=20) == "High"
    assert ConditionalValue(param=5) == "Low"


def test_conditional_nested():
    """Test nested if-then-else expression."""
    assert ConditionalNested(param=20) == "High"
    assert ConditionalNested(param=8) == "Medium"
    assert ConditionalNested(param=2) == "Low"


if __name__ == "__main__":
    test_conditional_value()
    test_conditional_nested()
