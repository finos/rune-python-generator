'''to-zoned-date-time unit tests'''

import datetime

import pytest

from rosetta_dsl.test.semantic.zoned_date_time_operator.ZonedDateTimeOperatorTest import ZonedDateTimeOperatorTest

def test_to_zoned_date_time_passes(): 
    '''no doc'''
    to_zoned_date_time_test= ZonedDateTimeOperatorTest(a="2025-05-26 14:30:00 +0900 UTC",b=datetime.datetime(2025, 5, 26, 14, 30, 0, tzinfo=datetime.timezone(datetime.timedelta(hours=9), 'UTC')))
    to_zoned_date_time_test.validate_model()

def test_to_zoned_date_time_fails():
    '''no doc'''
    to_zoned_date_time_test= ZonedDateTimeOperatorTest(a="2025-05-26 14:30:00 +09x0 UTC",b=datetime.datetime(2025, 5, 26, 14, 30, 0, tzinfo=datetime.timezone(datetime.timedelta(hours=9), 'UTC')))
    with pytest.raises(Exception):
        to_zoned_date_time_test.validate_model()
        
if __name__ == "__main__":
    test_to_zoned_date_time_passes()
    test_to_zoned_date_time_fails()