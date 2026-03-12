"""Type conversion unit tests"""

from rosetta_dsl.test.semantic.expressions.type_conversion.StringToIntTest import (
    StringToIntTest,
)
from rosetta_dsl.test.semantic.expressions.type_conversion.IntToStringTest import (
    IntToStringTest,
)


def test_string_to_int():
    """Test 'to-int' conversion."""
    StringToIntTest(s="123", target=123).validate_model()


def test_int_to_string():
    """Test 'to-string' conversion."""
    IntToStringTest(i=456, target="456").validate_model()
