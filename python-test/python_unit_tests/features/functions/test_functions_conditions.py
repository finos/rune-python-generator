import pytest
from rune.runtime.conditions import ConditionViolationError
from rosetta_dsl.test.functions.MinMaxWithSimpleCondition import (
    MinMaxWithSimpleCondition,
)
from rosetta_dsl.test.functions.MinMaxWithPostCondition import (
    MinMaxWithPostCondition,
)


def test_min_max_simple_conditions():
    """Test min max simple conditions"""
    assert MinMaxWithSimpleCondition(in1=5, in2=10, direction="min") == 5
    assert MinMaxWithSimpleCondition(in1=5, in2=10, direction="max") == 10
    with pytest.raises(ConditionViolationError):
        MinMaxWithSimpleCondition(in1=5, in2=-10, direction="none")


def test_min_max_post_conditions():
    """Test min max post conditions"""
    assert MinMaxWithPostCondition(in1=5, in2=10, direction="min") == 5
    assert MinMaxWithPostCondition(in1=5, in2=10, direction="max") == 10
    with pytest.raises(ConditionViolationError):
        MinMaxWithPostCondition(in1=5, in2=-10, direction="none")
