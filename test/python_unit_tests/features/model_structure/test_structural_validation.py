"""Structural validation unit tests"""

import pytest
from rune.runtime.conditions import ConditionViolationError
from rosetta_dsl.test.semantic.model_structure.structural_validation.OneOfTest import (
    OneOfTest,
)
from rosetta_dsl.test.semantic.model_structure.structural_validation.RequiredChoiceTest import (
    RequiredChoiceTest,
)
from rosetta_dsl.test.semantic.model_structure.structural_validation.OptionalChoiceTest import (
    OptionalChoiceTest,
)


def test_one_of_valid():
    """Test one-of valid cases"""
    # Only one set
    t1 = OneOfTest(attr1="a")
    t1.validate_model()
    t2 = OneOfTest(attr2=1)
    t2.validate_model()


def test_one_of_invalid():
    """Test one-of invalid cases"""
    # Both set
    with pytest.raises(ConditionViolationError):
        t = OneOfTest(attr1="a", attr2=1)
        t.validate_model()

    # None set (standard one-of implies required choice among all fields)
    with pytest.raises(ConditionViolationError):
        t = OneOfTest()
        t.validate_model()


def test_required_choice_valid():
    """Test required choice valid cases"""
    t1 = RequiredChoiceTest(attr1="a")
    t1.validate_model()


def test_required_choice_invalid():
    """Test required choice invalid cases"""
    # Both set (choice allows only one)
    with pytest.raises(ConditionViolationError):
        t = RequiredChoiceTest(attr1="a", attr2=1)
        t.validate_model()

    # None set (required means must have one)
    with pytest.raises(ConditionViolationError):
        t = RequiredChoiceTest()
        t.validate_model()


def test_optional_choice_valid():
    """Test optional choice valid cases"""
    # None set (optional allows empty)
    t0 = OptionalChoiceTest()
    t0.validate_model()

    # One set
    t1 = OptionalChoiceTest(attr1="a")
    t1.validate_model()


def test_optional_choice_invalid():
    """Test optional choice invalid cases"""
    # Both set
    with pytest.raises(ConditionViolationError):
        t = OptionalChoiceTest(attr1="a", attr2=1)
        t.validate_model()
