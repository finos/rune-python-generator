# Release Notes

## Current Release

> **Runtime Dependency**: this release requires the next major release of [rune-python-runtime](https://github.com/finos/rune-python-runtime). The generated `pyproject.toml` declares `rune.runtime>=2.0.0,<3.0.0`. Both PRs must be merged and the runtime published to PyPI before this generator release is usable.

### 1. Function Support

Rune functions are now generated as Python callables. (Issues [#108](https://github.com/finos/rune-python-generator/issues/108), [#144](https://github.com/finos/rune-python-generator/issues/144), [#156](https://github.com/finos/rune-python-generator/issues/156), [#157](https://github.com/finos/rune-python-generator/issues/157))

- **Comprehensive support**: stepwise object construction (`ObjectBuilder`), enum-based dispatch (`match`), and pre/post conditions are all generated.
- **Native "hand-crafted" functions**: functions annotated `[codeImplementation]` or with an empty body are bridged to custom Python implementations via `rune_execute_native`. (Issues [#147](https://github.com/finos/rune-python-generator/issues/147), [#158](https://github.com/finos/rune-python-generator/issues/158)) See [NATIVE_FUNCTIONS_INTEGRATION.md](docs/NATIVE_FUNCTIONS_INTEGRATION.md) for integration details.
- **Side-effect-free pass-by-value inputs**: function parameters are wrapped in a COW (copy-on-write) proxy at call time, ensuring the caller's objects are never mutated by function execution. (Issue [#169](https://github.com/finos/rune-python-generator/issues/169))
- **Enum-based dispatch**: functions overloaded by enum value are generated as isolated per-case helpers. (Issue [#165](https://github.com/finos/rune-python-generator/issues/165))

---

### 2. Completion of Support for All Rune Defined Expressions

All standard Rune expressions are now generated. (Issues [#7](https://github.com/finos/rune-python-generator/issues/7), [#168](https://github.com/finos/rune-python-generator/issues/168))

- **Missing collection and list operators**: added support for use of `sort`, `min`, `max`, `reduce`, `distinct`, `flatten`, `reverse`, `sum`, `one-of`, when operating across a list.  All other remaining operators are fully generated.
- **Implicit closure parameters**: `extract`, `filter`, and `reduce` with no named parameter fall back to the implicit `item` variable (or `a`/`b` for `reduce`).
- **Closure-based keys**: `sort`, `min`, and `max` with a closure block (e.g., `sort [ item -> price -> amount ]`) are translated to `key=lambda` syntax.
- **Null / "nothing" propagation**: all collection operators filter `None` values and short-circuit to `None` when the input collection is absent, consistent with the Rune specification.
- **List comparison null semantics**: `null` values are correctly handled in list comparisons, including pairwise `<>` semantics.
- **`with-meta`**: inline `with-meta` expressions are translated to type-aware Python — `*WithMeta` constructors for basic types, `EnumWithMetaMixin.deserialize()` for enum types, and `set_meta()` for complex types.
- **`as-key`**: single and multi-cardinality `as-key` operations are translated to `Reference(x)` and list comprehensions.

For the full coverage matrix see [RUNE_LANGUAGE_GAPS.md](docs/RUNE_LANGUAGE_GAPS.md).

---

### 3. Circular Reference Support

Mutually recursive types (circular inheritance and attribute references) are now fully handled. (Issue [#145](https://github.com/finos/rune-python-generator/issues/145))

- Closed gaps in the handling of circular inheritance and attribute cycles.
- Circular dependencies are identified automatically and grouped into a small bundle; all other types are emitted as standalone files.

---

### 4. Significant Load Performance Improvement

Load time reduced by approximately 85% (~120 s → ~15 s for CDM 6.0). (Issue [#148](https://github.com/finos/rune-python-generator/issues/148))

The generator partitions types into two categories:

- **Standalone** (~94% of CDM types): each type is emitted to its own `.py` file with annotations written directly in the class body. No `model_rebuild()` is called at import time.
- **Bundled** (~6% of CDM types): types involved in circular dependency cycles are grouped into `_bundle.py` with `model_rebuild()` called once per bundle.

---

### 5. Completion of Support for Type Aliases

Type aliases (`typeAlias`) are now resolved to their underlying Python types at generation time. Where multiple aliases would otherwise produce the same Python type name, unique disambiguating names are generated to avoid conflicts.

Note: Basic type constraints on a `typeAlias` (pattern, min/max length, numeric ranges) are propagated to the generated Python field. However, named `condition` blocks on a `typeAlias` are silently discarded — see [RUNE_LANGUAGE_GAPS.md](docs/RUNE_LANGUAGE_GAPS.md) for details. In CDM, the only `typeAlias` carrying a named condition is `FpMLCodingScheme`, which is used exclusively for FpML codelist domain validation and delegates to the native function `ValidateFpMLCodingSchemeDomain`.

---

### 6. Refactored Object, Attribute, and Expression Generation

Internal generator refactoring to simplify code and support reuse:

- Introduced `PythonExpressionScope` to manage receiver context and lambda symbol shadowing during expression generation.
- Multi-statement expressions that require intermediate values are now emitted as inline immediately-invoked lambdas rather than requiring function-level statement hoisting.
- Consolidated object, attribute, and expression generation to remove duplication and improve maintainability.

---

## Known Gaps

See [RUNE_LANGUAGE_GAPS.md](docs/RUNE_LANGUAGE_GAPS.md) for the full feature coverage matrix.
