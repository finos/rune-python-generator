#
# Copyright (c) 2023 CLOUDRISK Limited and FT Advisory LLC
# SPDX-License-Identifier: Apache-2.0
#

'''only exists unit tests

`x -> a only exists` is true when `a` is set on the parent object `x` and every
other attribute of the parent's type (including inherited ones) is absent.
`(x -> a, x -> b) only exists` requires exactly `a` and `b` to be set.
'''
import pytest

from rune.runtime.conditions import ConditionViolationError
from rosetta_dsl.test.only_exists.Bar import Bar
from rosetta_dsl.test.only_exists.SubBar import SubBar
from rosetta_dsl.test.only_exists.Terms import Terms
from rosetta_dsl.test.only_exists.Holder import Holder
from rosetta_dsl.test.only_exists.functions.BeforeOnlyExists import BeforeOnlyExists
from rosetta_dsl.test.only_exists.functions.BeforeAndAfterOnlyExist import BeforeAndAfterOnlyExist
from rosetta_dsl.test.only_exists.functions.SubBarBeforeOnlyExists import SubBarBeforeOnlyExists


def test_single_path_only_attribute_set():
    assert BeforeOnlyExists(bar=Bar(before=1)) is True


def test_single_path_other_attribute_also_set():
    assert BeforeOnlyExists(bar=Bar(before=1, other=2)) is False


def test_single_path_attribute_absent():
    assert BeforeOnlyExists(bar=Bar(after=1)) is False


def test_single_path_nothing_set():
    assert BeforeOnlyExists(bar=Bar()) is False


def test_multiple_paths_exactly_those_set():
    assert BeforeAndAfterOnlyExist(bar=Bar(before=1, after=2)) is True


def test_multiple_paths_one_missing():
    assert BeforeAndAfterOnlyExist(bar=Bar(before=1)) is False


def test_multiple_paths_extra_attribute_set():
    assert BeforeAndAfterOnlyExist(bar=Bar(before=1, after=2, other=3)) is False


def test_inherited_type_only_attribute_set():
    assert SubBarBeforeOnlyExists(subBar=SubBar(before=1)) is True


def test_inherited_type_own_attribute_also_set():
    assert SubBarBeforeOnlyExists(subBar=SubBar(before=1, extra=2)) is False


def test_condition_on_bare_attribute_satisfied():
    Terms(a=1).validate_model()


def test_condition_on_bare_attribute_not_applicable():
    Terms(b=1, c=2).validate_model()


def test_condition_on_bare_attribute_violated():
    with pytest.raises(ConditionViolationError):
        Terms(a=1, b=2).validate_model()


def test_condition_on_path_satisfied():
    Holder(bar=Bar(before=1)).validate_model()


def test_condition_on_path_not_applicable():
    Holder(bar=Bar(after=1, other=2)).validate_model()


def test_condition_on_path_violated():
    with pytest.raises(ConditionViolationError):
        Holder(bar=Bar(before=1, after=2)).validate_model()
