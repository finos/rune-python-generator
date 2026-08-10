import logging
import os
import pytest
from pydantic import BaseModel, ValidationError
from finos.cdm.event.common.TradeState import TradeState

logging.basicConfig(level=logging.INFO, format="%(levelname)s %(name)s: %(message)s")
logger = logging.getLogger(__name__)

_SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
_DEFAULT_SAMPLE = os.path.join(_SCRIPT_DIR, "cdm-samples", "cd-ex01-long-asia-corp-fixreg.json")
sample_path = os.environ.get("CDM_SAMPLE_PATH", _DEFAULT_SAMPLE)


def _log_result(trade_state: BaseModel):
    logger.info("  result type : %s", type(trade_state).__name__)
    logger.info("  result class: %s.%s", type(trade_state).__module__, type(trade_state).__name__)


def test_deserialize_tradestate_from_string():
    """Deserialize from a JSON string using the concrete TradeState class.

    BaseDataClass.rune_deserialize cannot resolve CDM @type values because it has
    no namespace prefix; the CDM package lives under 'finos.*' but the @type field
    in the JSON omits that prefix (e.g. 'cdm.event.common.TradeState'). Calling via
    TradeState carries the 'finos' prefix so the runtime can construct the correct
    import path ('finos.cdm.event.common.TradeState').
    """
    logger.info("--- test_deserialize_tradestate_from_string ---")
    logger.info("  caller      : TradeState.rune_deserialize(str)")
    logger.info("  sample      : %s", sample_path)
    try:
        with open(sample_path, "r", encoding="utf-8") as f:
            json_data = f.read()
        logger.info("  input size  : %d bytes", len(json_data))
        trade_state = TradeState.rune_deserialize(json_data, validate_model=False)
        _log_result(trade_state)
        logger.info("  PASSED")
    except ValidationError as e:
        logger.error("  ValidationError:\n%s", e)
        pytest.fail(f"Deserialization failed with ValidationError: {e}")
    except Exception as e:
        logger.error("  Unexpected error: %s: %s", type(e).__name__, e)
        pytest.fail(f"Deserialization failed with {type(e).__name__}: {e}")

'''
def test_deserialize_tradestate_from_file():
    """Deserialize from a file path using BaseDataClass."""
    logger.info("--- test_deserialize_tradestate_from_file ---")
    logger.info("  caller      : BaseDataClass.rune_deserialize(path)")
    logger.info("  sample      : %s", sample_path)
    try:
        trade_state = BaseDataClass.rune_deserialize(sample_path)
        _log_result(trade_state)
        logger.info("  PASSED")
    except ValidationError as e:
        logger.error("  ValidationError:\n%s", e)
        pytest.fail(f"Deserialization failed with ValidationError: {e}")
    except Exception as e:
        logger.error("  Unexpected error: %s: %s", type(e).__name__, e)
        pytest.fail(f"Deserialization failed with {type(e).__name__}: {e}")


def test_deserialize_tradestate_from_file_from_TradeState():
    """Deserialize from a file path using the concrete TradeState class."""
    logger.info("--- test_deserialize_tradestate_from_file_from_TradeState ---")
    logger.info("  caller      : TradeState.rune_deserialize(path)")
    logger.info("  sample      : %s", sample_path)
    try:
        trade_state = TradeState.rune_deserialize(sample_path)
        _log_result(trade_state)
        logger.info("  PASSED")
    except ValidationError as e:
        logger.error("  ValidationError:\n%s", e)
        pytest.fail(f"Deserialization failed with ValidationError: {e}")
    except Exception as e:
        logger.error("  Unexpected error: %s: %s", type(e).__name__, e)
        pytest.fail(f"Deserialization failed with {type(e).__name__}: {e}")
'''

if __name__ == "__main__":
    test_deserialize_tradestate_from_string()
#    test_deserialize_tradestate_from_file()
#    test_deserialize_tradestate_from_file_from_TradeState()
