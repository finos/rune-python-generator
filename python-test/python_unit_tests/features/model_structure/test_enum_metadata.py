"""ENUM metadata test- parked until backlog addressed"""

import pytest
from decimal import Decimal
from rosetta_dsl.test.model.enum_metadata.CurrencyEnum import CurrencyEnum
from rosetta_dsl.test.model.enum_metadata.CashTransfer import CashTransfer
from rune.runtime.metadata import _EnumWrapper


@pytest.mark.skip(reason="Blocked by Enum Wrapper implementation - see Backlog")
def test_enum_metadata_behavior():
    """
    Test that Enums are wrapped at runtime to support metadata,
    while preserving equality with the raw Enum member.
    """
    # Create a transfer with an enum value
    transfer = CashTransfer(amount=Decimal("100.00"), currency=CurrencyEnum.USD)

    wrapped_currency = transfer.currency

    # 1. Assert it is wrapped in the runtime _EnumWrapper (to hold metadata)
    assert isinstance(wrapped_currency, _EnumWrapper), (
        "Enum usage should be wrapped in _EnumWrapper at runtime"
    )

    # 2. Assert metadata access exists (even if empty initially)
    # The wrapper should expose a 'meta' attribute for keys/references
    assert hasattr(wrapped_currency, "meta"), "Wrapper should expose metadata attribute"

    # 3. Assert equality with the raw Enum member
    # The wrapper should proxy equality checks to the underlying enum
    assert wrapped_currency == CurrencyEnum.USD, (
        "Wrapped enum should be equal to the raw Enum member"
    )
    assert wrapped_currency.value == CurrencyEnum.USD.value, (
        "Wrapped value should match raw enum value"
    )
