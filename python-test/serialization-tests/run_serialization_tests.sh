#!/bin/bash
#
# Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
# SPDX-License-Identifier: Apache-2.0
#

function error
{
    echo
    echo "***************************************************************************"
    echo "*                                                                         *"
    echo "*                                Testing failed!                          *"
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

# Parse args: default is skip; pass 'getsource' to fetch sources
DO_GET_SERIALIZATION_SOURCE=0
for arg in "$@"; do
  case "$arg" in
    --getsource)
      DO_GET_SERIALIZATION_SOURCE=1
      ;;
    -h|--help)
      echo "Usage: $0 [getsource]"
      echo "  --getsource  Fetch serialization sources before generating tests"
      exit 0
      ;;
  esac
done

MY_PATH="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd "${MY_PATH}" || error
PROJECT_ROOT_PATH="$MY_PATH/../.."
PYTHON_SETUP_PATH="$MY_PATH/../env-setup"

JAR_PATH="$PROJECT_ROOT_PATH/target/python-0.0.0.main-SNAPSHOT.jar"
SERIALIZATION_SOURCE_ROOT="$MY_PATH/rune-common/serialization/src/test/resources"
# Models to generate, all from different namespaces:
# - the round-trip suite from rune-common;
# - the error-handling cases, kept in this repo since rune-common removed them;
# - the invalid choice option cases from rune-common's choice deserializer suite.
# Other suites in rune-common reuse the round-trip namespaces, so they're left out:
# generating them together rejects those models as duplicates.
ROUND_TRIP_SOURCE_DIR="$SERIALIZATION_SOURCE_ROOT/rune-serializer-round-trip-test"
ERROR_HANDLING_SOURCE_DIR="$MY_PATH/error-handling"
CHOICE_DESERIALIZER_SOURCE_DIR="$SERIALIZATION_SOURCE_ROOT/rune-json-choice-type-deserializer-test"
INVALID_CHOICE_GROUPS=("invalidchoiceoption" "invalidnestedchoiceoption")
GENERATION_SOURCE_DIR="$PROJECT_ROOT_PATH/target/python-tests/serialization_sources"
PYTHON_TESTS_TARGET_PATH="$PROJECT_ROOT_PATH/target/python-tests/serialization_unit_tests"
PYTHON_PROJECT_NAME="serialization_unit_tests"

# Optionally fetch the serialization sources
if [[ $DO_GET_SERIALIZATION_SOURCE -eq 1 ]]; then
  echo "***** fetching serialization sources (get_serialization_source.sh)"
  source "$MY_PATH/get_serialization_source.sh" || error
else
  echo "***** skipping fetch of serialization sources (pass '--getsource' to fetch)"
fi

# Validate inputs/existence
if [[ ! -f "$JAR_PATH" ]]; then
  echo "Could not find generator jar at: $JAR_PATH"
  echo "Building with maven..."
  if ! (cd "$PROJECT_ROOT_PATH" && mvn clean package); then
    echo "Maven build failed - exiting."
    exit 1
  fi
  if [[ ! -f "$JAR_PATH" ]]; then
    echo "Maven build completed but $JAR_PATH still missing - exiting."
    exit 1
  fi
fi
for SOURCE_DIR in "$ROUND_TRIP_SOURCE_DIR" "$CHOICE_DESERIALIZER_SOURCE_DIR" "$ERROR_HANDLING_SOURCE_DIR"; do
  if [[ ! -d "$SOURCE_DIR" ]]; then
    echo "Serialization sources not found at: $SOURCE_DIR"
    echo "Run with '--getsource' or ensure the path exists."
    exit 1
  fi
done

# The generator reads one source directory, so stage the models together
echo "***** staging serialization models"
rm -rf "$GENERATION_SOURCE_DIR"
mkdir -p "$GENERATION_SOURCE_DIR"
cp -R "$ROUND_TRIP_SOURCE_DIR" "$GENERATION_SOURCE_DIR/round-trip" || error
cp -R "$ERROR_HANDLING_SOURCE_DIR" "$GENERATION_SOURCE_DIR/error-handling" || error
for GROUP in "${INVALID_CHOICE_GROUPS[@]}"; do
  cp -R "$CHOICE_DESERIALIZER_SOURCE_DIR/$GROUP" "$GENERATION_SOURCE_DIR/$GROUP" || error
done

# ChoiceDeepNested in rune-common's choicetype model reaches the same leaves through two options.
# The DSL warns about that and will reject it; the generator already rejects it. Leave it out of
# the staged copy until rune-common changes the model (https://github.com/finos/rune-dsl/issues/1464);
# serialization_test.py skips its fixtures.
echo "***** removing ChoiceDeepNested from the staged choicetype model"
python3 - "$GENERATION_SOURCE_DIR/round-trip/choicetype/choice.rosetta" <<'EOF' || error
import re
import sys

path = sys.argv[1]
text = open(path, encoding='utf-8').read()
for name in ('MiddleChoiceA', 'MiddleChoiceB', 'ChoiceDeepNested'):
    text, count = re.subn(rf'^choice {name}:\n(?:[ \t]+\S.*\n)*\n?', '', text, flags=re.M)
    if count != 1:
        sys.exit(f'{path}: choice {name} not found; check whether this step is still needed')
text, count = re.subn(r'^[ \t]+choiceDeepNested ChoiceDeepNested .*\n', '', text, flags=re.M)
if count != 1:
    sys.exit(f'{path}: attribute choiceDeepNested not found; check whether this step is still needed')
open(path, 'w', encoding='utf-8').write(text)
EOF

# Generate Python serialization tests using the CLI
echo "***** generating Python serialization tests"
# Clear earlier output so modules from previous runs aren't packaged
rm -rf "$PYTHON_TESTS_TARGET_PATH"
mkdir -p "$PYTHON_TESTS_TARGET_PATH"
java -cp "$JAR_PATH" com.regnosys.rosetta.generator.python.PythonCodeGeneratorCLI \
  -s "$GENERATION_SOURCE_DIR" \
  -t "$PYTHON_TESTS_TARGET_PATH" \
  -p "$PYTHON_PROJECT_NAME"
JAVA_EXIT_CODE=$?
if [[ $JAVA_EXIT_CODE -ne 0 ]]; then
    echo "Java program returned exit code $JAVA_EXIT_CODE. Stopping script."
    exit 1
fi

echo "***** setting up common environment"
source "$PYTHON_SETUP_PATH/setup_python_env.sh"

echo "***** activating virtual environment"
VENV_NAME=".pyenv"
if [ -z "${WINDIR}" ]; then PY_SCRIPTS='bin'; else PY_SCRIPTS='Scripts'; fi
source "$PROJECT_ROOT_PATH/$VENV_NAME/${PY_SCRIPTS}/activate" || error

echo "***** Build and Install Helper"
cd "$MY_PATH/test_helper" || error
python -m pip wheel --no-deps --only-binary :all: . || error
python -m pip install test_helper-0.0.0-py3-none-any.whl || error
rm -f test_helper-0.0.0-py3-none-any.whl

echo "***** Build and Install Generated Unit Tests"
cd "$PYTHON_TESTS_TARGET_PATH" || error
rm -f ./*.whl
python -m pip wheel --no-deps --only-binary :all: . || error
WHEEL_FILE=$(ls -1 ./*.whl 2>/dev/null | head -n 1)
if [[ -z "$WHEEL_FILE" ]]; then
  echo "No wheel produced in $PYTHON_TESTS_TARGET_PATH"
  deactivate
  source "$PYTHON_SETUP_PATH/cleanup_python_env.sh"
  exit 1
fi
# With a local or GitHub runtime (see setup_python_env.sh), skip dependencies so pip keeps it
# instead of replacing it with a release that satisfies the generated pin
if [ -n "$RUNE_RUNTIME_DIR" ] || [ -n "$RUNE_RUNTIME_REF" ]; then
  python -m pip install --no-deps "$WHEEL_FILE" || error
else
  python -m pip install "$WHEEL_FILE" || error
fi

# run tests
echo "***** run tests"
cd "$MY_PATH" || error
python -m pytest -p no:cacheprovider . || error

echo "***** cleanup"
deactivate
source "$PYTHON_SETUP_PATH/cleanup_python_env.sh"

echo ""
echo "***************************************************************************"
echo "*                          SERIALIZATION TESTS OK                         *"
echo "***************************************************************************"
echo ""