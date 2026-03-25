"""Tests for one-of expression support."""

from rosetta_dsl.test.expressions.one_of_expression.functions.CheckOneOf import (
    CheckOneOf,
)
from rosetta_dsl.test.expressions.one_of_expression.Message import Message


def test_one_of_expression():
    """
    Test CheckOneOf which is defined with `one-of` as a general expression.
    """

    # Exactly one field is set
    assert CheckOneOf(Message(fieldA=10)) is True
    assert CheckOneOf(Message(fieldB=20)) is True

    # No fields set or both fields set
    assert CheckOneOf(Message()) is False
    assert CheckOneOf(Message(fieldA=10, fieldB=20)) is False
