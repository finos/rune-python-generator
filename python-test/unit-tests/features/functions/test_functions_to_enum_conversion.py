#
# Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
# SPDX-License-Identifier: Apache-2.0
#

'''to-enum gives the matching enum value, or nothing if there is no match'''
from rune.runtime.metadata import StrWithMeta

from rosetta_dsl.test.functions.to_enum_conversion.Foo import Foo
from rosetta_dsl.test.functions.to_enum_conversion.Holder import Holder
from rosetta_dsl.test.functions.to_enum_conversion.Source import Source
from rosetta_dsl.test.functions.to_enum_conversion.Target import Target
from rosetta_dsl.test.functions.to_enum_conversion.functions.HolderColourToTarget import (
    HolderColourToTarget)
from rosetta_dsl.test.functions.to_enum_conversion.functions.HolderNameToFoo import HolderNameToFoo
from rosetta_dsl.test.functions.to_enum_conversion.functions.SchemeSourceToTarget import (
    SchemeSourceToTarget)
from rosetta_dsl.test.functions.to_enum_conversion.functions.SchemeStringToFoo import SchemeStringToFoo
from rosetta_dsl.test.functions.to_enum_conversion.functions.SourceToTarget import SourceToTarget
from rosetta_dsl.test.functions.to_enum_conversion.functions.StringToSchemeFoo import StringToSchemeFoo
from rosetta_dsl.test.functions.to_enum_conversion.functions.StringToFoo import StringToFoo


def test_string_matching_value_name():
    '''a value without a display name matches its name'''
    assert StringToFoo('VALUE1') == Foo.VALUE1


def test_string_matching_display_name():
    '''a value with a display name matches the display name'''
    assert StringToFoo('Value 2') == Foo.VALUE2


def test_string_matching_name_instead_of_display_name():
    '''a value with a display name does not match its name'''
    assert StringToFoo('VALUE2') is None


def test_string_matching_nothing():
    '''a string that matches no value gives nothing'''
    assert StringToFoo('not a value') is None


def test_absent_string():
    '''an absent string gives nothing'''
    assert StringToFoo(None) is None


def test_enum_to_enum_by_value_name():
    '''an enum converts to the value with the same name'''
    assert SourceToTarget(Source.RED) == Target.RED


def test_enum_to_enum_ignores_display_name():
    '''an enum converts by name even when its value has a display name'''
    assert SourceToTarget(Source.GREEN) == Target.GREEN


def test_absent_enum():
    '''an absent enum gives nothing'''
    assert SourceToTarget(None) is None


def _holder(**fields):
    '''A Holder read from a document, so attributes carry their scheme.'''
    return Holder.model_validate(
        {name: {'@data': value, '@scheme': 'http://example.org/scheme'}
         for name, value in fields.items()})


def test_string_attribute_with_scheme():
    '''a string read with a scheme converts by display name'''
    assert HolderNameToFoo(_holder(name='Value 2')) == Foo.VALUE2


def test_string_attribute_with_scheme_matching_nothing():
    '''a string read with a scheme that matches no value gives nothing'''
    assert HolderNameToFoo(_holder(name='not a value')) is None


def test_absent_attribute_with_scheme():
    '''an absent attribute gives nothing'''
    assert HolderNameToFoo(Holder()) is None


def test_enum_attribute_with_scheme():
    '''an enum read with a scheme converts by value name'''
    assert HolderColourToTarget(_holder(colour='Leaf green')) == Target.GREEN


def test_string_input_with_scheme():
    '''a string input declared with a scheme converts by display name'''
    assert SchemeStringToFoo(StrWithMeta('Value 2', scheme='http://example.org/scheme')) == Foo.VALUE2


def test_enum_input_with_scheme():
    '''an enum input declared with a scheme converts by value name'''
    assert SchemeSourceToTarget(Source.GREEN) == Target.GREEN


def test_result_stored_in_output_with_scheme():
    '''the converted value can be returned where the output allows a scheme'''
    assert StringToSchemeFoo('Value 2') == Foo.VALUE2


def test_unmatched_result_for_output_with_scheme():
    '''no match gives nothing, even where the output allows a scheme'''
    assert StringToSchemeFoo('not a value') is None
