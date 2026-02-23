"""Complex boolean logic unit tests"""

from rosetta_dsl.test.semantic.operators.complex_boolean_logic.NotOpTest import (
    NotOpTest,
)
from rosetta_dsl.test.semantic.operators.complex_boolean_logic.ComplexLogicTest import (
    ComplexLogicTest,
)


def test_not_op():
    """Test negation by equality."""
    NotOpTest(a=True, target=False).validate_model()
    NotOpTest(a=False, target=True).validate_model()


def test_complex_logic():
    """Test complex boolean expression with negation by equality."""
    # (a or b) and (not a)
    # T, T -> (T or T) and F -> T and F -> F
    ComplexLogicTest(a=True, b=True, target=False).validate_model()
    # F, T -> (F or T) and T -> T and T -> T
    ComplexLogicTest(a=False, b=True, target=True).validate_model()
    # F, F -> (F or F) and T -> F and T -> F
    ComplexLogicTest(a=False, b=False, target=False).validate_model()
