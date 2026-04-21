#
# Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
# SPDX-License-Identifier: Apache-2.0
#

from rosetta_dsl.test.functions.functions.MainFunction import MainFunction


def test_function_with_function_call():
    """Test function with function call"""
    assert MainFunction(value=5) == 10
