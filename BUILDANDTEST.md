# _Build and testing instructions_

## To build the generator and run the JUnit tests

```sh
mvn clean install
```

All tests should pass.

## To run the Python Unit Tests
Assuming the build and tests successfully complete.

```sh
test/run_tests.sh
```

## To run the Python unit tests
The repo includes Python unit tests using pytest
```sh
test/run_tests.sh
```
All tests should pass

## To test that the generated code successfully deserializes and serializes 

These tests use common standards found in another repo.  

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

As of Mar 2025, three tests are expected to fail:

- extension/extended-type-concrete.json
- metakey/node-ref.json: fails the dict comparison because the Python generated code drops the least specific reference when two are found
- metakey/attribute-ref.json: fails the dict comparison because the Python generated code drops the least specific reference when two are found

Testing leverages `serialization_test.py` which can be used to test a directory of JSON or a specific file.

To use this script:
1. From the [Project Root], setup the environment:
```sh
build/setup_python.sh
source .pyenv/bin/activate
```
2. Then to execute the script:
```sh
python test/serialization_test/serialization.py -h
```
3. To test whether the generated code can read and validate a CDM JSON file

```sh
test/cdm_tests/run_serialization_test.sh
```
4. To clean up the environment, execute the following from the Python project root:
```sh
deactivate
build/cleanup_python_env.sh
```
