#
# Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
# SPDX-License-Identifier: Apache-2.0
#

"""Switch expression unit tests — switch as a value-returning expression.

Tests the switch keyword used inside a function body to return a value based
on the input.  For switch used as a condition guard inside a type definition,
see language/test_switch_operator.py.
"""

from rosetta_dsl.test.expressions.switch_op.functions.SwitchTest import (
    SwitchTest,
)


def test_switch_op():
    """Test switch operation."""
    # Test valid cases
    assert SwitchTest(x=1) == "One"
    assert SwitchTest(x=2) == "Two"

    # Test default case
    assert SwitchTest(x=3) == "Other"


if __name__ == "__main__":
    test_switch_op()
