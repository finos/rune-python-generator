#
# Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
# SPDX-License-Identifier: Apache-2.0
#

"""Reduce expression unit tests"""

from rosetta_dsl.test.expressions.reduce_op.functions.SumList import SumList


def test_reduce_op():
    """Test reduce operation."""
    items = [1, 2, 3, 4, 5]
    result = SumList(items)
    assert result == 15


def test_reduce_op_empty():
    """Test reduce operation with empty list."""
    items = []
    # functools.reduce raises TypeError if the sequence is empty and no initial value is provided.
    try:
        SumList(items)
        assert False, "Should have raised TypeError"
    except TypeError:
        pass
