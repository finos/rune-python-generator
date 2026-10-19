#
# Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
# SPDX-License-Identifier: Apache-2.0
#

"""
Regression tests for known Python generator defects (D15 and related, gap G39).

Each test exercises a defective pattern directly. A test FAILS when the defect
is present and PASSES when the generator has been fixed.

The three silent defects (precondition runs before aliases are assigned;
set x: y then set x->attr writes through to the caller's y; if-then without
else returns True) are covered by test_functions_conditions.py and
test_functions_pass_by_value.py respectively.
"""

import pytest

from rosetta_dsl.test.robustness.generator_defects.functions.SetThroughAbsent import (
    SetThroughAbsent,
)
from rosetta_dsl.test.robustness.generator_defects.OuterContainer import (
    OuterContainer,
)
from rosetta_dsl.test.robustness.generator_defects.functions.ConstructListFromSingle import (
    ConstructListFromSingle,
)
from rosetta_dsl.test.robustness.generator_defects.functions.AddToAbsentList import (
    AddToAbsentList,
)
from rosetta_dsl.test.robustness.generator_defects.functions.FilterNonNegative import (
    FilterNonNegative,
)
from rosetta_dsl.test.robustness.generator_defects.functions.ConditionalExtract import (
    ConditionalExtract,
)
from rosetta_dsl.test.robustness.generator_defects.functions.EmptyWhenFalse import (
    EmptyWhenFalse,
)


def test_set_through_absent_intermediate():
    """set a->b->c on an absent b creates b.

    Defect: the generator does not create the intermediate (b) when it is
    absent; the attribute assignment raises AttributeError or b stays None.
    """
    result = SetThroughAbsent(val=42)
    assert result.inner is not None, "intermediate InnerValue was not created"
    assert result.inner.value == 42


def test_set_through_absent_input():
    """set a->b->c on an absent b in an input object creates b.

    Passing an OuterContainer with no inner and asking the function to set
    inner->value should produce an inner with that value, not fail.
    """
    outer = OuterContainer()
    result = SetThroughAbsent(val=7)
    assert result.inner is not None
    assert result.inner.value == 7


def test_construct_list_from_single_item():
    """A single item given to a list attribute in a constructor is wrapped in a list.

    Defect: the generated constructor passes the scalar value directly to the
    pydantic field; pydantic raises a validation error because the field
    expects a list.
    """
    result = ConstructListFromSingle(inp=5)
    assert isinstance(result.items, list), (
        f"items should be a list, got {type(result.items)}"
    )
    assert result.items == [5]


def test_add_to_absent_list():
    """add to an absent list initialises the list.

    Defect: the generated code calls list.append on None because the output
    attribute is not initialised before the first add, raising AttributeError.
    """
    result = AddToAbsentList(val=99)
    assert result.values == [99], (
        f"expected [99], got {result.values!r}"
    )


def test_filter_with_function_call():
    """A function called inside filter is imported in the generated module.

    Defect: the generator does not emit an import for IsNonNegative inside
    FilterNonNegative's module; calling the function raises NameError.
    """
    result = FilterNonNegative(vals=[1, -2, 3, 0, -5])
    assert result == [1, 3, 0], f"expected [1, 3, 0], got {result!r}"


def test_conditional_extract_item_in_scope():
    """item is in scope inside both branches of an if-then-else inside extract.

    Defect: the generator builds a closure for the conditional expression where
    item is not captured; the then/else branches raise NameError, or item
    resolves to the loop variable from an outer scope (wrong value).
    """
    result = ConditionalExtract(vals=[2, -3, 0, 5, -1])
    assert result == [2, 0, 0, 5, 0], (
        f"expected [2, 0, 0, 5, 0], got {result!r}"
    )


def test_empty_function_result_is_none_not_empty_list():
    """An absent optional output is None, not an empty list.

    Defect: when a function's output resolves to absent (e.g. via
    `if cond then val else empty`), the Python runtime returns [] instead
    of None, making downstream is-absent checks incorrect.
    """
    absent = EmptyWhenFalse(val=-1)
    assert absent is None, (
        f"expected None for absent result, got {absent!r}"
    )
    present = EmptyWhenFalse(val=10)
    assert present == 10
