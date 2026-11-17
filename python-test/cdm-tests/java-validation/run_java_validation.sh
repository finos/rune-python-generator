#!/bin/bash
#
# Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
# SPDX-License-Identifier: Apache-2.0
#

# Build CDM Java at a git ref, then read, round-trip and validate every ingestion sample.
#
# Usage (from the project root):
#   python-test/cdm-tests/java-validation/run_java_validation.sh <cdm-repo> <git-ref> <work-dir>
#
# Writes <work-dir>/results/{all-samples,java-summary,java-failures}.tsv
set -euo pipefail
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
if [[ $# -ne 3 ]]; then
  echo "Usage: $0 <cdm-repo> <git-ref> <work-dir>" >&2
  exit 1
fi
CDM_REPO="$1"; REF="$2"; WORK="$3"
B="$WORK/cdm-java"; OUT="$WORK/results"
rm -rf "$B"; mkdir -p "$B" "$OUT" "$WORK/classes"
git -C "$CDM_REPO" archive "$REF" | tar -x -C "$B"
(cd "$B" && mvn -q -B -DskipTests -Dcheckstyle.skip=true -Dmaven.javadoc.skip=true -Dmaven.source.skip=true \
    -pl rosetta-source -am package > "$WORK/cdm-java-build.log" 2>&1)
(cd "$B" && mvn -q -B -o -pl rosetta-source -am dependency:build-classpath \
    -Dmdep.outputFile="$WORK/cp.txt" -Dmdep.includeScope=test > /dev/null)
CP="$B/rosetta-source/target/classes:$(cat "$WORK/cp.txt")"
python3 -I "$HERE/make_sample_list.py" "$CDM_REPO" "$REF" "$OUT/all-samples.tsv"
javac -nowarn -cp "$CP" -d "$WORK/classes" "$HERE/java/"*.java
java -cp "$WORK/classes:$CP" ValidateSamples "$B/rosetta-source/src/main/resources" \
    "$OUT/all-samples.tsv" "$OUT/java-summary.tsv" "$OUT/java-failures.tsv" < /dev/null 2>&1 | grep -v SLF4J || true
wc -l "$OUT"/*.tsv
