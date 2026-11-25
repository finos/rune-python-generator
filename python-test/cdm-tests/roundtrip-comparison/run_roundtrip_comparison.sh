#!/bin/bash
#
# Copyright (c) 2023 CLOUDRISK Limited and FT Advisory LLC
# SPDX-License-Identifier: Apache-2.0
#

# Build CDM Java and CDM Python from the same CDM commit, round-trip and validate CDM's FpML
# ingestion samples, or the JSON files in CDM's functions/ folder, in both, have Java read what
# Python wrote, and compare the results.
#
# Usage (from the project root):
#   python-test/cdm-tests/roundtrip-comparison/run_roundtrip_comparison.sh \
#       <cdm-repo> <git-ref> <work-dir> [<pack-pattern>]
#
#   <cdm-repo>      a local clone of finos/common-domain-model, with <git-ref> fetched
#   <git-ref>       the CDM commit, branch or tag, for example upstream/master
#   <work-dir>      a directory outside this repository; it is emptied first
#   <pack-pattern>  a regular expression for the test packs to run (default: all of them);
#                   incomplete and invalid packs are always left out. The word "functions"
#                   runs the JSON files in rosetta-source/src/main/resources/functions instead
#
# CDM Java is built and run from its packaged jar. The Python is generated with the generator
# built from this checkout, packaged as a wheel, and installed with the rune.runtime release
# that the generated pyproject.toml requires.
#
# Writes <work-dir>/results/results.csv (one row per sample) and summary.md (from the CSV). With
# "functions", it also writes arguments.tsv, the function arguments that aren't Rune types,
# checked against the function's inputs.
set -euo pipefail
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$HERE/../../.." && pwd)"
if [[ $# -lt 3 || $# -gt 4 ]]; then
    sed -n '/^# Usage/,/^# Writes/p' "$0" | sed 's/^# \{0,1\}//' >&2
    exit 1
fi
CDM_REPO="$(cd "$1" && pwd)"; REF="$2"; WORK="$3"; PACKS="${4:-.}"
case "$(cd "$(dirname "$WORK")" && pwd)/" in
    "$ROOT"/*) echo "<work-dir> must be outside this repository" >&2; exit 1 ;;
esac

COMMIT="$(git -C "$CDM_REPO" rev-parse --verify "$REF^{commit}")"
rm -rf "$WORK"; mkdir -p "$WORK"; WORK="$(cd "$WORK" && pwd)"
CDM="$WORK/cdm"; OUT="$WORK/out"; RESULTS="$WORK/results"
mkdir -p "$CDM" "$OUT" "$RESULTS" "$WORK/classes" "$WORK/rosetta/common-domain-model" \
    "$WORK/rosetta/rune-fpml"
echo "***** CDM $COMMIT"
git -C "$CDM_REPO" archive "$COMMIT" | tar -x -C "$CDM"

echo "***** building CDM Java (log: $WORK/cdm-java-build.log)"
(cd "$CDM" && mvn -q -B -DskipTests -Dcheckstyle.skip=true -Dmaven.javadoc.skip=true \
    -Dmaven.source.skip=true -pl rosetta-source -am package > "$WORK/cdm-java-build.log" 2>&1)
(cd "$CDM" && mvn -q -B -o -pl rosetta-source -am dependency:build-classpath \
    -Dmdep.outputFile="$WORK/cp.txt" -Dmdep.includeScope=test > /dev/null)
CDM_JAR="$(ls "$CDM"/rosetta-source/target/*.jar | grep -Ev -- '-(sources|javadoc|tests)\.jar$' | head -1)"
[[ -n "$CDM_JAR" ]] || { echo "CDM's jar wasn't built" >&2; exit 1; }
echo "      $(basename "$CDM_JAR")"
CP="$CDM_JAR:$(cat "$WORK/cp.txt")"

# The Rune sources Python is generated from: CDM's, and the FpML model version CDM Java uses
cp -r "$CDM/rosetta-source/src/main/rosetta/"* "$WORK/rosetta/common-domain-model/"
FPML_JAR="$(tr ':' '\n' < "$WORK/cp.txt" | grep '/rune-fpml/rosetta-source/' | head -1)"
[[ -n "$FPML_JAR" ]] || { echo "rune-fpml not found on CDM's classpath" >&2; exit 1; }
(cd "$WORK" && unzip -q -o "$FPML_JAR" 'fpml/rosetta/*.rosetta' -d fpml-jar)
cp "$WORK/fpml-jar/fpml/rosetta/"*.rosetta "$WORK/rosetta/rune-fpml/"

echo "***** building the generator (log: $WORK/generator-build.log)"
(cd "$ROOT" && mvn -q -B -DskipTests package > "$WORK/generator-build.log" 2>&1)
GENERATOR_JAR="$ROOT/target/python-0.0.0.main-SNAPSHOT.jar"
echo "***** generating CDM Python (log: $WORK/python-generation.log)"
java -cp "$GENERATOR_JAR" com.regnosys.rosetta.generator.python.PythonCodeGeneratorCLI \
    -s "$WORK/rosetta" -t "$WORK/python-cdm" -p finos-cdm -v 0.0.0 -x finos \
    > "$WORK/python-generation.log" 2>&1
grep -A3 "Skipped .* model" "$WORK/python-generation.log" | grep -v "^--$" || true

echo "***** installing CDM Python"
# A Python without any active virtual environment, so the new one is clean
PYEXE="$(env -u VIRTUAL_ENV PATH="$(echo "$PATH" | sed -E 's|[^:]*/\.?[a-z]*pyenv/bin:?||g')" \
    bash -c 'command -v python3')"
"$PYEXE" -m venv "$WORK/venv"
"$WORK/venv/bin/python" -m pip -q install --upgrade pip
(cd "$WORK/python-cdm" && "$WORK/venv/bin/python" -m pip -q wheel --no-deps . -w "$WORK/wheel")
"$WORK/venv/bin/python" -m pip -q install "$WORK"/wheel/*.whl
RUNTIME="$("$WORK/venv/bin/python" -m pip show rune.runtime | sed -n 's/^Version: //p')"

if [[ "$PACKS" == functions ]]; then
    echo "***** listing the documents in CDM's functions/ folder"
    INPUT="$WORK/function-documents"
    python3 -I "$HERE/make_function_manifest.py" "$CDM_REPO" "$COMMIT" "$INPUT" \
        "$WORK/samples.tsv" "$RESULTS/arguments.tsv"
    SCOPE="Documents: the JSON files in CDM's functions/ folder"
    SCOPE_NOTE="Function arguments outside their cardinality: $(grep -c 'CARDINALITY$' \
        "$RESULTS/arguments.tsv" || true) (see arguments.tsv)"
else
    echo "***** listing the samples (packs matching $PACKS, without incomplete or invalid packs)"
    python3 -I "$HERE/../java-validation/make_sample_list.py" "$CDM_REPO" "$COMMIT" "$WORK/all-samples.tsv"
    awk -F'\t' -v p="$PACKS" '$1 ~ p && $1 !~ /incomplete|invalid/' "$WORK/all-samples.tsv" \
        > "$WORK/samples.tsv"
    echo "      $(wc -l < "$WORK/samples.tsv" | tr -d ' ') samples"

    INPUT="$CDM/rosetta-source/src/main/resources"
    SCOPE="Packs: matching \`$PACKS\`, without incomplete or invalid packs"
    SCOPE_NOTE=""
fi
javac -nowarn -cp "$CP" -d "$WORK/classes" "$HERE/java/RoundTrip.java"
echo "***** Java: read, write, validate"
java -cp "$WORK/classes:$CP" RoundTrip "$WORK/samples.tsv" "$INPUT" "$OUT/java" \
    "$OUT/java.jsonl" < /dev/null 2>&1 | grep -v SLF4J || true
echo "***** Python: read, write, validate"
(cd "$WORK" && "$WORK/venv/bin/python" -I "$HERE/python_roundtrip.py" "$WORK/samples.tsv" \
    "$INPUT" "$OUT/python" "$OUT/python.jsonl" > "$WORK/python-run.log" 2>&1)
echo "***** Java: read, write, validate what Python wrote"
java -cp "$WORK/classes:$CP" RoundTrip "$WORK/samples.tsv" "$OUT/python" \
    "$OUT/java-reads-python" "$OUT/java-reads-python.jsonl" < /dev/null 2>&1 | grep -v SLF4J || true

echo "***** comparing"
RUNE_COMMON="$(tr ':' '\n' < "$WORK/cp.txt" | sed -n 's|.*/rune-common/rune-common/\([^/]*\)/.*|\1|p' | head -1)"
FPML_VERSION="$(basename "$(dirname "$FPML_JAR")")"
python3 -I "$HERE/compare.py" "$WORK/rosetta" "$WORK/samples.tsv" "$INPUT" "$OUT" \
    "$RESULTS/results.csv"
python3 -I "$HERE/summarize.py" "$RESULTS/results.csv" "$RESULTS/summary.md" \
    "$SCOPE" ${SCOPE_NOTE:+"$SCOPE_NOTE"} \
    "CDM: $COMMIT ($REF)" \
    "rune-fpml: $FPML_VERSION; rune-common (Java): $RUNE_COMMON" \
    "CDM Java: $(basename "$CDM_JAR")" \
    "rune-python-generator: $(git -C "$ROOT" branch --show-current) at $(git -C "$ROOT" rev-parse --short HEAD)$(git -C "$ROOT" diff --quiet HEAD -- src || echo ' with local changes')" \
    "rune.runtime: $RUNTIME"
echo "***** results in $RESULTS"
