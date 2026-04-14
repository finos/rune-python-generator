#
# Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
# SPDX-License-Identifier: Apache-2.0
#

import pytest
from rosetta_dsl.test.logical_op.AndOperatorTest import AndOperatorTest
from rosetta_dsl.test.logical_op.OrOperatorTest import OrOperatorTest

def test_and_operator():
    logicalTest = AndOperatorTest(aValue=4, bValue=4, cValue=4)
    logicalTest.validate_model()

def test_and_operator_failure():
    logicalTest = AndOperatorTest(aValue=4, bValue=2, cValue=4)
    with pytest.raises(Exception):
        logicalTest.validate_model()

def test_or_operator():
    logicalTest= OrOperatorTest(aValue=1,bValue=2,cValue=1)
    logicalTest.validate_model()

def test_or_operator_failure():
    logicalTest= OrOperatorTest(aValue=1,bValue=2,cValue=3)
    with pytest.raises(Exception):
        logicalTest.validate_model()