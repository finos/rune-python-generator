#
# Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
# SPDX-License-Identifier: Apache-2.0
#

"""Summarize the per-sample results that compare.py wrote, as Markdown tables.

    python3 -I summarize.py <results.csv> <summary.md> [<heading line> ...]

Everything in the summary is counted from the CSV, so it can be checked against it.
Any further arguments are written as lines under the title, for example the versions tested.
"""
import csv
import re
import sys
from collections import Counter

TOP = 10


def table(headers: list[str], rows: list[list]) -> list[str]:
    lines = ['| ' + ' | '.join(headers) + ' |',
             '|' + '|'.join('---' if i == 0 else '---:' for i in range(len(headers))) + '|']
    lines += ['| ' + ' | '.join(str(c) for c in row) + ' |' for row in rows]
    return lines + ['']


def parts(value: str) -> list[str]:
    return [p for p in value.split('; ') if p]


def without_count(part: str) -> tuple[str, int]:
    m = re.match(r'^(.*) \(x(\d+)\)$', part)
    return (m.group(1), int(m.group(2))) if m else (part, 1)


def main(csv_path: str, out_path: str, *heading: str) -> None:
    with open(csv_path, encoding='utf-8') as f:
        rows = list(csv.DictReader(f))
    versions = sorted({r['fpml_version'] for r in rows})
    out = ['# CDM FpML ingestion samples: Python and Java', '']
    out += [f'{line}  ' for line in heading] + [''] if heading else []

    def by_version(test, label):
        return [label] + [sum(test(r) for r in rows if r['fpml_version'] == v) for v in versions] \
            + [sum(test(r) for r in rows)]

    columns = [''] + [f'FpML {v}' for v in versions] + ['All']

    # The files, grouped by what is wrong with them
    def java_total(r):
        return int(r['java_cardinality_failures'] or 0) + int(r['java_condition_failures'] or 0)

    def python_total(r):
        return int(r['python_cardinality_failures'] or 0) + int(r['python_condition_failures'] or 0)

    def python_reads(r):
        return r['python_read'] in ('ok', 'dangling reference')

    def missing_required(r):
        return 'required attribute missing' in parts(r['model_issue_kinds'])

    def fails_conditions(r):
        return int(r['java_condition_failures'] or 0) > 0

    def malformed(r):
        return r['file_class'] == 'malformed'

    file_groups = [(label, [r for r in rows if test(r)]) for label, test in [
        ('Well-formed — conditions pass',
         lambda r: not malformed(r) and not fails_conditions(r)),
        ('Well-formed — conditions fail',
         lambda r: not malformed(r) and fails_conditions(r)),
        ('Malformed, required attributes present — conditions fail',
         lambda r: malformed(r) and not missing_required(r) and fails_conditions(r)),
        ('Malformed, required attributes present — conditions pass',
         lambda r: malformed(r) and not missing_required(r) and not fails_conditions(r)),
        ('Malformed, required attribute missing — conditions fail',
         lambda r: malformed(r) and missing_required(r) and fails_conditions(r)),
        ('Malformed, required attribute missing — conditions pass',
         lambda r: malformed(r) and missing_required(r) and not fails_conditions(r)),
    ]]
    file_groups = [(label, g) for label, g in file_groups if g]

    def mark(group, test, yes='✓', no='✗'):
        hits = sum(bool(test(r)) for r in group)
        return yes if hits == len(group) else no if hits == 0 else f'{hits} of {len(group)}'

    def python_mark(group, test):
        read = [r for r in group if python_reads(r)]
        return mark(read, test) if read else "can't read"

    out += ['## The files', '',
            'Each file is checked against the Rune model without either language\'s runtime '
            '(the model check). It is malformed when its data doesn\'t match the model; an '
            'attribute with optional cardinality needn\'t have a value, so a dangling reference '
            'on one doesn\'t make it malformed. Conditions are evaluated by Java and Python. '
            'CDM\'s ingestion report isn\'t used here.', '',
            '**What each check finds** (✓ means the check finds no problem)', '']
    out += table(['', 'Files', 'Structure: model check', 'Structure: Java',
                  'Conditions: Java', 'Conditions: Python', 'Valid'], [
        [label, len(g),
         mark(g, lambda r: not malformed(r)),
         mark(g, lambda r: int(r['java_cardinality_failures'] or 0) == 0),
         mark(g, lambda r: not fails_conditions(r)),
         python_mark(g, lambda r: int(r['python_condition_failures'] or 0) == 0),
         mark(g, lambda r: r['verdict'] == 'valid')]
        for label, g in file_groups])
    out += ['**Files by FpML version**', '']
    out += table(columns, [by_version(lambda r, g=g: r in g, label) for label, g in file_groups]
                 + [by_version(lambda r: True, '**All**')])
    unconfirmed = Counter(r['verdict'] for r in rows if r['verdict'] not in (
        'valid', 'invalid: confirmed by the model check', 'invalid: conditions confirmed by both'))
    if unconfirmed:
        out += ['Files whose result is not confirmed:', '']
        out += table(['Not confirmed', 'Files'], [[k, v] for k, v in unconfirmed.most_common()])
    out += [f'With a dangling reference on an optional attribute: '
            f'{sum(r["dangling_optional_refs"] not in ("", "0") for r in rows)} files.', '']
    # Reading the files and reporting failures
    readers = [('Ingestion', lambda r: r['cdm_runtime_error'] != 'true'),
               ('Java', lambda r: r['java_read'] == 'ok'),
               ('Python', python_reads)]
    out += ['## Reading the files and reporting failures', '',
            '"Ingestion" is CDM\'s ingestion test, as recorded in the test-pack configs; '
            '"Java" and "Python" are this run.', '', '**Can it read the file?**', '']
    out += table(['', 'Files'] + [name for name, _ in readers],
                 [[label, len(g)] + [mark(g, test) for _, test in readers]
                  for label, g in file_groups if g] +
                 [['**Read**', f'**{len(rows)}**'] +
                  [f'**{sum(bool(test(r)) for r in rows)}**' for _, test in readers]])

    def failures(group, total, reads=lambda r: True):
        read = [r for r in group if reads(r)]
        if not read:
            return "can't read"
        return mark(read, lambda r: total(r) > 0, 'failures', 'no failures')

    out += ['**Does it report validation failures?**', '']
    out += table(['', 'Files', 'Valid', 'Ingestion', 'Java', 'Python'], [
        [label, len(g), mark(g, lambda r: r['verdict'] == 'valid'),
         failures(g, lambda r: int(r['cdm_expected_failures'] or 0)),
         failures(g, java_total), failures(g, python_total, python_reads)]
        for label, g in file_groups if g])

    # CDM's ingestion status of the invalid files
    invalid = [r for r in rows if r['verdict'].startswith('invalid')]
    if invalid:
        groups = [('Malformed', [r for r in invalid if r['file_class'] == 'malformed']),
                  ('Well-formed — conditions fail',
                   [r for r in invalid if r['file_class'] == 'well-formed']),
                  ('All invalid', invalid)]

        def ratio(group):
            values = sorted(int(r['cdm_output_path_count']) / int(r['cdm_input_path_count'])
                            for r in group if r['cdm_input_path_count'] not in ('', '0')
                            and r['cdm_output_path_count'] != '')
            return f'{values[len(values) // 2]:.2f}' if values else '-'

        out += ['## CDM\'s ingestion status of the invalid files', '',
                'From the assertions in each sample\'s test-pack config. The path counts are '
                'rune-common\'s counts of the populated values in the FpML input and in the CDM '
                'output.', '']
        def expected(r):
            return int(r['cdm_expected_failures'] or 0)

        def java_found(r):
            return int(r['java_cardinality_failures'] or 0) + int(r['java_condition_failures'] or 0)

        def median(values):
            values = sorted(values)
            return values[len(values) // 2] if values else '-'

        names = [name for name, _ in groups]
        out += ['**Files**', '']
        out += table(['Files'] + names, [
            ['Invalid'] + [len(g) for _, g in groups],
            ['Ingestion raised a runtime error'] +
            [sum(r['cdm_runtime_error'] == 'true' for r in g) for _, g in groups],
            ['Schema validation failure recorded'] +
            [sum(r['cdm_schema_validation_failure'] == 'true' for r in g) for _, g in groups],
            ['CDM expects no validation failures'] +
            [sum(r['cdm_expected_failures'] == '0' for r in g) for _, g in groups],
            ["Java's failure count equals CDM's expected count"] +
            [sum(r['java_failures_match_cdm_expected'] == 'yes' for r in g) for _, g in groups],
            ["Java's failure count differs from CDM's expected count"] +
            [sum(r['java_failures_match_cdm_expected'] == 'no' for r in g) for _, g in groups],
        ])
        out += ['**Validation failures**', '',
                'Java counts a referenced object\'s failures again at each reference, so these '
                'are counts of reported failures, not of distinct problems.', '']
        out += table(['Validation failures'] + names, [
            ['Expected by CDM, total'] + [sum(expected(r) for r in g) for _, g in groups],
            ['Found by Java, total'] + [sum(java_found(r) for r in g) for _, g in groups],
            ['Expected by CDM but not found by Java (in the files that differ)'] +
            [sum(expected(r) - java_found(r) for r in g
                 if r['java_failures_match_cdm_expected'] == 'no') for _, g in groups],
            ['Expected by CDM, median per file'] +
            [median(expected(r) for r in g) for _, g in groups],
            ['Expected by CDM, most in one file'] +
            [max((expected(r) for r in g), default='-') for _, g in groups],
        ])
        out += ['**Size of the output**', '']
        out += table(['Populated values'] + names,
                     [['Median of CDM output / FpML input'] + [ratio(g) for _, g in groups]])
        differs = [r for r in invalid if r['java_failures_match_cdm_expected'] == 'no']
        if differs:
            out += ['Files where Java, validating the published JSON, finds a different number '
                    'of failures from the number CDM expects:', '']
            out += table(['FpML', 'File', 'Class', 'CDM expects', 'Java finds'], [
                [r['fpml_version'], f"`{r['sample'].rsplit('/', 1)[-1]}`", r['file_class'],
                 r['cdm_expected_failures'],
                 int(r['java_cardinality_failures'] or 0) + int(r['java_condition_failures'] or 0)]
                for r in differs])

    # The expectations
    python_read = [r for r in rows if r['python_read'] in ('ok', 'dangling reference')]

    def expectation(label, applies, met, failed):
        return [label, len(applies), sum(met(r) for r in applies),
                sum(failed(r) for r in applies)]

    validated = [r for r in rows if r['validation_agrees']]
    well_validated = [r for r in validated if r['file_class'] == 'well-formed']
    malformed_validated = [r for r in validated if r['file_class'] == 'malformed']
    out += ['## What was expected', '']
    out += table(['Expectation', 'Files it applies to', 'Met', 'Not met'], [
        expectation('Python reads the file with validation off', rows,
                    lambda r: r['python_read'] == 'ok', lambda r: r['python_read'] != 'ok'),
        expectation('Java reads the file', rows,
                    lambda r: r['java_read'] == 'ok', lambda r: r['java_read'] != 'ok'),
        expectation('Well-formed: Java and Python report the same failures', well_validated,
                    lambda r: r['validation_agrees'] == 'yes',
                    lambda r: r['validation_agrees'] == 'no'),
        expectation('Python writes the file it read', python_read,
                    lambda r: r['python_write'] == 'ok', lambda r: r['python_write'] != 'ok'),
        expectation('Python writes the same data it read', python_read,
                    lambda r: r['python_roundtrip'] == 'same',
                    lambda r: r['python_roundtrip'] != 'same'),
        expectation("Java reads Python's output", python_read,
                    lambda r: r['java_reads_python'] == 'ok',
                    lambda r: r['java_reads_python'] != 'ok'),
        expectation("Java reports the same failures on Python's output as on the input",
                    python_read, lambda r: r['java_reads_python_failures_same'] == 'yes',
                    lambda r: r['java_reads_python_failures_same'] != 'yes'),
    ])
    out += [f'Malformed files validated by both languages: {len(malformed_validated)}; '
            f'the same failures in {sum(r["validation_agrees"] == "yes" for r in malformed_validated)}'
            f', different (noted, not expected to match) in '
            f'{sum(r["validation_agrees"] == "no" for r in malformed_validated)}.', '']

    # Why Python failed to read, and what would fix it
    failed = [r for r in rows if r['python_read'] != 'ok']
    kinds = Counter()
    for r in failed:
        for kind in parts(r['python_read_error_kind']) or ['(none recorded)']:
            kinds[(kind, r['file_class'])] += 1
    if failed:
        out += ['## Why Python failed to read', '',
                'The error Python\'s read stopped on. A file can have more than one problem; '
                'only the first is reported.', '']
        names = sorted({k for k, _ in kinds}, key=lambda k: -sum(v for (n, _), v in kinds.items()
                                                               if n == k))
        out += table(['Error', 'Well-formed files', 'Malformed files', 'All'],
                     [[k, kinds[(k, 'well-formed')], kinds[(k, 'malformed')],
                       kinds[(k, 'well-formed')] + kinds[(k, 'malformed')]] for k in names])
        suggestions = Counter(s for r in rows for s in parts(r['suggested_change']))
        out += ['### Suggested changes', '']
        out += table(['Change', 'Files'], [[k, v] for k, v in suggestions.most_common()])
        fields = Counter(re.sub(r'^[^.]+\.', '', p) for r in failed
                         for p in parts(r['python_read_error_fields']))
        if fields:
            out += ['Most common fields in the read errors (one count per file):', '']
            out += table(['Field', 'Files'], [[f'`{k}`', v] for k, v in fields.most_common(TOP)])

    # The model check
    issue_files, issue_counts = Counter(), Counter()
    for r in rows:
        for kind in parts(r['model_issue_kinds']):
            issue_files[kind] += 1
        for p in parts(r['model_issues_detail']):
            what, times = without_count(p)
            issue_counts[what] += times
    if issue_files:
        out += ['## Why files are malformed', '',
                'A file can have more than one kind of issue.', '']
        out += table(['Issue', 'Files'], [[k, v] for k, v in issue_files.most_common()])
        out += ['Most common issues (by attribute):', '']
        out += table(['Issue', 'Count'], [[k, v] for k, v in issue_counts.most_common(TOP * 2)])

    # Differences
    noted = Counter(n for r in rows for n in parts(r['differences_noted']))
    unmet = Counter(n for r in rows for n in parts(r['expectations_failed']))
    out += ['## Expectations not met, and differences noted', '']
    out += table(['', 'Files'],
                 [[f'Not met: {k}', v] for k, v in unmet.most_common()] +
                 [[f'Noted: {k}', v] for k, v in noted.most_common()])

    differing = Counter()
    for r in validated:
        for column, side in (('validation_java_only', 'Java only'),
                             ('validation_python_only', 'Python only')):
            for p in parts(r[column]):
                differing[(p, side, r['file_class'])] += 1
    if differing:
        out += ['### Validation failures found by only one language', '']
        out += table(['Failure', 'Found by', 'File class', 'Files'],
                     [[k[0], k[1], k[2], v] for k, v in differing.most_common(TOP * 2)])

    round_trip = Counter()
    for r in rows:
        for p in parts(r['python_roundtrip_detail']):
            round_trip[without_count(p)[0]] += 1
    if round_trip:
        out += ['### Python round-trip differences', '']
        out += table(['Difference', 'Files'],
                     [[f'`{k}`', v] for k, v in round_trip.most_common(TOP * 2)])

    conditions = Counter(p for r in rows if r['verdict'] == 'invalid: conditions confirmed by both'
                         for p in parts(r['java_failures_detail']))
    if conditions:
        out += ['## Conditions that fail in well-formed files', '',
                'Reported by both languages.', '']
        out += table(['Condition', 'Files'], [[k, v] for k, v in conditions.most_common(TOP * 2)])

    with open(out_path, 'w', encoding='utf-8') as f:
        f.write('\n'.join(out))


if __name__ == '__main__':
    main(*sys.argv[1:])
