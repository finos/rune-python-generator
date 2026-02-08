"""Inheritance unit tests"""

import pytest
from rosetta_dsl.test.semantic.model_structure.inheritance.Sub import Sub
from rosetta_dsl.test.semantic.model_structure.inheritance.functions.ProcessSuper import (
    ProcessSuper,
)


def test_inheritance_structure():
    """Test that Sub has both superAttr and subAttr"""
    sub = Sub(superAttr="parent", subAttr=10)
    assert sub.superAttr == "parent"
    assert sub.subAttr == 10

    # check if superAttr is part of validation
    with pytest.raises(Exception):  # ValidationError or strict check
        sub_fail = Sub(subAttr=10)  # Missing superAttr
        sub_fail.validate_model()


def test_polymorphism():
    """Test passing Sub to a function expecting Super"""
    sub = Sub(superAttr="hello", subAttr=20)
    result = ProcessSuper(s=sub)
    assert result == "hello"


if __name__ == "__main__":
    test_inheritance_structure()
    test_polymorphism()
