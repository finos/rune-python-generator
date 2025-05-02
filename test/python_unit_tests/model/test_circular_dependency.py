'''circular dependency unit test'''
import pytest

from rosetta_dsl.test.model.circular_dependency.Bar1 import Bar1
from rosetta_dsl.test.model.circular_dependency.Bar2 import Bar2

def test_model_validates():
    '''test model validates'''
    bar1 = Bar1(number1=1)
    bar2 = Bar2(number2=2)
    bar2.bar1 = bar1
    bar2.validate_model()

if __name__ == "__main__":
    test_model_validates()
