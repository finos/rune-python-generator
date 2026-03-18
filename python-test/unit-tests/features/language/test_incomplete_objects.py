"""Incomplete objects unit tests"""

from rosetta_dsl.test.language.IncompleteObjects.A import A
from rosetta_dsl.test.language.IncompleteObjects.CreateA import CreateA


def test_create_incomplete_object():
    """
    Test creating an object in steps within a function.

    EXPECTED TO FAIL: The runtime _get_rune_object helper fails with KeyError
    because it cannot resolve model classes from the bundle namespace.
    See docs/FUNCTION_SUPPORT_DEV_ISSUES.md for details.
    """
    # TODO: remove old test
    #    with pytest.raises(KeyError, match="rosetta_dsl_test_language_IncompleteObjects_A"):
    #        CreateA(a1=10)
    res = CreateA(a1=10)
    assert isinstance(res, A)
    assert res.a1 == 10
    assert res.a2 == 20
