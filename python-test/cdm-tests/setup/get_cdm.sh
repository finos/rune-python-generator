#!/bin/bash
#
# Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
# SPDX-License-Identifier: Apache-2.0
#

#
# utility script - pulls the version of CDM specified by its first argument (default to master)
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

CDM_BRANCH="${1:-master}"
CDM_REMOTE="https://github.com/finos/common-domain-model.git"
FPML_REMOTE="https://github.com/rosetta-models/rune-fpml.git"
FPML_BRANCH_OVERRIDE=""

idx=2
while [ $idx -le $# ]; do
    arg="${!idx}"
    ((next_idx=idx+1))
    val="${!next_idx}"
    case "$arg" in
        --cdm-repo)    CDM_REMOTE="$val";          ((idx+=2)) ;;
        --fpml-repo)   FPML_REMOTE="$val";         ((idx+=2)) ;;
        --fpml-branch) FPML_BRANCH_OVERRIDE="$val"; ((idx+=2)) ;;
        *) ((idx++)) ;;
    esac
done

echo "***** pull CDM rosetta definitions ($CDM_BRANCH from $CDM_REMOTE)"
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

git remote add origin "$CDM_REMOTE"
git pull --depth 1 origin $CDM_BRANCH || error "git pull for CDM failed"

# Copy CDM files to the target 'common-domain-model' folder
cp -r rosetta-source/src/main/rosetta/* "${ROSETTA_DIR}/common-domain-model/"

# Resolve FpML branch: explicit override > pom.xml lookup
if [ -n "$FPML_BRANCH_OVERRIDE" ]; then
    FPML_VERSION="$FPML_BRANCH_OVERRIDE"
    echo "***** Using explicit FpML branch: $FPML_VERSION"
else
    FPML_VERSION=$(sed -n 's/.*<rune-fpml[-.]version>\(.*\)<\/rune-fpml[-.]version>.*/\1/p' pom.xml | head -n 1)
fi

if [ -n "$FPML_VERSION" ]; then
    echo "***** pulling FpML definitions ($FPML_VERSION from $FPML_REMOTE)"

    mkdir -p "${ROSETTA_DIR}/rune-fpml"

    TEMP_FPML="${MY_PATH}/../temp_fpml"
    rm -rf "${TEMP_FPML}"
    mkdir -p "${TEMP_FPML}"
    cd "${TEMP_FPML}"

    git init
    git config core.sparseCheckout true
    echo "rosetta-source/src/main/rosetta/*" >> .git/info/sparse-checkout
    git remote add origin "$FPML_REMOTE"

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
