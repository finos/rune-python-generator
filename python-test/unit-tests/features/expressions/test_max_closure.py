"""Max Closure expression unit tests"""

from rosetta_dsl.test.expressions.max_closure.MaxItem import MaxItem
from rosetta_dsl.test.expressions.max_closure.functions.GetMaxItem import GetMaxItem


def test_max_closure():
    """Test max closure expression."""
    items = [MaxItem(val1=1, val2=100), MaxItem(val1=5, val2=10)]

    assert GetMaxItem(items=items) == MaxItem(val1=5, val2=10)
