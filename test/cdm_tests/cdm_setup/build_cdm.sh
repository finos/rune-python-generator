#!/bin/bash
#
# utility script - builds CDM using PythonFilesGeneratorTest::generateCDMPythonFromRosetta
# to use:
# 1. remove disabled test by commenting @Disabled
# 2. specify which version of CDM to pull in get_cdm.sh
# 3. run this script
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
PROJECT_ROOT_PATH="$MY_PATH/../../.."
CDM_SOURCE_PATH="$MY_PATH/../rosetta"
PYTHON_TARGET_PATH=$PROJECT_ROOT_PATH/target/python-cdm
PYTHON_SETUP_PATH="$MY_PATH/../../python_setup"
JAR_PATH="$PROJECT_ROOT_PATH/target/python-0.0.0.main-SNAPSHOT.jar"
cd ${MY_PATH} || error


source "$MY_PATH/../../common.sh" || { echo "Failed to source common.sh"; exit 1; }

# Parse command-line arguments for --skip-cdm
SKIP_CDM=0
for arg in "$@"; do
  if [[ "$arg" == "--skip-cdm" ]]; then
    SKIP_CDM=1
  fi
done

if [[ $SKIP_CDM -eq 0 ]]; then
    source $MY_PATH/get_cdm.sh
else
    echo "Skipping get_cdm.sh as requested."
fi

ensure_jar_exists "$PROJECT_ROOT_PATH" "$JAR_PATH"

echo "***** build CDM"
java -cp "$JAR_PATH" com.regnosys.rosetta.generator.python.PythonCodeGeneratorCLI -s $CDM_SOURCE_PATH -t $PYTHON_TARGET_PATH
JAVA_EXIT_CODE=$?
if [[ $JAVA_EXIT_CODE -eq 1 ]]; then
    echo "Java program returned exit code 1. Stopping script."
    exit 1
fi

echo "***** setting up common environment"
source $PYTHON_SETUP_PATH/setup_python_env.sh

echo "***** activating virtual environment"
VENV_NAME=".pyenv"
if [ -z "${WINDIR}" ]; then PY_SCRIPTS='bin'; else PY_SCRIPTS='Scripts'; fi
source "$PROJECT_ROOT_PATH/$VENV_NAME/${PY_SCRIPTS}/activate" || error

echo "***** build CDM Python package"
cd $PYTHON_TARGET_PATH
rm python_cdm-*.*.*-py3-none-any.whl
python -m pip wheel --no-deps --only-binary :all: . || processError

echo "***** cleanup"

deactivate
if [[ "${REUSE_ENV}" != "1" && "${REUSE_ENV}" != "true" ]]; then
    source $PYTHON_SETUP_PATH/cleanup_python_env.sh
else
    echo "Skipping cleanup (REUSE_ENV set)"
fi

echo ""
echo ""
echo "***************************************************************************"
echo "*                                                                         *"
echo "*                                 SUCCESS!!!                              *"
echo "*                                                                         *"
echo "*     Finished installing dependencies and building the cdm package!      *"
echo "*                                                                         *"
echo "*                      package placed in target/python-cdm                *"
echo "*                                                                         *"
echo "***************************************************************************"
echo ""