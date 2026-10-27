#
# Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
# SPDX-License-Identifier: Apache-2.0
#

"""
## Bug Report

G1 – Circular ImportError when non-bundle class is imported before _bundle initialises

### Steps to Reproduce:
 1. Generate Python from a Rune model where some types are in a circular-dependency
    bundle (Bar1/Bar2) and _bundle.py uses `from … ClassName import ClassName` for
    any non-bundle module.
 2. In a fresh Python process import the non-bundle module first; it imports a bundle
    type at module level, triggering _bundle to load; _bundle then tries to import the
    non-bundle module again from inside its own initialisation.
 3. The circular initialisation raises ImportError.

### Expected Result:
_bundle.py must use `import module` style (never `from module import ClassName`) for
all external references so that no module-level class import creates a circular chain.

### Actual Result (bug scenario – CDM 7.7.0):
  ImportError: cannot import name 'Asset' from partially initialized module
  'finos.cdm.base.staticdata.asset.common.asset'
CDM 7.7.0's _bundle.py contained `from … Asset import Asset` (line 4880), while
Asset.py imported bundle types at module level — forming the cycle.

### Environment:
rune-python-generator; CDM 7.7.0 exhibited this; regression test for the fix.

### Additional Context:
The current generator emits `import module` style for all non-bundle module references
in _bundle.py. This test confirms that pattern is preserved in the generated unit-test
bundle.
"""

import inspect

import pytest

import rosetta_dsl._bundle as bundle


def test_g1_bundle_does_not_use_direct_class_import_for_non_bundle_modules():
    """
    _bundle.py must not contain any `from <module> import <ClassName>` line for
    modules outside the bundle itself. Such a line would create a circular import
    if that module imports a bundle type at module level.

    The generator must emit `import module` style for enum/non-bundle references.
    """
    source = inspect.getsource(bundle)

    # Collect any `from X import Y` lines that are NOT standard-library / runtime imports
    forbidden_prefixes = (
        'rosetta_dsl.test.',
        'finos.',
    )
    violations = []
    for line in source.splitlines():
        stripped = line.strip()
        if not stripped.startswith('from '):
            continue
        # Allow runtime / pydantic imports
        if any(stripped.startswith(f'from {p}') for p in forbidden_prefixes):
            violations.append(stripped)

    assert not violations, (
        "G1 regression: _bundle.py uses direct class imports for non-bundle modules:\n"
        + "\n".join(violations)
    )
