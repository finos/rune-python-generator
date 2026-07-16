#
# Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
# SPDX-License-Identifier: Apache-2.0
#

"""Constructor expression runtime tests."""

from rosetta_dsl.test.expressions.constructor.Foo import Foo
from rosetta_dsl.test.expressions.constructor.Item import Item
from rosetta_dsl.test.expressions.constructor.Container import Container
from rosetta_dsl.test.expressions.constructor.functions.BuildFooComplete import BuildFooComplete
from rosetta_dsl.test.expressions.constructor.functions.BuildFooPartial import BuildFooPartial
from rosetta_dsl.test.expressions.constructor.functions.BuildContainer import BuildContainer


def test_constructor_with_all_fields():
    """Foo { a: seed, b: 2 } populates both fields."""
    result = BuildFooComplete(seed=1)
    assert result.a == 1
    assert result.b == 2


def test_constructor_with_ellipsis_omits_optional_field():
    """Foo { a: val, ... } leaves the optional field b as None."""
    result = BuildFooPartial(val=42)
    assert result.a == 42
    assert result.b is None


def test_constructor_with_list_literal():
    """Container { items: [...] } produces a list of the specified items."""
    result = BuildContainer(n=3)
    assert isinstance(result, Container)
    assert len(result.items) == 3
    assert result.items[0].val == 1
    assert result.items[1].val == 2
    assert result.items[2].val == 3
