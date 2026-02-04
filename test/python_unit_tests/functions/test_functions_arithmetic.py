from rosetta_dsl.test.functions.functions.ArithmeticOperation import ArithmeticOperation
from rosetta_dsl.test.functions.ArithmeticOperationEnum import ArithmeticOperationEnum


def test_arithmetic_operation():
    """Test arithmetic operation"""
    assert ArithmeticOperation(n1=5, op=ArithmeticOperationEnum.ADD, n2=10) == 15
    assert ArithmeticOperation(n1=5, op=ArithmeticOperationEnum.SUBTRACT, n2=10) == -5
    assert ArithmeticOperation(n1=5, op=ArithmeticOperationEnum.MULTIPLY, n2=10) == 50
    assert ArithmeticOperation(n1=5, op=ArithmeticOperationEnum.DIVIDE, n2=10) == 0.5
    assert ArithmeticOperation(n1=5, op=ArithmeticOperationEnum.MAX, n2=10) == 10
    assert ArithmeticOperation(n1=5, op=ArithmeticOperationEnum.MIN, n2=10) == 5
