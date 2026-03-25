"""Min Closure expression unit tests"""

from rosetta_dsl.test.expressions.min_closure.MinItem import MinItem
from rosetta_dsl.test.expressions.min_closure.functions.GetMinItem import GetMinItem


def test_min_closure():
    """Test min closure expression."""
    items = [MinItem(val1=5, val2=10), MinItem(val1=1, val2=100)]

    assert GetMinItem(items=items) == MinItem(val1=1, val2=100)
