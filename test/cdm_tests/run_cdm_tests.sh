#!/bin/bash
type -P python > /dev/null && PYEXE=python || PYEXE=python3
if ! $PYEXE -c 'import sys; assert sys.version_info >= (3,11)' > /dev/null 2>&1; then
        echo "Found $($PYEXE -V)"
        echo "Expecting at least python 3.11 - exiting!"
        exit 1
fi

export PYTHONDONTWRITEBYTECODE=1

ACDIR=$($PYEXE -c "import sys;print('Scripts' if sys.platform.startswith('win') else 'bin')")
$PYEXE -m venv --clear .pytest
source .pytest/$ACDIR/activate

MYPATH="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
RUNERUNTIMEDIR="../../../../../../rune-python-runtime"
echo "**** Install Runtime ****"
$PYEXE -m pip install $RUNERUNTIMEDIR/rune.runtime*-py3-*.whl --force-reinstall

# install cdm package
PYTHONCDMDIR="../../target/python-cdm"
echo "**** Install CDM package ****"
$PYEXE -m pip install $MYPATH/$PYTHONCDMDIR/python_cdm-*-py3-none-any.whl

# run tests
$PYEXE -m pip install pytest
$PYEXE -m pytest -p no:cacheprovider $MYPATH
rm -rf .pytest