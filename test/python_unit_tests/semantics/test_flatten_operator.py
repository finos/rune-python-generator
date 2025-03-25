'''flatten operator unit tests'''
import pytest
from rosetta_dsl.test.semantic.flatten_operator.intA import intA
from rosetta_dsl.test.semantic.flatten_operator.intAB import intAB
from rosetta_dsl.test.semantic.flatten_operator.FlattenTest import FlattenTest
def test_flatten_operator_passes():
    a1=intA(field1=0,field2=1,field3=2)
    a2 = intA(field1=3, field2=4, field3=5)
    a3= intA(field1=6,field2=7,field3=8)
    a4=intA(field1=9,field2=10,field3=11)
    ab1= intAB(fieldList=[a1,a2])
    ab2=intAB(fieldList=[a3,a4])
    ftest=FlattenTest(abValue=[ab1,ab2],field3=2)
    ftest.validate_model()


