#!/bin/bash
#
# Orchestration script for Codelist Integration Tests
#

function error
{
    echo
    echo "***************************************************************************"
    echo "*                 CODELIST TEST ORCHESTRATION FAILED!                     *"
    echo "***************************************************************************"
    echo "Reason: $1"
    echo
    exit -1
}

# ==============================================================================
# ARGUMENT PARSING
# ==============================================================================

# Allow the developer to keep or reuse the test environment for debugging
KEEP_ENV=0
REUSE_ENV=0
for arg in "$@"; do
  case "$arg" in
    -k|--keep-env|--no-clean)
      KEEP_ENV=1
      echo "Notice: Virtual environment will be kept after tests finish."
      ;;
    -r|--reuse-env)
      export REUSE_ENV=1
      echo "Notice: Will attempt to reuse the existing virtual environment."
      ;;
  esac
done

export PYTHONDONTWRITEBYTECODE=1

# ==============================================================================
# ENVIRONMENTAL SCRUBBING
# ==============================================================================

# Remove any currently active .pyenv paths from the system PATH. 
# This ensures the script utilizes the global system Python to orchestrate the test setup.
VENV_NAME=".pyenv"
CLEAN_PATH=$(echo "$PATH" | sed -E "s|[^:]*/$VENV_NAME/[^:]*:?||g")

if command -v python3 &>/dev/null; then
  PYEXE=$(PATH="$CLEAN_PATH" command -v python3)
elif command -v python &>/dev/null; then
  PYEXE=$(PATH="$CLEAN_PATH" command -v python)
else
  error "Python is not installed."
fi

if ! $PYEXE -c 'import sys; assert sys.version_info >= (3,11)' >/dev/null 2>&1; then
  error "Expecting at least python 3.11 - exiting!"
fi

# ==============================================================================
# PATH RESOLUTION & ARTIFACT CLEANUP
# ==============================================================================

MY_PATH="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd "$MY_PATH" || error "Could not change to script directory"

# Define relative paths based on the python-test/code-list-tests directory structure
PROJECT_ROOT_PATH="$MY_PATH/../.."
PYTHON_SETUP_PATH="$MY_PATH/../env-setup"
JAR_PATH="$PROJECT_ROOT_PATH/target/python-0.0.0.main-SNAPSHOT.jar"

TARGET_DIR="$MY_PATH/target"
WHEEL_DIR="$TARGET_DIR/wheels"
GENERATED_CODE_DIR="$TARGET_DIR/generated-code"

# Clean previous local artifact runs to ensure a clean build state
rm -rf "$TARGET_DIR"
mkdir -p "$WHEEL_DIR"

# ==============================================================================
# MAVEN AUTO-BUILD
# ==============================================================================

# Source the standard repository script to build the Java Generator JAR if missing
source "$MY_PATH/../ensure_jar_exists.sh" || error "Failed to source ensure_jar_exists.sh"
ensure_jar_exists "$PROJECT_ROOT_PATH" "$JAR_PATH"

# ==============================================================================
# RUNE CODE GENERATION
# ==============================================================================

echo "***** 1. Generating Python code from test.rosetta model..."

# Invoke the CLI to generate the testing models containing codelist references
java -cp "$JAR_PATH" com.regnosys.rosetta.generator.python.PythonCodeGeneratorCLI \
    -s "$MY_PATH/rune" \
    -t "$GENERATED_CODE_DIR" \
    -p "test-codelists" \
    -x "test_integration" \
    -v "1.0.0" || error "Failed to generate Python code from Rune model."

# ==============================================================================
# COMMON VIRTUAL ENVIRONMENT SETUP
# ==============================================================================

echo "***** 2. Setting up common virtual environment..."

# Rely on the repository's standard setup script to build the root .pyenv
source "$PYTHON_SETUP_PATH/setup_python_env.sh"

echo "***** 3. Activating virtual environment..."
if [ -z "${WINDIR}" ]; then PY_SCRIPTS='bin'; else PY_SCRIPTS='Scripts'; fi
source "$PROJECT_ROOT_PATH/$VENV_NAME/${PY_SCRIPTS}/activate" || error "Failed to activate venv"

# Note: Overriding the default runtime installation to test the new codelist extension 
# features prior to the upstream merge in finos/rune-python-runtime.
echo "***** 4. Installing target Rune Runtime fork (feature/load_codelist_extension)..."
python -m pip install "git+https://github.com/jserrano-spec/th-rune-python-runtime.git@feature/load_codelist_extension" || error "Failed to install custom runtime branch."

# ==============================================================================
# WHEEL PACKAGING
# ==============================================================================

# Build a wheel out of the mock codelist JSON to test importlib.resources module tracking
echo "***** 5. Building the mock codelist data wheel..."
cd "$MY_PATH/json" || error "Could not enter json directory."
python -m pip wheel --no-deps --only-binary :all: . -w "$WHEEL_DIR" || error "Failed to build JSON wheel."

# Build a wheel out of the auto-generated test.codelists python code
echo "***** 6. Building the generated Python code wheel..."
cd "$GENERATED_CODE_DIR" || error "Could not enter generated code directory."
python -m pip wheel --no-deps --only-binary :all: . -w "$WHEEL_DIR" || error "Failed to build generated code wheel."

# ==============================================================================
# INSTALLATION & TESTING
# ==============================================================================
echo "***** 7. Installing built wheels into the environment..."
cd "$MY_PATH" || error

# Force reinstall to ensure pytest picks up the newly generated artifacts for this run
python -m pip install "$WHEEL_DIR"/*.whl --force-reinstall --no-deps || error "Failed to install test wheels."

echo "***** 8. Running Pytest Integration Tests..."
python -m pytest -p no:cacheprovider "$MY_PATH/tests/" -v
TEST_EXIT_CODE=$?

# ==============================================================================
# TEARDOWN ENVIRONMENT
# ==============================================================================
deactivate

# Leverage the repository's standard cleanup script unless the developer explicitly retains it
if [[ $KEEP_ENV -eq 1 ]]; then
    echo "***** -k specified: virtual environment kept at $PROJECT_ROOT_PATH/$VENV_NAME"
    echo "***** Generated artifacts kept in $TARGET_DIR"
else
    echo "***** Cleaning up virtual environment and temporary artifacts..."
    source "$PYTHON_SETUP_PATH/cleanup_python_env.sh"
    rm -rf "$TARGET_DIR"
fi

if [[ $TEST_EXIT_CODE -eq 0 ]]; then
    echo "***************************************************************************"
    echo "*                    CODELIST INTEGRATION TESTS PASSED!                   *"
    echo "***************************************************************************"
else
    error "Pytest failed with exit code $TEST_EXIT_CODE."
fi

exit $TEST_EXIT_CODE