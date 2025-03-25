'''count operator unit tests'''
import pytest

from rosetta_dsl.test.semantic.count_operator.classA import classA
from rosetta_dsl.test.semantic.count_operator.classB import classB
from rosetta_dsl.test.semantic.count_operator.CountTest import CountTest

def create_classA(name:str,value:int):
    return classA(name=name,value=value)
def create_classB (field1:list[int],field2:classA):
    return classB(field1=field1,field2=field2)
def test_count_operator_passes ():
    a1=create_classA("value1",1)
    a2=create_classA("value2",2)
    b=create_classB([],[a1,a2])
    ctest=CountTest(bValue=[b])
    ctest.validate_model()

def test_count_operator_fails ():
    b=create_classB([0,1],[])
    ctest=CountTest(bValue=[b])
    with pytest.raises(Exception):
        ctest.validate_model()

if __name__ == "__main__":
    test_count_operator_passes()
    test_count_operator_fails()