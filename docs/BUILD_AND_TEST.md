# Build and Testing Instructions

For a detailed description of how each test suite is organized and how to write new tests, see [TESTING.md](TESTING.md).

## Prerequisites

- **Java 21** — required to build the generator
- **Maven** — used to build and run JUnit tests
- **Python 3.11+** — required to run Python unit and serialization tests

## Setup

Clone the repository and change to the project root:

```sh
git clone https://github.com/finos/rune-python-generator
cd rune-python-generator
```

All commands below are run from the **project root** unless stated otherwise.

---

## Build and Run JUnit Tests

```sh
mvn clean package
```

The JUnit tests compare generated Python output against expected results. All tests should pass.

---

## Run Python Unit Tests

Assuming the build has completed successfully:

```sh
python-test/unit-tests/run_python_unit_tests.sh
```

All tests should pass.

---

## Run Serialization Tests

These tests implement the common serialization standards from the [Rune Common Repo](https://github.com/finos/rune-common.git).

**Step 1** — Fetch the Rune source and JSON test fixtures:

```sh
python-test/serialization-tests/get_serialization_source.sh
```

**Step 2** — Generate Python from the Rune definitions:

```sh
mvn clean package
```

**Step 3** — Run the tests:

```sh
python-test/serialization-tests/run_serialization_tests.sh
```

All tests are expected to pass.

---

## Using the Serialization Test Script Directly

`serialization_test.py` can run against a directory of JSON files or a single file.

**Step 1** — Set up the Python environment:

```sh
python-test/env-setup/setup_python_env.sh
source .pyenv/bin/activate
```

**Step 2** — Show usage help:

```sh
python python-test/serialization-tests/serialization_test.py -h
```

**Step 3** — When finished, deactivate and clean up the environment:

```sh
deactivate
python-test/env-setup/cleanup_python_env.sh
```
