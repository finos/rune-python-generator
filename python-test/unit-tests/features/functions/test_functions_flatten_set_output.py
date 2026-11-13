#
# Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
# SPDX-License-Identifier: Apache-2.0
#

'''flatten, and multi-valued function outputs filled with set'''
from rosetta_dsl.test.functions.flatten_set_output.Branch import Branch
from rosetta_dsl.test.functions.flatten_set_output.Leaf import Leaf
from rosetta_dsl.test.functions.flatten_set_output.Tree import Tree
from rosetta_dsl.test.functions.flatten_set_output.TreeOfSetOutputs import TreeOfSetOutputs
from rosetta_dsl.test.functions.flatten_set_output.functions.AddFromSingle import AddFromSingle
from rosetta_dsl.test.functions.flatten_set_output.functions.CountSetOutput import CountSetOutput
from rosetta_dsl.test.functions.flatten_set_output.functions.ExtractFromSetOutput import (
    ExtractFromSetOutput)
from rosetta_dsl.test.functions.flatten_set_output.functions.FirstOfSetOutput import FirstOfSetOutput
from rosetta_dsl.test.functions.flatten_set_output.functions.FlattenAddOutputs import FlattenAddOutputs
from rosetta_dsl.test.functions.flatten_set_output.functions.FlattenConditionalLists import (
    FlattenConditionalLists)
from rosetta_dsl.test.functions.flatten_set_output.functions.FlattenModelLists import FlattenModelLists
from rosetta_dsl.test.functions.flatten_set_output.functions.FlattenSetListOutputs import (
    FlattenSetListOutputs)
from rosetta_dsl.test.functions.flatten_set_output.functions.FlattenSetOutputs import FlattenSetOutputs
from rosetta_dsl.test.functions.flatten_set_output.functions.FlattenStringLists import FlattenStringLists
from rosetta_dsl.test.functions.flatten_set_output.functions.SetFromList import SetFromList
from rosetta_dsl.test.functions.flatten_set_output.functions.SetFromSingle import SetFromSingle
from rosetta_dsl.test.functions.flatten_set_output.functions.SetFromSingleString import (
    SetFromSingleString)


def _leaf(value):
    return Leaf(value=value)


def _values(leaves):
    return [leaf.value for leaf in leaves]


def test_flatten_model_lists():
    '''lists of models flatten to the models, not to their fields'''
    branches = [Branch(leaves=[_leaf(1), _leaf(2)]), Branch(leaves=[_leaf(3)])]
    assert _values(FlattenModelLists(branches)) == [1, 2, 3]


def test_flatten_with_empty_inner_list():
    '''a branch with no leaves adds nothing'''
    assert _values(FlattenModelLists([Branch(), Branch(leaves=[_leaf(1)])])) == [1]


def test_flatten_string_lists_keeps_strings_whole():
    '''strings are not split into characters'''
    branches = [Branch(tags=['ab', 'cd']), Branch(tags=['e'])]
    assert FlattenStringLists(branches) == ['ab', 'cd', 'e']


def test_flatten_conditional_lists():
    '''inner lists may come from a conditional'''
    branches = [Branch(leaves=[_leaf(1), _leaf(2)]), Branch()]
    assert _values(FlattenConditionalLists(branches)) == [1, 2]


def test_flatten_in_condition_passes():
    '''two leaves are within the limit'''
    tree = Tree(branches=[Branch(leaves=[_leaf(1), _leaf(2)])])
    assert not tree.validate_conditions(raise_exc=False)


def test_flatten_in_condition_fails():
    '''three leaves are over the limit'''
    tree = Tree(branches=[Branch(leaves=[_leaf(1), _leaf(2), _leaf(3)])])
    assert len(tree.validate_conditions(raise_exc=False)) == 1


def test_flatten_outputs_filled_with_add():
    '''control: add always produces a list'''
    branches = [Branch(leaf=_leaf(1)), Branch(leaf=_leaf(2))]
    assert _values(FlattenAddOutputs(branches)) == [1, 2]


def test_flatten_outputs_filled_with_set_from_list():
    '''control: set from a list produces a list'''
    branches = [Branch(leaves=[_leaf(1), _leaf(2)]), Branch(leaves=[_leaf(3)])]
    assert _values(FlattenSetListOutputs(branches)) == [1, 2, 3]


def test_flatten_outputs_filled_with_set_from_single():
    '''each inner output has one model, so there is one result per branch'''
    branches = [Branch(leaf=_leaf(1)), Branch(leaf=_leaf(2))]
    assert _values(FlattenSetOutputs(branches)) == [1, 2]


def test_flatten_set_outputs_in_condition():
    '''the same pattern must also work in a class condition'''
    tree = TreeOfSetOutputs(branches=[Branch(leaf=_leaf(1))])
    assert not tree.validate_conditions(raise_exc=False)


def test_add_from_single_returns_list():
    '''control: a multi-valued output filled with add is a list'''
    assert _values(AddFromSingle(_leaf(1))) == [1]


def test_set_from_list_returns_list():
    '''control: a multi-valued output set from a list is a list'''
    assert _values(SetFromList([_leaf(1), _leaf(2)])) == [1, 2]


def test_set_from_single_model_returns_list():
    '''a multi-valued output is a list even when set from one model'''
    result = SetFromSingle(_leaf(1))
    assert isinstance(result, list)
    assert _values(result) == [1]


def test_set_from_single_string_returns_list():
    '''a multi-valued output is a list even when set from one basic value'''
    assert SetFromSingleString('a') == ['a']


def test_set_from_absent_single_has_no_items():
    '''an absent input gives an output with no items'''
    assert not SetFromSingle(None)


def test_extract_from_set_output():
    '''extract treats the declared multi-valued output as a list'''
    assert ExtractFromSetOutput(_leaf(1)) == [1]


def test_count_set_output():
    '''count sees one item'''
    assert CountSetOutput(_leaf(1)) == 1


def test_first_of_set_output():
    '''first returns the model, not one of its fields'''
    assert FirstOfSetOutput(_leaf(1)).value == 1
