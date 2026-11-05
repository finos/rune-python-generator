#
# Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
# SPDX-License-Identifier: Apache-2.0
#

'''count of a single model object is 1, not its number of fields'''
from rosetta_dsl.test.functions.count_single_value.Holder import Holder
from rosetta_dsl.test.functions.count_single_value.Inner import Inner
from rosetta_dsl.test.functions.count_single_value.Parent import Parent
from rosetta_dsl.test.functions.count_single_value.functions.CountSeveral import CountSeveral
from rosetta_dsl.test.functions.count_single_value.functions.CountSingle import CountSingle
from rosetta_dsl.test.functions.count_single_value.functions.CountSingleAttribute import (
    CountSingleAttribute)


def test_condition_counts_single_object_as_one():
    '''a condition sees the model object itself'''
    assert not Holder(lone=Inner(value=1)).validate_conditions(raise_exc=False)


def test_condition_counts_absent_object_as_zero():
    '''an absent object counts as 0'''
    assert not Holder().validate_conditions(raise_exc=False)


def test_condition_counts_list():
    '''control: a list longer than the limit still fails the condition'''
    inner = Inner(value=1)
    assert len(Holder(several=[inner, inner, inner]).validate_conditions(raise_exc=False)) == 1


def test_function_counts_single_object_as_one():
    '''a function sees a copy-on-write proxy for the object'''
    assert CountSingle(Inner(value=1)) == 1


def test_function_counts_absent_object_as_zero():
    '''an absent input counts as 0'''
    assert CountSingle(None) == 0


def test_function_counts_attribute_path_to_single_object():
    '''a path ending in a single object counts as 1'''
    assert CountSingleAttribute(Parent(inner=Inner(value=1))) == 1


def test_function_counts_list():
    '''control: a list counts its items'''
    inner = Inner(value=1)
    assert CountSeveral([inner, inner]) == 2
