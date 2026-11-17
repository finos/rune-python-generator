#
# Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
# SPDX-License-Identifier: Apache-2.0
#

'''filter on a single value gives that value, or nothing'''
from rosetta_dsl.test.functions.filter_single_value.Holder import Holder
from rosetta_dsl.test.functions.filter_single_value.Leaf import Leaf
from rosetta_dsl.test.functions.filter_single_value.functions.FilterSingle import FilterSingle
from rosetta_dsl.test.functions.filter_single_value.functions.FilterSingleThenCount import (
    FilterSingleThenCount)


def test_filter_keeps_single_value_that_passes():
    '''the value is returned when the test holds'''
    result = FilterSingle(Leaf(value=1))
    assert isinstance(result, Leaf)
    assert result.value == 1


def test_filter_drops_single_value_that_fails():
    '''nothing is returned when the test fails'''
    assert FilterSingle(Leaf()) is None


def test_filter_of_absent_value():
    '''an absent value gives nothing'''
    assert FilterSingle(None) is None


def test_count_after_filter_of_value_that_passes():
    '''the kept value counts as one'''
    assert FilterSingleThenCount(Leaf(value=1)) == 1


def test_count_after_filter_of_value_that_fails():
    '''a dropped value counts as none'''
    assert FilterSingleThenCount(Leaf()) == 0


def test_condition_with_value_that_passes():
    '''the condition sees one kept value'''
    assert not Holder(lone=Leaf(value=1)).validate_conditions(raise_exc=False)


def test_condition_with_value_that_fails():
    '''the condition sees no kept value'''
    assert not Holder(lone=Leaf()).validate_conditions(raise_exc=False)
