#!/bin/bash
#
# Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
# SPDX-License-Identifier: Apache-2.0
#

type -P python > /dev/null && PYEXE=python || PYEXE=python3
if ! $PYEXE -c 'import sys; assert sys.version_info >= (3,11)' > /dev/null 2>&1; then
        echo "Found $($PYEXE -V)"
        echo "Expecting at least python 3.11 - exiting!"
        exit 1
fi

export PYTHONDONTWRITEBYTECODE=1

MY_PATH="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd ${MY_PATH} || error
PROJECT_ROOT_PATH="$MY_PATH/../.."
PYTHON_SETUP_PATH="$MY_PATH/../env-setup"

source "$MY_PATH/../ensure_jar_exists.sh" || { echo "Failed to source ensure_jar_exists.sh"; exit 1; }


usage() {
    echo "Usage: $0 [options]"
    echo "Options:"
    echo "  -r, --reuse-env           Reuse the existing virtual environment"
    echo "  -k, --keep-venv           Skip cleanup of the virtual environment"
    echo "  -s, --skip-cdm            Skip CDM fetch and build (use existing wheel)"
    echo "  -v <version>              CDM branch/tag to fetch (default: master)"
    echo "  -h, --help                Show this help"
}

REUSE_ENV=0
SKIP_CDM=0
CLEANUP=1
CDM_VERSION="master"

while [[ $# -gt 0 ]]; do
    case "$1" in
        -r|--reuse-env)
            export REUSE_ENV=1
            CLEANUP=0
            shift
            ;;
        -k|--keep-venv|--no-clean)
            CLEANUP=0
            shift
            ;;
        -s|--skip-cdm)
            SKIP_CDM=1
            shift
            ;;
        -v)
            CDM_VERSION="$2"
            shift 2
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            usage
            exit 1
            ;;
    esac
done

if [[ $SKIP_CDM -eq 0 ]]; then
    echo "***** Fetching and building CDM version: $CDM_VERSION..."
    "$MY_PATH/setup/build_cdm.sh" "$CDM_VERSION" || exit 1
else
    echo "***** Skipping CDM fetch and build (using existing wheel)"
fi

echo "***** setting up common environment"
# source $PYTHON_SETUP_PATH/setup_python_env.sh # Called by setup_cdm_test_env.sh
_SAVED_MY_PATH="$MY_PATH"
source $MY_PATH/setup/setup_cdm_test_env.sh || error
MY_PATH="$_SAVED_MY_PATH"
unset _SAVED_MY_PATH


# run tests — venv is active at this point; use bare 'python' so the venv's
# interpreter is used, not the system python path cached in $PYEXE
echo "***** run tests"
echo $MY_PATH
python -m pip install pytest
python -m pytest -p no:cacheprovider $MY_PATH
TEST_EXIT_CODE=$?
rm -rf .pytest

# Cleanup
deactivate
if [[ $CLEANUP -eq 1 ]]; then
    echo "***** cleaning up environment"
    source "$PYTHON_SETUP_PATH/cleanup_python_env.sh"
else
    echo "***** skipping cleanup (requested)"
fi

exit $TEST_EXIT_CODE