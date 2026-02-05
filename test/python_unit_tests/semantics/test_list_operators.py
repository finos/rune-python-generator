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


# count tests
def test_count_passes():
    item1 = CountItem(name="item1", value=1)
    container = CountContainer(field1=[1, 2], field2=[item1])
    count_test = CountTest(bValue=[container])
    count_test.validate_model()


# sum tests
def test_sum_passes():
    sum_test = SumTest(aValue=2, bValue=3, target=5)
    sum_test.validate_model()


# min/max tests
def test_min_passes():
    min_test = MinTest(a=10)
    min_test.validate_model()


def test_min_fails():
    min_test = MinTest(a=-1)
    with pytest.raises(Exception):
        min_test.validate_model()


def test_max_passes():
    max_test = MaxTest(a=1)
    max_test.validate_model()


def test_max_fails():
    max_test = MaxTest(a=100)
    with pytest.raises(Exception):
        max_test.validate_model()


# last tests
def test_last_passes():
    last_test = LastTest(aValue=1, bValue=2, cValue=3, target=3)
    last_test.validate_model()


# sort tests
def test_sort_passes():
    sort_test = SortTest()
    sort_test.validate_model()


# join tests
def test_join_passes():
    join_test = JoinTest(field1="a", field2="b")
    join_test.validate_model()


# flatten tests
def test_flatten_passes():
    item1 = FlattenItem(field1=1, field2=2, field3=3)
    container = FlattenContainer(fieldList=[item1])
    flatten_test = FlattenTest(bValue=[container], field3=10)
    flatten_test.validate_model()


def test_flatten_foo_passes():
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
