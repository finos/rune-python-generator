import logging
import os
from pathlib import Path
import pytest
from pydantic import BaseModel, ValidationError
import finos

logging.basicConfig(level=logging.INFO, format="%(levelname)s %(name)s: %(message)s")
logger = logging.getLogger(__name__)

_SAMPLES_DIR = os.environ.get("CDM_FPML510_SAMPLES_DIR")


def _collect_samples() -> list[tuple[str, str]]:
    """Return [(abs_path, relative_label), ...] sorted by label."""
    if not _SAMPLES_DIR:
        return []
    base = Path(_SAMPLES_DIR)
    return sorted(
        (str(p), p.relative_to(base).as_posix())
        for p in base.rglob("*.json")
    )


_SAMPLES = _collect_samples()
# When the env var is absent, inject a single sentinel so pytest collects one
# skipped test rather than raising "no parametrize values" at collection time.
_PARAMS = [path for path, _ in _SAMPLES] or [None]
_IDS = [label for _, label in _SAMPLES] or ["skip"]


def _deserialize_file(path: str) -> BaseModel:
    with open(path, "r", encoding="utf-8") as f:
        json_data = f.read()
    logger.info("  input size  : %d bytes", len(json_data))
    return finos.rune_deserialize(json_data, validate_model=False)


@pytest.mark.parametrize("path", _PARAMS, ids=_IDS)
def test_deserialize_fpml510_sample(path: str):
    """Deserialize each fpml-5-10-products-* CDM sample via finos.rune_deserialize."""
    if path is None:
        pytest.skip("CDM_FPML510_SAMPLES_DIR not set")
    logger.info("  sample: %s", path)
    try:
        obj = _deserialize_file(path)
        logger.info("  result: %s.%s", type(obj).__module__, type(obj).__name__)
    except ValidationError as e:
        pytest.fail(f"ValidationError: {e}")
    except Exception as e:
        pytest.fail(f"{type(e).__name__}: {e}")
