#
# Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
# SPDX-License-Identifier: Apache-2.0
#

"""attribute assignment unit tests"""

from rosetta_dsl.test.functions.functions.TestSetAttributeOnObject import (
    TestSetAttributeOnObject,
)
from rosetta_dsl.test.functions.functions.TestAddAttributeOnObject import (
    TestAddAttributeOnObject,
)


def test_set_attribute_on_object():
    """Test set attribute on nested object"""
    val = 42
    result = TestSetAttributeOnObject(val=val)
    assert result.nested.value == 42


def test_add_attribute_on_object():
    """Test add attribute on object"""
    vals = [10, 20, 30]
    result = TestAddAttributeOnObject(vals=vals)
    assert result.values == [10, 20, 30]
