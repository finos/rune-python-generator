"""list operator unit tests"""

import pytest

from rosetta_dsl.test.semantic.collections.CountItem import CountItem
from rosetta_dsl.test.semantic.collections.CountContainer import CountContainer
from rosetta_dsl.test.semantic.collections.CountTest import CountTest
from rosetta_dsl.test.semantic.collections.SumTest import SumTest
from rosetta_dsl.test.semantic.collections.MinTest import MinTest
from rosetta_dsl.test.semantic.collections.MaxTest import MaxTest
from rosetta_dsl.test.semantic.collections.LastTest import LastTest
from rosetta_dsl.test.semantic.collections.SortTest import SortTest
from rosetta_dsl.test.semantic.collections.JoinTest import JoinTest
from rosetta_dsl.test.semantic.collections.FlattenItem import FlattenItem
from rosetta_dsl.test.semantic.collections.FlattenContainer import FlattenContainer
from rosetta_dsl.test.semantic.collections.FlattenTest import FlattenTest
from rosetta_dsl.test.semantic.collections.FlattenBar import FlattenBar
from rosetta_dsl.test.semantic.collections.FlattenFoo import FlattenFoo
from rosetta_dsl.test.semantic.collections.FilterItem import FilterItem
from rosetta_dsl.test.semantic.collections.FilterTest import FilterTest


def test_count_passes():
    """count tests"""
    item1 = CountItem(name="item1", value=1)
    container = CountContainer(field1=[1, 2], field2=[item1])
    count_test = CountTest(bValue=[container])
    count_test.validate_model()


def test_sum_passes():
    """sum tests"""
    sum_test = SumTest(aValue=2, bValue=3, target=5)
    sum_test.validate_model()


def test_min_passes():
    """min tests passes"""
    min_test = MinTest(a=10)
    min_test.validate_model()


def test_min_fails():
    """min tests fails"""
    min_test = MinTest(a=-1)
    with pytest.raises(Exception):
        min_test.validate_model()


def test_max_passes():
    """max tests passes"""
    max_test = MaxTest(a=1)
    max_test.validate_model()


def test_max_fails():
    """max tests fails"""
    max_test = MaxTest(a=100)
    with pytest.raises(Exception):
        max_test.validate_model()


def test_last_passes():
    """last tests passes"""
    last_test = LastTest(aValue=1, bValue=2, cValue=3, target=3)
    last_test.validate_model()


def test_sort_passes():
    """sort tests passes"""
    sort_test = SortTest()
    sort_test.validate_model()


def test_join_passes():
    """join tests passes"""
    JoinTest(field1="a", field2="b", delimiter="", target="ab").validate_model()


def test_flatten_passes():
    """flatten tests passes"""
    flatten_item = FlattenItem(items=[1, 2, 3])
    flatten_container = FlattenContainer(
        items=[flatten_item, flatten_item, flatten_item]
    )
    FlattenTest(
        fc=[flatten_container], target=[1, 2, 3, 1, 2, 3, 1, 2, 3]
    ).validate_model()


def test_flatten_foo_passes():
    """flatten foo tests passes"""
    bar1 = FlattenBar(numbers=[1, 2])
    bar2 = FlattenBar(numbers=[3])
    foo = FlattenFoo(bars=[bar1, bar2])
    foo.validate_model()


# filter tests
def test_filter_passes():
    item1 = FilterItem(fi=1)
    item2 = FilterItem(fi=2)
    filter_test = FilterTest(fis=[item1, item2], target=1)
    filter_test.validate_model()
