#
# Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
# SPDX-License-Identifier: Apache-2.0
#

'''date serialization unit test'''
import datetime
import pytest


from rosetta_dsl.test.date_serialization.DateSerializatonTest import DateSerializatonTest
from rune.runtime.base_data_class import BaseDataClass
from rune.runtime.metadata import DateTimeWithMeta

def test_date_serialization():
    '''test date serialization'''
    dt_in = DateSerializatonTest(dateValue=datetime.datetime(2025, 10, 30))
    dt_in.validate_model()
    dt_str = dt_in.rune_serialize()
    dt_out = BaseDataClass.rune_deserialize(dt_str)
    assert dt_out == dt_in

if __name__ == "__main__":
    test_date_serialization()