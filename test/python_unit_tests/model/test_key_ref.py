'''key ref unit test'''
from rune.runtime.metadata import IntWithMeta, Reference

from rosetta_dsl.test.model.key_ref.A import A
from rosetta_dsl.test.model.key_ref.B import B

def test_key_ref():
    '''test key ref'''
    a = A(aValue=IntWithMeta(value=1, key="key-123"))
    b = B(aReference=Reference(target=a, ext_key="key-123"))
    assert(len(b.validate_model())== 0)

if __name__ == "__main__":
    test_key_ref()