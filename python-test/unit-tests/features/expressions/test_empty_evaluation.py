#
# Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
# SPDX-License-Identifier: Apache-2.0
#

"""Empty / absent value evaluation runtime tests.

Mirrors EmptyEvaluationTest in the rune-dsl Java generator tests, adapted for
the Python generator's runtime: optional attributes default to None, is-absent
returns True when the attribute is None, and exists returns True when it is set.
"""

from rosetta_dsl.test.expressions.empty_evaluation.Foo import Foo
from rosetta_dsl.test.expressions.empty_evaluation.functions.IsAbsent import IsAbsent
from rosetta_dsl.test.expressions.empty_evaluation.functions.IsPresent import IsPresent


def test_is_absent_when_none():
    """is absent returns True when the optional attribute is None."""
    foo = Foo(someBoolean=None, alwaysFalse=False)
    assert IsAbsent(foo=foo) is True


def test_is_absent_when_set():
    """is absent returns False when the optional attribute has a value."""
    foo = Foo(someBoolean=True, alwaysFalse=False)
    assert IsAbsent(foo=foo) is False


def test_exists_when_set():
    """exists returns True when the optional attribute is set."""
    foo = Foo(someBoolean=True, alwaysFalse=False)
    assert IsPresent(foo=foo) is True


def test_exists_when_none():
    """exists returns False when the optional attribute is None."""
    foo = Foo(someBoolean=None, alwaysFalse=False)
    assert IsPresent(foo=foo) is False
