"""Complex boolean logic unit tests"""

from rosetta_dsl.test.semantic.operators.complex_boolean_logic.functions.NotOp import (
    NotOp,
)
from rosetta_dsl.test.semantic.operators.complex_boolean_logic.functions.ComplexLogic import (
    ComplexLogic,
)


def test_not_op():
    """Test logical negation via equality"""
    assert NotOp(b=True) is False
    assert NotOp(b=False) is True


def test_complex_logic():
    """Test logic: (a or b) and (not a)"""
    # effectively: (not a) and b
    assert ComplexLogic(a=True, b=True) is False  # (T or T) and F -> F
    assert ComplexLogic(a=True, b=False) is False  # (T or F) and F -> F
    assert ComplexLogic(a=False, b=True) is True  # (F or T) and T -> T
    assert ComplexLogic(a=False, b=False) is False  # (F or F) and T -> F


if __name__ == "__main__":
    test_not_op()
    test_complex_logic()
