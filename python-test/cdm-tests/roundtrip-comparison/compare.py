#
# Copyright (c) 2023 CLOUDRISK Limited and FT Advisory LLC
# SPDX-License-Identifier: Apache-2.0
#

"""Classify each sample, compare what Java and Python did with it, and write one CSV row each.

    python3 -I compare.py <rosetta-dir> <samples.tsv> <input-root> <out-dir> <results.csv>

An optional tenth column of <samples.tsv> gives the sample's root type, as a Rune name such as
cdm.event.common.BusinessEvent; without it, the type follows from the test pack. The eleventh
and twelfth, which make_function_manifest.py writes, give whether the file has the
@model/@type/@version header and which file the sample comes from.

<out-dir> holds what the run wrote: java/, python/ and java-reads-python/, each with the
samples written back at their outputPath, and java.jsonl, python.jsonl and
java-reads-python.jsonl with each sample's read, write and validation results.

Each sample is first checked against the Rune model in <rosetta-dir> by model_check.py, which
uses neither language's runtime. A sample is malformed when its data doesn't match the model.
What's expected then depends on the class:
- every sample: Python reads it with validation off;
- a well-formed sample: Java and Python report the same validation failures;
- a malformed sample: a difference in validation is noted, not expected to match;
- whenever Python reads a sample: Python writes the same data back, and Java reads what Python
  wrote and reports the same failures as on the input.
A sample that fails validation is confirmed invalid by the model check, or, for conditions,
by both languages reporting them.

Documents are compared as data, not text:
- the order of an object's fields doesn't matter;
- the order of a list's items does, since Rune lists are ordered;
- numbers compare by value, so 1, 1.0 and "1.0" are the same;
- an absent field, null and [] are the same;
- date-times compare by instant, so Z and +00:00 are the same;
- @version is ignored, since each language writes its own.
"""
import csv
import json
import os
import re
import sys
from collections import Counter
from datetime import datetime
from decimal import Decimal, InvalidOperation
from pathlib import Path

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from model_check import RuneModel, check, short  # noqa: E402  pylint: disable=wrong-import-position

MAX_DETAIL = 5
EMPTY = (None, [], {})

# ---------------------------------------------------------------------------
# Comparing documents
# ---------------------------------------------------------------------------

DATE_TIME = re.compile(r'^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}')
LIST_INDEX = re.compile(r'\[\d+\]')


def load(path: Path):
    document = json.loads(path.read_text(encoding='utf-8'), parse_float=Decimal)
    if isinstance(document, dict):
        document.pop('@version', None)
    return document


def as_number(value):
    if isinstance(value, bool):
        return None
    if isinstance(value, (int, Decimal)):
        return Decimal(value)
    if isinstance(value, str):
        try:
            return Decimal(value)
        except InvalidOperation:
            return None
    return None


def as_instant(value):
    if isinstance(value, str) and DATE_TIME.match(value):
        try:
            return datetime.fromisoformat(value.replace('Z', '+00:00'))
        except ValueError:
            return None
    return None


def same_scalar(a, b) -> bool:
    if a == b and type(a) is type(b):
        return True
    numbers = as_number(a), as_number(b)
    if None not in numbers and (not isinstance(a, str) or not isinstance(b, str)):
        return numbers[0] == numbers[1]
    instants = as_instant(a), as_instant(b)
    if None not in instants:
        try:
            return instants[0] == instants[1]
        except TypeError:  # one with a time zone and one without
            return False
    return False


def differences(expected, actual, path='') -> list[tuple[str, str]]:
    '''(kind, path) for every difference between two documents.'''
    if expected in EMPTY and actual in EMPTY:
        return []
    if isinstance(expected, dict) and isinstance(actual, dict):
        found = []
        for key in sorted(set(expected) | set(actual)):
            child = f'{path}.{key}' if path else key
            mine, theirs = expected.get(key), actual.get(key)
            if theirs in EMPTY and mine not in EMPTY:
                found.append(('missing', child))
            elif mine in EMPTY and theirs not in EMPTY:
                found.append(('extra', child))
            else:
                found.extend(differences(mine, theirs, child))
        return found
    if isinstance(expected, list) and isinstance(actual, list):
        if len(expected) != len(actual):
            return [('list length', path)]
        found = []
        for i, (mine, theirs) in enumerate(zip(expected, actual)):
            found.extend(differences(mine, theirs, f'{path}[{i}]'))
        return found
    if isinstance(expected, (dict, list)) or isinstance(actual, (dict, list)):
        return [('shape', path)]
    return [] if same_scalar(expected, actual) else [('value', path)]


def compare(expected: Path, actual: Path) -> tuple[str, list[tuple[str, str]]]:
    if not expected.exists() or not actual.exists():
        return '', []
    found = differences(load(expected), load(actual))
    return ('same' if not found else 'different'), found


def describe_differences(found: list[tuple[str, str]]) -> str:
    '''The fields that differ, without list indices, most frequent first.'''
    counts = Counter(f"{kind} {LIST_INDEX.sub('', where)}" for kind, where in found)
    return '; '.join(f'{what} (x{n})' if n > 1 else what
                     for what, n in counts.most_common(MAX_DETAIL))


# ---------------------------------------------------------------------------
# Validation results
# ---------------------------------------------------------------------------

def failure_keys(failures: list[dict]) -> Counter:
    return Counter((f['kind'], f['type'], f['rule'], f['path']) for f in failures)


def read_results(path: Path) -> dict:
    if not path.exists():
        return {}
    with path.open(encoding='utf-8') as f:
        return {r['sample']: r for r in map(json.loads, f)}


# ---------------------------------------------------------------------------
# One row per sample
# ---------------------------------------------------------------------------

COLUMNS = [
    # The sample
    'fpml_version', 'pack', 'sample', 'root_type', 'source_file', 'file_header',
    # CDM's ingestion status: the assertions in the sample's test-pack config
    'cdm_input_path', 'cdm_runtime_error', 'cdm_schema_validation_failure',
    'cdm_input_path_count', 'cdm_output_path_count', 'cdm_expected_failures',
    'java_failures_match_cdm_expected',
    # The independent check against the model
    'file_class', 'model_issues', 'model_issue_kinds', 'model_issues_detail',
    'dangling_optional_refs', 'refs', 'verdict',
    # Reading
    'python_read', 'python_read_error_kind', 'python_read_error_fields', 'python_read_error',
    'java_read', 'java_read_error',
    # Validation
    'java_validation', 'java_cardinality_failures', 'java_condition_failures', 'java_failures_detail',
    'python_validation', 'python_cardinality_failures', 'python_condition_failures',
    'python_failures_detail',
    'validation_agrees', 'validation_java_only', 'validation_python_only',
    # The round trip, when Python read the sample
    'python_write', 'python_write_error', 'python_roundtrip', 'python_roundtrip_detail',
    'java_reads_python', 'java_reads_python_error', 'java_reads_python_failures_same',
    'java_reads_python_detail',
    # The outcome
    'expectations_failed', 'differences_noted', 'suggested_change', 'cdm_data_issue',
]

ROOTS = {'workflow-step': 'cdm.event.workflow.WorkflowStep',
         'trade-state': 'cdm.event.common.TradeState'}

SUGGESTIONS = {
    'required field missing':
        'generator: generate required attributes as optional to pydantic and check their '
        'cardinality in validate_model (issue 1)',
    'reference to wrong type':
        'runtime: leave a reference to an object of the wrong type unresolved and report it '
        'in validate_model (issue 2)',
    'dangling reference':
        'runtime: read a dangling reference unresolved and write it back unchanged (issue 3)',
    'duplicate key':
        'runtime: read a document with a duplicate key and report it in validate_model (issue 4)',
}

CARDINALITY_ATTRIBUTE = re.compile(r"'(\w+)'")


def failure_names(result: dict, language: str) -> Counter | None:
    '''A language's validation failures, named the same way in both: cardinality failures by
    type and attribute, condition failures by type and condition.'''
    if 'failures' not in result:
        return None
    names = Counter()
    for f in result['failures']:
        if language == 'java' and f['kind'] == 'CARDINALITY':
            m = CARDINALITY_ATTRIBUTE.search(f['reason'])
            names[f"cardinality {m.group(1) if m else f['reason']}"] += 1
        elif language == 'python' and f['kind'] == 'ATTRIBUTE':
            attribute = [p for p in f['path'].split('.') if not p.isdigit()]
            name = attribute[-1] if attribute else f['path']
            if f['rule'] in ('missing', 'too_short', 'too_long'):
                names[f'cardinality {name}'] += 1
            else:
                names[f"attribute {name} {f['rule']}"] += 1
        elif language == 'java' and f['kind'] == 'DATA_RULE':
            names[f"condition {f['rule']}"] += 1
        elif language == 'python' and f['kind'] == 'CONDITION':
            names[f"condition {short(f['type'])}{f['rule']}"] += 1
        else:
            names[f"{f['kind'].lower()} {f['rule']}"] += 1
    return names


def kinds_of(names: Counter | None, kind: str) -> int | str:
    return '' if names is None else sum(n for k, n in names.items() if k.startswith(kind))


def listed(names) -> str:
    return '; '.join(sorted(names)[:MAX_DETAIL * 4])


def describe_issues(issues: list[dict]) -> str:
    counts = Counter(f"{i['kind']}: {i['detail'].split(': ')[0]}" for i in issues)
    return '; '.join(f'{what} (x{n})' if n > 1 else what
                     for what, n in counts.most_common(MAX_DETAIL * 2))


def build_row(pack, sample, status, inputs, out, results, model) -> dict:
    version = re.search(r'fpml-(\d+-\d+)', pack)
    row = {'fpml_version': version.group(1).replace('-', '.') if version else '',
           'pack': pack, 'sample': sample, **status}
    source = inputs / sample
    root = status['root_type'] or next(fqn for prefix, fqn in ROOTS.items()
                                       if pack.startswith(prefix))

    # The independent check
    issues, refs = check(load(source), root, model) if source.exists() else ([], 0)
    malformed = [i for i in issues if i['malformed']]
    row.update({
        'file_class': 'malformed' if malformed else 'well-formed',
        'model_issues': len(malformed),
        'model_issue_kinds': '; '.join(sorted({i['kind'] for i in malformed})),
        'model_issues_detail': describe_issues(malformed),
        'dangling_optional_refs': sum(not i['malformed'] and i['kind'].startswith('dangling')
                                      for i in issues),
        'refs': refs,
    })

    # Reading
    java = results['java'].get(sample, {})
    python = results['python'].get(sample, {})
    row['python_read'] = python.get('read', 'not run')
    row['python_read_error_kind'] = python.get('read_error_kind', '')
    row['python_read_error_fields'] = '; '.join(python.get('read_error_paths', [])[:MAX_DETAIL])
    row['python_read_error'] = python.get('read_error', '')
    row['java_read'] = java.get('read', 'not run')
    row['java_read_error'] = java.get('read_error', '')

    # Validation
    java_names = failure_names(java, 'java')
    python_names = failure_names(python, 'python')
    row['java_validation'] = java.get('validate', '')
    row['java_cardinality_failures'] = kinds_of(java_names, 'cardinality')
    row['java_condition_failures'] = kinds_of(java_names, 'condition')
    row['java_failures_detail'] = listed(java_names or ())
    if 'failures' in java and row['cdm_expected_failures'] != '':
        row['java_failures_match_cdm_expected'] = \
            'yes' if len(java['failures']) == int(row['cdm_expected_failures']) else 'no'
    row['python_validation'] = python.get('validate', '')
    row['python_cardinality_failures'] = kinds_of(python_names, 'cardinality')
    row['python_condition_failures'] = kinds_of(python_names, 'condition')
    row['python_failures_detail'] = listed(python_names or ())
    if java_names is not None and python_names is not None:
        # A referenced object is checked once in Python and at each reference in Java, so the
        # failures are compared as sets
        java_set, python_set = set(java_names), set(python_names)
        row['validation_agrees'] = 'yes' if java_set == python_set else 'no'
        row['validation_java_only'] = listed(java_set - python_set)
        row['validation_python_only'] = listed(python_set - java_set)

    # The verdict
    java_fails = bool(java_names)
    python_fails = bool(python_names)
    if malformed:
        verdict = 'invalid: confirmed by the model check'
    elif java_names is None and python_names is None:
        verdict = 'not validated'
    elif python_names is None:
        verdict = ('invalid by Java: not confirmed, Python did not validate' if java_fails
                   else 'valid by Java: Python did not validate')
    elif java_names is None:
        verdict = ('invalid by Python: not confirmed, Java did not validate' if python_fails
                   else 'valid by Python: Java did not validate')
    elif not java_fails and not python_fails:
        verdict = 'valid'
    elif row['validation_agrees'] == 'yes':
        verdict = 'invalid: conditions confirmed by both'
    else:
        verdict = 'not confirmed: Java and Python disagree'
    row['verdict'] = verdict

    # The round trip
    if python.get('read') in ('ok', 'dangling reference'):
        row['python_write'] = python.get('write', '')
        row['python_write_error'] = python.get('write_error', '')
        if python.get('write') == 'ok':
            status, found = compare(source, out / 'python' / sample)
            row['python_roundtrip'] = status
            row['python_roundtrip_detail'] = describe_differences(found)
        java_python = results['java-reads-python'].get(sample, {})
        if java_python and java_python.get('read') != 'no input':
            row['java_reads_python'] = java_python.get('read', '')
            row['java_reads_python_error'] = java_python.get('read_error', '')
            again = failure_names(java_python, 'java')
            if again is not None and java_names is not None:
                same = failure_keys(java_python['failures']) == failure_keys(java['failures'])
                row['java_reads_python_failures_same'] = 'yes' if same else 'no'
                if not same:
                    row['java_reads_python_detail'] = '; '.join(
                        [f'only reading Python: {n}' for n in sorted(set(again) - set(java_names))] +
                        [f'only reading the input: {n}'
                         for n in sorted(set(java_names) - set(again))])[:400]

    # What was expected, and what was only noted
    failed, noted = [], []
    if row['python_read'] != 'ok':
        failed.append('Python read with validation off')
    if row['java_read'] != 'ok':
        noted.append('Java read failed')
    if row.get('validation_agrees') == 'no':
        (noted if malformed else failed).append('Java and Python validation differ')
    if row.get('python_write') == 'error':
        failed.append('Python write')
    if row.get('python_roundtrip') == 'different':
        (noted if malformed else failed).append('Python round trip differs')
    if row.get('java_reads_python') == 'error':
        failed.append("Java reading Python's output")
    if row.get('java_reads_python_failures_same') == 'no':
        (noted if malformed else failed).append("Java's failures on Python's output differ")
    row['expectations_failed'] = '; '.join(failed)
    row['differences_noted'] = '; '.join(noted)

    suggestions = []
    if row['python_read'] != 'ok':
        kinds = row['python_read_error_kind'].split('; ')
        suggestions += [SUGGESTIONS.get(k, f'investigate: {k}') for k in kinds if k]
    if 'Unbound reference' in row.get('python_write_error', ''):
        suggestions.append(SUGGESTIONS['dangling reference'])
    row['suggested_change'] = '; '.join(dict.fromkeys(suggestions))
    row['cdm_data_issue'] = row['model_issue_kinds']
    return row


def ingestion_status(fields: list[str]) -> dict:
    '''CDM's assertions for a sample, from the columns make_sample_list.py writes.'''
    def value(i):
        return '' if fields[i] in ('None', '') else fields[i].lower() \
            if fields[i] in ('True', 'False') else fields[i]
    return {'cdm_expected_failures': value(2), 'cdm_runtime_error': value(3),
            'cdm_input_path': value(5), 'cdm_input_path_count': value(6),
            'cdm_output_path_count': value(7), 'cdm_schema_validation_failure': value(8),
            'root_type': fields[9], 'file_header': fields[10], 'source_file': fields[11]}


def main(rosetta_dir, samples, input_root, out_dir, csv_path):
    model = RuneModel(rosetta_dir)
    out = Path(out_dir)
    results = {name: read_results(out / f'{name}.jsonl')
               for name in ('java', 'python', 'java-reads-python')}
    with open(samples, encoding='utf-8') as rows, \
            open(csv_path, 'w', newline='', encoding='utf-8') as f:
        writer = csv.DictWriter(f, fieldnames=COLUMNS)
        writer.writeheader()
        for line in rows:
            fields = line.rstrip('\n').split('\t') + [''] * 12
            pack, sample = fields[:2]
            writer.writerow(build_row(pack, sample, ingestion_status(fields),
                                      Path(input_root), out, results, model))


if __name__ == '__main__':
    main(*sys.argv[1:6])
