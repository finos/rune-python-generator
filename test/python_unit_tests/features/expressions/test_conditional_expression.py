"""Conditional expression unit tests"""

from rosetta_dsl.test.semantic.expressions.conditional.ConditionalValueTest import (
    ConditionalValueTest,
)
from rosetta_dsl.test.semantic.expressions.conditional.ConditionalNestedTest import (
    ConditionalNestedTest,
)


def test_conditional_value():
    """Test simple if-then-else expression."""
    ConditionalValueTest(param=20, target="High").validate_model()
    ConditionalValueTest(param=5, target="Low").validate_model()


def test_conditional_nested():
    """Test nested if-then-else expression."""
    ConditionalNestedTest(param=20, target="High").validate_model()
    ConditionalNestedTest(param=8, target="Medium").validate_model()
    ConditionalNestedTest(param=2, target="Low").validate_model()


if __name__ == "__main__":
    test_conditional_value()
    test_conditional_nested()
