'''Enum qualified path name unit tests.

Tests that enum values can be referenced via qualified path names in conditions
(e.g. TestEnum.B) and that condition evaluation works correctly.
'''
import pytest

from rosetta_dsl.test.semantic.enum_qualified_path_name.TestEnum import TestEnum
from rosetta_dsl.test.semantic.enum_qualified_path_name.EnumTestType import EnumTestType
from rune.runtime.conditions import ConditionViolationError


def test_enum_qualified_path_condition_passes():
    '''Condition passes when enum attribute matches the qualified path value'''
    ett = EnumTestType(ett=TestEnum.B)
    ett.validate_model()


def test_enum_qualified_path_condition_fails():
    '''Condition fails when enum attribute does not match the qualified path value'''
    ett = EnumTestType(ett=TestEnum.A)
    with pytest.raises(ConditionViolationError):
        ett.validate_model()


if __name__ == "__main__":
    test_enum_qualified_path_condition_passes()
    test_enum_qualified_path_condition_fails()


# EOF
