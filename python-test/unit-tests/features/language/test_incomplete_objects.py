#
# Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
# SPDX-License-Identifier: Apache-2.0
#

"""Incomplete objects unit tests"""

from rosetta_dsl.test.language.IncompleteObjects.A import A
from rosetta_dsl.test.language.IncompleteObjects.functions.CreateA import CreateA


def test_create_incomplete_object():
    """Test creating an object in steps within a function."""
    res = CreateA(a1=10)
    assert isinstance(res, A)
    assert res.a1 == 10
    assert res.a2 == 20
