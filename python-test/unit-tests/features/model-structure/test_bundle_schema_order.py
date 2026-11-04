#
# Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
# SPDX-License-Identifier: Apache-2.0
#

'''schema and import behaviour must not depend on which module loads first'''
import subprocess
import sys
import textwrap

import pytest

NAMESPACE = 'rosetta_dsl.test.model.bundle_schema_order'
BUNDLE = 'rosetta_dsl._bundle'
ENTRY_POINTS = [BUNDLE] + [f'{NAMESPACE}.{name}' for name in
                           ('Inner', 'Outer', 'GroupA1', 'GroupB1')]


def _run_in_new_process(code: str) -> subprocess.CompletedProcess:
    '''Import order is the behaviour under test, so each case needs a fresh
    interpreter: modules already loaded by pytest would hide the bug.'''
    return subprocess.run([sys.executable, '-c', textwrap.dedent(code)],
                          capture_output=True, text=True, check=False)


@pytest.mark.parametrize('entry_point', ENTRY_POINTS)
def test_module_can_be_imported_first(entry_point):
    '''no import order may raise a circular ImportError'''
    result = _run_in_new_process(f'import {entry_point}')
    assert result.returncode == 0, result.stderr


@pytest.mark.parametrize('entry_point', ENTRY_POINTS)
def test_standalone_type_validates_nested_bundled_type(entry_point):
    '''a standalone type must not keep the bundle's `None` placeholders'''
    result = _run_in_new_process(f'''
        import {entry_point}
        from {NAMESPACE}.Inner import Inner
        inner = Inner.model_validate(
            {{'groupB1': {{'value': 1, 'groupB2': {{'value': 2}}}}}})
        assert inner.groupB1.groupB2.value == 2
    ''')
    assert result.returncode == 0, result.stderr
