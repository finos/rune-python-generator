#
# Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
# SPDX-License-Identifier: Apache-2.0
#

'''Collects round-trip results and prints a summary grouped by error type.'''
from collections import Counter, defaultdict

import pytest

_ROUNDTRIP_KEY = pytest.StashKey[dict]()


def pytest_configure(config):
    config.stash[_ROUNDTRIP_KEY] = {}


@pytest.fixture
def roundtrip_results(request) -> dict:
    '''Maps each sample to its problems, as (error type, example) pairs: the
    field for a difference, or the sample for a read or write failure. An empty
    list means the sample came back unchanged.'''
    return request.config.stash[_ROUNDTRIP_KEY]


def pytest_terminal_summary(terminalreporter, exitstatus, config):
    '''Prints one row per error type, with the number of samples it affects.'''
    del exitstatus  # unused
    results = config.stash.get(_ROUNDTRIP_KEY, {})
    if not results:
        return

    files = defaultdict(set)
    occurrences = Counter()
    details = defaultdict(Counter)
    for sample, problems in results.items():
        for category, detail in problems:
            files[category].add(sample)
            occurrences[category] += 1
            details[category][detail] += 1

    unchanged = sum(1 for problems in results.values() if not problems)
    write = terminalreporter.write_line
    terminalreporter.write_sep('=', 'FpML 5.10 round-trip summary')
    write(f'Samples: {len(results)}   unchanged: {unchanged}   '
          f'with problems: {len(results) - unchanged}')
    if not files:
        return

    rows = [(category, str(len(files[category])), str(occurrences[category]),
             details[category].most_common(1)[0][0])
            for category in sorted(files, key=lambda c: (-len(files[c]), c))]
    headers = ('Error type', 'Samples', 'Occurrences', 'Most common field or example sample')
    widths = [max(len(row[i]) for row in rows + [headers]) for i in range(3)]
    write('')
    write('  '.join(h.ljust(w) for h, w in zip(headers, widths)) + '  ' + headers[3])
    write('  '.join('-' * w for w in widths) + '  ' + '-' * len(headers[3]))
    for row in rows:
        write('  '.join(cell.ljust(w) for cell, w in zip(row, widths)) + '  ' + row[3])
