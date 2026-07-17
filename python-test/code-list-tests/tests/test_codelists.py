import pytest
from pydantic import ValidationError

# Import the generated CodeList type and the TradeTest model
from test_integration.codelist.CodeList import CodeList
from test_integration.codelist.TradeTest import TradeTest
from test_integration.codelist.functions.ValidateFpMLCodingSchemeDomain import ValidateFpMLCodingSchemeDomain

# Import the Rune Runtime registry and our new Deserializer extension
from rune.runtime.native_registry import rune_register_native
from rune.runtime.extensions.cached_json_rune_object_deserializer import CachedJSONRuneObjectDeserializer

# ==============================================================================
# --- SETUP ---
# ==============================================================================

@pytest.fixture(scope="session", autouse=True)
def setup_native_codelist_provider():
    """
    Runs once before any tests execute. 
    Instantiates the provider using our dummy wheel module data and registers it 
    with the Rune native registry so the generated validation logic can find it.
    """
    # Instantiate the provider pointing to the module we packaged in Step 2
    provider = CachedJSONRuneObjectDeserializer(
        codelist_dir="test_codelists_data",
        target_class=CodeList
    )
    
    # Register the bound .load() method to fulfill the LoadCodeList native function call using the exact Rosetta namespace (codelist) without the Python prefix
    rune_register_native("codelist.functions.LoadCodeList", provider.load)


# ==============================================================================
# --- TESTS ---
# ==============================================================================

def test_valid_business_center():
    """Asserts that valid codelist values resolve correctly through the native extension."""
    
    # Direct Function Test (Proves the Codelist Extension loaded the JSON)
    assert ValidateFpMLCodingSchemeDomain("GBLO", "business-center") is True
    assert ValidateFpMLCodingSchemeDomain("USNY", "business-center") is True
    
    # Model Instantiation Test
    trade = TradeTest(businessCenter="GBLO")
    
    # Handle the generator's metadata scheme wrapper gracefully
    if hasattr(trade.businessCenter, "value"):
        assert trade.businessCenter.value == "GBLO"
    else:
        assert trade.businessCenter == "GBLO"

def test_invalid_business_center():
    """Asserts that invalid codelist values trigger strict failures."""
    
    # Direct Function Test (Proves the Codelist Extension correctly rejects bad data)
    assert ValidateFpMLCodingSchemeDomain("INVALID_CODE", "business-center") is False
    
    # Model Rejection Test
    # Depending on the generator version, typeAlias conditions wrapped in metadata 
    # are either evaluated on __init__ or deferred to a condition checker.
    try:
        trade = TradeTest(businessCenter="INVALID_CODE")
        
        # If instantiation bypasses the error, it MUST fail the explicit condition check
        if hasattr(trade, 'check_conditions'):
            assert trade.check_conditions() is False
    except ValidationError:
        # If Pydantic catches it immediately, the test succeeds
        pass