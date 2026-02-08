"""
Test that the generator generates the correct order of classes and functions.
"""

from rosetta_dsl.test.functions.order.ClassA import ClassA
from rosetta_dsl.test.functions.order.ClassB import ClassB
from rosetta_dsl.test.functions.order.functions.MyFunc import MyFunc


def test_function_order():
    """
    Test that the generator generates the correct order of classes and functions.
    """
    # If the ordering is wrong, the import of MyFunc will fail during decorator execution
    # with a NameError because ClassB depends on ClassA, and MyFunc depends on both.
    a = ClassA(val="hello")
    b = ClassB(attr=a)
    result = MyFunc(b)
    assert result.val == "hello"
