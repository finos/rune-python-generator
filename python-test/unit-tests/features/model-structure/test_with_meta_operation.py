#
# Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
# SPDX-License-Identifier: Apache-2.0
#

"""Unit tests for with-meta operation on basic, enum and complex types."""

from decimal import Decimal

from rune.runtime.metadata import StrWithMeta, BaseMetaDataMixin, EnumWithMetaMixin

from rosetta_dsl.test.model.with_meta_operation.CurrencyEnum import CurrencyEnum
from rosetta_dsl.test.model.with_meta_operation.Quantity import Quantity
from rosetta_dsl.test.model.with_meta_operation.functions.AddSchemeToString import AddSchemeToString
from rosetta_dsl.test.model.with_meta_operation.functions.AddIdToString import AddIdToString
from rosetta_dsl.test.model.with_meta_operation.functions.AddSchemeToEnum import AddSchemeToEnum
from rosetta_dsl.test.model.with_meta_operation.functions.AddLocationToQuantity import AddLocationToQuantity


# -------------------------------------------------------------------------
# Basic type: string with scheme
# -------------------------------------------------------------------------

def test_with_meta_string_scheme_returns_str_with_meta():
    """Result of with-meta { scheme: ... } on a string is a StrWithMeta."""
    result = AddSchemeToString("USD", "http://fpml.org/coding-scheme/currency")
    assert isinstance(result, StrWithMeta)


def test_with_meta_string_scheme_value_preserved():
    """The string value is unchanged after adding scheme metadata."""
    result = AddSchemeToString("USD", "http://fpml.org/coding-scheme/currency")
    assert result == "USD"


def test_with_meta_string_scheme_metadata_attached():
    """The scheme metadata is accessible on the result."""
    scheme = "http://fpml.org/coding-scheme/currency"
    result = AddSchemeToString("USD", scheme)
    assert result.get_meta("scheme") == scheme


# -------------------------------------------------------------------------
# Basic type: string with id
# -------------------------------------------------------------------------

def test_with_meta_string_id_returns_str_with_meta():
    """Result of with-meta { id: ... } on a string is a StrWithMeta."""
    result = AddIdToString("my-value", "ref-001")
    assert isinstance(result, StrWithMeta)


def test_with_meta_string_id_value_preserved():
    """The string value is unchanged after adding id metadata."""
    result = AddIdToString("my-value", "ref-001")
    assert result == "my-value"


def test_with_meta_string_id_metadata_attached():
    """The id metadata is accessible on the result."""
    result = AddIdToString("my-value", "ref-001")
    assert result.get_meta("id") == "ref-001"


# -------------------------------------------------------------------------
# Enum type: enum with scheme
# -------------------------------------------------------------------------

def test_with_meta_enum_scheme_returns_meta_capable_object():
    """Result of with-meta { scheme: ... } on an enum supports metadata."""
    result = AddSchemeToEnum(CurrencyEnum.USD, "http://fpml.org/coding-scheme/currency")
    assert isinstance(result, BaseMetaDataMixin)


def test_with_meta_enum_scheme_value_preserved():
    """The enum value is unchanged after adding scheme metadata."""
    result = AddSchemeToEnum(CurrencyEnum.USD, "http://fpml.org/coding-scheme/currency")
    assert result == CurrencyEnum.USD


def test_with_meta_enum_scheme_metadata_attached():
    """The scheme metadata is accessible on the result."""
    scheme = "http://fpml.org/coding-scheme/currency"
    result = AddSchemeToEnum(CurrencyEnum.USD, scheme)
    assert result.get_meta("scheme") == scheme


# -------------------------------------------------------------------------
# Complex type: Quantity with location (scoped key)
# -------------------------------------------------------------------------

def test_with_meta_complex_location_returns_same_type():
    """Result of with-meta { location: ... } on a complex type is still that type."""
    q = Quantity(value=Decimal("100.0"))
    result = AddLocationToQuantity(q, "quantity-key-1")
    assert isinstance(result, Quantity)


def test_with_meta_complex_location_value_preserved():
    """The object fields are unchanged after adding location metadata."""
    q = Quantity(value=Decimal("100.0"))
    result = AddLocationToQuantity(q, "quantity-key-1")
    assert result.value == Decimal("100.0")


def test_with_meta_complex_location_metadata_attached():
    """The location metadata is accessible on the result."""
    q = Quantity(value=Decimal("100.0"))
    result = AddLocationToQuantity(q, "quantity-key-1")
    assert result.get_meta("location") == "quantity-key-1"


if __name__ == "__main__":
    test_with_meta_string_scheme_returns_str_with_meta()
    test_with_meta_string_scheme_value_preserved()
    test_with_meta_string_scheme_metadata_attached()
    test_with_meta_string_id_returns_str_with_meta()
    test_with_meta_string_id_value_preserved()
    test_with_meta_string_id_metadata_attached()
    test_with_meta_enum_scheme_returns_meta_capable_object()
    test_with_meta_enum_scheme_value_preserved()
    test_with_meta_enum_scheme_metadata_attached()
    test_with_meta_complex_location_returns_same_type()
    test_with_meta_complex_location_value_preserved()
    test_with_meta_complex_location_metadata_attached()
