'''to-int unit tests'''

import pytest

from rosetta_dsl.test.semantic.toIntOp.TestToIntOp import TestToIntOp


def test_to_int_passes():
    to_int_test = TestToIntOp(a="1",b=1)
    to_int_test.validate_model()

def test_to_int_fails():
    to_int_test= TestToIntOp(a="a",b=1)
    with pytest.raises(Exception):
        to_int_test.validate_model()
        
if __name__ == "__main__":
    test_to_int_passes()
    test_to_int_fails()