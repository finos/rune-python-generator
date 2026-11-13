#
# Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
# SPDX-License-Identifier: Apache-2.0
#

'''functions and conditions that build types from a circular dependency group work'''
from rosetta_dsl.test.functions.bundled_type_name.Checker import Checker
from rosetta_dsl.test.functions.bundled_type_name.GroupB1 import GroupB1
from rosetta_dsl.test.functions.bundled_type_name.GroupB2 import GroupB2
from rosetta_dsl.test.functions.bundled_type_name.functions.MakeGroupB1 import MakeGroupB1
from rosetta_dsl.test.functions.bundled_type_name.functions.MakeMid import MakeMid
from rosetta_dsl.test.functions.bundled_type_name.functions.SetPathGroupB1 import SetPathGroupB1
from rosetta_dsl.test.functions.bundled_type_name.functions.ValueViaGroupB1 import ValueViaGroupB1


def test_function_builds_type():
    '''a function returns the type it builds'''
    result = MakeGroupB1(1)
    assert isinstance(result, GroupB1)
    assert result.value == 1


def test_function_sets_attribute_of_output():
    '''a function that sets one attribute of its output returns the type'''
    result = SetPathGroupB1(1)
    assert isinstance(result, GroupB1)
    assert result.value == 1


def test_function_builds_type_inside_another():
    '''a function builds one type inside another'''
    assert MakeMid(1).groupB1.value == 1


def test_function_uses_type_only_internally():
    '''a function builds a type it doesn't take or return'''
    assert ValueViaGroupB1(1) == 1


def test_standalone_condition_builds_type_from_group():
    '''a condition on a type outside the group builds a type from the group'''
    assert not Checker(value=1).validate_conditions(raise_exc=False)


def test_group_condition_builds_type_outside_group():
    '''a condition on a type in the group builds a type outside the group'''
    assert not GroupB2().validate_conditions(raise_exc=False)
