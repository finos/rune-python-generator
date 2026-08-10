#!/bin/bash
#
# Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
# SPDX-License-Identifier: Apache-2.0
#
# Run the CDM test suite: import smoke test + deserialization ingestion test.
#
# Usage (from project root):
#   python-test/cdm-tests/run_cdm_tests.sh [options]
#
# Options:
#   -b <branch>             CDM branch/tag to fetch (default: master)
#   -v, --cdm-version <v>   Version string for the Python package (default: 0.0.0 for master)
#   -s, --skip-cdm          Skip CDM fetch and build; use the existing wheel
#   -i, --skip-ingestion    Skip fetching the live sample and skip test_deserialize_trade_state.py
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
PROJECT_ROOT_PATH="$MY_PATH/../.."
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
# Uses system Python only — no virtualenv required.
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
# Step 2: set up the Python test environment and install the pre-built wheel.
# Source (not exec) so the activated venv carries through to Step 4.
# setup_cdm_test_env.sh resets MY_PATH to its own directory; save and restore.
# ---------------------------------------------------------------------------
echo "***** Step 2: setting up Python environment"
_SAVED_MY_PATH="$MY_PATH"
source "$MY_PATH/setup/setup_cdm_test_env.sh" || exit 1
MY_PATH="$_SAVED_MY_PATH"
unset _SAVED_MY_PATH

# ---------------------------------------------------------------------------
# Step 3: fetch the ingestion sample from the CDM repo (default)
# ---------------------------------------------------------------------------
PYTEST_ARGS=(-p no:cacheprovider)

if [[ $SKIP_INGESTION -eq 0 ]]; then
    SAMPLE_REPO_PATH="rosetta-source/src/main/resources/ingest/output/fpml-confirmation-to-trade-state/fpml-5-13-products-credit-derivatives/cd-ex01-long-asia-corp-fixreg.json"
    SAMPLE_RAW_URL="https://raw.githubusercontent.com/finos/common-domain-model/$CDM_BRANCH/$SAMPLE_REPO_PATH"
    SAMPLE_DIR="$PROJECT_ROOT_PATH/target/cdm-samples"
    SAMPLE_FILE="$SAMPLE_DIR/cd-ex01-long-asia-corp-fixreg.json"
    mkdir -p "$SAMPLE_DIR"
    echo "***** Step 3: fetching sample from $SAMPLE_RAW_URL"
    curl -sSL --fail "$SAMPLE_RAW_URL" -o "$SAMPLE_FILE" \
        || { echo "ERROR: failed to fetch CDM sample"; exit 1; }
    echo "***** Sample saved ($( wc -c < "$SAMPLE_FILE" | tr -d ' ') bytes)"
    export CDM_SAMPLE_PATH="$SAMPLE_FILE"
else
    echo "***** Step 3: skipping ingestion test (-i)"
    PYTEST_ARGS+=(--ignore="$MY_PATH/test_deserialize_trade_state.py")
fi

# ---------------------------------------------------------------------------
# Step 4: run tests
# ---------------------------------------------------------------------------
echo "***** Step 4: running CDM tests"
python -m pip install pytest --quiet
python -m pytest "${PYTEST_ARGS[@]}" "$MY_PATH"
TEST_EXIT_CODE=$?
rm -rf .pytest

# ---------------------------------------------------------------------------
# Cleanup
# ---------------------------------------------------------------------------
deactivate
if [[ $CLEANUP -eq 1 ]]; then
    echo "***** cleaning up environment"
    source "$PYTHON_SETUP_PATH/cleanup_python_env.sh"
else
    echo "***** skipping cleanup"
fi

exit $TEST_EXIT_CODE
