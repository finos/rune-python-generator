"""
Test that the generator processes lists correctly.
"""

from rosetta_dsl.test.functions.order.functions.TestListFunction import TestListFunction


def test_function_list():
    """
    Test that the generator processes lists correctly.
    """
    result = TestListFunction([10, 20, 30], 5)
    assert result == [50, 100, 150]
