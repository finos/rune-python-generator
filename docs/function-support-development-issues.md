# Development Documentation: Function Support Dev Issues

## Unresolved Issues and Proposals

Title: Bundle Loading Performance (Monolithic `_bundle.py`)

### Problem Description

The current architecture generates all model types (classes, enums, functions) into a single `_bundle.py` file. For large models like the Common Domain Model (CDM), this results in a monolithic module that must be fully parsed and executed by Python upon any import. This causes significant startup latency and high memory overhead even when only a small subset of the model is needed.

### Steps to Reproduce

1. Generate the full CDM using the Python generator.
2. Attempt to import any type from the model (e.g., `from cdm.base.datetime.AdjustableDate import AdjustableDate`).
3. Measure the time spent in the `import` statement and peak memory usage.

### Expected Result

The model should support granular or lazy loading, allowing sub-packages to be imported independently without parsing the entire universe of types.

### Actual Result

Every import triggers the execution of the entire `_bundle.py`, leading to multi-second delays for large models.

### Additional Context

Resolving this is functionally blocked by the **Circular Dependencies** issue (now partially resolved via String forward references).

---

Title: Conditions on Basic Types (Strings)

### Problem Description

Rune (using `.rosetta` files) allows attaching conditions directly to basic type aliases (e.g., `typeAlias BusinessCenter: string condition IsValid`). The Python generator does not yet support wrapping simple types to trigger these validators upon assignment.

### Steps to Reproduce

Example Rune code: (Type alias with condition)

```rosetta
typeAlias Currency:
    string

    condition C:
        item count = 3
```

1. Define a `typeAlias` on a basic type (string, number, etc.) with an attached `condition`.
2. Generate Python code and check the Pydantic field definition for the alias.

### Expected Result

The assignment should be intercepted and validated according to the condition.

### Actual Result

No validation occurs as the type is treated as a plain Python `str`.

### Additional Context

This is a missing feature in the current Python model generation.

---

Title: Standard Library for External Data (Codelist Support)

### Problem Description

Rune models often rely on external data sources, such as FpML coding schemes or ISO code lists, for validating field values. In the Java implementation, this is handled by a standard `CodelistLoader`. The Python generator and runtime currently lack an equivalent mechanism to load, cache, and query these external data sources during validation.

### Steps to Reproduce

Example Rune code: (Business Center with metadata scheme)

```rosetta
type BusinessCenters:
    businessCenter string (1..*)
        [metadata scheme]
```

1. Define a Rune field that refers to an external scheme (e.g., `[metadata scheme]`).
2. Generate Python code and attempt to validate an instance with an external value.
3. Observe that no validation logic is emitted, or it fails due to the missing library.

### Expected Result

A standard Python library or runtime utility should be available to handle external data loading, allowing the generator to emit consistent validation calls.

### Actual Result

There is no "Standard Library" for external data in the Python runtime, leaving external scheme validation unhandled.

### Additional Context

This has a major impact on the **Common Domain Model (CDM)**, which uses external schemes for validating dozens of critical fields (e.g., Business Centers, Currencies, Instrument Identifiers). Without this support, the Python-generated CDM cannot achieve full validation parity with the Java implementation.

---

## Backlog: Proposed Architectural / Runtime Changes

* **Uniform Enum Metadata Wrapper**: Implement a global runtime proxy for all Enums to allow attaching metadata (keys/references) consistently.

### Problem Description

Plain Enums in the current implementation do not have a uniform wrapper at runtime. This leads to inconsistent behavior when trying to attach metadata dynamically to any enum instance, whereas complex types always have a metadata dictionary.

### Steps to Reproduce

Example Rune code: [EnumMetadata.rosetta](https://github.com/finos/rune-python-generator/blob/feature/function_support/test/python_unit_tests/features/model_structure/EnumMetadata.rosetta)

```rosetta
enum CurrencyEnum:
    USD
    EUR
    GBP

type CashTransfer:
    amount number (1..1)
    currency CurrencyEnum (1..1)
        [metadata id]
```

1. Define an Enum without explicit metadata.
2. Attempt to treat it as a metadata-carrying object at runtime.

### Expected Result

All enums should behave consistently at runtime, ideally wrapped in a proxy that can hold a metadata payload.

### Actual Result

Only Enums with explicit metadata annotations are wrapped; plain ones remain as standard Python `Enum` members.

### Additional Context

Necessary for metadata handling consistency across types and enums.
