 #!/bin/bash
function error {
    echo
    echo "***************************************************************************"
    echo "*                                                                         *"
    echo "*                     DEV ENV Initialization FAILED!                      *"
    echo "*                                                                         *"
    echo "***************************************************************************"
    echo
    exit 1
}
# Determine the Python executable
if command -v python &>/dev/null; then
    PYEXE=python
elif command -v python3 &>/dev/null; then
    PYEXE=python3
else
    echo "Python is not installed."
    error
fi
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
VENV_PATH="../"
# Determine the scripts directory
if [ -z "${WINDIR}" ]; then
    PY_SCRIPTS='bin'
else
    PY_SCRIPTS='Scripts'
fi
rm -rf "${VENV_PATH}/${VENV_NAME}"
${PYEXE} -m venv --clear "${VENV_PATH}/${VENV_NAME}" || error
source "${VENV_PATH}/${VENV_NAME}/${PY_SCRIPTS}/activate" || error
${PYEXE} -m pip install --upgrade pip || error
${PYEXE} -m pip install -r requirements.txt || error
echo "***** Get and Install Runtime"
RUNTIMEURL="https://api.github.com/repos/finos/rune-python-runtime/releases/latest"
# Fetch the latest release data from the GitHub API
release_data=$(curl -s $RUNTIMEURL)
# Extract the download URL of the first asset
download_url=$(echo "$release_data" | grep '"browser_download_url":' | head -n 1 | sed -E 's/.*"([^"]+)".*/\1/')
# Download the artifact using wget or curl
if command -v wget &>/dev/null; then
    wget "$download_url"
elif command -v curl &>/dev/null; then
    curl -LO "$download_url"
else
    echo "Neither wget nor curl is installed."
    error
fi
${PYEXE} -m pip install rune_runtime*-py3-*.whl --force-reinstall || error
rm rune_runtime*-py3-*.whl
deactivate
 