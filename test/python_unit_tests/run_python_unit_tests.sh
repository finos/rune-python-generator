#!/bin/bash

function usage {
  cat <<EOF
Usage: $(basename "$0") [options]

Options:
  -r, --reuse-env                              Reuse the .pyenv environment if it exists
  -k, --no-clean, --skip-clean, --keep-venv  Skip the cleanup step (leave venv active; do not run cleanup script)
  -h, --help                                  Show this help
Env:
  SKIP_CLEANUP=1                              Same as --no-clean
  REUSE_ENV=1                                 Same as -r
EOF
}

function error {
    echo
    echo "***************************************************************************"
    echo "*                                                                         *"
    echo "*                     DEV ENV Initialization FAILED!                      *"
    echo "*                                                                         *"
    echo "***************************************************************************"
    echo
    exit 1
}

# Default: perform cleanup unless asked not to
CLEANUP=1
# Env toggle
if [[ "${SKIP_CLEANUP:-}" == "1" || "${SKIP_CLEANUP:-}" == "true" ]]; then
  CLEANUP=0
fi

if [[ "${REUSE_ENV:-}" == "1" || "${REUSE_ENV:-}" == "true" ]]; then
  REUSE_ENV=1
else
  REUSE_ENV=0
fi
# CLI options
while [[ $# -gt 0 ]]; do
  case "$1" in
    -r|--reuse-env)
      REUSE_ENV=1
      CLEANUP=0
      shift
      ;;
    -k|--no-clean|--skip-clean|--keep-venv)
      CLEANUP=0
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown option: $1"
      usage
      exit 2
      ;;
  esac
done

export PYTHONDONTWRITEBYTECODE=1

type -P python >/dev/null && PYEXE=python || PYEXE=python3
if ! $PYEXE -c 'import sys; assert sys.version_info >= (3,11)' >/dev/null 2>&1; then
  echo "Found $($PYEXE -V)"
  echo "Expecting at least python 3.11 - exiting!"
  exit 1
fi

MY_PATH="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd "${MY_PATH}" || error
PROJECT_ROOT_PATH="$MY_PATH/../.."
PYTHON_SETUP_PATH="$MY_PATH/../python_setup"

JAR_PATH="$PROJECT_ROOT_PATH/target/python-0.0.0.main-SNAPSHOT.jar"
INPUT_ROSETTA_PATH="$PROJECT_ROOT_PATH/test/python_unit_tests/rosetta"
PYTHON_TESTS_TARGET_PATH="$PROJECT_ROOT_PATH/target/python-tests/unit_tests"

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

VENV_NAME=".pyenv"
VENV_PATH="$PROJECT_ROOT_PATH/$VENV_NAME"

if [[ $REUSE_ENV -eq 1 && -d "$VENV_PATH" ]]; then
  echo "***** reusing virtual environment"
  if [ -z "${WINDIR}" ]; then PY_SCRIPTS='bin'; else PY_SCRIPTS='Scripts'; fi
  # shellcheck disable=SC1090
  source "$VENV_PATH/${PY_SCRIPTS}/activate" || error
  echo "***** removing existing python_rosetta_dsl package"
  $PYEXE -m pip uninstall -y python_rosetta_dsl || true
else
  echo "***** setting up common environment"
  # shellcheck disable=SC1090
  source "$PYTHON_SETUP_PATH/setup_python_env.sh"

  echo "***** activating virtual environment"
  # shellcheck disable=SC1090
  source "$VENV_PATH/${PY_SCRIPTS}/activate" || error
fi

# package and install generated Python
cd "$PYTHON_TESTS_TARGET_PATH" || error
$PYEXE -m pip wheel --no-deps --only-binary :all: . || error
$PYEXE -m pip install python_rosetta_dsl-0.0.0-py3-none-any.whl

# run tests
echo "***** run unit tests"
cd "$MY_PATH" || error
$PYEXE -m pytest -p no:cacheprovider "$MY_PATH"

if (( CLEANUP )); then
  echo "***** cleanup"
  deactivate 2>/dev/null || true
  # shellcheck disable=SC1090
  source "$PYTHON_SETUP_PATH/cleanup_python_env.sh"
else
  echo "***** skipping cleanup (requested)"
fi