#
# Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
# SPDX-License-Identifier: Apache-2.0
#

'''run serialization unit tests'''
from pathlib import Path
import os
import json
import glob
import argparse
import pytest

from rune.runtime.base_data_class import BaseDataClass
from rune.runtime.object_registry import _OBJECT_REGISTRY

from test_helper.test_helper.dict_comp import dict_comp

ROUNDTRIP_DIR = 'rune-common/serialization/src/test/resources/rune-serializer-round-trip-test'
# ChoiceDeepNested in rune-common's choicetype model reaches the same leaves through two
# options. The DSL warns about that and will reject it; the generator already rejects it, so
# run_serialization_tests.sh leaves it out of the generated model. Skip the fixtures that use
# it until rune-common changes the model (https://github.com/finos/rune-dsl/issues/1464).
SKIPPED_ROUND_TRIP_FILES = {
    'choicetype/choice-deep-nested.json',
    'choicetype/choice-deep-nested-2.json',
    'choicetype/choice-deep-nested-extended.json',
}
# Cases that must fail: the error-handling cases kept in this repo (rune-common
# removed them), and the invalid choice options from rune-common's choice
# deserializer suite
CHOICE_DESERIALIZER_DIR = 'rune-common/serialization/src/test/resources/rune-json-choice-type-deserializer-test'
SHOULDFAIL_DIRS = [
    'error-handling',
    CHOICE_DESERIALIZER_DIR + '/invalidchoiceoption',
    CHOICE_DESERIALIZER_DIR + '/invalidnestedchoiceoption',
]


def extract_dir_and_file(path_name) -> str:
    '''extract the directory and file name'''
    return Path(path_name).parent.name + '/' + os.path.basename(path_name)


def process_file(path_name,
                 compare: bool = True,
                 reset_registry: bool = True,
                 show_results: bool = False) -> bool:
    '''process a file'''
    result = False
    file_name = extract_dir_and_file(path_name)
    try:
        json_str_in = Path(path_name).read_text(encoding='utf8')
        if (reset_registry):
            _OBJECT_REGISTRY = {}
        obj = BaseDataClass.rune_deserialize(json_str_in)
        json_str_out = obj.rune_serialize()
        if compare:
            dict_file_in = json.loads(json_str_in)
            dict_file_out = json.loads(json_str_out)
            result = dict_comp(dict_file_in, dict_file_out)
            if show_results:
                result_str = 'serialization matches' if result else 'serialization does not match'
                print('.... processed file: ', file_name, ' result: ', result_str)
            return result
    except Exception as error_msg:
        if show_results:
            print('.... exception processing file:', file_name, ' exception:',
                  error_msg)
        # Raise the exception to be caught in the test function
        raise RuntimeError(f"Exception processing file {file_name}: {error_msg}")

def process_directory(dir_name):
    '''process all files in a directory'''
    path_names = glob.glob(dir_name + os.sep + '**/*.json', recursive=True)
    results = []
    for path_name in path_names:
        results.append({
            "path_name": path_name,
            "result": process_file(path_name)
        })
    matches = 0
    failures = 0
    print('---- result summary for dir:', dir_name, '----- matches')
    for result in results:
        file_name = extract_dir_and_file(result['path_name'])
        if (result['result']):
            print('file:', file_name, '...', 'serialization matches')
            matches += 1
    print('---- result summary for dir:', dir_name, '----- failures')
    for result in results:
        file_name = extract_dir_and_file(result['path_name'])
        if (not result['result']):
            print('file:', file_name, '...', 'something failed')
            process_file(result['path_name'], show_results=True)
            failures += 1
    print('---- result summary for dir:', dir_name, ' matches:', matches,
          'failures:', failures)


def _round_trip_files() -> list[str]:
    '''the round-trip fixtures, without the skipped ones; fails if a skipped one is missing'''
    files = glob.glob(ROUNDTRIP_DIR + os.sep + '**/*.json', recursive=True)
    found = {extract_dir_and_file(f) for f in files}
    missing = SKIPPED_ROUND_TRIP_FILES - found
    if missing and files:
        raise RuntimeError(f'skipped round-trip fixtures not found: {sorted(missing)}; '
                           'check whether SKIPPED_ROUND_TRIP_FILES is still needed')
    return [f for f in files if extract_dir_and_file(f) not in SKIPPED_ROUND_TRIP_FILES]


@pytest.mark.parametrize("round_trip_file", _round_trip_files())
def test_round_trip_files(round_trip_file):
    '''test round trip files'''
    try:
        assert process_file(round_trip_file), f"failed dict comparison for {round_trip_file}"
    except RuntimeError as e:
        pytest.fail(str(e))

@pytest.mark.parametrize(
    "should_fail_file",
    sorted(path_name
           for dir_name in SHOULDFAIL_DIRS
           for path_name in glob.glob(dir_name + os.sep + '**/*.json', recursive=True)))
def test_should_fail_files(should_fail_file):
    '''test should fail files'''
    try:
        result = process_file(should_fail_file)
        assert not result, f"dict comparison should not have passed for {should_fail_file}"
    except RuntimeError as e:
        # If an exception is raised, it is expected, so the test should pass
        print(f"Expected failure for {should_fail_file}: {e}")

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument('-t',
                        '--test',
                        help='Run Unit Tests',
                        action="store_true")
    parser.add_argument('-f', '--file', help='Test a JSON File')
    parser.add_argument('-d',
                        '--directory',
                        help='Test All Files in a Directory')
    args = parser.parse_args()
    if args.test:
        print('run tests')
        pytest_args = [
            '-v', __file__
        ]  # '-v' for verbose output, '__file__' to specify the current file
        pytest.main(pytest_args)
    elif args.directory:
        print('testing files in the directory: ', args.directory)
        process_directory(args.directory)
    elif args.file:
        try:
            print('testing file: ', args.file)
            process_file(args.file, show_results=True)
        except Exception as error_msg:
            print('procesing file:', args.file, ' created exception:',
                  error_msg)
    else:
        parser.print_help()
