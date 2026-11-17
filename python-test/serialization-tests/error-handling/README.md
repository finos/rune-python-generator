# Serialization error-handling cases

These models and JSON files are copied from
`serialization/src/test/resources/rune-serializer-error-handling-test` in
[finos/rune-common](https://github.com/finos/rune-common), at commit
`cb9c389438cbfd6f78ce38bd4a7d48823c20f42d`.

One change was made: `metalocation/meta-location.rosetta` declared namespace
`serialization.failing.metalocation`, while its JSON, the other cases and the
removed Java test all use `serialization.test.failing`. The namespace is
corrected to `serialization.test.failing.metalocation`, so the case is rejected
for its dangling address rather than because its type can't be found.

rune-common removed the suite in `cec660462` ("Disable polymorphic
deserialisation of choice options", #605), along with its Java test, which was
disabled. The cases are kept here because the behaviour they check still
applies to the Python runtime:

| Folder | Case | Expected |
|---|---|---|
| `cardinality` | `too-many-elements` | More elements than the attribute allows is rejected |
| `metakey` | `dangling-attribute-ref`, `dangling-node-ref` | A reference to a key that isn't defined is rejected |
| `metalocation` | `dangling-address` | An address with no matching location is rejected |
| `parameterised` | `number-too-large`, `number-fraction-too-large` | A number with more digits than its type allows is rejected |
| `parameterised` | `string-too-large`, `string-illegal-pattern` | A string longer than its type allows, or not matching its pattern, is rejected |

`serialization_test.py` runs each JSON file in `test_should_fail_files`. A case
passes when reading or writing it raises an error, or when the written JSON
differs from the input.

rune-common is licensed under the Apache License, Version 2.0, as is this
project.
