"""Implicit closure parameters tests."""

from rosetta_dsl.test.semantic.expressions.implicit_closure.MapImplicit import (
    MapImplicit,
)
from rosetta_dsl.test.semantic.expressions.implicit_closure.ReduceImplicit import (
    ReduceImplicit,
)


def test_implicit_closure_map():
    """
    Attempting to import MapImplicit which is defined with `extract [ item * 2 ]`
    Currently, the generator / parser does not support implicit closure parameters correctly,
    so the Python generation fails to produce this function.
    """
    result = MapImplicit(items=[1, 2, 3])
    assert result == [2, 4, 6]


def test_implicit_closure_reduce():
    """
    Attempting to import ReduceImplicit which is defined with `reduce [ a + b ]`
    Currently, the generator / parser does not support implicit closure parameters correctly.
    """
    result = ReduceImplicit(items=[1, 2, 3])
    assert result == 6
