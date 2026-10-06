import logging
import os
import pytest
from pydantic import BaseModel, ValidationError
import finos

logging.basicConfig(level=logging.INFO, format="%(levelname)s %(name)s: %(message)s")
logger = logging.getLogger(__name__)

# Both samples are fetched from the CDM repo at test time by run_cdm_tests.sh.
# Tests are skipped when the env vars are not set (e.g. when running pytest directly).
_FX_SAMPLE = os.environ.get("CDM_FX_SAMPLE_PATH")
_IRD_SAMPLE = os.environ.get("CDM_IRD_SAMPLE_PATH")


def _deserialize_file(path: str) -> BaseModel:
    with open(path, "r", encoding="utf-8") as f:
        json_data = f.read()
    logger.info("  input size  : %d bytes", len(json_data))
    return finos.rune_deserialize(json_data, validate_model=False)


@pytest.mark.parametrize("sample", [_FX_SAMPLE, _IRD_SAMPLE], ids=["fx-spot", "ird-vanilla-swap"])
def test_deserialize_trade_state(sample: str):
    """Deserialize a CDM TradeState sample via finos.rune_deserialize.

    finos.rune_deserialize resolves @type values without knowing the concrete class
    by pre-wiring namespace_prefix='finos'.
    """
    if sample is None:
        pytest.skip("sample path env var not set")
    logger.info("--- test_deserialize_trade_state ---")
    logger.info("  caller : finos.rune_deserialize(str)")
    logger.info("  sample : %s", sample)
    try:
        obj = _deserialize_file(sample)
        logger.info("  result type : %s", type(obj).__name__)
        logger.info("  result class: %s.%s", type(obj).__module__, type(obj).__name__)
        logger.info("  PASSED")
    except ValidationError as e:
        logger.error("  ValidationError:\n%s", e)
        pytest.fail(f"Deserialization failed with ValidationError: {e}")
    except Exception as e:
        logger.error("  Unexpected error: %s: %s", type(e).__name__, e)
        pytest.fail(f"Deserialization failed with {type(e).__name__}: {e}")
