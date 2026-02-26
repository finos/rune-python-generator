#!/bin/bash
# Build and install the native implementation package in editable mode

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR"

echo "Building and installing native implementation package from $SCRIPT_DIR..."
# Debug: list files to ensure pyproject.toml is visible
ls -F
python -m pip install -e "$SCRIPT_DIR"

echo "Done. Installed from $SCRIPT_DIR"
