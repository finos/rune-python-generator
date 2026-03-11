"""Max Closure expression unit tests"""

from rosetta_dsl._bundle import (
    rosetta_dsl_test_semantic_expressions_max_closure_MaxItem as MaxItem,
    rosetta_dsl_test_semantic_expressions_max_closure_GetMaxItem as GetMaxItem,
)


def test_max_closure():
    """Test max closure expression."""
    items = [MaxItem(val1=1, val2=100), MaxItem(val1=5, val2=10)]

    assert GetMaxItem(items=items) == MaxItem(val1=5, val2=10)
