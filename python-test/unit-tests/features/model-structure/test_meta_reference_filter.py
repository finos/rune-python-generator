#
# Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
# SPDX-License-Identifier: Apache-2.0
#

"""Unit tests for metadata reference filtering.

Objects are constructed without @reference metadata (passing @validate_call),
then metadata is set in-place before calling filter functions via
rune_call_unchecked_raw — the same mechanism CDM uses for internal calls —
which bypasses @validate_call so Pydantic does not re-validate the metadata.
"""

from rosetta_dsl.test.model.meta_reference_filter.Party import Party
from rosetta_dsl.test.model.meta_reference_filter.Leg import Leg
from rosetta_dsl.test.model.meta_reference_filter.Counterparty import Counterparty
from rosetta_dsl.test.model.meta_reference_filter.functions.FilterByReference import FilterByReference
from rosetta_dsl.test.model.meta_reference_filter.functions.FilterByPartyReference import FilterByPartyReference
from rune.runtime.func_proxy import rune_call_unchecked_raw


# ---------------------------------------------------------------------------
# filter reference exists  (standalone — items extracted from a field)
# ---------------------------------------------------------------------------

def test_filter_reference_exists_no_match_returns_none():
    """All legs have partyRef without @reference — filter returns None."""
    leg = Leg(partyRef=Party(name="Bob"))

    result = rune_call_unchecked_raw(FilterByReference, legs=[leg])

    assert result is None


def test_filter_reference_exists_empty_input_returns_none():
    """Empty input list — filter returns None."""
    result = rune_call_unchecked_raw(FilterByReference, legs=[])

    assert result is None


def test_filter_reference_exists_returns_first_match():
    """One leg has partyRef with @reference set — filter returns that party."""
    party_with_ref = Party(name="Alice")
    leg_with_ref = Leg(partyRef=party_with_ref)
    party_with_ref.set_meta(check_allowed=False, reference="party-001")

    leg_without_ref = Leg(partyRef=Party(name="Bob"))

    result = rune_call_unchecked_raw(FilterByReference,
                                     legs=[leg_without_ref, leg_with_ref])

    assert result is not None
    assert result.get_meta("reference") == "party-001"


def test_filter_reference_exists_multiple_matches_returns_first():
    """Two legs have partyRef with @reference — filter returns the first."""
    party_a = Party(name="Alice")
    leg_a = Leg(partyRef=party_a)
    party_a.set_meta(check_allowed=False, reference="party-001")

    party_b = Party(name="Bob")
    leg_b = Leg(partyRef=party_b)
    party_b.set_meta(check_allowed=False, reference="party-002")

    result = rune_call_unchecked_raw(FilterByReference, legs=[leg_a, leg_b])

    assert result is not None
    assert result.get_meta("reference") == "party-001"


# ---------------------------------------------------------------------------
# field -> reference exists  (feature navigation)
# ---------------------------------------------------------------------------

def test_filter_field_reference_exists_no_match_returns_none():
    """No counterparty has @reference on partyReference — filter returns None."""
    cp = Counterparty(partyReference=Party(name="Bob"), role="payer")

    result = rune_call_unchecked_raw(FilterByPartyReference,
                                     counterparties=[cp])

    assert result is None


def test_filter_field_reference_exists_empty_input_returns_none():
    """Empty input list — filter returns None."""
    result = rune_call_unchecked_raw(FilterByPartyReference, counterparties=[])

    assert result is None


def test_filter_field_reference_exists_returns_match():
    """One counterparty has @reference on partyReference — filter returns it."""
    party_with_ref = Party(name="Alice")
    cp_with_ref = Counterparty(partyReference=party_with_ref, role="payer")
    party_with_ref.set_meta(check_allowed=False, reference="party-001")

    cp_without_ref = Counterparty(partyReference=Party(name="Bob"),
                                  role="receiver")

    result = rune_call_unchecked_raw(FilterByPartyReference,
                                     counterparties=[cp_without_ref,
                                                     cp_with_ref])

    assert result is not None
    assert result.role == "payer"


def test_filter_field_reference_exists_excludes_unset():
    """Only the counterparty with @reference set is returned; others excluded."""
    party_a = Party(name="Alice")
    cp_a = Counterparty(partyReference=party_a, role="payer")
    party_a.set_meta(check_allowed=False, reference="party-001")

    cp_b = Counterparty(partyReference=Party(name="Bob"), role="receiver")
    cp_c = Counterparty(partyReference=Party(name="Carol"), role="receiver")

    result = rune_call_unchecked_raw(FilterByPartyReference,
                                     counterparties=[cp_b, cp_a, cp_c])

    assert result is not None
    assert result.role == "payer"
    assert result.partyReference.name == "Alice"
