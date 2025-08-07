'''to-date unit tests'''
from datetime import date

import pytest

from rosetta_dsl.test.semantic.date_operator.DateOperatorTest import DateOperatorTest

def test_to_date_passes():
    '''no doc'''
    to_date_test= DateOperatorTest(a="2025-05-26",b=date(2025, 5, 26))
    to_date_test.validate_model()
def test_to_date_invalid_format_fails():
    '''no doc'''
    to_date_test= DateOperatorTest(a="2025/05/26", b=date(2025,5,26))
    with pytest.raises(Exception):
        to_date_test.validate_model()

if __name__ == "__main__":
    test_to_date_passes()
    test_to_date_invalid_format_fails()
