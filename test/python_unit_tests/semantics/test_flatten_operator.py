'''flatten operator unit tests'''
import pytest
from rosetta_dsl.test.semantic.flatten_operator.ClassA import ClassA
from rosetta_dsl.test.semantic.flatten_operator.ClassB import ClassB
from rosetta_dsl.test.semantic.flatten_operator.FlattenTest import FlattenTest
def test_flatten_operator_passes():
    a1=ClassA(field1=0,field2=1,field3=2)
    a2 = ClassA(field1=3, field2=4, field3=5)
    a3= ClassA(field1=6,field2=7,field3=8)
    a4=ClassA(field1=9,field2=10,field3=11)
    ab1= ClassB(fieldList=[a1,a2])
    ab2=ClassB(fieldList=[a3,a4])
    ftest=FlattenTest(bValue=[ab1,ab2],field3=2)
    ftest.validate_model()


