'''key ref unit test'''
from rune.runtime.metadata import IntWithMeta, Reference

from rosetta_dsl.test.model.key_ref.A import A
from rosetta_dsl.test.model.key_ref.BRef import BRef
from rosetta_dsl.test.model.key_ref.BNoRef import BNoRef
 
def test_resuse_ref():
    '''test key ref'''
    a = A(aValue=IntWithMeta(value=1, key="key-123"))
    b1 = BRef(aReference=a)
    assert(len(b1.validate_model())== 0)
    b2 = BRef(aReference=Reference(target=a, ext_key="key-123"))
    assert(len(b2.validate_model())== 0)

def test_reuse_no_ref():
    '''test key ref'''
    a = A(aValue=IntWithMeta(value=1))
    b1 = BNoRef(aReference=a)
    assert(len(b1.validate_model())== 0)
    b2 = BNoRef(aReference=a)
    assert(len(b2.validate_model()) == 0)

if __name__ == "__main__":
    test_resuse_ref()
    test_reuse_no_ref()

#EOF    