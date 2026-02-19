"""Enum usage unit tests"""

from rosetta_dsl.test.semantic.test_enum_usage.TrafficLight import TrafficLight
from rosetta_dsl.test.semantic.test_enum_usage.CheckLightTest import CheckLightTest


def test_enum_values():
    """Test enum member access and values."""
    # Generator uppercases enum members
    assert TrafficLight.RED.name == "RED"
    assert TrafficLight.YELLOW.name == "YELLOW"


def test_enum_function():
    """Test passing enum as input."""
    CheckLightTest(color=TrafficLight.RED, target="Stop").validate_model()
    CheckLightTest(color=TrafficLight.GREEN, target="Go").validate_model()
