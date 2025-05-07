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

MY_PATH="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd ${MY_PATH} || error

echo "***** make the directory for the CDM"
rm -rf common-domain-model
mkdir common-domain-model
cd common-domain-model
echo "***** pull CDM rosetta definitions"
# CDM_VERSION="5.22.1"
CDM_VERSION="master"
git init
git config core.sparseCheckout true
echo "rosetta-source/src/main/rosetta" >> .git/info/sparse-checkout
git remote add origin https://github.com/finos/common-domain-model.git
git pull origin $CDM_VERSION
echo "***** CDM Rune version: $CDM_VERSION retrieved"