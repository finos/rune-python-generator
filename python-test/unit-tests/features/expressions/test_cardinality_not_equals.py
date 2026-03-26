"""Unit tests for any/all <> (not-equals) with list ."""

from rosetta_dsl.test.expressions.cardinalitynotequals.functions.TestAnyNotEqualsList import TestAnyNotEqualsList
from rosetta_dsl.test.expressions.cardinalitynotequals.functions.TestAllNotEqualsList import TestAllNotEqualsList
from rosetta_dsl.test.expressions.cardinalitynotequals.functions.TestNotEqualsList import TestNotEqualsList

# any <> list: cross-product — true if any element of x != any element of y

def test_any_not_equals_list__true_when_any_pair_differs():
    # "A" != "B" satisfies the condition
    assert TestAnyNotEqualsList(x=["A", "B"], y=["A", "B"])


def test_any_not_equals_list__false_when_all_pairs_equal():
    # every x_i == every y_j (all same value), cross-product all equal
    assert not TestAnyNotEqualsList(x=["A", "A"], y=["A", "A"])


def test_any_not_equals_list__true_when_x_has_extra_value():
    assert TestAnyNotEqualsList(x=["A", "B"], y=["A"])


def test_any_not_equals_list__true_when_y_has_extra_value():
    assert TestAnyNotEqualsList(x=["A"], y=["A", "B"])


# all <> list: pairwise zip — true if every x_i != y_i (same length required)

def test_all_not_equals_list__true_when_all_pairs_differ():
    assert TestAllNotEqualsList(x=["A", "B"], y=["C", "D"])


def test_all_not_equals_list__false_when_any_pair_equal():
    assert not TestAllNotEqualsList(x=["A", "B"], y=["A", "D"])


def test_all_not_equals_list__false_when_lengths_differ():
    # pairwise zip requires equal lengths
    assert not TestAllNotEqualsList(x=["A", "B"], y=["C"])


def test_all_not_equals_list__false_when_all_equal():
    assert not TestAllNotEqualsList(x=["A", "B"], y=["A", "B"])


def test_all_not_equals_list__single_elements():
    assert TestAllNotEqualsList(x=["A"], y=["B"])
    assert not TestAllNotEqualsList(x=["A"], y=["A"])

def test_not_equals_list__false_when_all_pairs_equal():
    # every x_i == every y_j (all same value), cross-product all equal
    assert not TestNotEqualsList(x=["A", "B"], y=["A", "B"])

def test_not_equals_list__true_when_any_pair_differs():
    # "x" != "y" satisfies the condition
    assert TestNotEqualsList(x=["A", "B"], y=["C", "D"])

def test_not_equals_list__false_when_list_lengths_differ():
    # every x_i == every y_j (all same value), cross-product all equal
    assert TestNotEqualsList(x=["A", "B"], y=["C"])