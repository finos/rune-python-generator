"""Switch expression unit tests"""

from rosetta_dsl.test.semantic.expressions.switch_op.functions.SwitchTest import (
    SwitchTest,
)


def test_switch_op():
    """Test switch operation."""
    # Test valid cases
    assert SwitchTest(x=1) == "One"
    assert SwitchTest(x=2) == "Two"

    # Test default case
    assert SwitchTest(x=3) == "Other"


if __name__ == "__main__":
    test_switch_op()
