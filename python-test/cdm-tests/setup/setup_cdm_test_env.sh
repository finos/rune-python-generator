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

# Parse arguments (only meaningful when run directly, not sourced)
KEEP_ENV=0
FORCE_REBUILD=0
CDM_VERSION=""
CDM_BRANCH=""
CDM_REPO=""
FPML_REPO=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    -k|--keep-env)
      KEEP_ENV=1
      shift
      ;;
    -f|--force-rebuild)
      FORCE_REBUILD=1
      shift
      ;;
    -v|--cdm-version)
      CDM_VERSION="$2"
      shift 2
      ;;
    -b|--branch)
      CDM_BRANCH="$2"
      shift 2
      ;;
    --cdm-repo)
      CDM_REPO="$2"
      shift 2
      ;;
    --fpml-repo)
      FPML_REPO="$2"
      shift 2
      ;;
    *)
      shift
      ;;
  esac
done

echo "***** setting up common environment"
PYTHONSETUPPATH="../../env-setup"
source $MY_PATH/$PYTHONSETUPPATH/setup_python_env.sh

echo "***** activating virtual environment"
VENV_NAME=".pyenv"
VENV_PATH="../../$VENV_NAME"
if [ -z "${WINDIR}" ]; then PY_SCRIPTS='bin'; else PY_SCRIPTS='Scripts'; fi
source "$MY_PATH/$PYTHONSETUPPATH/$VENV_PATH/${PY_SCRIPTS}/activate" || error


# install cdm package
PYTHONCDMDIR="$MY_PATH/../../../target/python-cdm"

# Build the CDM wheel if absent or if a forced rebuild was requested
CDM_WHL=$(ls "$PYTHONCDMDIR"/*-*-py3-none-any.whl 2>/dev/null | head -1)
if [ -z "$CDM_WHL" ] || [ "$FORCE_REBUILD" -eq 1 ]; then
    [ -z "$CDM_WHL" ] \
        && echo "***** No CDM wheel found in $PYTHONCDMDIR — building CDM..." \
        || echo "***** Force-rebuild requested — rebuilding CDM..."
    BUILD_ARGS=()
    [ -n "$CDM_BRANCH" ]  && BUILD_ARGS+=("$CDM_BRANCH")
    [ -n "$CDM_VERSION" ] && BUILD_ARGS+=("-v" "$CDM_VERSION")
    [ -n "$CDM_REPO" ]    && BUILD_ARGS+=("--cdm-repo" "$CDM_REPO")
    [ -n "$FPML_REPO" ]   && BUILD_ARGS+=("--fpml-repo" "$FPML_REPO")
    "$MY_PATH/build_cdm.sh" "${BUILD_ARGS[@]}" || error
fi

# Install the CDM wheel with --no-deps so pip does not resolve rune-runtime as a
# transitive dependency and overwrite the editable local install set up above.
# All CDM dependencies (pydantic, dateutil, etc.) are already covered by the
# rune-runtime install performed by setup_python_env.sh.
echo "**** Install CDM package ****"
python -m pip install --no-deps --force-reinstall --pre \
    "$PYTHONCDMDIR"/*-*-py3-none-any.whl

# When run directly (not sourced), clean up unless -k was given
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    if [[ $KEEP_ENV -eq 1 ]]; then
        echo "***** -k specified: virtual environment kept at $MY_PATH/$PYTHONSETUPPATH/$VENV_PATH"
    else
        echo "***** cleaning up virtual environment"
        deactivate
        source "$MY_PATH/$PYTHONSETUPPATH/cleanup_python_env.sh"
    fi
fi