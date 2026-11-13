#
# Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
# SPDX-License-Identifier: Apache-2.0
#

import json
import logging
import os
import re
from decimal import Decimal, InvalidOperation
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


# ---------------------------------------------------------------------------
# Round trip: read each sample, write it back and compare with the original
# ---------------------------------------------------------------------------

# Written by Python from its own package version, so it is expected to differ
_IGNORED_ROOT_FIELDS = ('@version',)
_MAX_REPORTED_DIFFERENCES = 10
_LABELS = dict(_SAMPLES)


def _load_json(text: str):
    # Decimal keeps precise numbers exact on both sides of the comparison
    document = json.loads(text, parse_float=Decimal)
    for field in _IGNORED_ROOT_FIELDS:
        document.pop(field, None)
    return document


def _value_kind(value) -> str:
    if isinstance(value, bool):
        return 'bool'
    if isinstance(value, (int, Decimal)):
        return 'number'
    return type(value).__name__


def _same_number(expected, actual) -> bool:
    '''True for a number and a string holding the same value. Python writes
    numbers as strings and Java as numbers; both read back as the same value.'''
    if {_value_kind(expected), _value_kind(actual)} != {'number', 'str'}:
        return False
    try:
        return Decimal(str(expected)) == Decimal(str(actual))
    except InvalidOperation:
        return False


def _compare(expected, actual, path: str = '') -> list[tuple[str, str, object, object]]:
    '''Returns (kind, path, expected, actual) for every difference.'''
    differences = []
    if isinstance(expected, dict) and isinstance(actual, dict):
        for key in sorted(set(expected) | set(actual)):
            child = f'{path}.{key}' if path else key
            if key not in actual:
                differences.append(('missing in output', child, expected[key], None))
            elif key not in expected:
                differences.append(('extra in output', child, None, actual[key]))
            else:
                differences.extend(_compare(expected[key], actual[key], child))
    elif isinstance(expected, list) and isinstance(actual, list):
        if len(expected) != len(actual):
            differences.append(('list length changed', path, len(expected), len(actual)))
        for i, (item_expected, item_actual) in enumerate(zip(expected, actual)):
            differences.extend(_compare(item_expected, item_actual, f'{path}[{i}]'))
    elif _same_number(expected, actual):
        pass
    elif _value_kind(expected) != _value_kind(actual):
        kind = f'type changed ({_value_kind(expected)} -> {_value_kind(actual)})'
        differences.append((kind, path, expected, actual))
    elif expected != actual:
        differences.append(('value changed', path, expected, actual))
    return differences


def _field_name(path: str) -> str:
    '''The last field in a path, without list indices, for grouping.'''
    return re.sub(r'\[\d+\]', '', path).rsplit('.', 1)[-1] or '(root)'


def _describe_error(error: Exception) -> str:
    '''A short form of the error for grouping, without values that vary.'''
    if isinstance(error, ValidationError):
        error_types = sorted({detail['type'] for detail in error.errors()})
        return f"ValidationError ({', '.join(error_types)})"
    message = re.sub(r'\s+', ' ', str(error))
    # Keep the underlying error that serialization wraps
    message = re.sub(r'^.*Error calling function `\w+`: ', '', message)
    message = re.sub(r'^(Error serializing to JSON: )?(PydanticSerializationError: )?', '', message)
    message = re.sub(r', got .*$', '', message)  # the value received varies, and may be long
    message = re.sub(r"'[^']*'|<class '[^']*'>", "'...'", message)
    if not message.startswith(type(error).__name__) and not re.match(r'^\w+Error: ', message):
        message = f'{type(error).__name__}: {message}'
    return message[:80]


@pytest.mark.parametrize("path", _PARAMS, ids=_IDS)
def test_roundtrip_fpml510_sample(path: str, roundtrip_results: dict):
    """Read each sample, write it back, and check the JSON is unchanged."""
    if path is None:
        pytest.skip("CDM_FPML510_SAMPLES_DIR not set")
    label = _LABELS[path]
    with open(path, "r", encoding="utf-8") as f:
        json_in = f.read()

    try:
        obj = finos.rune_deserialize(json_in, validate_model=False)
    except Exception as e:
        roundtrip_results[label] = [(f'read failed: {_describe_error(e)}', label)]
        pytest.fail(f"read failed: {type(e).__name__}: {e}")

    try:
        json_out = obj.rune_serialize(validate_model=False)
    except Exception as e:
        roundtrip_results[label] = [(f'write failed: {_describe_error(e)}', label)]
        pytest.fail(f"write failed: {type(e).__name__}: {e}")

    differences = _compare(_load_json(json_in), _load_json(json_out))
    roundtrip_results[label] = [(kind, _field_name(where)) for kind, where, _, _ in differences]
    if differences:
        shown = '\n'.join(f'  {kind}: {where}: {expected!r} -> {actual!r}'
                          for kind, where, expected, actual
                          in differences[:_MAX_REPORTED_DIFFERENCES])
        pytest.fail(f"{len(differences)} difference(s) after round trip:\n{shown}")
