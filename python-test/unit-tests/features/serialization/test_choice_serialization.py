#
# Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
# SPDX-License-Identifier: Apache-2.0
#

'''choice serialization unit test'''

from rune.runtime.base_data_class import BaseDataClass
from rosetta_dsl.test.choice_serialization.A import A
from rosetta_dsl.test.choice_serialization.B import B
from rosetta_dsl.test.choice_serialization.ChoiceSerializatonTest import ChoiceSerializatonTest
from rosetta_dsl.test.choice_serialization.functions.CreateExample import CreateExample

from rune.runtime.base_data_class import BaseDataClass

def test_choice_serialization():
    '''test choice serialization'''
    a = A(value=1)
    choice_from_a_in = ChoiceSerializatonTest(A=a)
    serialized_a = choice_from_a_in.rune_serialize()
    choice_from_a_out = BaseDataClass.rune_deserialize(serialized_a)
    assert choice_from_a_out == choice_from_a_in

    b = B(value=2)
    choice_from_b_in = ChoiceSerializatonTest(B=b)
    serialized_b = choice_from_b_in.rune_serialize()
    choice_from_b_out = BaseDataClass.rune_deserialize(serialized_b)
    assert choice_from_b_out == choice_from_b_in

def test_nested_choice_serialization():
    '''test nested choice serialization'''
    result = CreateExample()
    result_serialized = result.rune_serialize()
    deserialized_result = BaseDataClass.rune_deserialize(result_serialized)
    deserialized_result_serialized = deserialized_result.rune_serialize()
    print(result_serialized)
    print(deserialized_result_serialized)
    assert deserialized_result == result

if __name__ == "__main__":
    test_choice_serialization()
    test_nested_choice_serialization()