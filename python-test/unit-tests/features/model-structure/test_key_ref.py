"""key ref unit tests — verifies that references resolve to the correct object and data."""

from rune.runtime.metadata import IntWithMeta, Reference

from rosetta_dsl.test.model.key_ref.AKey import AKey
from rosetta_dsl.test.model.key_ref.ANoRef import ANoRef
from rosetta_dsl.test.model.key_ref.BRef import BRef
from rosetta_dsl.test.model.key_ref.BNoRefAKey import BNoRefAKey
from rosetta_dsl.test.model.key_ref.BNoRefANoRef import BNoRefANoRef


def test_direct_assignment():
    """An object assigned directly to a [metadata reference] field is accessible."""
    a = AKey(aValue=IntWithMeta(value=42, key="key-123"))
    b = BRef(aKey=a)
    assert b.aKey.aValue == 42


def test_reference_assignment():
    """A Reference wrapper is accepted and resolves to the original object."""
    a = AKey(aValue=IntWithMeta(value=42, key="key-123"))
    b = BRef(aKey=Reference(target=a, ext_key="key-123"))
    assert len(b.validate_model()) == 0  # Reference wrapper accepted by validator
    assert b.aKey is a                   # resolves to the exact original object
    assert b.aKey.aValue == 42           # data accessible via the reference


def test_reference_data_matches_direct_assignment():
    """Data accessible via a Reference is the same as via direct assignment."""
    a = AKey(aValue=IntWithMeta(value=42, key="key-123"))
    b_direct = BRef(aKey=a)
    b_ref = BRef(aKey=Reference(target=a, ext_key="key-123"))
    assert b_ref.aKey.aValue == b_direct.aKey.aValue


def test_keyed_type_in_plain_field():
    """A [metadata key] type stored in a plain (non-reference) field is accessible."""
    a = AKey(aValue=IntWithMeta(value=7))
    b = BNoRefAKey(aKey=a)
    assert b.aKey.aValue == 7


def test_same_keyed_object_reused_across_plain_fields():
    """The same keyed object can be assigned to multiple plain fields."""
    a = AKey(aValue=IntWithMeta(value=5))
    b1 = BNoRefAKey(aKey=a)
    b2 = BNoRefAKey(aKey=a)
    assert b1.aKey.aValue == b2.aKey.aValue


def test_plain_type_in_plain_field():
    """A plain (non-keyed) type stored in a plain field is accessible."""
    a = ANoRef(aValue=IntWithMeta(value=3))
    b = BNoRefANoRef(aKey=a)
    assert b.aKey.aValue == 3


if __name__ == "__main__":
    test_direct_assignment()
    test_reference_assignment()
    test_reference_data_matches_direct_assignment()
    test_keyed_type_in_plain_field()
    test_same_keyed_object_reused_across_plain_fields()
    test_plain_type_in_plain_field()

# EOF
