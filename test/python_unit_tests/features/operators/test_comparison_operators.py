"""Comparison operators unit tests"""

from rosetta_dsl.test.semantic.comparison_op.ComparisonTest import ComparisonTest


def test_less_than():
    """Test '<' operator."""
    ComparisonTest(a=5, b=10, op="LT", target=True).validate_model()
    ComparisonTest(a=10, b=5, op="LT", target=False).validate_model()
    ComparisonTest(a=5, b=5, op="LT", target=False).validate_model()


def test_less_than_or_equal():
    """Test '<=' operator."""
    ComparisonTest(a=5, b=10, op="LE", target=True).validate_model()
    ComparisonTest(a=5, b=5, op="LE", target=True).validate_model()
    ComparisonTest(a=10, b=5, op="LE", target=False).validate_model()


def test_greater_than():
    """Test '>' operator."""
    ComparisonTest(a=10, b=5, op="GT", target=True).validate_model()
    ComparisonTest(a=5, b=10, op="GT", target=False).validate_model()
    ComparisonTest(a=5, b=5, op="GT", target=False).validate_model()


def test_greater_than_or_equal():
    """Test '>=' operator."""
    ComparisonTest(a=10, b=5, op="GE", target=True).validate_model()
    ComparisonTest(a=5, b=5, op="GE", target=True).validate_model()
    ComparisonTest(a=5, b=10, op="GE", target=False).validate_model()
