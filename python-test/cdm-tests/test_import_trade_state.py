#
# Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
# SPDX-License-Identifier: Apache-2.0
#

'''test the generated Python'''
# pylint: disable=import-outside-toplevel at top, unused-import
import pytest

def test_import_tradestate():
    '''confirm that tradestate can be imported'''
    try:
        from finos.cdm.event.common.TradeState import TradeState
    except ImportError:
        pytest.fail("Importing cdm.event.common.TradeState failed")