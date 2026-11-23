# CDM samples: Python and Java

This harness reads, validates and writes back CDM's FpML ingestion samples, or the JSON files in CDM's `functions/` folder, with both CDM Python and CDM Java, built from the same CDM commit. Java then reads what Python wrote. The results go in a CSV with one row per sample, and a summary is built from that CSV.

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
| `<pack-pattern>` | Optional. A regular expression for the test packs to run. All packs run by default. `functions` runs the files in CDM's `functions/` folder instead; see [The functions folder](#the-functions-folder) |

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
- the same external or scoped key on two objects in one scope, or the same global key on two different objects.

Keys are scoped as CDM Java's reference resolver scopes them (`CdmReferenceConfig`). An external or scoped key, and a reference to one, belongs to its nearest enclosing `TradeState`, or to the document when there is none. So the same key in a business event's `before` and `after` states isn't a duplicate, and a reference whose key is only in another scope is dangling; its detail says so. A global key belongs to the document. The scope type is `SCOPE_TYPE` in `model_check.py`.

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
| The sample | Its FpML version, pack and path. For the `functions/` folder, its root type, the file it comes from, and whether that file has the header |
| CDM's ingestion status | The assertions in the sample's test-pack config: the FpML input, whether ingestion raised a runtime error, any schema validation failure, the input and output path counts, the failure count CDM expects, and whether Java's count matches it |
| The model check | The file class, the issues and their kinds, the dangling optional references, and the verdict |
| Reading | Each language's read result. For Python, the error kind, the fields and the message |
| Validation | Each language's cardinality and condition failures, whether they agree, and what only one language found |
| The round trip | Run when Python reads the file: Python's write, whether the data is the same, and Java's read and validation of Python's output |
| The outcome | The expectations not met, the differences noted, the suggested change, and the CDM data issue |

For the `functions/` folder, CDM's ingestion status is empty, and the pack is the folder and the kind of file, for example `business-event:output`.

## The functions folder

`rosetta-source/src/main/resources/functions` holds the inputs and expected outputs of the functions in CDM's execution descriptors, and a few other files that CDM's Java tests read. `make_function_manifest.py` turns them into Rune documents, each with a root type:

| File | Documents |
|---|---|
| An output, or any file with a root `@type` | The whole file, with that type |
| A `Create_BusinessEvent` or `Create_Workflow` input | The whole file, as `EventInstruction` or `Workflow` |
| A `RunReturnSettlementWorkflow` input | One document per argument of a Rune type: the `TradeState` and the `ReturnInstruction` |
| Any other file | The type CDM's Java tests read it as. A list gives one per item |

A function input is a record of the function's arguments. CDM writes it from a Java test class (`CreateBusinessEventInput`, `CreateWorkflowInput`, `RunReturnSettlementWorkflowInput`), not from a Rune type, and it has no `@type`. `EventInstruction` and `Workflow` have the same attributes as the inputs of `Create_BusinessEvent` and `Create_Workflow`, so those files are read whole, with their keys and references in one document. The types are chosen for the check; CDM doesn't declare them. The function's own cardinalities can differ from the type's (`Create_BusinessEvent` requires `effectiveDate`, `EventInstruction` doesn't), so every input's arguments are also checked against the function's inputs and listed in `results/arguments.tsv`.

The CSV's `file_header` column says whether the file has the `@model`, `@type` and `@version` header: `complete`, `missing`, or `list` for a file that is a JSON list. A document without a root `@type` is written with the header added, so both languages read it the same way. The summary counts the files without one.

The function signatures and the types of the other files are listed in `make_function_manifest.py`. A new function or file needs an entry there, and the script stops on a file it has no type for.
