#
# Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
# SPDX-License-Identifier: Apache-2.0
#

'''Multiline docstring smoke test.

Verifies that a Rosetta type whose attribute carries a multiline description
compiles to valid, instantiable Python. There is no behavioural condition to
check; the test passes simply by constructing and validating the object.
'''
from rosetta_dsl.test.multiline.Multiline import Multiline


def test_multiline_instantiation():
    '''Type with multiline Rosetta docstring can be constructed and validated'''
    m = Multiline(attr=1)
    m.validate_model()


if __name__ == "__main__":
    test_multiline_instantiation()


# EOF
