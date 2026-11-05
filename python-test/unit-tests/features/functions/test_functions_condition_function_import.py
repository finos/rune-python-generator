#
# Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
# SPDX-License-Identifier: Apache-2.0
#

'''conditions that call functions must work whichever module is imported first'''
import subprocess
import sys
import textwrap

import pytest

NAMESPACE = 'rosetta_dsl.test.functions.condition_function_import'
ENTRY_POINTS = [f'{NAMESPACE}.{name}' for name in
                ('GroupB1', 'GroupB2', 'WithBundledAttribute', 'WithoutBundledAttribute',
                 'functions.HasValue')]


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
def test_conditions_calling_functions_pass(entry_point):
    '''every condition can reach the function it calls'''
    result = _run_in_new_process(f'''
        import {entry_point}
        from {NAMESPACE}.GroupB1 import GroupB1
        from {NAMESPACE}.WithBundledAttribute import WithBundledAttribute
        from {NAMESPACE}.WithoutBundledAttribute import WithoutBundledAttribute
        group_b1 = GroupB1(value=1)
        for obj in (group_b1, WithBundledAttribute(groupB1=group_b1),
                    WithoutBundledAttribute(value=1)):
            failures = obj.validate_conditions(raise_exc=False)
            assert not failures, (type(obj).__name__, failures)
    ''')
    assert result.returncode == 0, result.stderr
