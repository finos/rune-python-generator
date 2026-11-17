#
# Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
# SPDX-License-Identifier: Apache-2.0
#

'''scoped keys are written for standalone and bundled classes'''
import json

import pytest

from rune.runtime.base_data_class import BaseDataClass
# Imported so that the types can be resolved from @type when reading
import rosetta_dsl.test.serialization.scoped_key.BundledHolder  # noqa: F401
import rosetta_dsl.test.serialization.scoped_key.Holder  # noqa: F401

NAMESPACE = 'rosetta_dsl.test.serialization.scoped_key'


@pytest.mark.parametrize('holder', ['Holder', 'BundledHolder'])
def test_scoped_key_round_trip(holder):
    '''a scoped key and the reference to it are both written back'''
    data = {
        '@type': f'{NAMESPACE}.{holder}',
        '@model': 'rosetta_dsl',
        '@version': '0.0.0',
        'measure': {'@key:scoped': 'm1', 'value': 5},
        'measureRef': {'@ref:scoped': 'm1'},
    }
    obj = BaseDataClass.rune_deserialize(json.dumps(data))
    written = json.loads(obj.rune_serialize())
    assert written['measure'] == data['measure']
    assert written['measureRef'] == data['measureRef']
