"""Reduce expression unit tests"""

from rosetta_dsl._bundle import (
    rosetta_dsl_test_semantic_expressions_reduce_op_SumList as SumList,
)


def test_reduce_op():
    """Test reduce operation."""
    items = [1, 2, 3, 4, 5]
    result = SumList(items)
    assert result == 15


def test_reduce_op_empty():
    """Test reduce operation with empty list."""
    items = []
    # functools.reduce raises TypeError if the sequence is empty and no initial value is provided.
    try:
        SumList(items)
        assert False, "Should have raised TypeError"
    except TypeError:
        pass
