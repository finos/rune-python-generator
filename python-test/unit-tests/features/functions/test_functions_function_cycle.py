#
# Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
# SPDX-License-Identifier: Apache-2.0
#

'''functions that call each other work whichever one is imported first'''
import subprocess
import sys
import textwrap

import pytest

NAMESPACE = 'rosetta_dsl.test.functions.function_cycle.functions'


def _run_in_new_process(code: str) -> subprocess.CompletedProcess:
    '''Import order is the behaviour under test, so each case needs a fresh
    interpreter: modules already loaded by pytest would hide the bug.'''
    return subprocess.run([sys.executable, '-c', textwrap.dedent(code)],
                          capture_output=True, text=True, check=False)


@pytest.mark.parametrize('first', ['IsEven', 'IsOdd'])
def test_functions_in_cycle_work_whichever_is_imported_first(first):
    '''each function can be imported first, and both then give the right answer'''
    result = _run_in_new_process(f'''
        from {NAMESPACE}.{first} import {first}
        from {NAMESPACE}.IsEven import IsEven
        from {NAMESPACE}.IsOdd import IsOdd
        assert IsEven(4) is True
        assert IsOdd(4) is False
        assert IsOdd(3) is True
    ''')
    assert result.returncode == 0, result.stderr


def test_function_calling_into_cycle_imported_first():
    '''a function outside the cycle that calls into it can be imported before the cycle'''
    result = _run_in_new_process(f'''
        from {NAMESPACE}.UsesIsEven import UsesIsEven
        assert UsesIsEven(4) is True
        assert UsesIsEven(3) is False
    ''')
    assert result.returncode == 0, result.stderr


def test_recursive_function():
    '''a function that calls itself works'''
    result = _run_in_new_process(f'''
        from {NAMESPACE}.Countdown import Countdown
        assert Countdown(3) == 0
    ''')
    assert result.returncode == 0, result.stderr


def test_function_outside_cycle():
    '''control: a call outside any cycle still works'''
    result = _run_in_new_process(f'''
        from {NAMESPACE}.UsesHelper import UsesHelper
        assert UsesHelper(1) == 2
    ''')
    assert result.returncode == 0, result.stderr
