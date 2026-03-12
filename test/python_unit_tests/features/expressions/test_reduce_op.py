"""Reduce expression unit tests"""

from rosetta_dsl.test.semantic.expressions.reduce_op.SumList import SumList


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
