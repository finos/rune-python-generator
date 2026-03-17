"""Switch expression unit tests"""

from rosetta_dsl.test.semantic.expressions.switch_op.SwitchTest import SwitchTest


def test_switch_op():
    """Test switch operation."""
    # Test valid cases
    SwitchTest(x=1, target="One").validate_model()
    SwitchTest(x=2, target="Two").validate_model()

    # Test default case
    SwitchTest(x=3, target="Other").validate_model()


if __name__ == "__main__":
    test_switch_op()
