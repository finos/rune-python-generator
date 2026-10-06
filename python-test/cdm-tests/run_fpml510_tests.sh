#!/bin/bash
#
# Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
# SPDX-License-Identifier: Apache-2.0
#
# Run deserialization tests against all CDM fpml-5-10-products-* samples.
#
# Usage (from project root):
#   python-test/cdm-tests/run_fpml510_tests.sh [options]
#
# Options:
#   -b <branch>             CDM branch/tag to fetch (default: master)
#   -v, --cdm-version <v>   Version string for the Python package (default: 0.0.0 for master)
#   -s, --skip-cdm          Skip CDM fetch and build; use the existing wheel
#   -i, --skip-ingestion    Skip fetching samples and skip test_fpml510_samples.py
#   -r, --reuse-env         Reuse the existing virtual environment
#   -k, --keep-venv         Skip cleanup of the virtual environment after tests
#   --cdm-repo <url>        CDM git repo URL (default: finos/common-domain-model)
#   --fpml-repo <url>       FpML git repo URL (default: rosetta-models/rune-fpml)
#   -h, --help              Show this help

type -P python > /dev/null && PYEXE=python || PYEXE=python3
if ! $PYEXE -c 'import sys; assert sys.version_info >= (3,11)' > /dev/null 2>&1; then
    echo "Found $($PYEXE -V)"
    echo "Expecting at least python 3.11 - exiting!"
    exit 1
fi

export PYTHONDONTWRITEBYTECODE=1

MY_PATH="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
PROJECT_ROOT_PATH="$( cd "$MY_PATH/../.." >/dev/null 2>&1 && pwd )"
PYTHON_SETUP_PATH="$MY_PATH/../env-setup"

source "$MY_PATH/../ensure_jar_exists.sh" || { echo "Failed to source ensure_jar_exists.sh"; exit 1; }

usage() {
    sed -n '/^# Options:/,/^[^#]/{ /^# /{ s/^# //; p } }' "$0"
}

REUSE_ENV=0
SKIP_CDM=0
SKIP_INGESTION=0
CLEANUP=1
CDM_BRANCH="master"
CDM_VERSION=""
CDM_REPO="https://github.com/finos/common-domain-model.git"
FPML_REPO="https://github.com/rosetta-models/rune-fpml.git"

while [[ $# -gt 0 ]]; do
    case "$1" in
        -r|--reuse-env)      export REUSE_ENV=1; CLEANUP=0; shift ;;
        -k|--keep-venv)      CLEANUP=0; shift ;;
        -s|--skip-cdm)       SKIP_CDM=1; shift ;;
        -i|--skip-ingestion) SKIP_INGESTION=1; shift ;;
        -b)                  CDM_BRANCH="$2"; shift 2 ;;
        -v|--cdm-version)    CDM_VERSION="$2"; shift 2 ;;
        --cdm-repo)          CDM_REPO="$2"; shift 2 ;;
        --fpml-repo)         FPML_REPO="$2"; shift 2 ;;
        -h|--help)           usage; exit 0 ;;
        *)                   echo "Unknown option: $1"; usage; exit 1 ;;
    esac
done

# ---------------------------------------------------------------------------
# Step 1: pull CDM, run the generator, build the Python wheel
# ---------------------------------------------------------------------------
if [[ $SKIP_CDM -eq 0 ]]; then
    echo "***** Step 1: building CDM wheel (branch: $CDM_BRANCH)"
    "$MY_PATH/setup/build_cdm.sh" "$CDM_BRANCH" \
        --cdm-repo "$CDM_REPO" \
        --fpml-repo "$FPML_REPO" \
        ${CDM_VERSION:+--cdm-version "$CDM_VERSION"} \
        || exit 1
else
    echo "***** Step 1: skipping CDM build (using existing wheel)"
fi

# ---------------------------------------------------------------------------
# Step 2: set up the Python test environment and install the pre-built wheel
# ---------------------------------------------------------------------------
echo "***** Step 2: setting up Python environment"
_SAVED_MY_PATH="$MY_PATH"
_SAVED_CDM_BRANCH="$CDM_BRANCH"
source "$MY_PATH/setup/setup_cdm_test_env.sh" || exit 1
MY_PATH="$_SAVED_MY_PATH"
CDM_BRANCH="$_SAVED_CDM_BRANCH"
unset _SAVED_MY_PATH _SAVED_CDM_BRANCH

# ---------------------------------------------------------------------------
# Step 3: fetch all fpml-5-10-products-* samples from the CDM repo
# Uses the GitHub Contents API to discover files, then downloads via raw URLs.
# ---------------------------------------------------------------------------
PYTEST_ARGS=(-p no:cacheprovider)
SAMPLES_TMPDIR=""

if [[ $SKIP_INGESTION -eq 0 ]]; then
    CDM_GH_REPO="finos/common-domain-model"
    INGEST_PATH="rosetta-source/src/main/resources/ingest/output/fpml-confirmation-to-trade-state"
    API_ROOT="https://api.github.com/repos/${CDM_GH_REPO}/contents/${INGEST_PATH}"
    RAW_ROOT="https://raw.githubusercontent.com/${CDM_GH_REPO}/${CDM_BRANCH}/${INGEST_PATH}"

    echo "***** Step 3: discovering fpml-5-10-products-* directories (branch: $CDM_BRANCH)"
    _LISTING_TMP=$(mktemp)
    curl -sSL --fail "${API_ROOT}?ref=${CDM_BRANCH}" -o "$_LISTING_TMP" \
        || { echo "ERROR: failed to list CDM ingest output directory"; rm -f "$_LISTING_TMP"; exit 1; }

    DIRS=$(python3 - "$_LISTING_TMP" <<'PYEOF'
import sys, json
with open(sys.argv[1]) as f:
    data = json.load(f)
for e in data:
    if e['type'] == 'dir' and e['name'].startswith('fpml-5-10-products-'):
        print(e['name'])
PYEOF
)
    rm -f "$_LISTING_TMP"

    if [[ -z "$DIRS" ]]; then
        echo "ERROR: no fpml-5-10-products-* directories found in CDM branch $CDM_BRANCH"
        exit 1
    fi

    echo "***** Found directories:"
    echo "$DIRS" | sed 's/^/         /'

    SAMPLES_TMPDIR="$(mktemp -d)"
    FAILED=0
    TOTAL=0

    while IFS= read -r dir; do
        [[ -z "$dir" ]] && continue
        mkdir -p "$SAMPLES_TMPDIR/$dir"

        _DIR_TMP=$(mktemp)
        curl -sSL --fail "${API_ROOT}/${dir}?ref=${CDM_BRANCH}" -o "$_DIR_TMP" \
            || { echo "WARN: failed to list directory $dir — skipping"; rm -f "$_DIR_TMP"; continue; }

        DOWNLOAD_URLS=$(python3 - "$_DIR_TMP" <<'PYEOF'
import sys, json
with open(sys.argv[1]) as f:
    data = json.load(f)
for e in data:
    if e['type'] == 'file' and e['name'].endswith('.json'):
        print(e['name'])
PYEOF
)
        rm -f "$_DIR_TMP"

        COUNT=0
        while IFS= read -r fname; do
            [[ -z "$fname" ]] && continue
            curl -sSL --fail "${RAW_ROOT}/${dir}/${fname}" \
                -o "$SAMPLES_TMPDIR/$dir/$fname" \
                || { echo "WARN: failed to download $dir/$fname"; ((FAILED++)); continue; }
            ((COUNT++))
            ((TOTAL++))
        done <<< "$DOWNLOAD_URLS"

        echo "  $dir: $COUNT files"
    done <<< "$DIRS"

    if [[ $FAILED -gt 0 ]]; then
        echo "ERROR: $FAILED file download(s) failed"
        rm -rf "$SAMPLES_TMPDIR"
        exit 1
    fi

    echo "***** $TOTAL sample files downloaded to $SAMPLES_TMPDIR"
    export CDM_FPML510_SAMPLES_DIR="$SAMPLES_TMPDIR"
else
    echo "***** Step 3: skipping sample fetch (-i)"
    PYTEST_ARGS+=(--ignore="$MY_PATH/test_fpml510_samples.py")
fi

# ---------------------------------------------------------------------------
# Step 4: run tests
# ---------------------------------------------------------------------------
echo "***** Step 4: running fpml-5-10 deserialization tests"
python -m pip install pytest --quiet
python -m pytest "${PYTEST_ARGS[@]}" "$MY_PATH/test_fpml510_samples.py"
TEST_EXIT_CODE=$?
rm -rf .pytest

# ---------------------------------------------------------------------------
# Cleanup
# ---------------------------------------------------------------------------
[[ -n "$SAMPLES_TMPDIR" ]] && rm -rf "$SAMPLES_TMPDIR"
deactivate
if [[ $CLEANUP -eq 1 ]]; then
    echo "***** cleaning up environment"
    source "$PYTHON_SETUP_PATH/cleanup_python_env.sh"
else
    echo "***** skipping cleanup"
fi

exit $TEST_EXIT_CODE
