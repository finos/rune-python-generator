#
# Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
# SPDX-License-Identifier: Apache-2.0
#

'''deep path unit tests'''
import pytest

from rosetta_dsl.test.deep_path.Deep1 import Deep1
from rosetta_dsl.test.deep_path.Bar1 import Bar1
from rosetta_dsl.test.deep_path.Bar2 import Bar2
from rosetta_dsl.test.deep_path.Bar3 import Bar3
from rosetta_dsl.test.deep_path.Bar4 import Bar4
from rosetta_dsl.test.deep_path.Foo import Foo
from rosetta_dsl.test.deep_path.FooBar import FooBar
from rosetta_dsl.test.deep_path.functions.GetDeepAttr import GetDeepAttr
from rune.runtime.conditions import ConditionViolationError

def create_foo_from_bar1 (attr: int):
    deep1 = Deep1(attr = attr)
    bar1 = Bar1(deep1 = deep1, b1 = 10, a = 10)
    return Foo(bar1 = bar1)

def create_foo_from_bar3_with_bar2 (attr: int):
    deep1 = Deep1(attr = attr)
    bar2 = Bar2(deep1 = deep1, b1 = 10, c = 10)
    bar3 = Bar3(bar2 = bar2)
    return Foo(bar3 = bar3)

def create_foo_from_bar3_with_bar4 (attr: int):
    deep1 = Deep1(attr = attr)
    bar4 = Bar4(deep1 = deep1, b1 = 10)
    bar3 = Bar3(bar4 = bar4)
    return Foo(bar3 = bar3)

def test_deep_path_bar1():
    foo = create_foo_from_bar1(attr=2)
    with pytest.raises(Exception):
        foo.validate_model()
    foo = create_foo_from_bar1(attr=3)
    assert len(foo.validate_model()) == 0

def test_deep_path_bar3_with_bar2():
    foo = create_foo_from_bar3_with_bar2(attr=2)
    with pytest.raises(Exception):
        foo.validate_model()
    foo = create_foo_from_bar3_with_bar2(attr=3)
    assert len(foo.validate_model()) == 0

def test_deep_path_bar3_with_bar4():
    foo = create_foo_from_bar3_with_bar4(attr=2)
    with pytest.raises(Exception):
        foo.validate_model()
    foo = create_foo_from_bar3_with_bar4(attr=3)
    assert len(foo.validate_model()) == 0

def test_deep_path_on_named_receiver_in_condition():
    '''foo->>deep1->attr resolves on foo, not on the FooBar instance'''
    assert len(FooBar(foo=create_foo_from_bar1(attr=3)).validate_model()) == 0
    assert len(FooBar(foo=create_foo_from_bar3_with_bar2(attr=3)).validate_model()) == 0

def test_deep_path_on_named_receiver_in_condition_fails():
    '''the FooBar condition fails for the wrong value, independently of Foo's own condition'''
    foo_bar = FooBar(foo=create_foo_from_bar1(attr=2))
    errors = foo_bar.validate_conditions(recursively=False, raise_exc=False)
    assert len(errors) == 1
    assert isinstance(errors[0], ConditionViolationError)

def test_deep_path_on_function_input():
    '''foo->>deep1->attr resolves on the foo input, not on the function frame'''
    assert GetDeepAttr(foo=create_foo_from_bar1(attr=3)) == 3
    assert GetDeepAttr(foo=create_foo_from_bar3_with_bar2(attr=5)) == 5
    assert GetDeepAttr(foo=create_foo_from_bar3_with_bar4(attr=7)) == 7

if __name__ == "__main__":
    test_deep_path_bar1()
    test_deep_path_bar3_with_bar2()
    test_deep_path_bar3_with_bar4()
    test_deep_path_on_named_receiver_in_condition()
    test_deep_path_on_named_receiver_in_condition_fails()
    test_deep_path_on_function_input()
