#
# Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
# SPDX-License-Identifier: Apache-2.0
#

"""`as` operator unit tests — choice narrowing and data type narrowing.

Tests the as keyword used inside a function body to narrow a choice to one of
its options, or a data type to one of its extensions, and to chain `->`
attribute access onto the narrowed result. See docs/AS_OPERATOR_IMPACT.md.
"""

from rune.runtime.metadata import StrWithMeta

from rosetta_dsl.test.expressions.as_op.Bar import Bar
from rosetta_dsl.test.expressions.as_op.Qux import Qux
from rosetta_dsl.test.expressions.as_op.ChoiceBar import ChoiceBar
from rosetta_dsl.test.expressions.as_op.ChoiceQux import ChoiceQux
from rosetta_dsl.test.expressions.as_op.ChoiceFoo import ChoiceFoo
from rosetta_dsl.test.expressions.as_op.ChoiceFooMeta import ChoiceFooMeta
from rosetta_dsl.test.expressions.as_op.functions.AsDataSingle import AsDataSingle
from rosetta_dsl.test.expressions.as_op.functions.AsDataMulti import AsDataMulti
from rosetta_dsl.test.expressions.as_op.functions.AsChained import AsChained
from rosetta_dsl.test.expressions.as_op.functions.AsChoiceSingle import AsChoiceSingle
from rosetta_dsl.test.expressions.as_op.functions.AsChoiceMulti import AsChoiceMulti
from rosetta_dsl.test.expressions.as_op.functions.AsDataTypeSingleMatchTest import AsDataTypeSingleMatchTest
from rosetta_dsl.test.expressions.as_op.functions.AsDataTypeSingleNoMatchReturnsEmptyTest import AsDataTypeSingleNoMatchReturnsEmptyTest
from rosetta_dsl.test.expressions.as_op.functions.AsDataTypeMultiFiltersTest import AsDataTypeMultiFiltersTest
from rosetta_dsl.test.expressions.as_op.functions.AsChoiceSingleMatchTest import AsChoiceSingleMatchTest
from rosetta_dsl.test.expressions.as_op.functions.AsChoiceSingleNoMatchReturnsEmptyTest import AsChoiceSingleNoMatchReturnsEmptyTest
from rosetta_dsl.test.expressions.as_op.functions.AsChoiceMultiFiltersTest import AsChoiceMultiFiltersTest
from rosetta_dsl.test.expressions.as_op.functions.AsChoiceOptionWithMetadataTest import AsChoiceOptionWithMetadataTest
from rosetta_dsl.test.expressions.as_op.functions.AsNestedChoiceOptionTest import AsNestedChoiceOptionTest
from rosetta_dsl.test.expressions.as_op.functions.AsNestedChoiceOptionTest2 import AsNestedChoiceOptionTest2


def test_as_data_single_match():
    """Narrowing to the actual runtime type returns the value."""
    bar = Bar(barAttr=42, inner=Qux(quxAttr=0, attr=0))
    assert AsDataSingle(foo=bar) == 42


def test_as_data_single_no_match():
    """Narrowing to a type the value is not an instance of returns None."""
    qux = Qux(quxAttr=12, attr=0)
    assert AsDataSingle(foo=qux) is None


def test_as_data_multi_filters():
    """Narrowing a list filters to elements that are instances of the target type."""
    bar1 = Bar(barAttr=1, inner=Qux(quxAttr=0, attr=0))
    qux = Qux(quxAttr=2, attr=0)
    bar2 = Bar(barAttr=3, inner=Qux(quxAttr=0, attr=0))
    assert AsDataMulti(foos=[bar1, qux, bar2]) == [1, 3]


def test_as_chained_attribute_access():
    """`->` after `as` operates on the narrowed type."""
    bar = Bar(barAttr=0, inner=Qux(quxAttr=0, attr=7))
    assert AsChained(foo=bar) == 7


def test_as_choice_single_match():
    """Narrowing a choice to the option it currently holds returns the option's value."""
    choice_foo = ChoiceFoo(ChoiceBar=ChoiceBar(barAttr=42))
    assert AsChoiceSingle(choiceFoo=choice_foo) == 42


def test_as_choice_single_no_match():
    """Narrowing a choice to an option it does not hold returns None."""
    choice_foo = ChoiceFoo(ChoiceQux=ChoiceQux(quxAttr=12))
    assert AsChoiceSingle(choiceFoo=choice_foo) is None


def test_as_choice_multi_filters():
    """Narrowing a list of choices filters to those holding the named option."""
    cf1 = ChoiceFoo(ChoiceBar=ChoiceBar(barAttr=1))
    cf2 = ChoiceFoo(ChoiceQux=ChoiceQux(quxAttr=2))
    cf3 = ChoiceFoo(ChoiceBar=ChoiceBar(barAttr=3))
    assert AsChoiceMulti(choiceFoos=[cf1, cf2, cf3]) == [1, 3]


# --- Inline construction — data type narrowing ---

def test_as_data_type_single_match_inline():
    """Inline Bar literal narrowed to Bar returns barAttr."""
    assert AsDataTypeSingleMatchTest() == 42


def test_as_data_type_single_no_match_inline():
    """Inline Qux literal narrowed to Bar returns None."""
    assert AsDataTypeSingleNoMatchReturnsEmptyTest() is None


def test_as_data_type_multi_filters_count_inline():
    """Inline mixed list narrowed to Bar counts two matching instances."""
    assert AsDataTypeMultiFiltersTest() == 2


# --- Inline construction — choice narrowing ---

def test_as_choice_single_match_inline():
    """Inline ChoiceFoo holding ChoiceBar narrowed to ChoiceBar returns barAttr."""
    assert AsChoiceSingleMatchTest() == 42


def test_as_choice_single_no_match_inline():
    """Inline ChoiceFoo holding ChoiceQux narrowed to ChoiceBar returns None."""
    assert AsChoiceSingleNoMatchReturnsEmptyTest() is None


def test_as_choice_multi_filters_sum_inline():
    """Inline list of two choices: only the ChoiceBar element contributes to the sum."""
    assert AsChoiceMultiFiltersTest() == 1


# --- Choice option with metadata ---

def test_as_choice_option_with_metadata_match_returns_scheme():
    """Narrowing to a choice option carrying scheme metadata exposes the scheme value."""
    choice = ChoiceFooMeta(MyString=StrWithMeta("hello", scheme="http://example.com/scheme"))
    assert AsChoiceOptionWithMetadataTest(choiceFooMeta=choice) == "http://example.com/scheme"


def test_as_choice_option_with_metadata_no_match_returns_none():
    """Empty optional input (None) narrowed to MyString returns None."""
    assert AsChoiceOptionWithMetadataTest(choiceFooMeta=None) is None


# --- Nested choice ---

def test_as_nested_choice_option_match_returns_value():
    """as resolves through two levels of nested choice to reach Opt2 and returns opt2Attr."""
    print ('AsNestedChoiceOptionTest:',AsNestedChoiceOptionTest())
    assert AsNestedChoiceOptionTest() == 7


def test_as_nested_choice_option_no_match_returns_none():
    """as to Opt3 when the inner choice holds Opt2 returns None."""
    assert AsNestedChoiceOptionTest2() is None


if __name__ == "__main__":
    test_as_data_single_match()
    test_as_data_single_no_match()
    test_as_data_multi_filters()
    test_as_chained_attribute_access()
    test_as_choice_single_match()
    test_as_choice_single_no_match()
    test_as_choice_multi_filters()
    test_as_data_type_single_match_inline()
    test_as_data_type_single_no_match_inline()
    test_as_data_type_multi_filters_count_inline()
    test_as_choice_single_match_inline()
    test_as_choice_single_no_match_inline()
    test_as_choice_multi_filters_sum_inline()
    test_as_choice_option_with_metadata_match_returns_scheme()
    test_as_choice_option_with_metadata_no_match_returns_none()
    test_as_nested_choice_option_match_returns_value()
    test_as_nested_choice_option_no_match_returns_none()
