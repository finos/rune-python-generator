#
# Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
# SPDX-License-Identifier: Apache-2.0
#

'''
Tests for generator/packaging issues found in CDM 7.7.0 (finos-cdm 7.7.0,
rune.runtime 2.2.0, pydantic 2.14.1). G1, G2 and G3 are fixed, and their tests
guard against a regression. G4 documents one open defect. These are generator or
CDM-packaging issues, not runtime issues.

Each test imports only the CDM modules it uses, as a user would; none imports the
bundle directly.

G1  Fixed: circular import when Asset is the first CDM import
G2  Fixed: non-bundle class (Instrument) had a stale pydantic schema for a bundle
    class (Security) after bundle rebuild
G3  Fixed: PriceQuantityTriangulation iterated over its scalar (0..1) quantity
    as if it were a list
G4  Now() has no registered native implementation in the CDM package
'''

import subprocess
import sys
import pytest


# ---------------------------------------------------------------------------
# G1: Circular import
# ---------------------------------------------------------------------------

def test_asset_can_be_imported_first():
    '''Importing Asset as the first CDM import must not raise ImportError.

    Regression test. Types on a path between bundled types used to stay standalone,
    so _bundle.py imported Asset while Asset.py was still initialising. The generator
    now promotes those types into the bundle, so Asset.py is a lazy stub.
    '''
    result = subprocess.run(
        [sys.executable, '-c',
         'from finos.cdm.base.staticdata.asset.common.Asset import Asset'],
        capture_output=True, text=True
    )
    assert result.returncode == 0, result.stderr


# ---------------------------------------------------------------------------
# G2: Placeholder type caching
# ---------------------------------------------------------------------------

def test_instrument_accepts_bundled_security():
    '''Constructing Instrument(Security=security) must not raise ValidationError.

    Regression test. Instrument used to be standalone and built its schema while the
    bundle was still loading, copying the None placeholder for Security.identifier.
    The generator now promotes types on a path between bundled types into the bundle,
    so Instrument is rebuilt with the final annotations.
    '''
    from finos.cdm.base.staticdata.asset.common.AssetIdentifier import AssetIdentifier
    from finos.cdm.base.staticdata.asset.common.AssetIdTypeEnum import AssetIdTypeEnum
    from finos.cdm.base.staticdata.asset.common.AssetTypeEnum import AssetTypeEnum
    from finos.cdm.base.staticdata.asset.common.SecurityTypeEnum import SecurityTypeEnum
    from finos.cdm.base.staticdata.asset.common.Security import Security
    from finos.cdm.base.staticdata.asset.common.Instrument import Instrument
    from rune.runtime.metadata import StrWithMeta

    asset_id = AssetIdentifier(
        identifier=StrWithMeta('AAPL'),
        identifierType=AssetIdTypeEnum.RIC,
    )
    security = Security(
        identifier=[asset_id],
        assetType=AssetTypeEnum.SECURITY,
        securityType=SecurityTypeEnum.EQUITY,
    )
    instrument = Instrument(Security=security)
    assert instrument.Security.securityType == SecurityTypeEnum.EQUITY


# ---------------------------------------------------------------------------
# G3: PriceQuantityTriangulation — extract over a scalar
# ---------------------------------------------------------------------------

def test_price_quantity_triangulation_accepts_scalar_quantity():
    '''PriceQuantityTriangulation must accept a single NonNegativeQuantitySchedule.

    Regression test. CDM's PriceQuantity.quantity is a scalar (0..1) field, but the
    generated `quantity extract ...` used to iterate over it as a list. Inside rune_cow
    the value is a _COWObject, so the function raised TypeError. The generator now
    applies extract to a single value directly.
    '''
    from finos.cdm.observable.common.functions.PriceQuantityTriangulation import (
        PriceQuantityTriangulation,
    )
    from finos.cdm.base.math.NonNegativeQuantitySchedule import NonNegativeQuantitySchedule
    from decimal import Decimal

    # Use model_construct to bypass runtime issue R5 (absent-metadata validator)
    qty = NonNegativeQuantitySchedule.model_construct(value=Decimal('1000'))

    # With no prices there is nothing to check, so the triangulation holds
    assert PriceQuantityTriangulation(price=None, quantity=qty) is True


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
    from finos.cdm.base.datetime.functions.Now import Now

    with pytest.raises(NotImplementedError, match="doesn't have an implementation"):
        Now()
