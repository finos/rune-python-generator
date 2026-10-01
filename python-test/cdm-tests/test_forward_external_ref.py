"""Tests for @ref:external / @key:external deserialization in CDM models.

eqs-ex09-compounding-swap.json raises KeyError: 'finalCashSettlementPaymentDate'
during rune_deserialize.  Root cause:

  EconomicTerms.payout uses Annotated[list[Payout|None], Payout.validator()].
  Payout is a *container* — its fields ARE the payout subtypes (PerformancePayout,
  InterestRatePayout, …); none are subclasses of Payout.  When Payout.deserialize
  sees {'@type': 'cdm.product.template.PerformancePayout', ...}, _type_to_cls
  resolves to PerformancePayout, and the issubclass guard raises ValueError.
  Pydantic catches that ValueError from the PlainValidator, falls back to the plain
  Payout schema (extra='ignore'), and creates an all-None Payout.  All paymentDates
  data — including the @key:external — is silently discarded, causing KeyError when
  rune_deserialize's second-pass resolve_references tries to look up the key.

The tests below assert the DESIRED behavior.  They currently FAIL; they should PASS
once the defect is fixed.  test_forward_external_ref_resolves is a baseline
confirming that forward external refs work correctly when the key IS registered.
"""
import pytest

from finos._bundle import (
    finos_cdm_product_template_EconomicTerms as EconomicTerms,
    finos_cdm_product_template_Payout as Payout,
)
from typing import Annotated, Optional
from pydantic import Field
from rune.runtime.base_data_class import BaseDataClass
from rune.runtime.metadata import KeyType


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def _performance_payout_data(key: str) -> dict:
    """Minimal PerformancePayout dict whose paymentDates carries @key:external."""
    return {
        '@type': 'cdm.product.template.PerformancePayout',
        'paymentDates': {
            'paymentDateSchedule': {
                'finalPaymentDate': {
                    '@key:external': key,
                }
            }
        },
    }


def _economic_terms_with_ref(key: str) -> dict:
    """EconomicTerms data: PerformancePayout defines key, terminationDate refs it."""
    return {
        'payout': [_performance_payout_data(key)],
        'terminationDate': {'@ref:external': key},
    }


# ---------------------------------------------------------------------------
# Desired behavior — these tests currently FAIL due to the defect
# ---------------------------------------------------------------------------

def test_performance_payout_field_is_populated():
    """model_validate must route @type:PerformancePayout data to Payout.PerformancePayout.

    Currently FAILS: Payout.deserialize raises ValueError and pydantic falls back to
    an all-None Payout, silently discarding all PerformancePayout data.
    """
    data = {'payout': [_performance_payout_data('someKey')]}
    obj = EconomicTerms.model_validate(data)
    p = obj.payout[0]
    assert p.PerformancePayout is not None, (
        f'Expected Payout.PerformancePayout to be populated; got all-None container '
        f'(type: {type(p).__name__})'
    )


def test_performance_payout_key_is_registered():
    """After model_validate, the @key:external from PerformancePayout must be in the
    external key map so that a subsequent resolve_references can honour it.

    Currently FAILS: the key is never registered because PerformancePayout data is
    silently dropped (see test_performance_payout_field_is_populated above).
    """
    data = {'payout': [_performance_payout_data('finalCashSettlementPaymentDate')]}
    obj = EconomicTerms.model_validate(data)
    registered = obj.__rune_object_maps.get(KeyType.EXTERNAL, {})
    assert 'finalCashSettlementPaymentDate' in registered, (
        f'Expected key to be registered; got {list(registered.keys())}'
    )


def test_rune_deserialize_resolves_performance_payout_external_ref():
    """rune_deserialize must not raise KeyError when a @ref:external refers to a
    @key:external defined inside a PerformancePayout element.

    This is the symptom reported for eqs-ex09-compounding-swap.json.
    Currently FAILS with KeyError: 'finalCashSettlementPaymentDate'.
    """
    data = _economic_terms_with_ref('finalCashSettlementPaymentDate')
    EconomicTerms.rune_deserialize(data, validate_model=False)


# ---------------------------------------------------------------------------
# Baseline — forward external refs resolve correctly when key IS registered
# ---------------------------------------------------------------------------

class DateRef(BaseDataClass):
    _ALLOWED_METADATA = {'@key', '@key:external'}
    value: str = Field(...)


class LegWithRef(BaseDataClass):
    terminationDate: Optional[Annotated[
        DateRef,
        DateRef.serializer(),
        DateRef.validator(('@ref:external',)),
    ]] = Field(None)
    _KEY_REF_CONSTRAINTS = {'terminationDate': {'@ref:external'}}


class LegWithKey(BaseDataClass):
    finalPaymentDate: Annotated[
        DateRef,
        DateRef.serializer(),
        DateRef.validator(('@key:external',)),
    ] = Field(...)


class Terms(BaseDataClass):
    refLegs: list[Annotated[LegWithRef, LegWithRef.validator()]] = Field(default_factory=list)
    keyLegs: list[Annotated[LegWithKey, LegWithKey.validator()]] = Field(default_factory=list)


def test_forward_external_ref_resolves():
    """rune_deserialize resolves a forward @ref:external in a second tree-wide pass.

    refLegs (containing @ref:external) are listed before keyLegs (containing
    @key:external), so the reference cannot be resolved during the first pass.
    rune_deserialize's second pass (resolve_references) wires it up correctly.
    This test passes today and must continue to pass after any fix.
    """
    data = {
        'refLegs': [
            {'terminationDate': {'@ref:external': 'finalCashSettlementPaymentDate'}},
        ],
        'keyLegs': [
            {'finalPaymentDate': {
                '@key:external': 'finalCashSettlementPaymentDate',
                'value': '2009-01-01',
            }},
        ],
    }
    model = Terms.rune_deserialize(data, validate_model=False)

    key_leg = model.keyLegs[0]
    ref_leg = model.refLegs[0]
    assert ref_leg.terminationDate is key_leg.finalPaymentDate, (
        'Forward @ref:external was not resolved to the @key:external target'
    )
