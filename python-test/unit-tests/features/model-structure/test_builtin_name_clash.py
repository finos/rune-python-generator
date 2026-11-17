#
# Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
# SPDX-License-Identifier: Apache-2.0
#

"""An attribute named after a Python name its own annotation uses is still validated (#331)."""

import datetime
from decimal import Decimal

import pytest
from pydantic import ValidationError

from rosetta_dsl.test.model_structure.builtin_name_clash.BundledChoice import BundledChoice
from rosetta_dsl.test.model_structure.builtin_name_clash.ChoiceBasic import ChoiceBasic
from rosetta_dsl.test.model_structure.builtin_name_clash.Clash import Clash
from rosetta_dsl.test.model_structure.builtin_name_clash.DateClash import DateClash
from rosetta_dsl.test.model_structure.builtin_name_clash.DecimalClash import DecimalClash
from rosetta_dsl.test.model_structure.builtin_name_clash.Node import Node
from rosetta_dsl.test.model_structure.builtin_name_clash.TimeClash import TimeClash


@pytest.mark.parametrize(
    "make",
    [
        pytest.param(lambda: ChoiceBasic(int="abc"), id="choice-int"),
        pytest.param(lambda: Clash(str=5), id="str"),
        pytest.param(lambda: Clash(bool="abc"), id="bool"),
        pytest.param(lambda: DateClash(datetime="not a date"), id="datetime-date"),
        pytest.param(lambda: TimeClash(datetime="not a time"), id="datetime-time"),
        pytest.param(lambda: DecimalClash(Decimal="abc"), id="decimal"),
        pytest.param(lambda: BundledChoice(int="abc"), id="bundled-choice-int"),
        pytest.param(lambda: Node(str=5), id="bundled-str"),
    ],
)
def test_invalid_value_is_rejected(make):
    """the attribute's own type is enforced"""
    with pytest.raises(ValidationError):
        make()


def test_valid_values_are_accepted():
    """valid values are kept, with the attribute's own type"""
    assert ChoiceBasic(int=5).int == 5
    assert Clash(str="x", bool=True).str == "x"
    assert Clash(str="x", bool=True).bool is True
    assert DecimalClash(Decimal="1.5").Decimal == Decimal("1.5")
    assert BundledChoice(int=5).int == 5
    assert Node(str="x").str == "x"


def test_datetime_attributes_hold_their_type():
    """a date or time attribute named datetime is defined and converts its value"""
    assert DateClash(datetime="2026-11-11").datetime == datetime.date(2026, 11, 11)
    assert TimeClash(datetime="10:30:00").datetime == datetime.time(10, 30)
