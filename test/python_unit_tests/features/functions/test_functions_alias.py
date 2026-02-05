from rosetta_dsl.test.functions.functions.TestAlias import TestAlias


def test_alias():
    """Test alias"""
    assert TestAlias(inp1=5, inp2=10) == 5
    assert TestAlias(inp1=10, inp2=5) == 5


def test_alias_with_base_model_inputs():
    """Test alias with base model inputs"""
    #    a = A(valueA=5)
    #    b = B(valueB=10)
    #    c = TestAliasWithBaseModelInputs(a=a, b=b)
    #    print(c)
    #    assert c.valueC == 50
    pass
