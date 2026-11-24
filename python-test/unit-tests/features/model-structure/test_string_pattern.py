#
# Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
# SPDX-License-Identifier: Apache-2.0
#

"""String pattern constraints.

A Rune string pattern must match the whole value exactly once, as the Java
generator's Pattern.matcher(value).matches() requires.
"""

import pytest
from pydantic import ValidationError
from rosetta_dsl.test.model_structure.string_pattern.PatternHolder import (
    PatternHolder,
)


@pytest.mark.parametrize(
    "field, value",
    [
        ("country", "US"),
        ("pair", "USD/EUR"),
        ("text", "a"),
        ("text", "A" * 35),
        ("alternative", "AB"),
        ("alternative", "CD"),
        ("anchored", "GB"),
        ("letter", "x"),
    ],
)
def test_matching_value_is_accepted(field, value):
    """A value that matches the pattern is accepted."""
    holder = PatternHolder(**{field: value})
    assert getattr(holder, field) == value


@pytest.mark.parametrize(
    "field, value",
    [
        # a fixed-count pattern must not repeat or match the empty string
        ("country", ""),
        ("country", "USGBFR"),
        # the last element of the pattern must not repeat
        ("pair", "USD/"),
        ("pair", "USD/EURGBP"),
        # a bounded count must hold at both ends
        ("text", ""),
        ("text", "A" * 36),
        # the anchors must apply to every alternative
        ("alternative", "ABxyz"),
        ("alternative", "xyzCD"),
        ("alternative", ""),
        # a single-character pattern allows exactly one character
        ("letter", ""),
        ("letter", "abc"),
        # an already anchored pattern keeps rejecting non-matching values
        ("anchored", "GBR"),
        ("anchored", ""),
    ],
)
def test_non_matching_value_is_rejected(field, value):
    """A value that does not match the pattern is rejected."""
    with pytest.raises(ValidationError):
        PatternHolder(**{field: value})


def test_list_items_are_checked_against_the_pattern():
    """Each item of a list attribute is checked against the pattern."""
    assert PatternHolder(codes=["US", "GB"]).codes == ["US", "GB"]
    with pytest.raises(ValidationError):
        PatternHolder(codes=["US", "USGBFR"])


def test_matching_value_round_trips():
    """A matching value survives serialization and deserialization."""
    holder = PatternHolder(country="US", alternative="CD")
    restored = PatternHolder.rune_deserialize(holder.rune_serialize())
    assert restored.country == "US"
    assert restored.alternative == "CD"
