#
# Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
# SPDX-License-Identifier: Apache-2.0
#

"""List references whose key is not defined anywhere in the same sample.

    python3 -I undefined_keys.py <sample.json> [...]
"""
import json
import sys


def scan(path):
    keys, refs = set(), []

    def walk(x, p):
        if isinstance(x, dict):
            for k, v in x.items():
                if k.startswith("@key") and isinstance(v, str):
                    keys.add(v)
                if k.startswith("@ref") and isinstance(v, str):
                    refs.append((k, v, p))
                walk(v, f"{p}.{k}")
        elif isinstance(x, list):
            for v in x:
                walk(v, p)

    walk(json.load(open(path)), "")
    return refs, [r for r in refs if r[1] not in keys]


for f in sys.argv[1:]:
    refs, undefined = scan(f)
    print(f"{f}: {len(refs)} references, {len(undefined)} undefined")
    for k, v, p in undefined:
        print(f"    {k}={v} at {p}")
