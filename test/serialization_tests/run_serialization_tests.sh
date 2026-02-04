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
PYTHON_SETUP_PATH="$MY_PATH/../python_setup"

JAR_PATH="$PROJECT_ROOT_PATH/target/python-0.0.0.main-SNAPSHOT.jar"
SERIALIZATION_SOURCE_ROOT="$MY_PATH/rune-common/serialization/src/test/resources"
PYTHON_TESTS_TARGET_PATH="$PROJECT_ROOT_PATH/target/python-tests/serialization_unit_tests"

# Optionally fetch the serialization sources
if [[ $DO_GET_SERIALIZATION_SOURCE -eq 1 ]]; then
  echo "***** fetching serialization sources (get_serialization_source.sh)"
  source "$MY_PATH/get_serialization_source.sh" || error
else
  echo "***** skipping fetch of serialization sources (pass 'getsource' to fetch)"
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
if [[ ! -d "$SERIALIZATION_SOURCE_ROOT" ]]; then
  echo "Serialization sources not found at: $SERIALIZATION_SOURCE_ROOT"
  echo "Run with 'getsource' or ensure the path exists."
  exit 1
fi

# Generate Python serialization tests using the CLI
echo "***** generating Python serialization tests"
mkdir -p "$PYTHON_TESTS_TARGET_PATH"
java -cp "$JAR_PATH" com.regnosys.rosetta.generator.python.PythonCodeGeneratorCLI \
  -s "$SERIALIZATION_SOURCE_ROOT" \
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
if [ -z "${WINDIR}" ]; then PY_SCRIPTS='bin'; else PY_SCRIPTS='Scripts'; fi
source "$PROJECT_ROOT_PATH/$VENV_NAME/${PY_SCRIPTS}/activate" || error

echo "***** Build and Install Helper"
cd "$MY_PATH/test_helper" || error
$PYEXE -m pip wheel --no-deps --only-binary :all: . || error
$PYEXE -m pip install test_helper-0.0.0-py3-none-any.whl || error
rm -f test_helper-0.0.0-py3-none-any.whl

echo "***** Build and Install Generated Unit Tests"
cd "$PYTHON_TESTS_TARGET_PATH" || error
rm -f ./*.whl
$PYEXE -m pip wheel --no-deps --only-binary :all: . || error
WHEEL_FILE=$(ls -1 ./*.whl 2>/dev/null | head -n 1)
if [[ -z "$WHEEL_FILE" ]]; then
  echo "No wheel produced in $PYTHON_TESTS_TARGET_PATH"
  deactivate
  source "$PYTHON_SETUP_PATH/cleanup_python_env.sh"
  exit 1
fi
$PYEXE -m pip install "$WHEEL_FILE" || error

# run tests
echo "***** run tests"
cd "$MY_PATH" || error
$PYEXE -m pytest -p no:cacheprovider . || error

echo "***** cleanup"
deactivate
source "$PYTHON_SETUP_PATH/cleanup_python_env.sh"

echo ""
echo "***************************************************************************"
echo "*                          SERIALIZATION TESTS OK                         *"
echo "***************************************************************************"
echo ""