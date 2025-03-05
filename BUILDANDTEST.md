# _Build and testing instructions_

## To build the generator and run the JUnit tests

```sh
mvn clean install
```

There are two types of JUnit tests to confirm that generated code matches expectations.

1. In memory string comparisons of generated Python to expected results.
2. Generated Python written to the file system (see the target directory) compared to expected results read from the file system (see the directory test/compare_generated_syntax/expected).

All tests should pass.

## To run the Python Unit Tests
Assuming the build and tests successfully complete and Python 3.11+ is installed.

```sh
test/run_tests.sh
```
All tests should pass

## To test that the generated code successfully deserializes and serializes 

These tests implement the common standards found in the [Rune Common Repo](https://github.com/finos/rune-common.git).  

1. Get the test Rune and JSON files

```sh
test/serialization/get_serialization_source.sh
```
2. Generate Python from Rune definitions
```sh
mvn clean install
```
3. Run the tests
```sh
test/serialization/run_serialization_tests.sh
```

All tests are expected to pass.

Testing leverages `serialization_test.py` which can be used to test a directory of JSON or a specific file.

To use this script:
1. From the [Project Root], setup the environment:
```sh
build/setup_python.sh
source .pyenv/bin/activate
```
2. Then execute the script to get help instructions:
```sh
python test/serialization_test/serialization.py -h
```
3. To clean up the environment, execute the following from the Python project root:
```sh
deactivate
build/cleanup_python_env.sh
```
