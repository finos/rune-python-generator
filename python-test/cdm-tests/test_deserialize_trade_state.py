import logging
import pytest
from pydantic import ValidationError
from rune.runtime.base_data_class import BaseDataClass
# from finos.cdm.event.common.TradeState import TradeState

# Initialize logger
logger = logging.getLogger(__name__)

# Path to standard FpML sample
sample_path = "cdm-samples/cd-ex01-long-asia-corp-fixreg.json"

def test_deserialize_tradestate():
    """Confirm that tradestate can be deserialized."""
    try:
        with open(sample_path, "r", encoding="utf-8") as f:
            json_data = f.read()
        
        # This crashes with: Input should be None 
        trade_state = BaseDataClass.rune_deserialize(json_data)

        logger.info("Trade State deserialization test successfully passed!")
    except ValidationError as e:
        logger.error(f"Validation failed: {e}")
        pytest.fail("Deserializing sample fpml-5-13-products-credit-derivatives/cd-ex01-long-asia-corp-fixreg.json failed")

if __name__ == "__main__":
    test_deserialize_tradestate()
