#
# Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
# SPDX-License-Identifier: Apache-2.0
#

"""ENUM metadata unit tests"""

from decimal import Decimal
from rune.runtime.metadata import EnumWithMetaMixin, BaseMetaDataMixin

from rosetta_dsl.test.model.enum_metadata.CurrencyEnum import CurrencyEnum
from rosetta_dsl.test.model.enum_metadata.CashTransfer import CashTransfer


def test_enum_has_meta_mixin():
    """Generated enums inherit from EnumWithMetaMixin"""
    assert issubclass(CurrencyEnum, EnumWithMetaMixin)


def test_enum_field_with_metadata_wraps_on_assignment():
    """
    Assigning a plain enum to a [metadata id] field should auto-wrap it
    in a metadata-capable wrapper via the pydantic validator.
    The resulting value must carry metadata support and remain equal to
    the raw enum member.
    """
    transfer = CashTransfer(amount=Decimal("100.00"), currency=CurrencyEnum.USD)
    wrapped_currency = transfer.currency

    # Wrapped value supports metadata (is a BaseMetaDataMixin instance)
    assert isinstance(wrapped_currency, BaseMetaDataMixin), (
        "Enum stored in [metadata id] field should be wrapped in a metadata-capable object"
    )

    # Equality with raw enum member is preserved
    assert wrapped_currency == CurrencyEnum.USD, (
        "Wrapped enum should compare equal to the raw Enum member"
    )
    assert wrapped_currency.value == CurrencyEnum.USD.value


def test_enum_metadata_can_be_set():
    """Metadata can be set on a wrapped enum value"""
    transfer = CashTransfer(amount=Decimal("100.00"), currency=CurrencyEnum.USD)
    wrapped_currency = transfer.currency

    wrapped_currency.set_meta(check_allowed=False, scheme="http://fpml.org/coding-scheme/currency")
    assert wrapped_currency.get_meta("scheme") == "http://fpml.org/coding-scheme/currency"


if __name__ == "__main__":
    test_enum_has_meta_mixin()
    test_enum_field_with_metadata_wraps_on_assignment()
    test_enum_metadata_can_be_set()
