"""conversion unit tests"""

import datetime
from datetime import date
from zoneinfo import ZoneInfo
import pytest

from rosetta_dsl.test.semantic.conversions.DateOperatorTest import DateOperatorTest
from rosetta_dsl.test.semantic.conversions.DateTimeOperatorTest import (
    DateTimeOperatorTest,
)
from rosetta_dsl.test.semantic.conversions.IntOperatorTest import IntOperatorTest
from rosetta_dsl.test.semantic.conversions.TimeOperatorTest import TimeOperatorTest
from rosetta_dsl.test.semantic.conversions.ZonedDateTimeOperatorTest import (
    ZonedDateTimeOperatorTest,
)


# to-int tests
def test_to_int_passes():
    to_int_test = IntOperatorTest(a="1", b=1)
    to_int_test.validate_model()


def test_to_int_fails():
    to_int_test = IntOperatorTest(a="a", b=1)
    with pytest.raises(Exception):
        to_int_test.validate_model()


# to-date tests
def test_to_date_passes():
    to_date_test = DateOperatorTest(a="2025-05-26", b=date(2025, 5, 26))
    to_date_test.validate_model()


def test_to_date_invalid_format_fails():
    to_date_test = DateOperatorTest(a="2025/05/26", b=date(2025, 5, 26))
    with pytest.raises(Exception):
        to_date_test.validate_model()


# to-date-time tests
def test_to_date_time_passes():
    to_date_time_test = DateTimeOperatorTest(
        a="2025-05-26 14:30:00", b=datetime.datetime(2025, 5, 26, 14, 30, 0)
    )
    to_date_time_test.validate_model()


def test_to_date_time_fails():
    to_date_time_test = DateTimeOperatorTest(
        a="2025-05-26 14-30-00", b=datetime.datetime(2025, 5, 26, 14, 30, 0)
    )
    with pytest.raises(Exception):
        to_date_time_test.validate_model()


# to-time tests
def test_to_time_passes():
    to_time_test = TimeOperatorTest(a="11:45:23", b=datetime.time(11, 45, 23))
    to_time_test.validate_model()


def test_to_time_fails():
    to_time_test = TimeOperatorTest(a="14-30-00", b=datetime.time(14, 30, 0))
    with pytest.raises(Exception):
        to_time_test.validate_model()


# to-zoned-date-time tests
def test_to_zoned_date_time_offset_and_time_zone():
    to_zoned_date_time_test = ZonedDateTimeOperatorTest(
        a="2025-07-07 15:30:00 +0100 Europe/Lisbon",
        b=datetime.datetime(2025, 7, 7, 15, 30, 0, tzinfo=ZoneInfo("Europe/Lisbon")),
    )
    to_zoned_date_time_test.validate_model()


def test_to_zoned_date_time_only_time_zone():
    to_zoned_date_time_test = ZonedDateTimeOperatorTest(
        a="2025-07-07 15:30:00 Europe/Lisbon",
        b=datetime.datetime(2025, 7, 7, 15, 30, 0, tzinfo=ZoneInfo("Europe/Lisbon")),
    )
    to_zoned_date_time_test.validate_model()


def test_to_zoned_date_time_only_time_zone2():
    to_zoned_date_time_test = ZonedDateTimeOperatorTest(
        a="2025-03-15 12:00:00 Zulu",
        b=datetime.datetime(2025, 3, 15, 12, 0, 0, tzinfo=ZoneInfo("Zulu")),
    )
    to_zoned_date_time_test.validate_model()


def test_to_zoned_date_time_only_offset():
    to_zoned_date_time_test = ZonedDateTimeOperatorTest(
        a="2025-05-26 14:30:00 +0900",
        b=datetime.datetime(
            2025,
            5,
            26,
            14,
            30,
            0,
            tzinfo=datetime.timezone(datetime.timedelta(hours=9)),
        ),
    )
    to_zoned_date_time_test.validate_model()


def test_to_zoned_date_time_fails():
    to_zoned_date_time_test = ZonedDateTimeOperatorTest(
        a="2025-12-15 15:30:00 +0100 Europe/Lisbon",
        b=datetime.datetime(2025, 5, 26, 14, 30, 0, tzinfo=ZoneInfo("Europe/Lisbon")),
    )
    with pytest.raises(Exception):
        to_zoned_date_time_test.validate_model()


def test_to_zoned_date_time_fails_format():
    to_zoned_date_time_test = ZonedDateTimeOperatorTest(
        a="2025-05-26 14:30:00 +09x0",
        b=datetime.datetime(
            2025,
            5,
            26,
            14,
            30,
            0,
            tzinfo=datetime.timezone(datetime.timedelta(hours=9)),
        ),
    )
    with pytest.raises(Exception):
        to_zoned_date_time_test.validate_model()
