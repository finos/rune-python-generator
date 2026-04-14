#!/bin/bash
#
# Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
# SPDX-License-Identifier: Apache-2.0
#

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
PYTHONSETUPPATH="../env-setup"
source $MY_PATH/$PYTHONSETUPPATH/setup_python_env.sh -r

echo "***** activating virtual environment"
VENV_NAME=".pyenv"
VENV_PATH="../../$VENV_NAME"
if [ -z "${WINDIR}" ]; then PY_SCRIPTS='bin'; else PY_SCRIPTS='Scripts'; fi
source "$MY_PATH/$PYTHONSETUPPATH/$VENV_PATH/${PY_SCRIPTS}/activate" || error


# install cdm package
PYTHONCDMDIR="../../target/python-cdm"

# Construct pip install command
PIP_ARGS=( "$MY_PATH/$PYTHONCDMDIR"/*-*-py3-none-any.whl "--force-reinstall" "--pre" )

if [[ -n "$RUNE_RUNTIME_DIR" && -d "$RUNE_RUNTIME_DIR" ]]; then
    PIP_ARGS+=( "--find-links" "$RUNE_RUNTIME_DIR" )
fi

echo "**** Install CDM package ****"
python -m pip install "${PIP_ARGS[@]}"

# The CDM wheel install (--force-reinstall) may have replaced the local editable
# runtime with a PyPI release. Re-source setup_python_env.sh with REUSE_ENV=1 so
# it reinstalls the runtime without recreating the venv, then re-activate.
_reuse_env_saved="${REUSE_ENV:-}"
export REUSE_ENV=1
source "$MY_PATH/$PYTHONSETUPPATH/setup_python_env.sh"
export REUSE_ENV="$_reuse_env_saved"
source "$MY_PATH/$PYTHONSETUPPATH/$VENV_PATH/${PY_SCRIPTS}/activate" || error