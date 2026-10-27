#
# Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
# SPDX-License-Identifier: Apache-2.0
#

"""
## Bug Report

G2 – Non-bundle class builds pydantic schema from Phase-1 placeholder; schema stale
     after bundle Phase-3 model_rebuild

### Steps to Reproduce:
 1. Generate Python where Bar1/Bar2 are in a circular-dependency bundle (defer_build=True,
    Phase-1 placeholder fields set to None) and a non-bundle class (BarContainer) holds
    a field typed as Bar1.
 2. When BarContainer.py loads it imports Bar1 via `from … Bar1 import Bar1 as _Bar1`.
    If Bar1.py defines the class directly (old pattern), pydantic builds BarContainer's
    schema immediately using Bar1 with None placeholder fields.
 3. Bundle Phase-3 then calls model_rebuild(force=True) on Bar1, updating its fields,
    but BarContainer's schema is already cached with the stale placeholder.
 4. Attempting BarContainer(bar1=Bar1(number1=1)) raises pydantic ValidationError
    because BarContainer's schema still expects the old placeholder type.

### Expected Result:
Bundle class modules (Bar1.py) must use the lazy __getattr__ pattern so that any
`from … Bar1 import Bar1` triggers __getattr__, which runs the full bundle
(Phases 1-2-3) before returning the class. The importing class then always receives
the fully-rebuilt type and builds its schema correctly.

### Actual Result (bug scenario – CDM 7.7.0):
pydantic.ValidationError: bar1 field expected <Security placeholder>, got <Security>.

### Environment:
rune-python-generator; CDM 7.7.0 exhibited this; regression test for the fix.

### Additional Context:
The current generator emits the lazy __getattr__ pattern for all bundle class modules.
This test confirms that pattern is present and that importing a bundle class via
`from module import ClassName` delivers the fully-rebuilt class, not the placeholder.
"""

import sys

import pytest


def _clear_bundle_modules():
    to_remove = [k for k in sys.modules
                 if 'rosetta_dsl.test.model.circular_dependency' in k
                 or k == 'rosetta_dsl._bundle']
    for k in to_remove:
        del sys.modules[k]


def test_g2_bundle_class_module_uses_lazy_getattr():
    """
    Each bundle class module must define __getattr__ rather than a top-level
    class. A module-level class definition is the old pattern that causes G2:
    the importing module gets the class before Phase-3 rebuilds it.
    """
    import inspect
    import rosetta_dsl.test.model.circular_dependency.Bar1 as bar1_module
    import rosetta_dsl.test.model.circular_dependency.Bar2 as bar2_module

    for name, mod in [('Bar1', bar1_module), ('Bar2', bar2_module)]:
        source = inspect.getsource(mod)
        assert 'def __getattr__' in source, (
            f"G2 regression: {name}.py does not use lazy __getattr__ pattern"
        )
        assert 'class ' not in source, (
            f"G2 regression: {name}.py defines the class directly (old pattern)"
        )


def test_g2_from_import_of_bundle_class_returns_rebuilt_type():
    """
    `from … Bar1 import Bar1` must return the fully-rebuilt bundle class, not
    the Phase-1 placeholder. This verifies that __getattr__ triggers the bundle
    and Phase-3 completes before the class is returned.

    If G2 were present, Bar1 would have None placeholder field types and
    Bar1(number1=1) would raise a pydantic ValidationError.
    """
    _clear_bundle_modules()

    from rosetta_dsl.test.model.circular_dependency.Bar1 import Bar1

    # number1 is required (int). If G2 were present the field annotation would
    # still be None (placeholder) and pydantic would reject the value.
    try:
        bar1 = Bar1(number1=1)
    except Exception as exc:
        pytest.fail(
            f"G2 regression: Bar1(number1=1) raised {type(exc).__name__}: {exc}"
        )
    assert bar1.number1 == 1


def test_g2_bundle_phase3_completes_before_schema_is_used():
    """
    After a clean import of a bundle class, the bundle's Phase-3 model_rebuild
    must have run. Bar2 has a condition that checks bar1.number1 > 0; this
    condition can only execute correctly if Bar2's schema resolved Bar1's
    number1 field to int (not None placeholder).
    """
    _clear_bundle_modules()

    from rosetta_dsl.test.model.circular_dependency.Bar1 import Bar1
    from rosetta_dsl.test.model.circular_dependency.Bar2 import Bar2

    bar1 = Bar1(number1=1)
    bar2 = Bar2(number2=2, bar1=bar1)
    # If G2 were present, bar2's schema would hold a stale Bar1 and the
    # condition evaluation would either fail or silently use wrong types.
    bar2.validate_model()
