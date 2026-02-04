"""functions unit test"""

import pytest
from rune.runtime.conditions import ConditionViolationError

from rosetta_dsl.test.functions.functions.TestAbsNumber import TestAbsNumber
from rosetta_dsl.test.functions.AInput import AInput
from rosetta_dsl.test.functions.functions.TestAbsInputType import TestAbsInputType
from rosetta_dsl.test.functions.functions.TestAbsOutputType import TestAbsOutputType
from rosetta_dsl.test.functions.functions.TestAlias import TestAlias
from rosetta_dsl.test.functions.functions.MinMaxWithSimpleCondition import (
    MinMaxWithSimpleCondition,
)
from rosetta_dsl.test.functions.functions.MinMaxWithPostCondition import (
    MinMaxWithPostCondition,
)
from rosetta_dsl.test.functions.functions.ArithmeticOperation import ArithmeticOperation
from rosetta_dsl.test.functions.ArithmeticOperationEnum import ArithmeticOperationEnum
from rosetta_dsl.test.functions.functions.MainFunction import MainFunction


def test_abs_positive():
    """Test abs positive"""
    result = TestAbsNumber(arg=5)
    assert result == 5


def test_abs_negative():
    """Test abs negative"""
    result = TestAbsNumber(arg=-5)
    assert result == 5


def test_abs_input_type_positive():
    """Test abs type positive"""
    a = AInput(a=5)
    result = TestAbsInputType(arg=a)
    assert result == 5


def test_abs_input_type_negative():
    """Test abs type negative"""
    a = AInput(a=-5)
    result = TestAbsInputType(arg=a)
    assert result == 5


def test_abs_output_type_positive():
    """Test abs output type positive"""
    result = TestAbsOutputType(arg=5)
    assert result.a == 5


def test_abs_output_type_negative():
    """Test abs output type negative"""
    result = TestAbsOutputType(arg=-5)
    assert result.a == 5


def test_alias():
    """Test alias"""
    assert TestAlias(inp1=5, inp2=10) == 5
    assert TestAlias(inp1=10, inp2=5) == 5


def test_alias_with_base_model_inputs():
    """Test alias with base model inputs"""


#    a = A(valueA=5)
#    b = B(valueB=10)
#    c = TestAliasWithBaseModelInputs(a=a, b=b)
#    print(c)
#    assert c.valueC == 50


def test_arithmetic_operation():
    """Test arithmetic operation"""
    assert ArithmeticOperation(n1=5, op=ArithmeticOperationEnum.ADD, n2=10) == 15
    assert ArithmeticOperation(n1=5, op=ArithmeticOperationEnum.SUBTRACT, n2=10) == -5
    assert ArithmeticOperation(n1=5, op=ArithmeticOperationEnum.MULTIPLY, n2=10) == 50
    assert ArithmeticOperation(n1=5, op=ArithmeticOperationEnum.DIVIDE, n2=10) == 0.5
    assert ArithmeticOperation(n1=5, op=ArithmeticOperationEnum.MAX, n2=10) == 10
    assert ArithmeticOperation(n1=5, op=ArithmeticOperationEnum.MIN, n2=10) == 5


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


def test_function_with_function_call():
    """Test function with function call"""
    assert MainFunction(value=5) == 10


if __name__ == "__main__":
    test_abs_positive()
    test_abs_negative()
    test_abs_input_type_positive()
    test_abs_input_type_negative()
    test_abs_output_type_positive()
    test_abs_output_type_negative()
    test_alias()
    test_alias_with_base_model_inputs()
    test_min_max_simple_conditions()
    test_min_max_post_conditions()
    test_arithmetic_operation()
    test_function_with_function_call()
# EOF
