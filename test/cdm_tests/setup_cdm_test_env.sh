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
if ! $PYEXE -c 'import sys; assert sys.version_info >= (3,11)' > /dev/null 2>&1; then
        echo "Found $($PYEXE -V)"
        echo "Expecting at least python 3.11 - exiting!"
        exit 1
fi

MY_PATH="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd ${MY_PATH} || error

echo "***** setting up common environment"
PYTHONSETUPPATH="../python_setup"
source $MY_PATH/$PYTHONSETUPPATH/setup_python_env.sh

echo "***** activating virtual environment"
VENV_NAME=".pyenv"
VENV_PATH="../../$VENV_NAME"
if [ -z "${WINDIR}" ]; then PY_SCRIPTS='bin'; else PY_SCRIPTS='Scripts'; fi
source "$MY_PATH/$PYTHONSETUPPATH/$VENV_PATH/${PY_SCRIPTS}/activate" || error

# install cdm package
PYTHONCDMDIR="../../target/python-cdm"

echo "**** Install CDM package ****"
$PYEXE -m pip install $MY_PATH/$PYTHONCDMDIR/python_cdm-*-py3-none-any.whl