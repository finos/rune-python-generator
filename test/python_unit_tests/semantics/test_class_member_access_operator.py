import pytest
from rune.runtime.base_data_class import BaseDataClass
from rune.runtime.metadata import *
from rune.runtime.utils import *
from rune.runtime.conditions import *
from rosetta_dsl.test.model.class_member_access.ClassMemberAccess import ClassMemberAccess
 
class_member_access = ClassMemberAccess(one=42, three=[1, 2, 3])

def test_attribute_single ():
    assert rune_resolve_attr(class_member_access, 'one') == 42
def test_attribute_optional ():
    assert rune_resolve_attr(class_member_access, 'two') is None
def test_attribute_multi ():
    assert rune_resolve_attr(class_member_access, 'three') == [1, 2, 3]
def test_attribute_single_collection (): 
    assert rune_resolve_attr([class_member_access, class_member_access], 'one') == [42, 42]
def test_attribute_optional_collection (): 
    assert rune_resolve_attr([class_member_access, class_member_access], 'two') is None
def test_attribute_multi_collection ():
    assert rune_resolve_attr([class_member_access, class_member_access], 'three') == [1, 2, 3, 1, 2, 3]

if __name__ == "__main__":
    test_attribute_single ()
    test_attribute_optional ()
    test_attribute_multi ()
    test_attribute_single_collection () 
    test_attribute_optional_collection () 
    test_attribute_multi_collection ()