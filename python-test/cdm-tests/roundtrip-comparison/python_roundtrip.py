#
# Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
# SPDX-License-Identifier: Apache-2.0
#

"""Read each sample with the generated CDM Python, write it back, and validate it.

    python python_roundtrip.py <samples.tsv> <input-root> <output-root> <results.jsonl>

Run it with the interpreter of the environment the CDM wheel is installed in. Each sample's
outputPath is read from <input-root> and written to <output-root>. The comparison of input
and output is done by compare.py, so that both languages are compared with the same rules.
The root type is the optional tenth column of <samples.tsv>; without it, it follows from the
test pack.

A sample is read with `finos.rune_deserialize(..., validate_model=False)`. That raises on a
reference whose key isn't in the file; Java reads such a reference as empty instead. So the
read is then retried with dangling references left unresolved, and the sample is written and
validated from that, so the rest of the comparison still covers it.
"""
import importlib
import inspect
import json
import logging
import re
import sys
from decimal import Decimal
from pathlib import Path

from pydantic import ValidationError

import finos
import finos._bundle as bundle
from finos.cdm.event.common.TradeState import TradeState
from finos.cdm.event.workflow.WorkflowStep import WorkflowStep
from rune.runtime.base_data_class import _rune_json_input  # pylint: disable=protected-access
from rune.runtime.conditions import ConditionViolationError

MAX_ERROR = 400
CONDITION_MESSAGE = re.compile(r'Condition "([^"]+)" for ')
CONDITION_NAME = re.compile(r'^condition_\d+_')

# Python class -> Rune type name, for naming the type a condition belongs to
RUNE_NAMES = {f'{c.__module__}.{c.__qualname__}': c._FQRTN
              for _, c in inspect.getmembers(bundle, inspect.isclass)
              if getattr(c, '_FQRTN', None)}


def describe(error: Exception) -> str:
    text = type(error).__name__ + ": " + re.sub(r"\s+", " ", str(error))
    return text[:MAX_ERROR]


def error_kind(error: Exception) -> str:
    '''What kind of problem a read failed on, for grouping.'''
    if isinstance(error, KeyError):
        return 'dangling reference'
    if isinstance(error, TypeError) and "Can't set reference" in str(error):
        return 'reference to wrong type'
    if isinstance(error, ValidationError):
        kinds = set()
        for d in error.errors():
            if d['type'] in ('missing', 'too_short'):
                kinds.add('required field missing')
            elif 'Duplicated keys' in d['msg']:
                kinds.add('duplicate key')
            elif "Can't set reference" in d['msg']:
                kinds.add('reference to wrong type')
            else:
                kinds.add(f"invalid value ({d['type']})")
        return '; '.join(sorted(kinds))
    return type(error).__name__


def error_paths(error: Exception) -> list[str]:
    '''The fields a pydantic error is about, without list indices.'''
    if not isinstance(error, ValidationError):
        return []
    return sorted({re.sub(r'\.\d+(?=\.|$)', '', '.'.join(str(p) for p in d['loc']))
                   for d in error.errors()})


def read_failed(result: dict, error: Exception) -> None:
    result['read_error'] = describe(error)
    result['read_error_kind'] = error_kind(error)
    result['read_error_paths'] = error_paths(error)


def rune_type(python_class: str) -> str:
    if python_class in RUNE_NAMES:
        return RUNE_NAMES[python_class]
    return python_class.removeprefix('finos.').rsplit('.', 1)[0]


def failures(errors: list) -> list[dict]:
    result = []
    for error in errors:
        if isinstance(error, ValidationError):
            for detail in error.errors():
                result.append({'kind': 'ATTRIBUTE', 'type': error.title, 'rule': detail['type'],
                               'path': '.'.join(str(p) for p in detail['loc']),
                               'reason': detail['msg']})
        elif isinstance(error, ConditionViolationError):
            match = CONDITION_MESSAGE.match(str(error))
            name = match.group(1) if match else str(error)
            owner, condition = name.rsplit('.', 1)
            result.append({'kind': 'CONDITION', 'type': rune_type(owner),
                           'rule': CONDITION_NAME.sub('', condition), 'path': '', 'reason': ''})
        else:
            result.append({'kind': 'OTHER', 'type': '', 'rule': type(error).__name__, 'path': '',
                           'reason': describe(error)})
    return result


def read_ignoring_dangling(text: str, root_class):
    data = json.loads(text, parse_float=Decimal)
    data.pop('@version', None)
    data.pop('@model', None)
    with _rune_json_input():
        model = root_class.model_validate(data, strict=False)
    model.resolve_references(ignore_dangling=True, recurse=True)
    return model


def run_one(pack: str, sample: str, in_root: Path, out_root: Path, root_type: str = '') -> dict:
    result = {'sample': sample}
    source = in_root / sample
    if not source.exists():
        result['read'] = 'no input'
        return result
    text = source.read_text(encoding='utf-8')
    if root_type:
        try:
            module = importlib.import_module('finos.' + root_type)
            root_class = getattr(module, root_type.rsplit('.', 1)[1])
        except (ImportError, AttributeError) as error:
            result['read'] = 'error'
            result['read_error'] = f'unknown root type: {root_type} ({describe(error)})'
            return result
    else:
        root_class = WorkflowStep if pack.startswith('workflow-step') else TradeState
    try:
        model = finos.rune_deserialize(text, validate_model=False)
        result['read'] = 'ok'
    except KeyError as error:
        result['read'] = 'dangling reference'
        read_failed(result, error)
        try:
            model = read_ignoring_dangling(text, root_class)
        except Exception as retry_error:  # pylint: disable=broad-except
            result['read'] = 'error'
            read_failed(result, retry_error)
            return result
    except Exception as error:  # pylint: disable=broad-except
        result['read'] = 'error'
        read_failed(result, error)
        return result

    try:
        target = out_root / sample
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(model.rune_serialize(validate_model=False, indent=2), encoding='utf-8')
        result['write'] = 'ok'
    except Exception as error:  # pylint: disable=broad-except
        result['write'] = 'error'
        result['write_error'] = describe(error)

    try:
        errors = model.validate_model(check_rune_constraints=True, raise_exc=False, strict=False)
        result['failures'] = failures(errors)
        result['validate'] = 'ok'
    except Exception as error:  # pylint: disable=broad-except
        result['validate'] = 'error'
        result['validate_error'] = describe(error)
    return result


def main(samples: str, in_root: str, out_root: str, results: str) -> None:
    # The runtime logs each failed condition, with the whole object; the results file has them
    logging.disable(logging.CRITICAL)
    with open(samples, encoding='utf-8') as rows, open(results, 'w', encoding='utf-8') as out:
        for line in rows:
            fields = line.rstrip('\n').split('\t') + [''] * 10
            pack, sample = fields[:2]
            result = run_one(pack, sample, Path(in_root), Path(out_root), fields[9])
            out.write(json.dumps(result) + '\n')
            out.flush()


if __name__ == '__main__':
    main(*sys.argv[1:5])
