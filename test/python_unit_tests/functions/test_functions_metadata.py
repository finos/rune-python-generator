from rune.runtime.metadata import Reference
from rosetta_dsl.test.functions.KeyEntity import KeyEntity
from rosetta_dsl.test.functions.RefEntity import RefEntity
from rosetta_dsl.test.functions.functions.MetadataFunction import MetadataFunction


def test_metadata_function():
    """Test metadata function"""
    key_entity = KeyEntity(value=5, key="key-123")  # noqa: F841
    ref_entity = RefEntity(ke=Reference(target=key_entity, ext_key="key-123"))
    assert MetadataFunction(ref=ref_entity) == 5
