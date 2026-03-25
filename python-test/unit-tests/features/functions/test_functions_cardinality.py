"""
Tests for cardinality modifier expressions: any = , any <> , all <> .
"""
from rosetta_dsl.test.functions.functions.TestAnyEquals import TestAnyEquals
from rosetta_dsl.test.functions.functions.TestAnyNotEquals import TestAnyNotEquals
from rosetta_dsl.test.functions.functions.TestAllNotEquals import TestAllNotEquals


def test_any_equals():
    """any = : True when at least one element equals the value."""
    assert TestAnyEquals(items=["A", "B", "C"], value="B")
    assert not TestAnyEquals(items=["A", "C", "D"], value="B")


def test_any_not_equals():
    """any <> : True when at least one element differs from the value."""
    # one element differs
    assert TestAnyNotEquals(items=["A", "B", "C"], value="B")
    # all elements match — no element differs
    assert not TestAnyNotEquals(items=["B", "B", "B"], value="B")


def test_all_not_equals():
    """all <> : True when no element equals the value (multi-element lists)."""
    # no element matches
    assert TestAllNotEquals(items=["A", "C", "D"], value="B")
    # one element matches — not all differ
    assert not TestAllNotEquals(items=["A", "B", "C"], value="B")
    # multi-element list, all differ — this is the case the old rune_all_elements zip approach got wrong
    assert TestAllNotEquals(items=["A", "C"], value="B")
