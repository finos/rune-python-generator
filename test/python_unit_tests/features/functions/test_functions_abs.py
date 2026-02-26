from rosetta_dsl.test.functions.TestAbsNumber import TestAbsNumber
from rosetta_dsl.test.functions.AInput import AInput
from rosetta_dsl.test.functions.TestAbsInputType import TestAbsInputType
from rosetta_dsl.test.functions.TestAbsOutputType import TestAbsOutputType


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
