'''test the generated Python'''
import pytest
import logging

# Initialize logger
logger = logging.getLogger(__name__)

def test_import_qualify_asset_class_foreign_exchange():
    '''confirm that qualification function can be imported'''
    try:
        from finos.cdm.product.qualification.functions.Qualify_AssetClass_ForeignExchange import Qualify_AssetClass_ForeignExchange
    except ImportError:
        logger.error(f"Validation failed: {e}")
        pytest.fail("Importing cdm.product.qualification.functions.Qualify_AssetClass_ForeignExchange failed")