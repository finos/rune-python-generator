#
# Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
# SPDX-License-Identifier: Apache-2.0
#

'''keys on a type with [metadata key] are written back'''
import json

import pytest

from rune.runtime.base_data_class import BaseDataClass
# Imported so that the types can be resolved from @type when reading
import rosetta_dsl.test.serialization.type_level_key.PartyList  # noqa: F401
import rosetta_dsl.test.serialization.type_level_key.SingleParty  # noqa: F401

NAMESPACE = 'rosetta_dsl.test.serialization.type_level_key'
PARTY = {'@key:external': 'p1', 'name': 'A'}


@pytest.mark.parametrize('holder, attr, value', [
    ('SingleParty', 'party', PARTY),
    ('PartyList', 'parties', [PARTY]),
])
def test_type_level_key_round_trip(holder, attr, value):
    '''a key read with the object is written back with it'''
    data = {
        '@type': f'{NAMESPACE}.{holder}',
        '@model': 'rosetta_dsl',
        '@version': '0.0.0',
        attr: value,
    }
    obj = BaseDataClass.rune_deserialize(json.dumps(data))
    written = json.loads(obj.rune_serialize())
    assert written[attr] == value
