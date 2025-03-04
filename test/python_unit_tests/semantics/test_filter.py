'''filter unit test'''
import pytest

from rosetta_dsl.test.semantic.filter_operator.FilterItem import FilterItem
from rosetta_dsl.test.semantic.filter_operator.FilterTest import FilterTest

def test_filter_passes():
    target = 5
    filter_test = FilterTest(fis=[FilterItem(fi=target)], target=target)
    filter_test.validate_model()

if __name__ == "__main__":
    test_filter_passes()