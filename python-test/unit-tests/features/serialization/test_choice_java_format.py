#
# Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
# SPDX-License-Identifier: Apache-2.0
#

'''choice types are read and written in the Rune JSON format used by Java'''
import json

import pytest

from rune.runtime.base_data_class import BaseDataClass
from rosetta_dsl.test.serialization.choice_java_format.A import A
from rosetta_dsl.test.serialization.choice_java_format.B import B
from rosetta_dsl.test.serialization.choice_java_format.ChoiceData import ChoiceData
from rosetta_dsl.test.serialization.choice_java_format.LoopHolder import LoopHolder
from rosetta_dsl.test.serialization.choice_java_format.Root import Root

NS = 'rosetta_dsl.test.serialization.choice_java_format'
A_JSON = {'@type': f'{NS}.A', 'fieldA': 'foo'}
B_JSON = {'@type': f'{NS}.B', 'fieldB': 'bar'}


def _read(body: dict, root: str = 'Root', validate: bool = True):
    '''reads a document whose root type is in the test namespace'''
    data = {'@type': f'{NS}.{root}', '@model': 'rosetta_dsl', '@version': '0.0.0', **body}
    return BaseDataClass.rune_deserialize(json.dumps(data), validate_model=validate)


@pytest.mark.parametrize('attribute, value', [
    pytest.param('data', A_JSON, id='data-option'),
    pytest.param('basic', {'@type': 'string', '@data': 'foo'}, id='string-option'),
    pytest.param('basic', {'@type': 'int', '@data': 10}, id='int-option'),
    pytest.param('nested', A_JSON, id='nested-choice'),
    pytest.param('entries', [A_JSON, B_JSON], id='list'),
    pytest.param('located', {'@key:scoped': 'outer', '@data': A_JSON},
                 id='choice-attribute-with-metadata'),
])
def test_round_trip(attribute, value):
    '''Java's JSON is read, and written back unchanged'''
    obj = _read({attribute: value}, validate=False)
    assert json.loads(obj.rune_serialize(validate_model=False))[attribute] == value


def test_read_selects_the_option():
    '''@type selects the option'''
    assert _read({'data': A_JSON}).data.A.fieldA == 'foo'
    assert _read({'nested': B_JSON}).nested.ChoiceData.B.fieldB == 'bar'
    assert _read({'basic': {'@type': 'string', '@data': 'foo'}}).basic.string == 'foo'


def test_write_object_built_in_python():
    '''an object built in Python is written in the same format'''
    written = json.loads(Root(data=ChoiceData(A=A(fieldA='foo'))).rune_serialize())
    assert written['data'] == A_JSON


def test_reference_to_key_inside_choice():
    '''a key inside the selected option can be referenced from elsewhere'''
    obj = _read({
        'dated': {'@type': f'{NS}.WithDate',
                  'paymentDate': {'@key:external': 'k1', 'day': 'Monday'}},
        'dateRef': {'@ref:external': 'k1'},
    }, validate=False)
    assert obj.dateRef is obj.dated.WithDate.paymentDate


def test_type_with_one_of_condition_keeps_attribute_names():
    '''a type that isn't a choice is read and written by attribute name'''
    value = {'A': {'fieldA': 'foo'}}
    obj = _read({'viaOneOf': value})
    assert obj.viaOneOf.A.fieldA == 'foo'
    assert json.loads(obj.rune_serialize())['viaOneOf'] == value


@pytest.mark.parametrize('validate', [True, False])
@pytest.mark.parametrize('attribute, value', [
    pytest.param('data', {'A': {'fieldA': 'foo'}}, id='nested-form'),
    pytest.param('basic', {'string': 'foo'}, id='nested-form-basic'),
    pytest.param('data', {'@type': f'{NS}.PaymentDate', 'day': 'Monday'}, id='not-an-option'),
])
def test_invalid_choice_is_rejected(attribute, value, validate):
    '''only a declared option in the Rune JSON format is accepted'''
    with pytest.raises(Exception):
        _read({attribute: value}, validate=validate)


def test_bundled_choice_round_trip():
    '''a choice in a bundled class is read and written the same way'''
    value = {'@type': f'{NS}.B', 'fieldB': 'bar'}
    obj = _read({'option': value}, root='LoopHolder', validate=False)
    assert isinstance(obj, LoopHolder)
    assert obj.option.B.fieldB == 'bar'
    assert json.loads(obj.rune_serialize(validate_model=False))['option'] == value


def test_bundled_list_of_choices_round_trip():
    '''a list of choices in a bundled class is read and written the same way.

    Fails until the runtime can write a list whose serializer wraps the whole list (#42, bug 4).'''
    value = [{'@type': f'{NS}.B', 'fieldB': 'bar'}]
    obj = _read({'options': value}, root='LoopHolder', validate=False)
    assert json.loads(obj.rune_serialize(validate_model=False))['options'] == value


def test_b_option_is_written_with_its_own_type():
    '''@type names the selected option, not the first one'''
    written = json.loads(Root(data=ChoiceData(B=B(fieldB='bar'))).rune_serialize())
    assert written['data'] == B_JSON
