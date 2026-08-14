#!/bin/bash
#
# Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
# SPDX-License-Identifier: Apache-2.0
#
# Profile CDM import memory usage.
#
# Creates (or reuses) a lightweight venv with the pre-built CDM wheel and
# psutil, then runs profile_import.py.
#
# Usage (from project root):
#   python-test/cdm-tests/run_memory_profile.sh [options]
#
# Options:
#   -r, --reuse-env    Reuse existing venv (skip install)
#   --top N            Show top N allocating entries (passed to profile_import.py)
#   -h, --help         Show this help

set -euo pipefail

MY_PATH="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$( cd "$MY_PATH/../.." && pwd )"
VENV_DIR="$MY_PATH/.profile-pyenv"
WHEEL_DIR="$PROJECT_ROOT/target/python-cdm"
PROFILE_SCRIPT="$MY_PATH/profile_import.py"

REUSE_ENV=0
TOP_ARG=""

while [[ $# -gt 0 ]]; do
    case "$1" in
        -r|--reuse-env) REUSE_ENV=1; shift ;;
        --top) TOP_ARG="--top $2"; shift 2 ;;
        -h|--help)
            sed -n '/^# Options:/,/^[^#]/{/^# /{ s/^# //; p }}' "$0"
            exit 0
            ;;
        *) echo "Unknown option: $1"; exit 1 ;;
    esac
done

# ── locate wheel ────────────────────────────────────────────────────────────
CDM_WHEEL=$(ls "$WHEEL_DIR"/*.whl 2>/dev/null | head -1)
if [[ -z "$CDM_WHEEL" ]]; then
    echo "ERROR: no CDM wheel found in $WHEEL_DIR"
    echo "Run the CDM build first:"
    echo "  python-test/cdm-tests/setup/build_cdm.sh ..."
    exit 1
fi
echo "Using wheel: $CDM_WHEEL"

# ── create or reuse venv ─────────────────────────────────────────────────────
if [[ $REUSE_ENV -eq 0 || ! -d "$VENV_DIR" ]]; then
    echo "Creating venv at $VENV_DIR …"
    python3 -m venv "$VENV_DIR"
    source "$VENV_DIR/bin/activate"
    pip install --quiet --upgrade pip
    pip install --quiet "$CDM_WHEEL"
    pip install --quiet psutil
else
    echo "Reusing existing venv at $VENV_DIR"
    source "$VENV_DIR/bin/activate"
fi

# ── run profiler ─────────────────────────────────────────────────────────────
echo ""
python "$PROFILE_SCRIPT" $TOP_ARG

deactivate
