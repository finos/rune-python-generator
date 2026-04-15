#!/bin/bash
#
# Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
# SPDX-License-Identifier: Apache-2.0
#

#
# utility script - builds CDM using PythonFilesGeneratorTest::generateCDMPythonFromRosetta
# to use:
# 1. remove disabled test by commenting @Disabled
# 2. specify which version of CDM to pull (optional, default to master)
# 3. run this script: ./build_cdm.sh master
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
PYTHON_SETUP_PATH="$MY_PATH/../../env-setup"
JAR_PATH="$PROJECT_ROOT_PATH/target/python-0.0.0.main-SNAPSHOT.jar"
CDM_PROJECT_NAME="finos-cdm"
CDM_PREFIX="finos"
CDM_VERSION="1.2.3"
cd ${MY_PATH} || error


source "$MY_PATH/../../ensure_jar_exists.sh" || { echo "Failed to source ensure_jar_exists.sh"; exit 1; }

# Parse command-line arguments
# CDM_BRANCH="master"
CDM_BRANCH="6.x.x"
SKIP_CDM=0
for arg in "$@"; do
  case "$arg" in
    --skip-cdm)
      SKIP_CDM=1
      ;;
    -r|--reuse-env)
      export REUSE_ENV=1
      ;;
    *)
      # default any other argument to the CDM version
      CDM_BRANCH="$arg"
      ;;
  esac
done

if [[ $SKIP_CDM -eq 0 ]]; then
    source $MY_PATH/get_cdm.sh "$CDM_BRANCH"
else
    echo "Skipping get_cdm.sh as requested."
fi

ensure_jar_exists "$PROJECT_ROOT_PATH" "$JAR_PATH"

echo "***** build CDM"
java -cp "$JAR_PATH" com.regnosys.rosetta.generator.python.PythonCodeGeneratorCLI -s $CDM_SOURCE_PATH -t $PYTHON_TARGET_PATH -p $CDM_PROJECT_NAME -v $CDM_VERSION -x $CDM_PREFIX || error "Failed to generate CDM Python code"
JAVA_EXIT_CODE=$?
if [[ $JAVA_EXIT_CODE -eq 1 ]]; then
    echo "Java program returned exit code 1. Stopping script."
    exit 1
fi

echo "***** build CDM Python package"
cd $PYTHON_TARGET_PATH
rm -f *-py3-none-any.whl
$PYEXE -m pip wheel --no-deps --only-binary :all: . || error

CDM_WHL=$(ls *-py3-none-any.whl 2>/dev/null | head -1)
if [ -z "$CDM_WHL" ]; then
    echo "ERROR: CDM wheel was not produced. Stopping."
    error
fi
echo "***** CDM wheel produced: $CDM_WHL"

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
