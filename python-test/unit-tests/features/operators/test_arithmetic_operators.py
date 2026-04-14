#
# Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
# SPDX-License-Identifier: Apache-2.0
#

from rosetta_dsl.test.arithmetic_op.AddTest import AddTest
from rosetta_dsl.test.arithmetic_op.SubtractTest import SubtractTest
from rosetta_dsl.test.arithmetic_op.MultiplyTest import MultiplyTest
from rosetta_dsl.test.arithmetic_op.DivideTest import DivideTest


def test_add():
    """Test add operator"""
    add_test = AddTest(aValue=5, bValue=10, target=15)
    add_test.validate_model()


def test_subtract():
    """Test subtract operator"""
    subtract_test = SubtractTest(aValue=5, bValue=10, target=-5)
    subtract_test.validate_model()


def test_multiply():
    """Test multiply operator"""
    multiply_test = MultiplyTest(aValue=5, bValue=10, target=50)
    multiply_test.validate_model()


def test_divide():
    """Test divide operator"""
    divide_test = DivideTest(aValue=10, bValue=2, target=5)
    divide_test.validate_model()


# EOF
