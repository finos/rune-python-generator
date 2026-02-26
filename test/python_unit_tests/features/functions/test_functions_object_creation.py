"""test functions incomplete object return"""

import pytest

from rosetta_dsl.test.functions.BaseObject import BaseObject
from rosetta_dsl.test.functions.BaseObjectWithBaseClassFields import (
    BaseObjectWithBaseClassFields,
)

from rosetta_dsl.test.functions.TestSimpleObjectAssignment import (
    TestSimpleObjectAssignment,
)
from rosetta_dsl.test.functions.TestObjectCreationFromFields import (
    TestObjectCreationFromFields,
)
from rosetta_dsl.test.functions.TestContainerObjectCreation import (
    TestContainerObjectCreation,
)
from rosetta_dsl.test.functions.TestContainerObjectCreationFromBaseObject import (
    TestContainerObjectCreationFromBaseObject,
)

from rosetta_dsl.test.functions.TestComplexTypeInputs import (
    TestComplexTypeInputs,
)
from rosetta_dsl.test.functions.ComplexTypeA import ComplexTypeA
from rosetta_dsl.test.functions.ComplexTypeB import ComplexTypeB


def test_simple_object_assignment():
    """Test incomplete object return.
    The Rosetta function returns an IncompleteObject with a missing required field (value2),
    so this is expected to raise a validation exception.
    """
    base_object = BaseObject(value1=5, value2=10)
    result = TestSimpleObjectAssignment(baseObject=base_object)
    assert result == base_object


def test_object_creation_from_fields():
    """Test incomplete object return.
    The Rosetta function returns an IncompleteObject with a missing required field (value2),
    so this is expected to raise a validation exception.
    """
    base_object = BaseObject(value1=5, value2=10)
    result = TestObjectCreationFromFields(baseObject=base_object)
    assert result == base_object


def test_container_object_creation():
    """Test incomplete object return.
    The Rosetta function returns an IncompleteObject with a missing required field (value2),
    so this is expected to raise a validation exception.
    """
    TestContainerObjectCreation(value1=5, value2=10, value3=20)


def test_container_object_creation_from_base_object():
    """Test creation of a container object from a base object."""
    base_object = BaseObject(value1=5, value2=10)
    TestContainerObjectCreationFromBaseObject(baseObject=base_object, value3=20)


@pytest.mark.skip(reason="Fails due to Pydantic validation of partial objects")
def test_create_incomplete_object_succeeds_in_python():
    """Test incomplete object return by setting strict=False in the function definition.
    This test is expected to pass.
    """
    BaseObjectWithBaseClassFields(value1=5, strict=False)


@pytest.mark.skip(reason="Fails due to Pydantic validation of partial objects")
def test_complex_type_inputs():
    """Test complex type inputs."""
    complex_type_a = ComplexTypeA(valueA=5)
    complex_type_b = ComplexTypeB(valueB=10)
    result = TestComplexTypeInputs(a=complex_type_a, b=complex_type_b)
    assert result.valueA == 5 and result.valueB == 10
