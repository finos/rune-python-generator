'''to-int unit tests'''

import pytest

from rosetta_dsl.test.semantic.int_operator.IntOperatorTest import IntOperatorTest


def test_to_int_passes():
    '''no doc'''
    to_int_test = IntOperatorTest(a="1",b=1)
    to_int_test.validate_model()

def test_to_int_fails():
    '''no doc'''
    to_int_test= IntOperatorTest(a="a",b=1)
    with pytest.raises(Exception):
        to_int_test.validate_model()
        
if __name__ == "__main__":
    test_to_int_passes()
    test_to_int_fails()
