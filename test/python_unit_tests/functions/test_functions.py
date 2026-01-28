from rosetta_dsl.test.functions.Abs import Abs


def test_abs_positive():
    """Test abs positive"""
    result = Abs(arg=5)
    assert result == 5


def test_abs_negative():
    """Test abs negative"""
    result = Abs(arg=-5)
    assert result == 5


# EOF
