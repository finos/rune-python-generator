#
# Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
# SPDX-License-Identifier: Apache-2.0
#

"""Unit tests for metadata reference filtering.

The generator emits item.resolve_ref_key("fieldName") (checking the parent's
__rune_references container) when filtering collections by reference existence.
Test data sets up references by passing Reference(obj) as a field value; the
runtime wires __rune_references on the parent during construction.

Functions are called directly — @validate_call does not recreate already-valid
model instances, so __rune_references is preserved through the call boundary.
"""

from rune.runtime.metadata import Reference, StrWithMeta
from rosetta_dsl.test.model.meta_reference_filter.Party import Party
from rosetta_dsl.test.model.meta_reference_filter.Leg import Leg
from rosetta_dsl.test.model.meta_reference_filter.Counterparty import Counterparty
from rosetta_dsl.test.model.meta_reference_filter.Trade import Trade
from rosetta_dsl.test.model.meta_reference_filter.TradeHolder import TradeHolder
from rosetta_dsl.test.model.meta_reference_filter.functions.FilterByReference import FilterByReference
from rosetta_dsl.test.model.meta_reference_filter.functions.FilterCounterpartiesByReference import FilterCounterpartiesByReference
from rosetta_dsl.test.model.meta_reference_filter.functions.FilterByPartyReference import FilterByPartyReference
from rosetta_dsl.test.model.meta_reference_filter.functions.FilterByScheme import FilterByScheme
from rosetta_dsl.test.model.meta_reference_filter.functions.FilterIdentifiersByScheme import FilterIdentifiersByScheme


# ---------------------------------------------------------------------------
# field -> reference exists  (generateFeatureCall path)
#
# FilterByReference: filter partyRef -> reference exists then first
# Generated: item.resolve_ref_key("partyRef") on each Leg — parent-side check.
# Legs whose partyRef was set via Reference() have __rune_references["partyRef"]
# populated; plain-Party legs do not.
# ---------------------------------------------------------------------------

def test_filter_reference_exists_no_match_returns_none():
    """No leg has partyRef stored as a Reference — filter returns None."""
    leg = Leg(partyRef=Party(name="Bob"))

    result = FilterByReference(legs=[leg])

    assert result is None


def test_filter_reference_exists_empty_input_returns_none():
    """Empty input list — filter returns None."""
    result = FilterByReference(legs=[])

    assert result is None


def test_filter_reference_exists_returns_first_match():
    """One leg stores partyRef as a Reference — filter returns that leg."""
    party_with_ref = Party(name="Alice")
    leg_with_ref = Leg(partyRef=Reference(party_with_ref))

    leg_without_ref = Leg(partyRef=Party(name="Bob"))

    result = FilterByReference(legs=[leg_without_ref, leg_with_ref])

    assert result is not None
    assert isinstance(result, Leg)
    assert isinstance(result.partyRef, Party)
    assert result.partyRef.name == "Alice"


def test_filter_reference_exists_multiple_matches_returns_first():
    """Two legs store partyRef as References — filter returns the first leg."""
    party_a = Party(name="Alice")
    leg_a = Leg(partyRef=Reference(party_a))

    party_b = Party(name="Bob")
    leg_b = Leg(partyRef=Reference(party_b))

    result = FilterByReference(legs=[leg_a, leg_b])

    assert result is not None
    assert isinstance(result, Leg)
    assert isinstance(result.partyRef, Party)
    assert result.partyRef.name == "Alice"


# ---------------------------------------------------------------------------
# field -> reference exists  (generateFeatureCall path)
#
# FilterCounterpartiesByReference: filter partyReference -> reference exists then first
# Generated: item.resolve_ref_key("partyReference") on each Counterparty — parent-side check.
# ---------------------------------------------------------------------------

def test_filter_counterparties_by_reference_no_match_returns_none():
    """No counterparty has partyReference stored as a Reference — filter returns None."""
    counterparties = [
        Counterparty(partyReference=Party(name="Alice"), role="payer"),
        Counterparty(partyReference=Party(name="Bob"), role="receiver"),
    ]

    result = FilterCounterpartiesByReference(counterparties=counterparties)

    assert result is None


def test_filter_counterparties_by_reference_empty_input_returns_none():
    """Empty input — filter returns None."""
    result = FilterCounterpartiesByReference(counterparties=[])

    assert result is None


def test_filter_counterparties_by_reference_returns_first_match():
    """First counterparty with partyReference stored as Reference returns that counterparty."""
    party_with_ref = Party(name="Alice")
    cp_with_ref = Counterparty(partyReference=Reference(party_with_ref), role="payer")

    result = FilterCounterpartiesByReference(counterparties=[
        Counterparty(partyReference=Party(name="Bob"), role="receiver"),
        cp_with_ref,
    ])

    assert result is not None
    assert isinstance(result, Counterparty)
    assert isinstance(result.partyReference, Party)
    assert result.partyReference.name == "Alice"


def test_filter_counterparties_by_reference_multiple_matches_returns_first():
    """Two counterparties have partyReference as Reference — first counterparty is returned."""
    party_a = Party(name="Alice")
    cp_a = Counterparty(partyReference=Reference(party_a), role="payer")

    party_b = Party(name="Bob")
    cp_b = Counterparty(partyReference=Reference(party_b), role="receiver")

    result = FilterCounterpartiesByReference(counterparties=[cp_a, cp_b])

    assert result is not None
    assert isinstance(result, Counterparty)
    assert isinstance(result.partyReference, Party)
    assert result.partyReference.name == "Alice"


# ---------------------------------------------------------------------------
# field -> reference exists  (feature call path)
#
# FilterByPartyReference: filter partyReference -> reference exists then only-element.
# Generated: item.resolve_ref_key("partyReference") on each Counterparty —
# parent-side check via the generateFeatureCall special case.
# ---------------------------------------------------------------------------

def test_filter_field_reference_exists_no_match_returns_none():
    """No counterparty has partyReference stored as a Reference — filter returns None."""
    cp = Counterparty(partyReference=Party(name="Bob"), role="payer")

    result = FilterByPartyReference(counterparties=[cp])

    assert result is None


def test_filter_field_reference_exists_empty_input_returns_none():
    """Empty input list — filter returns None."""
    result = FilterByPartyReference(counterparties=[])

    assert result is None


def test_filter_field_reference_exists_returns_match():
    """One counterparty stores partyReference as a Reference — filter returns it."""
    party_with_ref = Party(name="Alice")
    cp_with_ref = Counterparty(partyReference=Reference(party_with_ref), role="payer")
    cp_without_ref = Counterparty(partyReference=Party(name="Bob"), role="receiver")

    result = FilterByPartyReference(counterparties=[cp_without_ref, cp_with_ref])

    assert result is not None
    assert isinstance(result, Counterparty)
    assert result.role == "payer"
    assert isinstance(result.partyReference, Party)
    assert result.partyReference.name == "Alice"


def test_filter_field_reference_exists_excludes_unset():
    """Only the counterparty with a Reference partyReference is returned; others excluded."""
    party_a = Party(name="Alice")
    cp_a = Counterparty(partyReference=Reference(party_a), role="payer")
    cp_b = Counterparty(partyReference=Party(name="Bob"), role="receiver")
    cp_c = Counterparty(partyReference=Party(name="Carol"), role="receiver")

    result = FilterByPartyReference(counterparties=[cp_b, cp_a, cp_c])

    assert result is not None
    assert isinstance(result, Counterparty)
    assert result.role == "payer"
    assert isinstance(result.partyReference, Party)
    assert result.partyReference.name == "Alice"


# ---------------------------------------------------------------------------
# field -> scheme exists  (get_meta path — unchanged, no resolve_ref_key)
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


# ---------------------------------------------------------------------------
# generateSymbolReference -> RosettaMetaType (.get_meta path)
#
# FilterIdentifiersByScheme uses `filter scheme exists` after `extract identifier`.
# Inside the lambda, `scheme` is a bare RosettaMetaType symbol, so
# generateSymbolReference emits item.get_meta("scheme").
# ---------------------------------------------------------------------------

def test_filter_identifiers_by_scheme_no_match_returns_none():
    """No identifier has @scheme — filter returns None."""
    trades = [Trade(identifier="T-001"), Trade(identifier="T-002")]

    result = FilterIdentifiersByScheme(trades=trades)

    assert result is None


def test_filter_identifiers_by_scheme_empty_input_returns_none():
    """Empty input — filter returns None."""
    result = FilterIdentifiersByScheme(trades=[])

    assert result is None


def test_filter_identifiers_by_scheme_returns_first_match():
    """First identifier with @scheme is returned; plain string is skipped."""
    trade_with_scheme = Trade(identifier=StrWithMeta("T-001", scheme="http://example.com/scheme"))
    trade_without_scheme = Trade(identifier="T-002")

    result = FilterIdentifiersByScheme(trades=[trade_without_scheme, trade_with_scheme])

    assert isinstance(result, StrWithMeta)
    assert result == "T-001"
    assert result.get_meta("scheme") == "http://example.com/scheme"


def test_filter_identifiers_by_scheme_multiple_matches_returns_first():
    """Two identifiers with @scheme — first one is returned."""
    trade_a = Trade(identifier=StrWithMeta("T-001", scheme="http://example.com/a"))
    trade_b = Trade(identifier=StrWithMeta("T-002", scheme="http://example.com/b"))

    result = FilterIdentifiersByScheme(trades=[trade_a, trade_b])

    assert isinstance(result, StrWithMeta)
    assert result == "T-001"
    assert result.get_meta("scheme") == "http://example.com/a"


def test_filter_identifiers_by_scheme_preserves_scheme_value():
    """The scheme metadata value on the returned identifier is intact."""
    scheme_url = "http://example.com/my-scheme"
    trade = Trade(identifier=StrWithMeta("T-999", scheme=scheme_url))

    result = FilterIdentifiersByScheme(trades=[trade])

    assert isinstance(result, StrWithMeta)
    assert result.get_meta("scheme") == scheme_url
