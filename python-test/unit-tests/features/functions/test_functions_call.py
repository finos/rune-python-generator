from rosetta_dsl.test.functions.MainFunction import MainFunction


def test_function_with_function_call():
    """Test function with function call"""
    assert MainFunction(value=5) == 10
