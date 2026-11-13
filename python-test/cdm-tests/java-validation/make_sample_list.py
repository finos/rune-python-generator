#
# Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
# SPDX-License-Identifier: Apache-2.0
#

"""Write one row per ingestion sample: pack, outputPath, modelValidationFailures, runtimeError.

Reads the test-pack configs from a CDM git ref, so the list matches that commit.

    python3 -I make_sample_list.py <cdm-repo> <git-ref> <out.tsv>
"""
import json
import subprocess
import sys

repo, ref, out = sys.argv[1:4]
base = "rosetta-source/src/main/resources/ingest/config/"


def git(*args):
    return subprocess.run(["git", "-C", repo, *args], capture_output=True, text=True, check=True).stdout


with open(out, "w") as o:
    for f in sorted(git("ls-tree", "-r", "--name-only", ref, base).split()):
        if "/test-pack-" not in f:
            continue
        d = json.loads(git("show", f"{ref}:{f}"))
        pack = d["id"].replace("test-pack-translate-fpml-confirmation-to-", "")
        for s in d["samples"]:
            a = s["assertions"]
            print(pack, s["outputPath"], a.get("modelValidationFailures"), a.get("runtimeError"), sep="\t", file=o)
