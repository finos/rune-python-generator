"""functions unit test"""

from rosetta_dsl.test.functions.TestAbsNumber import TestAbsNumber
# from rosetta_dsl.test.functions.TestAbsType import TestAbsType


def test_abs_positive():
    """Test abs positive"""
    result = TestAbsNumber(arg=5)
    assert result == 5


def test_abs_negative():
    """Test abs negative"""
    result = TestAbsNumber(arg=-5)
    assert result == 5


# def test_abs_type():
#     """Test abs type"""
#     a = A(a=5)
#     result = TestAbsType(a=a)
#     assert result == 5


# EOF
