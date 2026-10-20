#
# Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
# SPDX-License-Identifier: Apache-2.0
#

'''
Tests for generator/packaging issues found in CDM 7.7.0 (finos-cdm 7.7.0,
rune.runtime 2.2.0, pydantic 2.14.1). Each test documents one defect and its
workaround. These are generator or CDM-packaging issues, not runtime issues.

G1  Circular import when Asset is the first CDM import
G2  Placeholder type caching: non-bundle class (Instrument) has a stale pydantic
    schema for a bundle class (Security) after bundle rebuild
G3  PriceQuantityTriangulation passes a scalar (0..1) quantity to a function
    that iterates over it as a list (same root cause as generator defect #300)
G4  Now() has no registered native implementation in the CDM package
'''

import subprocess
import sys
import pytest


# ---------------------------------------------------------------------------
# G1: Circular import
# ---------------------------------------------------------------------------

def test_circular_import_asset_without_bundle_fails():
    '''Importing Asset as the first CDM import raises ImportError.

    Root cause: Asset.py imports Instrument.py, which imports
    ListedDerivative.py (a lazy-loader that triggers finos._bundle).
    _bundle.py at line 4880 does `from ... Asset import Asset` while Asset.py
    is still initialising, returning a partially-initialised module.

    Generator fix: _bundle.py should use lazy attribute access for Asset
    rather than a module-level direct import.
    '''
    result = subprocess.run(
        [sys.executable, '-c',
         'from finos.cdm.base.staticdata.asset.common.Asset import Asset'],
        capture_output=True, text=True
    )
    assert result.returncode != 0, 'Expected ImportError but import succeeded'
    assert 'ImportError' in result.stderr or 'cannot import name' in result.stderr


# ---------------------------------------------------------------------------
# G2: Placeholder type caching
# ---------------------------------------------------------------------------

def test_placeholder_type_schema_stale_in_non_bundle_class():
    '''Constructing Instrument(Security=security) raises ValidationError even
    after finos._bundle is loaded.

    Root cause: Instrument.py is NOT in _bundle.py; pydantic builds Instrument's
    schema when Instrument.py is first loaded. At that point Security (a bundle
    class) has type None placeholders, so Instrument's schema embeds the
    placeholder definition for Security.identifier. _bundle.py Phase 3 rebuilds
    Security, but Instrument's cached schema is never updated.

    Generator fix: after Phase 3 rebuilds, the generator should also call
    model_rebuild(force=True) on non-bundle classes (like Instrument) whose
    fields reference bundle classes.

    Workaround: the CDM extension rebuilds all classes (bundle + non-bundle)
    with model_rebuild(force=True) at startup.
    '''
    import finos._bundle
    from finos.cdm.base.staticdata.asset.common.AssetIdentifier import AssetIdentifier
    from finos.cdm.base.staticdata.asset.common.AssetIdTypeEnum import AssetIdTypeEnum
    from finos.cdm.base.staticdata.asset.common.AssetTypeEnum import AssetTypeEnum
    from finos.cdm.base.staticdata.asset.common.SecurityTypeEnum import SecurityTypeEnum
    from finos.cdm.base.staticdata.asset.common.Security import Security
    from finos.cdm.base.staticdata.asset.common.Instrument import Instrument
    from rune.runtime.metadata import StrWithMeta
    from pydantic import ValidationError

    asset_id = AssetIdentifier(
        identifier=StrWithMeta('AAPL'),
        identifierType=AssetIdTypeEnum.RIC,
    )
    security = Security(
        identifier=[asset_id],
        assetType=AssetTypeEnum.SECURITY,
        securityType=SecurityTypeEnum.EQUITY,
    )
    # Security itself constructs fine (it is a bundle class, rebuilt in Phase 3).
    # Passing it to Instrument triggers re-validation against Instrument's stale
    # schema, which still sees Security.identifier as type None.
    with pytest.raises(ValidationError, match='Input should be None'):
        Instrument(Security=security)


# ---------------------------------------------------------------------------
# G3: PriceQuantityTriangulation — scalar passed as list (generator defect #300)
# ---------------------------------------------------------------------------

def test_price_quantity_triangulation_fails_with_scalar_quantity():
    '''PriceQuantityTriangulation iterates over quantity as a list, but CDM's
    PriceQuantity.quantity is a scalar (0..1) field.  Passing a single
    NonNegativeQuantitySchedule raises TypeError.

    Root cause: the generator wraps the function call in `map(lambda item: ...,
    rune_resolve_attr(self, "quantity") or [])`, iterating over the scalar value.
    Inside rune_cow, this becomes a _COWObject, which is not iterable.

    This is the same defect as generator issue #300 (list-wrapping for scalar
    constructor fields), manifesting in CDM's own generated code.
    '''
    import finos._bundle
    from finos.cdm.observable.common.functions.PriceQuantityTriangulation import (
        PriceQuantityTriangulation,
    )
    from finos.cdm.base.math.NonNegativeQuantitySchedule import NonNegativeQuantitySchedule
    from decimal import Decimal

    # Use model_construct to bypass runtime issue R5 (absent-metadata validator)
    qty = NonNegativeQuantitySchedule.model_construct(value=Decimal('1000'))

    with pytest.raises(TypeError, match="'_COWObject' object is not iterable"):
        PriceQuantityTriangulation(price=None, quantity=qty)


# ---------------------------------------------------------------------------
# G4: Now() native function not registered
# ---------------------------------------------------------------------------

def test_now_function_raises_not_implemented():
    '''Calling Now() raises NotImplementedError because the CDM Python package
    does not register a native implementation.

    Root cause: Now is declared [codeImplementation] in the CDM model; the
    generator emits a native-function stub that calls rune_execute_native, but
    CDM's packaging does not register the implementation.  This is a CDM
    packaging issue (not a generator or runtime issue) — the generator provides
    the registration mechanism; the CDM maintainers need to supply the
    implementation.
    '''
    import finos._bundle
    from finos.cdm.base.datetime.functions.Now import Now

    with pytest.raises(NotImplementedError, match="doesn't have an implementation"):
        Now()
