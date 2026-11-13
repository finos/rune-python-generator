# CDM ingestion samples: Java validation

Reads, round-trips and validates CDM's FpML ingestion samples with CDM's own Java code, to produce a Java baseline for comparison with the generated Python.

The samples are the ones listed in CDM's test-pack configs, `rosetta-source/src/main/resources/ingest/config/test-pack-*.json`. They cover FpML 5.10, 5.12 and 5.13, trade states and workflow steps, including the `incomplete` and `invalid` packs.

Run it by hand; it isn't part of the build or CI. It builds CDM Java from source, which takes a few minutes and needs network access the first time.

## Requirements

- Java 21 and Maven
- Python 3.11+
- A local git clone of [finos/common-domain-model](https://github.com/finos/common-domain-model)

## Run

From the project root:

```sh
python-test/cdm-tests/java-validation/run_java_validation.sh <cdm-repo> <git-ref> <work-dir>
```

- `<cdm-repo>`: path to the CDM clone.
- `<git-ref>`: the CDM commit, branch or tag to test, for example `upstream/master`. Fetch it first.
- `<work-dir>`: a directory **outside this repository**. The script builds CDM there and writes the results to `<work-dir>/results/`.

For each sample, `ValidateSamples`:

1. reads the sample's output JSON with `RuneJsonObjectMapper`, as a `TradeState`, or a `WorkflowStep` for the `workflow-step` packs;
2. writes it back and compares it with the original, ignoring `@version` and comparing numbers by value;
3. resolves its references with `ReferenceResolverProcessStep`;
4. validates it with `RosettaTypeValidator`.

## Results

| File | One row per | Columns |
|---|---|---|
| `all-samples.tsv` | sample | test pack, output path, `modelValidationFailures` and `runtimeError` from the config |
| `java-summary.tsv` | sample | test pack, sample, config failure count, Java failure count, round trip (`same` or `DIFF`), read error |
| `java-failures.tsv` | validation failure | test pack, sample, type (`CARDINALITY` or `DATA_RULE`), object, rule, path, reason |

A sample with a read error has no failure count or round-trip result. An empty output file in CDM shows up as a read error.

## Other tools

- **`undefined_keys.py <sample.json> ...`** lists the references in a sample whose key isn't defined anywhere in the same file. Java leaves such a reference's value empty without reporting a failure, so a sample can pass Java validation with data missing.
- **`java/ShowReferences.java`** prints what Java holds, after resolving references, for the `dateRelativeTo` and business center references of a trade state's first interest rate payout. Compile and run it with the classpath `run_java_validation.sh` builds:

  ```sh
  CP="<work-dir>/cdm-java/rosetta-source/target/classes:$(cat <work-dir>/cp.txt)"
  java -cp "<work-dir>/classes:$CP" ShowReferences <sample.json>
  ```

## Comparing with Python

`python-test/cdm-tests/run_fpml510_tests.sh` and `test_fpml510_samples.py` read and round-trip the same samples with the generated Python. Use the same CDM commit for both, then compare per sample: whether it reads, whether the round trip is unchanged, and the validation failures.
