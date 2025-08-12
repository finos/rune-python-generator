'''to-zoned-date-time unit tests'''
import datetime
from zoneinfo import ZoneInfo

import pytest

from rosetta_dsl.test.semantic.toZonedDateTimeOp.TestToZonedDateTimeOp import TestToZonedDateTimeOp


def test_to_zoned_date_time_offset_and_time_zone():
    to_zoned_date_time_test= TestToZonedDateTimeOp(a="2025-07-07 15:30:00 +0100 Europe/Lisbon",b=datetime.datetime(2025,7, 7, 15, 30, 0, tzinfo=ZoneInfo("Europe/Lisbon")))
    to_zoned_date_time_test.validate_model()
    
def test_to_zoned_date_time_only_time_zone():
    to_zoned_date_time_test= TestToZonedDateTimeOp(a="2025-07-07 15:30:00 Europe/Lisbon",b=datetime.datetime(2025,7, 7, 15, 30, 0, tzinfo=ZoneInfo("Europe/Lisbon")))
    to_zoned_date_time_test.validate_model()

def test_to_zoned_date_time_only_time_zone2():
    to_zoned_date_time_test= TestToZonedDateTimeOp(a="2025-03-15 12:00:00 Zulu",b=datetime.datetime(2025,3, 15, 12, 0, 0, tzinfo=ZoneInfo("Zulu")))
    to_zoned_date_time_test.validate_model()

def test_to_zoned_date_time_only_offset():
    to_zoned_date_time_test= TestToZonedDateTimeOp(a="2025-05-26 14:30:00 +0900",b=datetime.datetime(2025,5, 26, 14, 30, 0, tzinfo=datetime.timezone(datetime.timedelta(hours=9))))
    to_zoned_date_time_test.validate_model()
    
def test_to_zoned_date_time_fails():
    to_zoned_date_time_test= TestToZonedDateTimeOp(a="2025-12-15 15:30:00 +0100 Europe/Lisbon",b=datetime.datetime(2025, 5, 26, 14, 30, 0, tzinfo=ZoneInfo("Europe/Lisbon")))
    with pytest.raises(Exception):
        to_zoned_date_time_test.validate_model()

def test_to_zoned_date_time_fails_format():
    to_zoned_date_time_test= TestToZonedDateTimeOp(a="2025-05-26 14:30:00 +09x0",b=datetime.datetime(2025, 5, 26, 14, 30, 0, tzinfo=datetime.timezone(datetime.timedelta(hours=9))))
    with pytest.raises(Exception):
        to_zoned_date_time_test.validate_model()
        
if __name__ == "__main__":
    test_to_zoned_date_time_offset_and_time_zone()
    test_to_zoned_date_time_only_time_zone()
    test_to_zoned_date_time_only_time_zone2()
    test_to_zoned_date_time_only_offset()
    test_to_zoned_date_time_fails()
    test_to_zoned_date_time_fails_format()
