import pytest

from pydantic import ValidationError

from rosetta_dsl.test.model.reuse_type.BaseEntity import BaseEntity
from rosetta_dsl.test.model.reuse_type.BarNoRef import BarNoRef
from rosetta_dsl.test.model.reuse_type.BarRef import BarRef


def test_entity_reuse():
    """Test entity reuse."""
    base_entity = BaseEntity(number=1)
    BarRef(bar=base_entity)
    with pytest.raises(ValidationError):
        BarNoRef(bar=base_entity)


if __name__ == "__main__":
    test_entity_reuse()
