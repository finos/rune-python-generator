'''cardinality unit test'''
import pytest

from rosetta_dsl.test.semantic.conditions.UnitType import UnitType
from rosetta_dsl.test.semantic.conditions.Frequency import Frequency
from rosetta_dsl.test.semantic.conditions.NonNegativeQuantity import NonNegativeQuantity
from pydantic import ValidationError
from rune.runtime.conditions import ConditionViolationError


def test_correct_attrib_validation():
    '''Valid attribute assigned'''
    unit = UnitType(currency='EUR')
    mq = NonNegativeQuantity(value=10, unit=unit)
    mq.frequency = Frequency(periodMultiplier=1)
    mq.validate_model()

def test_bad_attrib_validation():
    '''Invalid attribute assigned'''
    unit = UnitType(currency='EUR')
    mq = NonNegativeQuantity(value=10, unit=unit)
    mq.frequency = 'Blah'
    with pytest.raises(ValidationError):
        mq.validate_model()

def test_recursive_conditions_direct_fail():
    '''Negative quantity condition violation'''
    unit = UnitType(currency='EUR')
    mq = NonNegativeQuantity(value=-10, unit=unit)
    with pytest.raises(ConditionViolationError):
        mq.validate_model()

def test_recursive_conditions_base_fail():
    '''condition_0_AmountOnlyExists violation'''
    unit = UnitType(currency='EUR')
    mq = NonNegativeQuantity(unit=unit)
    with pytest.raises(ConditionViolationError):
        mq.validate_model()

if __name__ == "__main__":
    print("test_correct_attrib_validation")
    test_correct_attrib_validation()
    print("test_bad_attrib_validation")
    test_bad_attrib_validation()
    print("test_recursive_conditions_direct_fail")
    test_recursive_conditions_direct_fail()
    print("test_recursive_conditions_base_fail")
    test_recursive_conditions_base_fail()


# EOF