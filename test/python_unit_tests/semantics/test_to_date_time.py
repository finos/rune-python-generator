'''to-date-time unit tests'''

import datetime

import pytest

from rosetta_dsl.test.semantic.toDateTimeOp.TestDateTimeOp import TestDateTimeOp

def test_to_date_time_passes():
    to_date_time_test= TestDateTimeOp(a="2025-05-26 14:30:00",b=datetime.datetime(2025, 5, 26,14,30,0))
    to_date_time_test.validate_model()

def test_to_date_time_fails():
    to_date_time_test=TestDateTimeOp(a="2025-05-26 14-30-00",b=datetime.datetime(2025, 5, 26,14,30,0))
    with pytest.raises(Exception):
        to_date_time_test.validate_model()
        
if __name__ == "__main__":
    test_to_date_time_passes()
    test_to_date_time_fails()