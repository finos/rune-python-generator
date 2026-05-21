import pytest

# Import IoC container bootstrap script from CDM package
from finos.runtime_extensions.cdm_runtime import setup_cdm_runtime

# Import the official CDM facade for coding scheme validation
import finos.cdm.base.staticdata.codelist.functions.ValidateFpMLCodingSchemeDomain as cdm_api

# Initialize the IoC container for the entire test session
@pytest.fixture(autouse=True, scope="session")
def initialize_runtime():
    setup_cdm_runtime()

# Run the tests
def test_valid_business_center_code():
    is_valid = cdm_api.ValidateFpMLCodingSchemeDomain("GBLO", "business-center")
    assert is_valid is True

def test_invalid_business_center_code():
    is_valid = cdm_api.ValidateFpMLCodingSchemeDomain("DUMMY_CODE", "business-center")
    assert is_valid is False

def test_missing_domain_raises_error():
    with pytest.raises(FileNotFoundError):
        cdm_api.ValidateFpMLCodingSchemeDomain("GBLO", "unknown-domain")

