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

echo "***** setting up common environment"
source $PYTHON_SETUP_PATH/setup_python_env.sh
source $MY_PATH/setup_cdm_test_env.sh || error


# run tests
$PYEXE -m pip install pytest
$PYEXE -m pytest -p no:cacheprovider $MY_PATH
rm -rf .pytest