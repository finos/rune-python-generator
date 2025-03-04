#!/bin/bash
function error
{
    echo
    echo "***************************************************************************"
    echo "*                                                                         *"
    echo "*                     DEV ENV Initialization FAILED!                      *"
    echo "*                                                                         *"
    echo "***************************************************************************"
    echo
    exit -1
}

export PYTHONDONTWRITEBYTECODE=1

type -P python > /dev/null && PYEXE=python || PYEXE=python3
if ! $PYEXE -c 'import sys; assert sys.version_info >= (3,10)' > /dev/null 2>&1; then
        echo "Found $($PYEXE -V)"
        echo "Expecting at least python 3.10 - exiting!"
        exit 1
fi

MY_PATH="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd ${MY_PATH} || error

echo "***** setting up common environment"
BUILDPATH="../../build"
source $MY_PATH/$BUILDPATH/setup_python_env.sh

echo "***** activating virtual environment"
VENV_NAME=".pyenv"
VENV_PATH=".."
source $MY_PATH/$BUILDPATH/$VENV_PATH/$VENV_NAME/${PY_SCRIPTS}/activate || error

echo "***** Build and Install Helper"
cd $MY_PATH/test_helper
$PYEXE -m pip wheel --no-deps --only-binary :all: . || error
$PYEXE -m pip install test_helper-0.0.0-py3-none-any.whl
rm test_helper-0.0.0-py3-none-any.whl

echo "***** Build and Install Generated Unit Tests"
SERIALIZATIONTESTSDIR="../../target/python/serialization_unit_tests"
cd $MY_PATH/$SERIALIZATIONTESTSDIR
$PYEXE -m pip wheel --no-deps --only-binary :all: . || error
$PYEXE -m pip install python_*-0.0.0-py3-none-any.whl

# run tests
echo "***** run tests"
cd $MY_PATH
$PYEXE -m pytest -p no:cacheprovider . 

echo "***** cleanup"

deactivate
source $MY_PATH/$BUILDPATH/cleanup_python_env.sh