#
# Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
# SPDX-License-Identifier: Apache-2.0
#

"""Implicit closure parameters tests."""

from rosetta_dsl.test.expressions.implicit_closure.functions.MapImplicit import (
    MapImplicit,
)


def test_implicit_closure_map():
    """
    Attempting to import MapImplicit which is defined with `extract [ item * 2 ]`
    Currently, the generator / parser does not support implicit closure parameters correctly,
    so the Python generation fails to produce this function.
    """
    result = MapImplicit(items=[1, 2, 3])
    assert result == [2, 4, 6]
