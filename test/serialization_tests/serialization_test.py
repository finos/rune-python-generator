'''run serialization unit tests'''
from hashlib import sha1
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
SHOULDFAIL_DIR = 'rune-common/serialization/src/test/resources/rune-serializer-error-handling-test'


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
                print('.... processed file: ', file_name, ' result: ',
                      result_str)
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


@pytest.mark.parametrize(
    "round_trip_file",
    glob.glob(ROUNDTRIP_DIR + os.sep + '**/*.json', recursive=True))
def test_round_trip_files(round_trip_file):
    '''test round trip files'''
    try:
        assert process_file(round_trip_file), f"failed dict comparison for {round_trip_file}"
    except RuntimeError as e:
        pytest.fail(str(e))

@pytest.mark.parametrize(
    "should_fail_file",
    glob.glob(SHOULDFAIL_DIR + os.sep + '**/*.json', recursive=True))
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
