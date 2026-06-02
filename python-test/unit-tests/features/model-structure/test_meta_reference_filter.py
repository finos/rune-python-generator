#
# Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
# SPDX-License-Identifier: Apache-2.0
#

"""Unit tests for metadata reference filtering.

`metaType reference string` maps to the Python metadata key `ref` (@ref),
which is already in the allowed metadata set for [metadata reference] fields.
Objects therefore carry valid @ref metadata and can be passed through
@validate_call without workarounds.

The filter functions use the runtime method resolve_ref("ref") which reads
@ref from __rune_metadata.  Tests require rune-runtime >= the version that
introduces BaseDataClass.resolve_ref.
"""

from rune.runtime.metadata import StrWithMeta
from rosetta_dsl.test.model.meta_reference_filter.Party import Party
from rosetta_dsl.test.model.meta_reference_filter.Leg import Leg
from rosetta_dsl.test.model.meta_reference_filter.Counterparty import Counterparty
from rosetta_dsl.test.model.meta_reference_filter.Trade import Trade
from rosetta_dsl.test.model.meta_reference_filter.TradeHolder import TradeHolder
from rosetta_dsl.test.model.meta_reference_filter.functions.FilterByReference import FilterByReference
from rosetta_dsl.test.model.meta_reference_filter.functions.FilterByPartyReference import FilterByPartyReference
from rosetta_dsl.test.model.meta_reference_filter.functions.FilterByScheme import FilterByScheme


# ---------------------------------------------------------------------------
# filter reference exists  (standalone — items extracted from a field)
# ---------------------------------------------------------------------------

def test_filter_reference_exists_no_match_returns_none():
    """All legs have partyRef without @ref — filter returns None."""
    leg = Leg(partyRef=Party(name="Bob"))

    result = FilterByReference(legs=[leg])

    assert result is None


def test_filter_reference_exists_empty_input_returns_none():
    """Empty input list — filter returns None."""
    result = FilterByReference(legs=[])

    assert result is None


def test_filter_reference_exists_returns_first_match():
    """One leg has partyRef with @ref set — filter returns that party."""
    party_with_ref = Party(name="Alice")
    leg_with_ref = Leg(partyRef=party_with_ref)
    party_with_ref.set_meta(ref="party-001")  # @ref is valid for [metadata reference] fields

    leg_without_ref = Leg(partyRef=Party(name="Bob"))

    result = FilterByReference(legs=[leg_without_ref, leg_with_ref])

    assert result is not None
    assert result.get_meta("ref") == "party-001"


def test_filter_reference_exists_multiple_matches_returns_first():
    """Two legs have partyRef with @ref — filter returns the first."""
    party_a = Party(name="Alice")
    leg_a = Leg(partyRef=party_a)
    party_a.set_meta(ref="party-001")

    party_b = Party(name="Bob")
    leg_b = Leg(partyRef=party_b)
    party_b.set_meta(ref="party-002")

    result = FilterByReference(legs=[leg_a, leg_b])

    assert result is not None
    assert result.get_meta("ref") == "party-001"


# ---------------------------------------------------------------------------
# field -> reference exists  (feature navigation)
# ---------------------------------------------------------------------------

def test_filter_field_reference_exists_no_match_returns_none():
    """No counterparty has @ref on partyReference — filter returns None."""
    cp = Counterparty(partyReference=Party(name="Bob"), role="payer")

    result = FilterByPartyReference(counterparties=[cp])

    assert result is None


def test_filter_field_reference_exists_empty_input_returns_none():
    """Empty input list — filter returns None."""
    result = FilterByPartyReference(counterparties=[])

    assert result is None


def test_filter_field_reference_exists_returns_match():
    """One counterparty has @ref on partyReference — filter returns it."""
    party_with_ref = Party(name="Alice")
    cp_with_ref = Counterparty(partyReference=party_with_ref, role="payer")
    party_with_ref.set_meta(ref="party-001")

    cp_without_ref = Counterparty(partyReference=Party(name="Bob"), role="receiver")

    result = FilterByPartyReference(counterparties=[cp_without_ref, cp_with_ref])

    assert result is not None
    assert result.role == "payer"


def test_filter_field_reference_exists_excludes_unset():
    """Only the counterparty with @ref set is returned; others excluded."""
    party_a = Party(name="Alice")
    cp_a = Counterparty(partyReference=party_a, role="payer")
    party_a.set_meta(ref="party-001")

    cp_b = Counterparty(partyReference=Party(name="Bob"), role="receiver")
    cp_c = Counterparty(partyReference=Party(name="Carol"), role="receiver")

    result = FilterByPartyReference(counterparties=[cp_b, cp_a, cp_c])

    assert result is not None
    assert result.role == "payer"
    assert isinstance(result.partyReference, Party)
    assert result.partyReference.name == "Alice"


# ---------------------------------------------------------------------------
# field -> scheme exists  (get_meta path — no resolve_ref dependency)
# Confirms the non-reference metadata path works end-to-end after rebuild.
# ---------------------------------------------------------------------------

def test_filter_scheme_exists_no_match_returns_none():
    """No trade has @scheme on identifier — filter returns None."""
    holder = TradeHolder(trade=Trade(identifier="T-001"))

    result = FilterByScheme(holders=[holder])

    assert result is None


def test_filter_scheme_exists_returns_match():
    """One holder has @scheme on identifier — filter returns it."""
    trade_with_scheme = Trade(identifier=StrWithMeta("T-001", scheme="http://example.com/scheme"))
    holder_with_scheme = TradeHolder(trade=trade_with_scheme)

    holder_without = TradeHolder(trade=Trade(identifier="T-002"))

    result = FilterByScheme(holders=[holder_without, holder_with_scheme])

    assert result is not None
    assert result.trade.identifier == "T-001"


def test_filter_scheme_exists_excludes_unset():
    """Only the holder with @scheme is returned; others excluded."""
    holder_a = TradeHolder(trade=Trade(identifier=StrWithMeta("T-001", scheme="http://example.com/scheme")))
    holder_b = TradeHolder(trade=Trade(identifier="T-002"))
    holder_c = TradeHolder(trade=Trade(identifier="T-003"))

    result = FilterByScheme(holders=[holder_b, holder_a, holder_c])

    assert result is not None
    assert result.trade.identifier == "T-001"
