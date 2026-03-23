# Rune Language Feature Coverage

> **Audience**: Generator contributors and integrators.
> This document tracks which Rune language features are supported, partially supported, or not yet implemented in the Python code generator. It is a living reference and is updated alongside each release.



## Annotation Support

The following table tracks support for Rosetta/Rune annotations.

Usage counts are taken from the full CDM repository (`common-domain-model`) and the DRR project (`digital-regulatory-reporting`), which include the complete production model rather than just the test subset.

| Annotation Type | Description | Needs Generator Support? | Status | CDM Usage | DRR Usage | Implementation Details |
| :--- | :--- | :---: | :---: | :---: | :---: | :--- |
| **`metadata`** | Assigns special metadata behaviors (e.g., references, schemes) to fields or types. | **Y** | ✅ | Extensive | Extensive | Generates `_ALLOWED_METADATA`, `_KEY_REF_CONSTRAINTS`, and `Annotated[...]` types for Pydantic. |
| **`codeImplementation`** | Indicates the function logic is implemented externally in the host language. | **Y** | ✅ | Rare | Extensive | Redirects function generation to `rune_execute_native` and registers in `native_registry`. |
| `calculation` | Marks a function as performing mathematical or financial calculations. | **N** | ❌ | 56 | 54 | Marker only. Logic is generated via standard function patterns. |
| `deprecated` | Indicates that a type, attribute, or function is slated for removal. | **Y** | ❌ | 84 | 0 | Should map to `@deprecated` decorator or Pydantic `Field(deprecated=True)`. |
| `rootType` | Identifies a type as a primary entry point or top-level document in the model. | **?** | ❌ | 163 | 167 | May require global registration or different serialization entry points. |
| `qualification` | Marks a function used to classify or identify a specific product or event. | **?** | ❌ | 230 | 0 | Used extensively in CDM (`Qualify_*` functions). May require registration in a qualification engine. |
| `projection` | Marks a function intended to project data into reporting or external formats. | **?** | ❌ | 0 | 12 | Purpose marker. May require registration in a projection registry. |
| `nonReportable` | DRR-specific annotation marking a rule as excluded from reporting output. | **?** | ❌ | 0 | 186 | DRR-only. Declared in `regulation-common-type.rosetta`. No CDM equivalent. |
| `synonym` | Maps a type or attribute to an equivalent concept in an external standard (e.g., FpML, FIX). | **N** | ❌ | 451 | 453 | Mapping metadata only; does not affect runtime logic. Safe to ignore for Python generation. |
| `ruleReference` | Links an element to a specific regulatory or institutional business rule. | **N** | ❌ | 0 | 0 | Declared in Rune standard lib but not applied in CDM or DRR. Primarily for traceability. Could be added to docstrings. |
| `docReference` | Links an element to standard documentation (e.g., ISDA definitions). | **N** | ❌ | 0 | 0 | Declared in Rune standard lib but not applied in CDM or DRR. Primarily for documentation. Could be added to docstrings. |
| `creation` | Marks a function involved in state transitions or the creation of new business events. | **N** | ❌ | 0 | 0 | Annotation is *declared* in CDM (`event-workflow-func.rosetta`) but never applied to any function in CDM or DRR. |
| `ingest` | Marks a function used for parsing or ingesting external data formats. | **?** | ❌ | 0 | 0 | Declared in Rune standard lib but not applied in CDM or DRR. Note: `cdm.ingest.*` is a namespace naming convention, not use of this annotation. |
| `enrich` | Marks a function used to add or derive additional data for an existing object. | **?** | ❌ | 0 | 0 | Declared in Rune standard lib but not applied in CDM or DRR. Note: `enrichment` appearing in DRR is a field/type name, not this annotation. |

**Legend:**

- ✅ : Fully supported and integrated into generated code.
- ❌ : Currently ignored by the generator.
- **Y** : Critical for functionality or code correctness.
- **N** : Informational only; doesn't affect Python runtime logic.
- **?** : Requirement depends on future runtime integration architecture.

## Core Rosetta Syntax

The following table tracks implementation status for core Rosetta language features.

| Syntax Feature | Needs Generator Support? | Status | CDM Usage | DRR Usage | Implementation Details |
| :--- | :---: | :---: | :---: | :---: | :--- |
| **`typeAlias`** (basic) | **N** | ⚠️ | Extensive | Extensive | The alias is expanded to its underlying base type at generation time (e.g., `typeAlias ISIN: string` → field typed as `str`). The domain name is not preserved in the output. See [ARCHITECTURE.md](ARCHITECTURE.md#6-design-decision-typealias-generation) for the rationale and the planned Rune Path approach. |
| **`typeAlias`** (with `condition`) | **Y** | ❌ | Unknown | Unknown | When a `typeAlias` carries a `condition` block, the alias is stripped to its base type and the condition is silently discarded. No validation is generated. See Known Validation and Runtime Gaps below. |
| **`extends`** (Functions) | **Y** | ❌ | 0 | 0 | Inherits shortcuts, logic, and structure from a base function. Defined in Rune grammar (`func ... extends ...`). Not currently used in CDM or DRR, but must be handled when encountered. |
| **`super`** (Calls) | **Y** | ❌ | 0 | 0 | Invokes logic from a parent function within the current scope (`RosettaSuperCall` in Rune grammar). Not currently used in CDM or DRR. |

## Reporting Components

The following section tracks implementation status and requirements for regulatory reporting structures utilized heavily in DRR.

### `report`

**Meaning**: The `report` block acts as the orchestrator for regulatory reporting. It specifies the timing (e.g., `in T+1`), the eligibility condition (the `when` clause), the input data source, and the target output schema.
**Requirements**:

- **Needs Generator Support**: **Y** (Status: ❌)
- The generator must emit an orchestrating construct (e.g., a wrapper class or module function) that wires together the target schema and its associated rules.
- The generated code must first evaluate the eligibility condition (`when` block) against the input. If the condition is met, it must instantiate the target output schema and execute every `reporting rule` associated with that schema, assigning the resulting values to the corresponding fields.
- The generator must also emit metadata annotations or registration logic to allow the system to dynamically identify the report within an execution engine.

### `reporting rule`

**Meaning**: A `reporting rule` defines the precise navigational and computational logic required to extract, filter, or compute a specific field for a regulatory report from the input transaction object.
**Requirements**:

- **Needs Generator Support**: **Y** (Status: ❌)
- The generator must emit a standalone, callable function or class for each rule.
- The generated function must take the root input object as an argument and return the computed field value.
- The generated code must be strictly null-safe. Because reporting rules frequently navigate deep into optional nested attributes, the code must gracefully evaluate to `None`/`null` rather than raising exceptions (like `AttributeError` in Python) when an intermediate attribute is missing.

### `eligibility rule`

**Meaning**: An `eligibility rule` acts as a gating filter that evaluates logical conditions to establish whether a specific transaction is eligible for a given report.
**Requirements**:

- **Needs Generator Support**: **Y** (Status: ❌)
- The generator must emit a function that takes the input object and returns a `Boolean` value.
- It must translate the declarative conditional logic into standard boolean execution flows. The output is ultimately called by the `report` orchestrator to determine if the reporting pipeline should proceed.

## Rosetta Expression Support

The following table tracks support for Rosetta/Rune expressions within the Python code generator.

| Feature | Handled | Description |
| :--- | :---: | :--- |
| **ArithmeticOperation** | ✅ | Binary arithmetic (+, -, *, /) over two expressions. |
| **AsKeyOperation** | ⚠️ | Partial. Emits `Reference(arg)` (or a list comprehension for multi-valued arguments). `Reference` creation and basic key/ref round-tripping work. The remaining gap is **scoped cross-object reference resolution**: when a function assigns a value `as-key`, downstream functions that hold the resulting `Reference` must be able to resolve it back to the source object by scanning a model-graph scope (e.g. `TradeState`). This resolution lifecycle is not yet wired up in the runtime. |
| **ChoiceOperation** | ✅ | Handled within type conditions (via `rune_check_one_of`). |
| **ComparisonOperation** | ✅ | Binary comparisons (e.g., <, <=, >, >=). |
| **DefaultOperation** | ✅ | Binary fallback (use right when left is missing/empty). |
| **DistinctOperation** | ✅ | Unary list operation removing duplicates. Filters nulls and propagates `None`. |
| **EqualityOperation** | ✅ | Binary equality/inequality (==, !=). |
| **FilterOperation** | ✅ | Filter elements from a list. Supported for both explicit and implicit closure parameters. |
| **FirstOperation** | ✅ | Returns the first non-null element. Returns `None` for empty or null inputs. |
| **FlattenOperation** | ✅ | Unary list operation flattening nested collections. Filters nulls and handles `COWList`. |
| **JoinOperation** | ✅ | Binary operation joining strings (via `str.join`). |
| **LastOperation** | ✅ | Returns the last non-null element. Returns `None` for empty or null inputs. |
| **ListLiteral** | ✅ | Literal list (e.g., `[1, 2, 3]`). |
| **LogicalOperation** | ✅ | Binary logical operations (`and`, `or`). |
| **MapOperation** | ✅ | Projection or mapping of values (via `extract` keyword). Supported for both explicit and implicit parameters. |
| **MaxOperation** | ✅ | Aggregation with null-filtering. Propagates `None` if input is `None`. |
| **MinOperation** | ✅ | Aggregation with null-filtering. Propagates `None` if input is `None`. |
| **OneOfOperation** | ✅ | Supported both in type conditions and as a general expression in functions (Dynamic attribute detection). |
| **ReverseOperation** | ✅ | Unary list operation reversing element order. Filters nulls and propagates `None`. |
| **SortOperation** | ✅ | Unary list operation sorting elements. Filters nulls and propagates `None`. |
| **SumOperation** | ✅ | Aggregation with null-filtering. Propagates `None` if input is `None`. |
| **SwitchOperation** | ✅ | Conditional selection among cases (via helper functions). |
| **ThenOperation** | ✅ | Unary functional pipeline step (via lambda). |
| **ToDateOperation** | ✅ | Conversion/Parse to date. |
| **ToDateTimeOperation** | ✅ | Conversion/Parse to date-time. |
| **ToEnumOperation** | ✅ | Conversion/Parse to an enum. |
| **ToIntOperation** | ✅ | Conversion to integer. |
| **ToNumberOperation** | ✅ | Conversion to number (Decimal). Emits `Decimal(...)`. `Decimal` is already in the standard file header so no extra import is required. |
| **ToStringOperation** | ✅ | Conversion to string (via `rune_str`). |
| **ToTimeOperation** | ✅ | Conversion/Parse to time. |
| **ToZonedDateTimeOperation** | ✅ | Conversion/Parse to zoned date-time. |
| **AbsentOperation** | ✅ | Unary "is absent" assertion (via `not rune_attr_exists`). |
| **ConditionalExpression** | ✅ | If-then-else logic (via `if_cond_fn` and helper functions). |
| **ConstructorExpression** | ✅ | Constructs an object or dict from key-value pairs. |
| **ContainsExpression** | ✅ | Binary "contains" assertion (via `rune_contains`). |
| **CountExpression** | ✅ | Counts list size (via `rune_count`). |
| **DeepFeatureCall** | ✅ | Nested attribute access (via `rune_resolve_deep_attr`). |
| **DisjointOperation** | ✅ | Binary "no common elements" assertion (via `rune_disjoint`). |
| **EnumValueReference** | ✅ | Reference to an enumeration value. |
| **ExistsExpression** | ✅ | Unary "exists" assertion (supports `single`/`multiple` modifiers). |
| **FeatureCall** | ✅ | Attribute access (via `rune_resolve_attr`). |
| **ImplicitVariable** | ✅ | Reference to the implicit variable `item`. |
| **LiteralValues** | ✅ | Support for Boolean, Int, Number, String literals. |
| **OnlyElement** | ✅ | Unary list operation (via `rune_get_only_element`). |
| **OnlyExistsExpression** | ✅ | Checks that only the listed expressions exist. |
| **SymbolReference** | ✅ | References to Data types, Enums, Attributes, and shortcuts. |
| **WithMetaOperation** | ✅ | Wraps a value with metadata (via `rune_with_meta`). |
| **ReduceOperation** | ✅ | List reduction. Supported for both explicit and implicit closure parameters (with fallback naming). |

## Known Validation and Runtime Gaps

The following are known gaps in validation and runtime behaviour that require work in both the generator and the [rune-python-runtime](https://github.com/finos/rune-python-runtime).

### `typeAlias` Conditions on Basic Types

Rune allows conditions to be attached directly to basic type aliases:

```rosetta
typeAlias Currency:
    string
    condition C:
        item count = 3
```

The generator expands every `typeAlias` to its underlying base type at generation time (the Java Path — see [ARCHITECTURE.md](ARCHITECTURE.md#6-design-decision-typealias-generation)). Because the alias definition is discarded at the generation step, any `condition` block attached to it is never reached. A field typed as `Currency` is emitted as a plain `str` field with no validation.

**Impact**: Any `typeAlias` that adds a condition to a primitive type silently skips that validation in generated Python. Addressing this requires either generating a constrained wrapper type (analogous to Pydantic's `Annotated[str, ...]` with a custom validator) or implementing the Rune Path so that the alias and its conditions are preserved in the output.

### Codelist / External Scheme Validation

Rune models commonly use `[metadata scheme]` to indicate that a field's value must come from an external coding scheme (e.g., FpML business center codes, ISO currency codes). In the Java implementation this is handled by a `CodelistLoader`. No equivalent exists in the Python generator or runtime.

```rosetta
type BusinessCenters:
    businessCenter string (1..*)
        [metadata scheme]
```

The generator emits no validation call for scheme-constrained fields. The runtime has no mechanism to load, cache, or query external code lists.

**Impact**: High. CDM uses external schemes for dozens of critical fields (Business Centers, Currencies, Instrument Identifiers). Without codelist support, generated CDM Python cannot achieve validation parity with the Java implementation.

### Uniform Enum Metadata Wrapper

Plain Enums (those without an explicit `[metadata id]` or `[metadata key]` annotation) are generated as standard Python `Enum` members with no metadata container. Complex data types always have a metadata dictionary. This asymmetry causes inconsistent behaviour when runtime code attempts to attach metadata dynamically to an enum instance.

```rosetta
enum CurrencyEnum:
    USD
    EUR

type CashTransfer:
    currency CurrencyEnum (1..1)
        [metadata id]   # only annotated enums get a wrapper today
```

**Impact**: Metadata handling is inconsistent between annotated and unannotated enum fields. A uniform runtime proxy wrapping all enums is needed for correctness.

---

### Outstanding Expression Gaps

1. **`as-key` — Scoped Reference Resolution**: The generator correctly emits `Reference(arg)` and basic key/ref round-tripping works (see `test_key_ref.py`). The remaining gap is in the runtime: when a function produces a new object via `as-key` and stores a `Reference` to it, downstream functions that hold that `Reference` must be able to resolve it back to the live object by walking a model-graph scope (in CDM, `TradeState` is the root scope). This in-memory cross-scope resolution is not yet wired up — `UnresolvedReference` handles the deserialization case only.

2. **List Comparison Semantics**:
   Several implementation details in `rune-python-runtime` violate the formal comparison rules:
   - **The `_ntoz` (None to Zero) Violation**: The runtime converts `None` to `0` for comparisons.
      - **Rune Expectation (Heading: [`Comparison Operator and Null`](https://github.com/finos/rune-dsl/blob/master/docs/rune-modelling-component.md#L990))**: `null = any value` must be `false` (including `null = null`). Python currently returns `true` for `None == None` and `None == 0`.
   - **Pairwise Inequality (`<>`)**: The generator currently calls `rune_any_elements` for list inequality, which performs a **segment-wise Cartesian product** check (`any(x != y for ...)`).
      - **Rune Expectation (Heading: [`List Comparison`](https://github.com/finos/rune-dsl/blob/master/docs/rune-modelling-component.md#L1382))**: `<>` must return `true` if lists have different lengths OR if any pairwise items differ. `rune_any_elements` ignores list length and relative order entirely.
