from rosetta_dsl.test.semantic.binary_op.ContainsTest import ContainsTest
from rosetta_dsl.test.semantic.binary_op.DisjointTest import DisjointTest
from rosetta_dsl.test.semantic.binary_op.EqualsTest import EqualsTest
from rosetta_dsl.test.semantic.binary_op.NotEqualsTest import NotEqualsTest


def test_equals():
    equalsTest=EqualsTest(aValue=5,target=5)
    equalsTest.validate_model()

def test_not_equals():
    notEqualsTets = NotEqualsTest(aValue=5, target=15)
    notEqualsTets.validate_model()

def test_contains():
    containsTest=ContainsTest(aValue=["a","b","c"],target="c")
    containsTest.validate_model()

def test_disjoint():
    disjointTest=DisjointTest(aValue=["a","b","c"], target=["d","e","f"])
    disjointTest.validate_model()
#EOF