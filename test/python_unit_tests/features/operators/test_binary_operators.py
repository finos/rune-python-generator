from rosetta_dsl.test.semantic.binary_op.ContainsTest import ContainsTest
from rosetta_dsl.test.semantic.binary_op.DisjointTest import DisjointTest
from rosetta_dsl.test.semantic.binary_op.EqualsTest import EqualsTest
from rosetta_dsl.test.semantic.binary_op.NotEqualsTest import NotEqualsTest


def test_equals():
    equals_test = EqualsTest(aValue=5, target=5)
    equals_test.validate_model()


def test_not_equals():
    not_equals_test = NotEqualsTest(aValue=5, target=15)
    not_equals_test.validate_model()


def test_contains():
    contains_test = ContainsTest(aValue=["a", "b", "c"], target="c")
    contains_test.validate_model()


def test_disjoint():
    disjoint_test = DisjointTest(aValue=["a", "b", "c"], target=["d", "e", "f"])
    disjoint_test.validate_model()


# EOF
