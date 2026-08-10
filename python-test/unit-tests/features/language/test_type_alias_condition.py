#
# Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
# SPDX-License-Identifier: Apache-2.0
#

"""Type alias condition tests.

Known gap: the Python generator strips type aliases to their underlying
primitive types (e.g. StringCode -> str).  Conditions defined on the
type alias (e.g. ValidCode: item <> "INVALID") are NOT enforced at
runtime in Python.

The Java generator, by contrast, generates a named validator (e.g.
StringCodeValidCode) that fires whenever an attribute of that alias
type is validated.

These tests document current behaviour: the alias is transparent and
the underlying primitive is used without condition enforcement.

Mirrors TypeAliasConditionTest in the rune-dsl Java generator tests.
"""

from rosetta_dsl.test.language.type_alias_condition.CodeHolder import CodeHolder
from rosetta_dsl.test.language.type_alias_condition.functions.BuildCodeHolder import (
    BuildCodeHolder,
)


def test_type_alias_reduces_to_underlying_type():
    """CodeHolder.code is a plain str field; the alias name is transparent."""
    holder = BuildCodeHolder(code="VALID")
    assert holder.code == "VALID"
    assert isinstance(holder.code, str)


def test_type_alias_condition_is_not_enforced():
    """
    Known gap: the ValidCode condition (item <> 'INVALID') is not enforced
    in Python.  A value that violates the alias condition is accepted without
    raising an error.
    """
    # This would fail validation in the Java generator but is silently accepted here.
    holder = BuildCodeHolder(code="INVALID")
    assert holder.code == "INVALID"  # condition NOT enforced
