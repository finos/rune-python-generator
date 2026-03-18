"""Enum usage unit tests"""

from rosetta_dsl.test.semantic.test_enum_usage.TrafficLight import TrafficLight
from rosetta_dsl.test.semantic.test_enum_usage.CheckLight import CheckLight


def test_enum_values():
    """Test enum member access and values."""
    # Generator uppercases enum members
    assert TrafficLight.RED.name == "RED"
    assert TrafficLight.YELLOW.name == "YELLOW"


def test_enum_function():
    """Test passing enum as function input."""
    # Function should handle enum correctly
    assert CheckLight(color=TrafficLight.RED) == "Stop"
    assert CheckLight(color=TrafficLight.GREEN) == "Go"
