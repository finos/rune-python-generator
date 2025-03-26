'''flatten operator unit tests'''
import pytest
from rosetta_dsl.test.semantic.flatten_with_comparison.Bar import Bar
from rosetta_dsl.test.semantic.flatten_with_comparison.Foo import Foo

def test_flatten_operator_passes():
    '''Test flatten operator passes'''
    bar = Bar(numbers=[1, 2, 3])
    foo = Foo(bars=[bar])
    foo.validate_model()

# EOF