#
# Copyright (c) 2023 CLOUDRISK Limited and FT Advisory LLC
# SPDX-License-Identifier: Apache-2.0
#

"""List every Rune document in CDM's functions/ folder.

    python3 -I make_function_manifest.py <cdm-repo> <git-ref> <extract-root> <samples.tsv> <args.tsv>

Each document is written under <extract-root> and listed in <samples.tsv> in the harness's
format: group, path, 7 empty ingestion columns, then the root type, the file's header and the
file the document comes from.

- An output file, or a file with a root @type, is one document of that type.
- A function input file is a record of the function's arguments. CDM writes it from a Java test
  class, not from a Rune type, and it has no @type. Where a Rune type has the same attributes
  as the function's inputs, the whole file is one document of that type (RECORD_TYPES).
  Otherwise each argument of a Rune type is one document, and a list argument gives one per
  item. Either way, the arguments are checked against the function's inputs and listed in
  <args.tsv>, since the function's cardinalities can differ from the record type's.
- The other files are typed by how CDM's Java tests load them. A list gives one per item.

The header column says whether the file has @model, @type and @version: "complete", "missing",
or "list" for a file that is a JSON list and so can't have one. A document without a root @type
is written with them added, so both languages read it the same way.
"""
import json
import subprocess
import sys
from pathlib import Path

repo, ref, extract_root, samples_path, args_path = sys.argv[1:6]
BASE = 'rosetta-source/src/main/resources/'
FUNCTIONS = BASE + 'functions/'
DESCRIPTORS = ['execution-descriptor', 'sec-lending-execution-descriptor',
               'fpml-processes-execution-descriptor', 'repo-and-bond-execution-descriptor']
HEADER = ('@model', '@type', '@version')

# function -> {argument: (type, cardinality)}, from the .rosetta signatures and, for the
# Java-only RunReturnSettlementWorkflow, its Java input class
SIGNATURES = {
    'Create_BusinessEvent': {'instruction': ('cdm.event.common.Instruction', (1, None)),
                             'intent': ('cdm.event.common.EventIntentEnum', (0, 1)),
                             'eventDate': ('date', (1, 1)),
                             'effectiveDate': ('date', (1, 1))},
    'Create_Workflow': {'steps': ('cdm.event.workflow.WorkflowStep', (1, None))},
    'RunReturnSettlementWorkflow': {'tradeState': ('cdm.event.common.TradeState', (1, 1)),
                                    'returnInstruction': ('cdm.event.common.ReturnInstruction', (1, 1)),
                                    'returnDate': ('date', (1, 1))},
}
NON_RUNE = {'date', 'cdm.event.common.EventIntentEnum'}

# Rune types whose attributes include all of a function's inputs, so that an input file can be
# read whole, with its keys and references in one document. They're chosen for the check; CDM
# doesn't declare them as the files' types.
RECORD_TYPES = {
    'Create_BusinessEvent': 'cdm.event.workflow.EventInstruction',
    'Create_Workflow': 'cdm.event.workflow.Workflow',
}

# Files loaded directly by CDM's Java tests (FunctionInputCreator, SecLendingFunctionInputCreationTest)
OTHER = {
    'repo-and-bond/partial-delivery-delivered-price-quantity.json': 'cdm.observable.asset.PriceQuantity',
    'repo-and-bond/repo-substitution-price-quantity.json': 'cdm.product.template.TradeLot',
    'repo-and-bond/shaping-primitive-instruction-trade-lots.json': 'cdm.product.template.TradeLot',
    'repo-and-bond/shaping-primitive-instruction-shape-identifier.json': 'cdm.base.staticdata.identifier.Identifier',
}
ADJUSTABLE = 'cdm.base.datetime.AdjustableOrRelativeDate'


def git(*a):
    return subprocess.run(['git', '-C', repo, *a], capture_output=True, text=True, check=True).stdout


def load(path):
    return json.loads(git('show', f'{ref}:{path}'))


def header_of(doc) -> str:
    if isinstance(doc, list):
        return 'list'
    return 'complete' if all(k in doc for k in HEADER) else 'missing'


descriptors = [d for name in DESCRIPTORS for d in load(f'{FUNCTIONS}{name}.json')]
function_of = {}
for d in descriptors:
    fn = d['executableFunctionClass'].rsplit('.', 1)[1]
    function_of[d['inputFile']] = fn
    function_of[d['expectedOutputFile']] = fn

rows, arg_rows = [], []
out = Path(extract_root)


def emit(group, rel, doc, rune_type, header, source):
    if isinstance(doc, dict) and '@type' not in doc:
        doc = {'@model': 'cdm', '@type': rune_type, '@version': '0.0.0', **doc}
    target = out / rel
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(doc, indent=2), encoding='utf-8')
    rows.append([group, rel] + [''] * 7 + [rune_type, header, source])


def split(stem, group, value, rune_type, header, source, name=None):
    '''One document per object; a list gives one per item.'''
    if isinstance(value, list):
        for i, item in enumerate(value):
            emit(group, f'{stem}.{name + "-" if name else ""}{i}.json', item, rune_type, header, source)
    elif name:
        emit(group, f'{stem}.{name}.json', value, rune_type, header, source)
    else:
        emit(group, f'{stem}.json', value, rune_type, header, source)


def check_arguments(rel, fn, doc):
    signature = SIGNATURES[fn]
    for name in sorted(doc.keys() - signature.keys() - set(HEADER)):
        arg_rows.append([rel, fn, name, '', 'not an argument of the function'])
    for name, (rune_type, (low, high)) in signature.items():
        value = doc.get(name)
        count = 0 if value is None else len(value) if isinstance(value, list) else 1
        ok = count >= low and (high is None or count <= high)
        arg_rows.append([rel, fn, name, rune_type,
                         f'{count} given, {low}..{high or "*"}: {"ok" if ok else "CARDINALITY"}'])


for path in sorted(git('ls-tree', '-r', '--name-only', ref, FUNCTIONS).split()):
    rel = path.removeprefix(BASE)          # functions/...
    short = path.removeprefix(FUNCTIONS)   # business-event/...
    if not path.endswith('.json') or short.removesuffix('.json') in DESCRIPTORS:
        continue
    doc = load(path)
    header = header_of(doc)
    fn = function_of.get(rel)
    kind = 'output' if path.endswith('-output.json') else 'input' if path.endswith('-input.json') else 'other'
    group = f"{short.split('/')[0]}:{kind}"
    stem = rel.removesuffix('.json')
    if kind == 'input' and fn in SIGNATURES:
        check_arguments(rel, fn, doc)
        if fn in RECORD_TYPES:
            emit(group, rel, doc, RECORD_TYPES[fn], header, rel)
        else:
            for name, (rune_type, _) in SIGNATURES[fn].items():
                if rune_type not in NON_RUNE and doc.get(name) is not None:
                    split(stem, group, doc[name], rune_type, header, rel, name)
    elif isinstance(doc, dict) and '@type' in doc:
        emit(group, rel, doc, doc['@type'], header, rel)
    elif short in OTHER or (isinstance(doc, dict) and 'adjustableDate' in doc):
        split(stem, group, doc, OTHER.get(short, ADJUSTABLE), header, rel)
    else:
        raise SystemExit(f'no type for {rel}')

with open(samples_path, 'w', encoding='utf-8') as f:
    for r in rows:
        print(*r, sep='\t', file=f)
with open(args_path, 'w', encoding='utf-8') as f:
    print('file', 'function', 'argument', 'type', 'check', sep='\t', file=f)
    for r in sorted(arg_rows):
        print(*r, sep='\t', file=f)
sources = {r[11]: r[10] for r in rows}
print(f'{len(rows)} documents from {len(sources)} files, '
      f'{sum(h != "complete" for h in sources.values())} of them without the @model/@type/@version '
      f'header; {len(arg_rows)} argument checks')
