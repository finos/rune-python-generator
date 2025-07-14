'''to-date-time unit tests'''

import datetime

import pytest

from rosetta_dsl.test.semantic.date_time_operator.DateTimeOperatorTest import DateTimeOperatorTest

def test_to_date_time_passes():
    '''no doc'''
    to_date_time_test= DateTimeOperatorTest(a="2025-05-26 14:30:00",b=datetime.datetime(2025, 5, 26,14,30,0))
    to_date_time_test.validate_model()

def test_to_date_time_fails():
    '''no doc'''
    to_date_time_test=DateTimeOperatorTest(a="2025-05-26 14-30-00",b=datetime.datetime(2025, 5, 26,14,30,0))
    with pytest.raises(Exception):
        to_date_time_test.validate_model()
        
if __name__ == "__main__":
    test_to_date_time_passes()
    test_to_date_time_fails()
