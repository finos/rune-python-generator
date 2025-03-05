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

type -P python > /dev/null && PYEXE=python || PYEXE=python3
if ! $PYEXE -c 'import sys; assert sys.version_info >= (3,11)' > /dev/null 2>&1; then
        echo "Found $($PYEXE -V)"
        echo "Expecting at least python 3.11 - exiting!"
        exit 1
fi

export PYTHONDONTWRITEBYTECODE=1
ENV_BUILD_PATH="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd ${ENV_BUILD_PATH} || error

echo "***** setup virtual environment in [project_root]/.pyenv"
VENV_NAME=".pyenv"
VENV_PATH="../"

type -P python > /dev/null && PY_EXE=python || PY_EXE=python3
if [ -z "${WINDIR}" ]; then
    PY_SCRIPTS='bin'
else
    PY_SCRIPTS='Scripts'
fi

${PY_EXE} -m venv --clear $VENV_PATH/$VENV_NAME || error
. $VENV_PATH/$VENV_NAME/${PY_SCRIPTS}/activate || error
${PY_EXE} -m pip install --upgrade pip || error
${PY_EXE} -m pip install "setuptools>=62.0" || error

echo "***** Install Runtime"
RUNTIMEURL="https://api.github.com/repos/CloudRisk/rune-python-runtime/releases/latest"

# Fetch the latest release data from the GitHub API
release_data=$(curl -s $RUNTIMEURL)

# Extract the download URL of the first asset
download_url=$(echo "$release_data" | grep '"browser_download_url":' | head -n 1 | sed -E 's/.*"([^"]+)".*/\1/')

# Download the artifact using wget or curl
wget "$download_url"

$PYEXE -m pip install rune_runtime*-py3-*.whl --force-reinstall
rm rune_runtime*-py3-*.whl

$PYEXE -m pip install pytest
deactivate