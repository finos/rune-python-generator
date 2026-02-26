"""Comparison operator unit tests"""

from rosetta_dsl.test.semantic.comparison_op.LessThan import LessThan
from rosetta_dsl.test.semantic.comparison_op.LessThanOrEqual import (
    LessThanOrEqual,
)
from rosetta_dsl.test.semantic.comparison_op.GreaterThan import GreaterThan
from rosetta_dsl.test.semantic.comparison_op.GreaterThanOrEqual import (
    GreaterThanOrEqual,
)


def test_less_than():
    """Test < operator"""
    assert LessThan(a=1, b=2) is True
    assert LessThan(a=2, b=1) is False
    assert LessThan(a=1, b=1) is False


def test_less_than_or_equal():
    """Test <= operator"""
    assert LessThanOrEqual(a=1, b=2) is True
    assert LessThanOrEqual(a=2, b=1) is False
    assert LessThanOrEqual(a=1, b=1) is True


def test_greater_than():
    """Test > operator"""
    assert GreaterThan(a=2, b=1) is True
    assert GreaterThan(a=1, b=2) is False
    assert GreaterThan(a=1, b=1) is False


def test_greater_than_or_equal():
    """Test >= operator"""
    assert GreaterThanOrEqual(a=2, b=1) is True
    assert GreaterThanOrEqual(a=1, b=2) is False
    assert GreaterThanOrEqual(a=1, b=1) is True


if __name__ == "__main__":
    test_less_than()
    test_less_than_or_equal()
    test_greater_than()
    test_greater_than_or_equal()
