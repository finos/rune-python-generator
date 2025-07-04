'''to-date unit tests'''
from datetime import date

import pytest

from rosetta_dsl.test.semantic.toDateOp.TestDateOp import TestDateOp

def test_to_date_passes():
    to_date_test= TestDateOp(a="2025-05-26",b=date(2025, 5, 26))
    to_date_test.validate_model()
def test_to_date_invalid_format_fails():
    to_date_test= TestDateOp(a="2025/05/26", b=date(2025,5,26))
    with pytest.raises(Exception):
        to_date_test.validate_model()

if __name__ == "__main__":
    test_to_date_passes()
    test_to_date_invalid_format_fails()