'''date serialization unit test'''
import datetime
import pytest

from rosetta_dsl.test.model.date_serialization.DateSerializatonTest import DateSerializatonTest
from rune.runtime.base_data_class import BaseDataClass
from rune.runtime.metadata import DateTimeWithMeta

def test_date_serialization():
    '''test date serialization'''
    dt_in = DateSerializatonTest(dateValue=DateTimeWithMeta(datetime.datetime(2025, 10, 30)))
    dt_in.validate_model()
    dt_str = dt_in.rune_serialize()
    print('dt_str:', dt_str)
    BaseDataClass.rune_deserialize(dt_str)

if __name__ == "__main__":
    test_date_serialization()