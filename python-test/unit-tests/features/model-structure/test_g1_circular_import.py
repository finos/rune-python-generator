#
# Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
# SPDX-License-Identifier: Apache-2.0
#

"""
G1 – circular ImportError when a module is imported before the bundle has loaded.

Bar1 and Bar2 form a cycle, so both are generated into _bundle.py. BarLeaf is a
standalone type that the bundle uses, and BarContainer is a standalone type that uses
the bundle. In CDM 7.7.0, importing such a standalone module first made the bundle
import it again while it was still loading:

  ImportError: cannot import name 'Asset' from partially initialized module
  'finos.cdm.base.staticdata.asset.common.asset'

Each module must be importable first, and usable afterwards. Import order is the
behaviour under test, so every case runs in a new interpreter: modules already loaded
by pytest would hide the bug. How the generator writes the bundle's imports is checked
by the JUnit tests.
"""
import subprocess
import sys
import textwrap

import pytest

NAMESPACE = 'rosetta_dsl.test.model.circular_dependency'
ENTRY_POINTS = [f'{NAMESPACE}.{name}' for name in ('Bar1', 'Bar2', 'BarLeaf', 'BarContainer')]


def _run_in_new_process(code: str) -> subprocess.CompletedProcess:
    return subprocess.run([sys.executable, '-c', textwrap.dedent(code)],
                          capture_output=True, text=True, check=False)


@pytest.mark.parametrize('entry_point', ENTRY_POINTS)
def test_g1_module_can_be_imported_first(entry_point):
    '''importing any module first must not raise a circular ImportError'''
    result = _run_in_new_process(f'import {entry_point}')
    assert result.returncode == 0, result.stderr


@pytest.mark.parametrize('entry_point', ENTRY_POINTS)
def test_g1_types_are_usable_after_importing_module_first(entry_point):
    '''every type works together whichever module was imported first'''
    result = _run_in_new_process(f'''
        import {entry_point}
        from {NAMESPACE}.Bar1 import Bar1
        from {NAMESPACE}.BarContainer import BarContainer
        from {NAMESPACE}.BarLeaf import BarLeaf
        container = BarContainer(bar1=Bar1(number1=1, leaf=BarLeaf(value=2)))
        assert container.bar1.leaf.value == 2
    ''')
    assert result.returncode == 0, result.stderr
