# CDM ingestion samples: Python and Java

This harness reads, validates and writes back CDM's FpML ingestion samples with both CDM Python and CDM Java, built from the same CDM commit. Java then reads what Python wrote. The results go in a CSV with one row per sample, and a summary is built from that CSV.

Exact agreement with Java isn't the goal. These are the expectations:

| Expectation | Applies to |
|---|---|
| Python reads the file with validation off | every file |
| Java and Python report the same validation failures | well-formed files |
| Python writes back the same data it read | every file Python reads |
| Java reads Python's output and reports the same failures as on the input | every file Python reads |

On a malformed file, a difference between Java and Python is recorded as a difference, not as a failure. When Python can't read a file, the row suggests a change.

## Running it

From the project root:

```bash
bash python-test/cdm-tests/roundtrip-comparison/run_roundtrip_comparison.sh \
    <cdm-repo> <git-ref> <work-dir> [<pack-pattern>]
```

| Argument | Meaning |
|---|---|
| `<cdm-repo>` | A local clone of `finos/common-domain-model`, with `<git-ref>` fetched |
| `<git-ref>` | The CDM commit, branch or tag, for example `upstream/master` |
| `<work-dir>` | A directory outside this repository. The script empties it first |
| `<pack-pattern>` | Optional. A regular expression for the test packs to run. All packs run by default |

Incomplete and invalid packs are always left out.

The script:

1. Extracts the CDM commit and builds CDM Java. It runs Java from CDM's packaged jar.
2. Builds the generator from this checkout. It generates CDM Python from CDM's `.rosetta` files and from the rune-fpml model version that CDM Java uses.
3. Builds the CDM Python wheel and installs it in a clean virtual environment. The install brings in the rune.runtime release that the generated `pyproject.toml` requires.
4. Lists the samples from the test-pack configs in `rosetta-source/src/main/resources/ingest/config`.
5. Runs Java (`java/RoundTrip.java`), Python (`python_roundtrip.py`), and then Java again on Python's output.
6. Checks each input against the model (`model_check.py`), compares the results (`compare.py`), and writes the summary (`summarize.py`).

The results are written to `<work-dir>/results/results.csv` and `<work-dir>/results/summary.md`.

## The model check

`model_check.py` reads the Rune model from the `.rosetta` files and walks each document against it. It uses neither language's runtime.

A document is **malformed** when its data doesn't match the model. That covers:

- a required attribute that is missing;
- a list with too few or too many items;
- an attribute that isn't in the model;
- a value of the wrong shape, basic type or enum;
- a choice without exactly one option;
- an `@type` that isn't the attribute's type or a subtype of it;
- a dangling reference on a required attribute;
- a reference to an object of the wrong type;
- the same external or scoped key on two objects, or the same global key on two different objects.

An attribute with optional cardinality doesn't need a value. A dangling reference on an optional attribute is therefore counted, but it doesn't make the document malformed. To change that, set `DANGLING_OPTIONAL_IS_MALFORMED` in `model_check.py`.

The check doesn't evaluate conditions. A file that fails validation gets one of these verdicts:

- invalid, confirmed by the model check;
- invalid, with conditions confirmed by both languages reporting them;
- not confirmed.

## Comparing documents

Documents are compared as data, not as text:

- the order of an object's fields doesn't matter;
- the order of a list's items does;
- numbers compare by value;
- an absent field, `null` and `[]` are the same;
- date-times compare by instant;
- `@version` is ignored.

A referenced object is validated once in Python but again at each reference in Java. Validation failures are therefore compared as sets of names, not as counts.

## The CSV

The columns are in groups:

| Group | What it holds |
|---|---|
| The sample | Its FpML version, pack and path |
| CDM's ingestion status | The assertions in the sample's test-pack config: the FpML input, whether ingestion raised a runtime error, any schema validation failure, the input and output path counts, the failure count CDM expects, and whether Java's count matches it |
| The model check | The file class, the issues and their kinds, the dangling optional references, and the verdict |
| Reading | Each language's read result. For Python, the error kind, the fields and the message |
| Validation | Each language's cardinality and condition failures, whether they agree, and what only one language found |
| The round trip | Run when Python reads the file: Python's write, whether the data is the same, and Java's read and validation of Python's output |
| The outcome | The expectations not met, the differences noted, the suggested change, and the CDM data issue |
