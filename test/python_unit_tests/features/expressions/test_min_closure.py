"""Min Closure expression unit tests"""

import pytest

from rosetta_dsl._bundle import (
    rosetta_dsl_test_semantic_expressions_min_closure_MinItem as MinItem,
    rosetta_dsl_test_semantic_expressions_min_closure_GetMinItem as GetMinItem,
)


def test_min_closure():
    """Test min closure expression."""
    items = [MinItem(val1=5, val2=10), MinItem(val1=1, val2=100)]

    assert GetMinItem(items=items) == MinItem(val1=1, val2=100)
