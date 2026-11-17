#
# Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
# SPDX-License-Identifier: Apache-2.0
#

'''conditions that use `as` to narrow to a data type work in standalone and bundled classes'''
import pytest

from rosetta_dsl.test.expressions.as_in_condition.Checker import Checker
from rosetta_dsl.test.expressions.as_in_condition.Circle import Circle
from rosetta_dsl.test.expressions.as_in_condition.DogB import DogB
from rosetta_dsl.test.expressions.as_in_condition.OwnerB import OwnerB
from rosetta_dsl.test.expressions.as_in_condition.Pet import Pet
from rosetta_dsl.test.expressions.as_in_condition.Shape import Shape

# Checker is standalone and OwnerB is bundled. DogB is bundled and Circle is standalone.
OWNERS = [Checker, OwnerB]


@pytest.mark.parametrize('owner', OWNERS)
@pytest.mark.parametrize('attr, value', [('pet', DogB), ('shape', Circle)])
def test_condition_passes_when_value_has_target_type(owner, attr, value):
    '''narrowing finds a value of the target type.

    Fails until the runtime keeps a subtype stored in a parent-typed attribute, rather than
    turning it into the parent type.'''
    assert not owner(**{attr: value()}).validate_conditions(raise_exc=False)


@pytest.mark.parametrize('owner', OWNERS)
@pytest.mark.parametrize('attr, value', [('pet', Pet), ('shape', Shape)])
def test_condition_fails_when_value_lacks_target_type(owner, attr, value):
    '''narrowing gives nothing for a value that isn't of the target type'''
    assert owner(**{attr: value()}).validate_conditions(raise_exc=False)


@pytest.mark.parametrize('owner', OWNERS)
def test_condition_passes_when_value_absent(owner):
    '''the conditions only apply when the attribute is set'''
    assert not owner().validate_conditions(raise_exc=False)
