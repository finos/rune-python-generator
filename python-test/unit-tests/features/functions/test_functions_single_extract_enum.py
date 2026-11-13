#
# Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
# SPDX-License-Identifier: Apache-2.0
#

'''extract over a single value, and enum values written without their type'''
import pytest

from rune.runtime.conditions import ConditionViolationError

from rosetta_dsl.test.functions.single_extract_enum.Multiplier import Multiplier
from rosetta_dsl.test.functions.single_extract_enum.Price import Price
from rosetta_dsl.test.functions.single_extract_enum.PriceKind import PriceKind
from rosetta_dsl.test.functions.single_extract_enum.PricedQuantity import PricedQuantity
from rosetta_dsl.test.functions.single_extract_enum.Quantity import Quantity
from rosetta_dsl.test.functions.single_extract_enum.functions.ConvertToEnum import (
    ConvertToEnum)
from rosetta_dsl.test.functions.single_extract_enum.functions.CountWithEnumValueInThen import (
    CountWithEnumValueInThen)
from rosetta_dsl.test.functions.single_extract_enum.functions.CountWithQualifiedEnumValue import (
    CountWithQualifiedEnumValue)
from rosetta_dsl.test.functions.single_extract_enum.functions.CountWithUnqualifiedEnumValue import (
    CountWithUnqualifiedEnumValue)
from rosetta_dsl.test.functions.single_extract_enum.functions.EnumValueInSwitch import (
    EnumValueInSwitch)
from rosetta_dsl.test.functions.single_extract_enum.functions.ExtractFromSingleValue import (
    ExtractFromSingleValue)
from rosetta_dsl.test.functions.single_extract_enum.functions.UseExtractedSingleValueInArithmetic import (
    UseExtractedSingleValueInArithmetic)


def test_extract_from_single_value():
    '''extract applies to the one value rather than iterating over it'''
    assert ExtractFromSingleValue(Quantity(value=2)) == 2


def test_extract_from_single_value_uses_its_attributes():
    '''the extract body can read the single value's attributes'''
    quantity = Quantity(value=2, multiplier=Multiplier(value=3))
    assert ExtractFromSingleValue(quantity) == 6


def test_extract_from_absent_single_value():
    '''an absent input gives an absent result, not an empty list'''
    assert ExtractFromSingleValue(None) is None


def test_extracted_single_value_in_arithmetic():
    '''the result of the extract is a number, not a one-item list'''
    assert UseExtractedSingleValueInArithmetic(Quantity(value=2), 5) == 10


def test_unqualified_enum_value_is_imported():
    '''an enum value written without its type must still resolve'''
    prices = [Price(priceKind=PriceKind.ASSET_PRICE), Price(priceKind=PriceKind.OTHER)]
    assert CountWithUnqualifiedEnumValue(prices) == 1


def test_qualified_enum_value_is_imported():
    '''control: an enum value written with its type resolves'''
    prices = [Price(priceKind=PriceKind.ASSET_PRICE), Price(priceKind=PriceKind.OTHER)]
    assert CountWithQualifiedEnumValue(prices) == 1


def test_enum_value_in_then_body_is_imported():
    '''the enum may be used only inside the body of a then'''
    prices = [Price(priceKind=PriceKind.ASSET_PRICE), Price(priceKind=PriceKind.OTHER)]
    assert CountWithEnumValueInThen(prices) == 1


def test_enum_value_in_switch_is_imported():
    '''the enum may be used only inside the cases of a switch'''
    assert EnumValueInSwitch('asset') == 'AssetPrice'
    assert EnumValueInSwitch('other') == 'Other'


def test_to_enum_resolves_the_enum():
    '''to-enum must refer to an enum that the module can reach'''
    assert ConvertToEnum('AssetPrice') == 'AssetPrice'


def test_enum_value_in_inherited_condition_is_imported():
    '''a class condition may be the only use of the enum in its module'''
    PricedQuantity(priceKind=PriceKind.ASSET_PRICE, value=1).validate_conditions()
    with pytest.raises(ConditionViolationError):
        PricedQuantity(priceKind=PriceKind.ASSET_PRICE).validate_conditions()
