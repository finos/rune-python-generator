# Release Notes

## Current Release

### Highlights

- **CDM package load time reduced from ~2 minutes to ~15 seconds** — generated types are now emitted as standalone files where possible, with `model_rebuild()` called only for the small cycle-forming subset.
- **Full CDM import support** — resolved `ImportError` and `ArbitraryTypeWarning` failures that previously blocked loading CDM types such as `TradeState`.
- **Comprehensive expression support** — null-safety, implicit closure parameters, `one-of`, `as-key`, `with-meta`, and all standard collection operators are now fully generated.
- **Native function support** — Rune functions annotated `[codeImplementation]` are bridged to custom Python implementations via a standard registration mechanism.
- **Serialization support** — generated code can serialize and deserialize objects consistent with the [CDM serialization specification](https://github.com/finos/common-domain-model/issues/3236).

---

### Package Load Performance

The generator now partitions types based on whether they participate in circular dependencies:

- **Standalone** (~94% of CDM types): each type is emitted to its own `.py` file. No `model_rebuild()` is called at import time.
- **Bundled** (~6% of CDM types): types that form mutual circular dependencies are grouped into `_bundle.py` using a deferred-annotation pattern with `model_rebuild()`.

CDM 6.0 partition metrics:

| Metric | Value |
| :--- | :--- |
| Total Rosetta types | 973 |
| Standalone (acyclic) | 911 (~93.6%) |
| Bundled (cyclic) | 62 (~6.4%) |
| Cycle groups | 8 |
| Largest cycle group | 44 types (`Trade`, `Product`, `EconomicTerms`, `Payout`, …) |

**Result**: CDM 6.0 load time reduced from ~120 seconds to ~15 seconds.

For full details, see [package-load-performance-release-notes.md](docs/release-notes/package-load-performance-release-notes.md).

---

### CDM Import Fixes

Several `ImportError` and `ArbitraryTypeWarning` failures encountered when loading CDM types have been resolved:

- **Lazy proxy stubs** (PEP 562 `__getattr__`): proxy stub files no longer eagerly import the bundle at load time, eliminating circular import errors during bundle initialization.
- **Deferred standalone imports in `_bundle.py`**: standalone-type imports are placed after all bundled class definitions, preventing partially-initialized module errors.
- **Phase 1 `None` placeholder**: bundled class definitions use `None` as the type placeholder in Phase 1, avoiding Pydantic `ArbitraryTypeWarning` during initial class parsing.
- **Naming conflict aliasing**: standalone classes where a field name matches its type name now use an aliased import (`_TypeName`) to prevent Pydantic's annotation evaluator from shadowing the class with a `FieldInfo` object.
- **Self-import guard**: self-referential types no longer emit a self-import, eliminating `ImportError` from partially-initialized modules.
- **Cross-namespace bundle imports**: external-namespace types are always imported via their public proxy stub path rather than directly from a foreign `_bundle` module.

For full details, see [cdm-import-fixes-release-notes.md](docs/release-notes/cdm-import-fixes-release-notes.md).

---

### Circular Reference Support

Rosetta models containing mutually recursive types (e.g., `A` has an attribute of type `B` and `B` has an attribute of type `A`) are now fully supported. The generator uses a three-phase strategy for bundled types:

1. **Phase 1** — bare class definitions with lightweight type hints.
2. **Phase 2** — deferred `__annotations__` updates with full `Annotated[...]` metadata, injected after all classes in the bundle are defined.
3. **Phase 3** — `model_rebuild()` forces Pydantic to compile the final validation and serialization logic.

The dependency graph (DAG) now correctly handles choice-type attributes and distinguishes hard dependencies (inheritance) from soft dependencies (attribute types) when detecting cycles.

For full details, see [circular-reference-release-notes.md](docs/release-notes/circular-reference-release-notes.md).

---

### Function Support

- **Direct Pydantic constructors**: object creation uses direct constructor calls (`MyClass(...)`) instead of fragile runtime namespace lookups.
- **Stepwise object construction**: the `ObjectBuilder` pattern supports field-by-field population of output objects without triggering Pydantic validation on intermediate (incomplete) state. Finalization is handled automatically by the `@replaceable` decorator.
- **Native function bridge**: functions annotated `[codeImplementation]` connect to custom Python implementations via `rune_execute_native`. See [NATIVE_FUNCTIONS_INTEGRATION.md](docs/NATIVE_FUNCTIONS_INTEGRATION.md) for integration instructions.
- **Enum-based dispatch**: functions overloaded by enum value are generated as Python `match` statements with isolated helper functions per case.
- **Dotted FQN type hints**: function signatures use fully qualified, period-delimited type names for readability and namespace correctness.
- **Standardized condition execution**: pre- and post-conditions are registered in local registries and executed via `rune_execute_local_conditions`.

For full details, see [function-support-release-notes.md](docs/release-notes/function-support-release-notes.md).

---

### Expression Support

All standard Rune expressions are now generated. Key improvements in this release:

- **Null-safety and "nothing" propagation**: all collection operators (`sum`, `max`, `min`, `sort`, `reverse`, `distinct`, `count`, `join`, `only-element`) filter `None` values and propagate `None` consistently with the [Rune specification](https://github.com/finos/rune-dsl/blob/master/docs/rune-modelling-component.md).
- **Implicit closure parameters**: `extract`, `filter`, and `reduce` expressions with no named parameter now correctly fall back to the implicit `item` variable (or `a`/`b` for `reduce`).
- **Closures in `max`, `min`, `sort`**: key-based closure blocks (e.g., `sort [ item -> price -> amount ]`) are correctly translated to `key=lambda` syntax.
- **`one-of` as a general expression**: `one-of` can now be used as a boolean expression inside functions, not only within type conditions.
- **`as-key` support**: single and multi-cardinality `as-key` operations are translated to `Reference(x)` and list comprehensions respectively.
- **`with-meta` support**: inline `with-meta` expressions are translated via the `rune_with_meta` utility.

For the full expression coverage matrix, see [RUNE_LANGUAGE_GAPS.md](docs/RUNE_LANGUAGE_GAPS.md).
For full expression change details, see [expression-changes-release-notes.md](docs/release-notes/expression-changes-release-notes.md).

---

### Test Suite Improvements

The JUnit generator test suite was reorganized from 54 classes to 22 classes, adopting a consistent `Python*` naming scheme and consolidating scattered single-operation classes into coherent groups. The Python unit test files were aligned so that Rosetta filename, declared namespace, and pytest filename all correspond without exception.

For full details, see [testing-improvements-release-notes.md](docs/release-notes/testing-improvements-release-notes.md).

---

### Serialization

The generated Python code supports serialization and deserialization consistent with the [CDM serialization specification](https://github.com/finos/common-domain-model/issues/3236).

#### Deserializing from JSON

```python
obj = BaseDataClass.rune_deserialize(rune_json)
```

| Parameter | Type | Default | Description |
| :--- | :--- | :--- | :--- |
| `rune_json` | `str` | — | JSON string to deserialize |
| `validate_model` | `bool` | `True` | Validate Rune constraints after deserializing |
| `strict` | `bool` | `True` | Require types to match exactly; if `False`, attempts type coercion |
| `raise_validation_exceptions` | `bool` | `True` | Raise on validation errors; if `False`, returns a list of errors |

#### Serializing to JSON

```python
json_str = obj.rune_serialize()
```

| Parameter | Type | Default | Description |
| :--- | :--- | :--- | :--- |
| `validate_model` | `bool` | `True` | Validate Rune constraints before serializing |
| `strict` | `bool` | `True` | Only serialize fields that match the model definition |
| `raise_validation_exceptions` | `bool` | `True` | Raise on validation errors |
| `indent` | `int \| None` | `None` | JSON indentation; `None` produces compact output |
| `exclude_unset` | `bool` | `True` | Exclude fields not explicitly set |
| `exclude_defaults` | `bool` | `True` | Exclude fields at their default value |
| `exclude_none` | `bool` | `False` | Exclude `None`-valued fields |
| `round_trip` | `bool` | `False` | Verify the output can be deserialized back to a valid object |

---

### Standalone CLI

Generate Python from a Rune source file or directory:

```bash
java -cp target/python-0.0.0.main-SNAPSHOT.jar \
  com.regnosys.rosetta.generator.python.PythonCodeGeneratorCLI \
  -s <rune-source-files> -t <output-directory>
```

---

### Known Gaps

See [RUNE_LANGUAGE_GAPS.md](docs/RUNE_LANGUAGE_GAPS.md) for the full feature coverage matrix. Outstanding items include:

- `report`, `reporting rule`, and `eligibility rule` constructs (not yet generated)
- `deprecated` annotation (not yet mapped to `@deprecated` or `Field(deprecated=True)`)
- `as-key` scoped cross-object reference resolution (generation is correct; runtime wiring is incomplete)
- List comparison null semantics (`_ntoz` violation and pairwise `<>` semantics)
- `typeAlias` conditions on basic types
- Codelist / external scheme validation
