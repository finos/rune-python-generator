from rosetta_dsl.test.semantic.logical_op.AndOperatorTest import AndOperatorTest
    
def test_and_operator():
    logicalTest = AndOperatorTest(aValue=4, bValue=4, cValue=4)
    logicalTest.validate_model()