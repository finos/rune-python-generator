#!/bin/bash
#
# utility script - pulls the version of CDM specified by CDM_VERSION 
# supports build_cdm.sh
#

function error
{
    echo
    echo "***************************************************************************"
    echo "*                                                                         *"
    echo "*                     DEV ENV Initialization FAILED!                      *"
    echo "*                                                                         *"
    echo "***************************************************************************"
    echo "Reason: $1"
    echo
    exit -1
}

MY_PATH="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd ${MY_PATH} || error "Could not change to script directory"

ROSETTA_DIR="${MY_PATH}/../rosetta"

echo "***** resetting the rosetta directory"
rm -rf "${ROSETTA_DIR}"
mkdir -p "${ROSETTA_DIR}/common-domain-model"

# CDM_VERSION="5.x.x"
# CDM_VERSION="6.10.0"
CDM_VERSION="master"

echo "***** pull CDM rosetta definitions ($CDM_VERSION)"
TEMP_CDM="${MY_PATH}/../temp_cdm"
rm -rf "${TEMP_CDM}"
mkdir -p "${TEMP_CDM}"
cd "${TEMP_CDM}"

git init
git config core.sparseCheckout true
cat <<EOF > .git/info/sparse-checkout
rosetta-source/src/main/rosetta/*
pom.xml
rosetta-source/pom.xml
EOF

git remote add origin https://github.com/finos/common-domain-model.git
git pull --depth 1 origin $CDM_VERSION || error "git pull for CDM failed"

# Copy CDM files to the target 'common-domain-model' folder
cp -r rosetta-source/src/main/rosetta/* "${ROSETTA_DIR}/common-domain-model/"

# Check for rune-fpml[-.]version property in pom.xml
FPML_VERSION=$(sed -n 's/.*<rune-fpml[-.]version>\(.*\)<\/rune-fpml[-.]version>.*/\1/p' pom.xml | head -n 1)

if [ -n "$FPML_VERSION" ]; then
    echo "***** Found rune-fpml-version: $FPML_VERSION"
    echo "***** pulling FpML definitions from https://github.com/rosetta-models/rune-fpml"
    
    mkdir -p "${ROSETTA_DIR}/rune-fpml"
    
    TEMP_FPML="${MY_PATH}/../temp_fpml"
    rm -rf "${TEMP_FPML}"
    mkdir -p "${TEMP_FPML}"
    cd "${TEMP_FPML}"
    
    git init
    git config core.sparseCheckout true
    echo "rosetta-source/src/main/rosetta/*" >> .git/info/sparse-checkout
    git remote add origin https://github.com/rosetta-models/rune-fpml.git
    
    # Attempt to pull the specific version/tag, fallback to master if it fails
    git pull --depth 1 origin "$FPML_VERSION" || {
        echo "WARNING: Failed to pull FpML version $FPML_VERSION. Falling back to master..."
        git pull --depth 1 origin master
    }
    
    # Copy FpML files to the target 'rune-fpml' folder
    cp -r rosetta-source/src/main/rosetta/* "${ROSETTA_DIR}/rune-fpml/"
    
    cd "${MY_PATH}"
    rm -rf "${TEMP_FPML}"
fi

cd "${MY_PATH}"
rm -rf "${TEMP_CDM}"

echo "***** Rosetta models organized in: ${ROSETTA_DIR}"
ls -F "${ROSETTA_DIR}"