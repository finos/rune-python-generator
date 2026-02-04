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

# If a virtual environment is active, or if .pyenv/bin is in PATH, scrub it
# This ensures we use a system python to create the new venv
VENV_NAME=".pyenv"
CLEAN_PATH=$(echo "$PATH" | sed -E "s|[^:]*/$VENV_NAME/[^:]*:?||g")

if command -v python3 &>/dev/null; then
  PYEXE=$(PATH="$CLEAN_PATH" command -v python3)
elif command -v python &>/dev/null; then
  PYEXE=$(PATH="$CLEAN_PATH" command -v python)
else
  echo "Python is not installed."
  error
fi

if ! $PYEXE -c 'import sys; assert sys.version_info >= (3,11)' >/dev/null 2>&1; then
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
python -m pip install $MY_PATH/$PYTHONCDMDIR/python_cdm-*-py3-none-any.whl