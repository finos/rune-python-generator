'''join unit test'''
import pytest

from rosetta_dsl.test.semantic.join_operator.JoinTest import JoinTest

def test_join_passes():
    sort_test= JoinTest(field1="a", field2="b")
    print(sort_test)
    sort_test.validate_model()

#def test_join_fails():
#    sort_test= JoinTest(field1="c", field2="b")
#    sort_test.validate_model()

if __name__ == "__main__":
    test_join_passes()