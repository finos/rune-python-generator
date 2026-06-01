#
# Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
# SPDX-License-Identifier: Apache-2.0
#

"""Unit tests for metadata reference filtering.

Generator output correctness (positive match cases) is verified by JUnit tests
in PythonWithMetaExpressionTest. Python unit tests cover the no-match runtime
paths, which exercise the generated filter lambda without hitting the Pydantic
@validate_call restriction on @reference metadata.
"""

from rosetta_dsl.test.model.meta_reference_filter.Party import Party
from rosetta_dsl.test.model.meta_reference_filter.Leg import Leg
from rosetta_dsl.test.model.meta_reference_filter.Counterparty import Counterparty
from rosetta_dsl.test.model.meta_reference_filter.functions.FilterByReference import FilterByReference
from rosetta_dsl.test.model.meta_reference_filter.functions.FilterByPartyReference import FilterByPartyReference


def test_filter_reference_exists_no_match_returns_none():
    """filter reference exists: returns None when no extracted item has @reference metadata."""
    leg = Leg(partyRef=Party(name="Bob"))

    result = FilterByReference(legs=[leg])

    assert result is None


def test_filter_reference_exists_empty_input_returns_none():
    """filter reference exists: returns None for empty input list."""
    result = FilterByReference(legs=[])

    assert result is None


def test_filter_field_reference_exists_no_match_returns_none():
    """field -> reference exists: returns None when no counterparty has @reference on partyReference."""
    cp = Counterparty(partyReference=Party(name="Bob"), role="payer")

    result = FilterByPartyReference(counterparties=[cp])

    assert result is None


def test_filter_field_reference_exists_empty_input_returns_none():
    """field -> reference exists: returns None for empty input list."""
    result = FilterByPartyReference(counterparties=[])

    assert result is None
