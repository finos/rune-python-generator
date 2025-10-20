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

export PYTHONDONTWRITEBYTECODE=1

type -P python > /dev/null && PYEXE=python || PYEXE=python3
if ! $PYEXE -c 'import sys; assert sys.version_info >= (3,11)' > /dev/null 2>&1; then
        echo "Found $($PYEXE -V)"
        echo "Expecting at least python 3.11 - exiting!"
        exit 1
fi

MY_PATH="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd ${MY_PATH} || error
PROJECT_ROOT_PATH="$MY_PATH/../.."
PYTHON_SETUP_PATH="$MY_PATH/../python_setup"

JAR_PATH="$PROJECT_ROOT_PATH/target/python-0.0.0.main-SNAPSHOT.jar"
INPUT_ROSETTA_PATH="$PROJECT_ROOT_PATH/test/python_unit_tests/rosetta"
PYTHON_TESTS_TARGET_PATH="$PROJECT_ROOT_PATH/target/python-tests/unit_tests"

# Validate inputs/existence
if [[ ! -f "$JAR_PATH" ]]; then
  echo "Could not find generator jar at: $JAR_PATH"
  echo "Build the jar first (e.g., mvn -q -DskipTests package) and re-run."
  exit 1
fi
if [[ ! -d "$INPUT_ROSETTA_PATH" ]]; then
  echo "Input Rune sources not found at: $INPUT_ROSETTA_PATH"
  exit 1
fi

# Generate Python unit-test package using the CLI
echo "***** generating Python for unit tests"
mkdir -p "$PYTHON_TESTS_TARGET_PATH"
java -cp "$JAR_PATH" com.regnosys.rosetta.generator.python.PythonCodeGeneratorCLI \
  -s "$INPUT_ROSETTA_PATH" \
  -t "$PYTHON_TESTS_TARGET_PATH"
JAVA_EXIT_CODE=$?
if [[ $JAVA_EXIT_CODE -ne 0 ]]; then
    echo "Java program returned exit code $JAVA_EXIT_CODE. Stopping script."
    exit 1
fi

echo "***** setting up common environment"
source "$PYTHON_SETUP_PATH/setup_python_env.sh"

echo "***** activating virtual environment"
VENV_NAME=".pyenv"
source "$PROJECT_ROOT_PATH/$VENV_NAME/${PY_SCRIPTS}/activate" || error

# Ensure generated code is importable
export PYTHONPATH="$PYTHON_TESTS_TARGET_PATH:$PYTHONPATH"

# Additional unit test env (if any)
source "$MY_PATH/setup_unit_test_env.sh"

# run tests
echo "***** run unit tests"
cd "$MY_PATH"
$PYEXE -m pytest -p no:cacheprovider "$MY_PATH"

echo "***** cleanup"
deactivate
source "$PYTHON_SETUP_PATH/cleanup_python_env.sh"