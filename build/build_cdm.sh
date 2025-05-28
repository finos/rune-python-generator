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

type -P python > /dev/null && PYEXE=python || PYEXE=python3
if ! $PYEXE -c 'import sys; assert sys.version_info >= (3,11)' > /dev/null 2>&1; then
        echo "Found $($PYEXE -V)"
        echo "Expecting at least python 3.11 - exiting!"
        exit 1
fi

MY_PATH="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd ${MY_PATH} || error

source $MY_PATH/../build/get_cdm.sh
echo "***** build CDM"
cd $MY_PATH/..
mvn clean install
echo "***** setting up common environment"
BUILDPATH="../build"
source $MY_PATH/$BUILDPATH/setup_python_env.sh

echo "***** activating virtual environment"
VENV_NAME=".pyenv"
VENV_PATH=".."
source $MY_PATH/$BUILDPATH/$VENV_PATH/$VENV_NAME/${PY_SCRIPTS}/activate || error

echo "***** build CDM Python package"
PYTHONSOURCEDIR=$MY_PATH/"../target/python-cdm"
cd $PYTHONSOURCEDIR
rm python_cdm-*.*.*-py3-none-any.whl
$PYEXE -m pip wheel --no-deps --only-binary :all: . || processError

echo "***** cleanup"

deactivate
source $MY_PATH/$BUILDPATH/cleanup_python_env.sh

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