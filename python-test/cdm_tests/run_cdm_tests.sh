#!/bin/bash
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
PYTHON_SETUP_PATH="$MY_PATH/../python_setup"


source "$MY_PATH/../common.sh" || { echo "Failed to source common.sh"; exit 1; }


usage() {
    echo "Usage: $0 [options]"
    echo "Options:"
    echo "  -r, --reuse-env      Reuse the existing virtual environment"
    echo "  -k, --keep-venv      Skip cleanup of the virtual environment"
    echo "  --update-cdm         Fetch latest CDM source and rebuild package"
    echo "  --rebuild-cdm        Rebuild CDM package from local source (skip fetch)"
    echo "  -h, --help           Show this help"
}

REUSE_ENV=0
UPDATE_CDM=0
REBUILD_CDM=0
CLEANUP=1

while [[ $# -gt 0 ]]; do
    case "$1" in
        -r|--reuse-env)
            export REUSE_ENV=1
            CLEANUP=0
            REBUILD_CDM=1
            shift
            ;;
        -k|--keep-venv|--no-clean)
            CLEANUP=0
            shift
            ;;
        --update-cdm)
            UPDATE_CDM=1
            shift
            ;;
        --rebuild-cdm)
            REBUILD_CDM=1
            shift
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

CDM_WHEEL_DIR="$MY_PATH/../../target/python-cdm"
CDM_WHEEL_COUNT=$(find "$CDM_WHEEL_DIR" -name "python_cdm-*-py3-none-any.whl" 2>/dev/null | wc -l)

if [[ $CDM_WHEEL_COUNT -eq 0 && $UPDATE_CDM -eq 0 && $REBUILD_CDM -eq 0 ]]; then
    echo "CDM Python wheel not found in $CDM_WHEEL_DIR. Triggering build (fetch + build)..."
    UPDATE_CDM=1
fi

if [[ $UPDATE_CDM -eq 1 ]]; then
    echo "***** Updating CDM (fetch + build)..."
    "$MY_PATH/cdm_setup/build_cdm.sh" || exit 1
elif [[ $REBUILD_CDM -eq 1 ]]; then
    echo "***** Rebuilding CDM (build only)..."
    "$MY_PATH/cdm_setup/build_cdm.sh" --skip-cdm || exit 1
fi

echo "***** setting up common environment"
# source $PYTHON_SETUP_PATH/setup_python_env.sh # Called by setup_cdm_test_env.sh
source $MY_PATH/setup_cdm_test_env.sh || error


# run tests

# Validating test execution
echo "***** run tests"
$PYEXE -m pip install pytest
$PYEXE -m pytest -p no:cacheprovider $MY_PATH
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