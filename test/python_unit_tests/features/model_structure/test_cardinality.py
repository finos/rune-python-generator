'''cardinality unit test'''
import pytest
from rosetta_dsl.test.semantic.cardinality.CardinalityTest import CardinalityTest
from pydantic import ValidationError


def test_1_many_fail():
    '''list cannot be empty'''
    with pytest.raises(ValidationError):
        ct = CardinalityTest(attr=[])
        ct.validate_conditions()

def test_1_many_fail_empty_constructor():
    '''list cannot be empty'''
    with pytest.raises(ValidationError):
        ct = CardinalityTest()
        ct.validate_conditions()

def test_1_many_pass():
    '''Valid list'''
    ct = CardinalityTest(attr=[1])
    ct.validate_conditions()

if __name__ == "__main__":
    print("test_1_many_pass")
    test_1_many_pass()
    print("test_1_many_fail")
    test_1_many_fail()
    print("test_1_many_fail_empty_constructor")
    test_1_many_fail_empty_constructor()


# EOF