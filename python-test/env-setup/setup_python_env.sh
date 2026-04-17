#!/bin/bash
#
# Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
# SPDX-License-Identifier: Apache-2.0
#


function error {
    echo
    echo "***************************************************************************"
    echo "*                                                                         *"
    echo "* DEV ENV Initialization FAILED!                                          *"
    echo "*                                                                         *"
    echo "***************************************************************************"
    echo
    exit 1
}

# Determine the Python executable
# IMPORTANT: Find a python that is NOT currently inside a virtual environment we might be destroying
if command -v python3 &>/dev/null && ! command -v python3 | grep -q ".pyenv"; then
    PYEXE=$(command -v python3)
elif command -v python &>/dev/null && ! command -v python | grep -q ".pyenv"; then
    PYEXE=$(command -v python)
else
    # Fallback to whatever is available but try to avoid the one in .pyenv if possible
    PYEXE=$(which -a python3 python | grep -v ".pyenv" | head -n 1)
    if [ -z "$PYEXE" ]; then
        PYEXE=python3
        if ! command -v $PYEXE &>/dev/null; then
            PYEXE=python
        fi
    fi
fi

if ! command -v $PYEXE &>/dev/null; then
    echo "Python is not installed."
    error
fi

echo "Using base Python: $PYEXE"

# Check Python version
if ! $PYEXE -c 'import sys; assert sys.version_info >= (3,11)' > /dev/null 2>&1; then
    echo "Found $($PYEXE -V)"
    echo "Expecting at least python 3.11 - exiting!"
    exit 1
fi

export PYTHONDONTWRITEBYTECODE=1
ENV_BUILD_PATH="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd "${ENV_BUILD_PATH}" || error


echo "***** setup virtual environment in [project_root]/.pyenv"
VENV_NAME=".pyenv"
VENV_PATH="../../$VENV_NAME"

# Determine the scripts directory
if [ -z "${WINDIR}" ]; then
    PY_SCRIPTS='bin'
else
    PY_SCRIPTS='Scripts'
fi


if [[ "${REUSE_ENV}" == "1" || "${REUSE_ENV}" == "true" ]]; then
    if [ -d "${VENV_PATH}" ]; then
        echo "Reusing existing virtual environment at ${VENV_PATH}"
        source "${VENV_PATH}/${PY_SCRIPTS}/activate" || error
        PYEXE=python
        # Skip package installation if reusing? Or verify?
        # Assuming reuse means "trust current state", but pip install requirements is usually safe/fast.
        # Let's proceed to pip install content but skip venv creation.
    else
        echo "REUSE_ENV set but ${VENV_PATH} not found. Creating new venv."
        rm -rf "${VENV_PATH}"
        ${PYEXE} -m venv --clear "${VENV_PATH}" || error
        source "${VENV_PATH}/${PY_SCRIPTS}/activate" || error
        PYEXE=python
    fi
else
    echo "Creating fresh virtual environment (removing ${VENV_PATH})"
    rm -rf "${VENV_PATH}"
    ${PYEXE} -m venv --clear "${VENV_PATH}" || error
    source "${VENV_PATH}/${PY_SCRIPTS}/activate" || error
    PYEXE=python
fi

${PYEXE} -m pip install --upgrade pip || error
${PYEXE} -m pip install -r requirements.txt || error

echo "***** Get and Install Runtime"

# Runtime source selection (evaluated in order):
#   1. RUNE_RUNTIME_DIR — editable install from the local sibling checkout (default, if it exists)
#   2. RUNE_RUNTIME_REF — override: install from a specific GitHub branch/tag/commit
#      Example: RUNE_RUNTIME_REF="feature/function_support"
#   3. (default) — no explicit install; rune.runtime is a declared dependency of the generated
#      wheel and will be pulled from PyPI automatically when that wheel is installed.
RUNE_RUNTIME_DIR="../../../../../rune-python-runtime/FINOS/rune-python-runtime"
RUNE_RUNTIME_REF="feature/function_support"

if [ -n "$RUNE_RUNTIME_DIR" ] && [ -d "$RUNE_RUNTIME_DIR" ]; then
    echo "Installing runtime as editable from: $RUNE_RUNTIME_DIR"
    ${PYEXE} -m pip install -e "$RUNE_RUNTIME_DIR" || error
elif [ -n "$RUNE_RUNTIME_REF" ]; then
    echo "Installing runtime from GitHub ref: $RUNE_RUNTIME_REF"
    ${PYEXE} -m pip install \
      "git+https://github.com/finos/rune-python-runtime.git@${RUNE_RUNTIME_REF}" || error
fi

deactivate
