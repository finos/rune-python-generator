from rosetta_dsl.test.functions.BaseObject import BaseObject
from rosetta_dsl.test.functions.TestPassByValue import TestPassByValue


def test_function_pass_by_value():
    """Test function with function call"""
    original = BaseObject(value1=5, value2=10)
    copy = TestPassByValue(original)
    assert original.value1 == 5
    assert copy.value1 == -5
