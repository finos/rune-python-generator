#
# Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
# SPDX-License-Identifier: Apache-2.0
#

"""A subtype in a field declared as its parent type keeps its type (#329)."""

import json

from rosetta_dsl.test.model_structure.subtype_field.Animal import Animal
from rosetta_dsl.test.model_structure.subtype_field.Dog import Dog
from rosetta_dsl.test.model_structure.subtype_field.Owner import Owner

DOG_TYPE = "rosetta_dsl.test.model_structure.subtype_field.Dog"
DOG_JSON = {"@type": DOG_TYPE, "name": "Rex", "breed": "Collie"}


def _dog() -> Dog:
    return Dog(name="Rex", breed="Collie")


def _written(owner: Owner) -> dict:
    return json.loads(owner.rune_serialize(validate_model=False))


def _read(fields: dict) -> Owner:
    written = _written(Owner())
    return Owner.rune_deserialize(json.dumps(written | fields), validate_model=False)


def test_build_keeps_subtype():
    """A Dog passed to the constructor is stored as a Dog, alone and in a list."""
    owner = Owner(pet=_dog(), pets=[_dog()])
    assert type(owner.pet) is Dog
    assert type(owner.pets[0]) is Dog


def test_write_includes_type():
    """A Dog is written with its own fields and @type, alone and in a list."""
    written = _written(Owner(pet=_dog(), pets=[_dog()]))
    assert written["pet"] == DOG_JSON
    assert written["pets"] == [DOG_JSON]


def test_read_keeps_subtype():
    """@type selects Dog when reading, alone and in a list."""
    owner = _read({"pet": DOG_JSON, "pets": [DOG_JSON]})
    assert type(owner.pet) is Dog
    assert owner.pet.breed == "Collie"
    assert type(owner.pets[0]) is Dog


def test_round_trip_keeps_subtype():
    """Writing and reading back gives the same Dog."""
    owner = Owner(pet=_dog(), pets=[_dog()])
    read = Owner.rune_deserialize(owner.rune_serialize(validate_model=False), validate_model=False)
    assert type(read.pet) is Dog
    assert read.pet == owner.pet
    assert type(read.pets[0]) is Dog
    assert read.pets == owner.pets


def test_declared_type_is_written_without_type():
    """A value of the declared type itself is written without @type."""
    written = _written(Owner(pet=Animal(name="Tom")))
    assert written["pet"] == {"name": "Tom"}
