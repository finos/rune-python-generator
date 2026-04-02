# PR Summary

This PR delivers comprehensive function and expression generation support

> **Dependency**: this PR requires the next major release of [rune-python-runtime](https://github.com/finos/rune-python-runtime). The generated `pyproject.toml` declares `rune.runtime>=2.0.0,<3.0.0`. Both PRs must be merged and the runtime published to PyPI before this generator release is usable.

## What's new

1. **Function support**
   - Comprehensive support for Rune defined functions, including stepwise object construction (`ObjectBuilder`), enum-based dispatch, and pre/post conditions.
   - Integration of externally defined native "hand-crafted" functions where the Rune definition specifies `[codeImplementation]` or is empty.
   - Support for side-effect-free pass-by-value inputs.

2. **Completion of support for all Rune defined expressions**
   - Closed the gap of missing collection and list operators (`sort`, `min`, `max`, `reduce`, `distinct`, `flatten`, `reverse`, `sum`, `one-of`).
   - Support for implicit closure parameters and closure-based keys for collection operations.
   - Correct null / "nothing" propagation through expression chains.
   - Support for `with-meta` and `as-key`.

3. **Circular reference support**
   - Closed gaps and resolved issues in the handling of inheritance and attributes.
   
4. **Significant load performance improvement**
   - Load time reduced by approximately 85% (~120 s → ~15 s).
   - Mutually recursive elements partitioned into a small cyclic bundle (~6% of CDM types) with the remainder emitted as standalone files

5. **Completion of support for Type Aliases** — type aliases are resolved with flattened naming conventions and collision handling.

6. **CLI improvements**
   - `--project-name` flag enables CDM builds with a custom package prefix (e.g. `finos_cdm`)
   - Added flags to control the response to Rune parsing errors (`--allow-errors`) and warnings (`--fail-on-warnings`)
   - Execution emits exit codes (`0`/`1`)

7. **Refactored object, attribute, and expression generation** — introduced `PythonExpressionScope` and companion blocks to simplify code and support reuse across the generator.

## Test suite

- Improved testing rigor and coverage.
- JUnit suite reorganized from 54 to 22 classes with consistent `Python*` naming and less fragile assertions.
- Python unit tests restructured so Rune filename, namespace, and pytest filename correspond without exception.

## Remaining gaps

See [RUNE_LANGUAGE_GAPS.md](docs/RUNE_LANGUAGE_GAPS.md)

---

For full details see [RELEASE.md](RELEASE.md).
