#
# Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
# SPDX-License-Identifier: Apache-2.0
#

"""Unit tests for any/all <> (not-equals) with list ."""

from rosetta_dsl.test.expressions.cardinalitynotequals.functions.TestAnyNotEqualsList import TestAnyNotEqualsList
from rosetta_dsl.test.expressions.cardinalitynotequals.functions.TestAllNotEqualsList import TestAllNotEqualsList
from rosetta_dsl.test.expressions.cardinalitynotequals.functions.TestNotEqualsList import TestNotEqualsList

# any <> y (scalar): true if any element of x != y

def test_any_not_equals_list__true_when_any_element_differs():
    # "B" != "A" satisfies the condition
    assert TestAnyNotEqualsList(x=["A", "B"], y="A")


def test_any_not_equals_list__false_when_all_elements_equal_y():
    # every x_i == y
    assert not TestAnyNotEqualsList(x=["A", "A"], y="A")


def test_any_not_equals_list__true_when_single_element_differs():
    assert TestAnyNotEqualsList(x=["B"], y="A")


def test_any_not_equals_list__false_when_single_element_equals_y():
    assert not TestAnyNotEqualsList(x=["A"], y="A")


# all <> y (scalar): true if no element of x equals y

def test_all_not_equals_list__true_when_all_differ_from_y():
    assert TestAllNotEqualsList(x=["A", "B"], y="C")


def test_all_not_equals_list__false_when_one_element_equals_y():
    assert not TestAllNotEqualsList(x=["A", "B"], y="A")


def test_all_not_equals_list__false_when_all_elements_equal_y():
    assert not TestAllNotEqualsList(x=["A", "A"], y="A")


def test_all_not_equals_list__false_when_last_element_equals_y():
    assert not TestAllNotEqualsList(x=["A", "B"], y="B")


def test_all_not_equals_list__single_elements():
    assert TestAllNotEqualsList(x=["A"], y="B")
    assert not TestAllNotEqualsList(x=["A"], y="A")

def test_not_equals_list__false_when_all_pairs_equal():
    # every x_i == every y_j (all same value), cross-product all equal
    assert not TestNotEqualsList(x=["A", "B"], y=["A", "B"])

def test_not_equals_list__true_when_any_pair_differs():
    # "x" != "y" satisfies the condition
    assert TestNotEqualsList(x=["A", "B"], y=["C", "D"])

def test_not_equals_list__false_when_list_lengths_differ():
    # every x_i == every y_j (all same value), cross-product all equal
    assert TestNotEqualsList(x=["A", "B"], y=["C"])