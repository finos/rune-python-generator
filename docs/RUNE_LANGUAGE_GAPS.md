# Rune Language Feature Coverage

> **Audience**: Generator contributors and integrators.
> This document tracks which Rune language features are supported, partially supported, or not yet implemented in the Python code generator. It is a living reference and is updated alongside each release.



## Annotation Support

The following table tracks support for Rosetta/Rune annotations.

Usage counts are taken from the full CDM repository (`common-domain-model`, `master` at `b8304b62f`, 2026-11-20) and the DRR project (`digital-regulatory-reporting`, `master` at `e51b59ae2d`, 2026-11-23), which include the complete production model rather than just the test subset. DRR counts cover DRR's own Rune files, not the CDM it depends on. Annotations quoted inside descriptions (`<"...">`) are not counted.

| Annotation Type | Description | Needs Generator Support? | Status | CDM Usage | DRR Usage | Implementation Details |
| :--- | :--- | :---: | :---: | :---: | :---: | :--- |
| **`metadata`** | Assigns special metadata behaviors (e.g., references, schemes) to fields or types. | **Y** | ✅ | 441 | 15 | Generates `_ALLOWED_METADATA`, `_KEY_REF_CONSTRAINTS`, and `Annotated[...]` types for Pydantic. |
| **`codeImplementation`** | Indicates the function logic is implemented externally in the host language. | **Y** | ✅ | 49 | 21 | The generated function calls `rune_execute_native`, and the module calls `rune_attempt_register_native_functions` to register a Python implementation if one is installed. Without one, calling the function raises `NotImplementedError`. |
| `calculation` | Marks a function as performing mathematical or financial calculations. | **N** | ❌ | 33 | 0 | Marker only. Logic is generated via standard function patterns. |
| `deprecated` | Indicates that a type, attribute, or function is slated for removal. | **N** | ❌ | 3 | 0 | Should map to `@deprecated` decorator or Pydantic `Field(deprecated=True)`. |
| `rootType` | Identifies a type as a primary entry point or top-level document in the model. | **?** | ❌ | 16 | 42 | May require global registration or different serialization entry points. |
| `qualification` | Marks a function used to classify or identify a specific product or event. | **?** | ❌ | 119 | 0 | Used extensively in CDM (`Qualify_*` functions). May require registration in a qualification engine. |
| `projection` | Marks a function intended to project data into reporting or external formats. | **?** | ❌ | 0 | 32 | A transform annotation in the Rune grammar (`[projection <schema>]`), like `ingest` and `enrich`. May require registration in a projection registry. |
| `nonReportable` | DRR-specific annotation marking a rule as excluded from reporting output. | **?** | ❌ | 0 | 41 | DRR-only. Declared in DRR's `regulation-common-type.rosetta`. No CDM equivalent. |
| `synonym` | Maps a type or attribute to an equivalent concept in an external standard (e.g., FpML, FIX). | **N** | ❌ | 0 | 0 | No longer part of the Rune language (rune-dsl 10.16.0 has no synonym construct), and neither CDM nor DRR uses it. Kept here for reference only. |
| `ruleReference` | Links a report attribute to the reporting rule that populates it. | **?** | ❌ | 0 | 2194 | A grammar construct (`[ruleReference <rule>]` or `[ruleReference empty]`), applied throughout DRR's report types. Needed once `report` and `reporting rule` are supported (see Reporting Components). |
| `docReference` | Links an element to standard documentation (e.g., ISDA definitions, regulations). | **N** | ❌ | 504 | 6404 | A grammar construct, used widely in both CDM (e.g. `[docReference ISDA CSA ...]`) and DRR. Documentation only. Could be added to docstrings. |
| `creation` | Marks a function involved in state transitions or the creation of new business events. | **N** | ❌ | 8 | 0 | Declared in CDM (`event-workflow-func.rosetta`) and applied to CDM functions such as `[creation WorkflowStep]` and `[creation BusinessEvent]`. Marker only. |
| `ingest` | Marks a function used for parsing or ingesting external data formats. | **?** | ❌ | 2 | 1 | A transform annotation in the Rune grammar, naming the source schema (e.g. `[ingest fpml.fpml514ConfirmationXML]`). Note: `cdm.ingest.*` is a namespace naming convention, not use of this annotation. |
| `enrich` | Marks a function used to add or derive additional data for an existing object. | **?** | ❌ | 0 | 2 | A transform annotation in the Rune grammar, used as `[enrich]` in DRR. Note: `enrichment` appearing in DRR is a field/type name, not this annotation. |

**Legend** (this table and Core Rosetta Syntax):

- ✅ : Fully supported and integrated into generated code.
- ⚠️ : Partially supported; the details say what is missing.
- ❌ : Not supported. The generator ignores it, or fails where the details say so.
- **Y** : Critical for functionality or code correctness.
- **N** : Informational only; doesn't affect Python runtime logic.
- **?** : Requirement depends on future runtime integration architecture.

## Core Rosetta Syntax

The following table tracks implementation status for core Rosetta language features.

| Syntax Feature | Needs Generator Support? | Status | CDM Usage | DRR Usage | Implementation Details |
| :--- | :---: | :---: | :---: | :---: | :--- |
| **`typeAlias`** (basic) | **N** | ✅ | 17 | 70 | The alias is expanded to its underlying base type at generation time (e.g., `typeAlias ISIN: string` → field typed as `str`), as in the Java generator. The domain name is not preserved in the output. See [ARCHITECTURE.md](ARCHITECTURE.md#6-design-decision-typealias-generation) for the rationale and the Rune Path approach, which would preserve it. |
| **`typeAlias`** (with `condition`) | **Y** | ⚠️ | 1 (`FpMLCodingScheme`) | 0 | Basic type constraints (pattern, min/max length, numeric ranges) on a `typeAlias` **are** propagated — the alias is stripped to its underlying base type and `processProperties` extracts those constraints. However, named `condition` blocks are silently discarded. In CDM, the only `typeAlias` with a named condition is `FpMLCodingScheme`, used exclusively for FpML codelist domain validation (calls `ValidateFpMLCodingSchemeDomain`, which depends on the native `LoadCodeList`). See Known Validation and Runtime Gaps below. |
| **`extends`** (Functions) | **Y** | ❌ | 0 | 0 | Inherits shortcuts, logic, and structure from a base function. Defined in Rune grammar (`func ... extends ...`). The generator ignores `extends`: the function is generated from its own declaration only, and nothing is inherited from the base function. Not currently used in CDM or DRR, but must be handled when encountered. |
| **`super`** (Calls) | **Y** | ❌ | 0 | 0 | Invokes logic from a parent function within the current scope (`RosettaSuperCall` in Rune grammar). Generation fails with `UnsupportedOperationException` when one is encountered. Not currently used in CDM or DRR. |

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

## Known Validation and Runtime Gaps

The following are known gaps in validation and runtime behaviour. Each requires work in the generator, the [rune-python-runtime](https://github.com/finos/rune-python-runtime), or both, as described.

### `typeAlias` Named Condition Blocks

Rune allows two kinds of constraints on a `typeAlias`:

1. **Basic type constraints** — pattern, min/max length, numeric ranges — declared on the underlying primitive (e.g., `RStringType`, `RNumberType`). These **are** supported: the generator strips the alias to its base type via `stripFromTypeAliases` and `processProperties` extracts the constraints into the generated Pydantic field.

2. **Named `condition` blocks** — explicit business-logic conditions attached to the alias body:

```rosetta
typeAlias FpMLCodingScheme(domain string):
    string

    condition IsValidCodingScheme:
        if item exists
        then ValidateFpMLCodingSchemeDomain(item, domain)
```

Named condition blocks are silently discarded. The alias is stripped to its base type and no validation is generated for the condition.

**CDM usage**: The only `typeAlias` in CDM that carries a named condition is `FpMLCodingScheme`, a parameterized alias used exclusively for FpML codelist domain validation. All leaf aliases (`BusinessCenter`, `SettlementRateOption`, etc.) are simple instantiations of `FpMLCodingScheme` with a specific domain string and do not carry their own conditions. The condition calls `ValidateFpMLCodingSchemeDomain`, which depends on the native function `LoadCodeList`, so generating the condition is not enough on its own: validation would raise `NotImplementedError` until a Python implementation of `LoadCodeList` exists. See Codelist Validation below.

**Impact**: Any `typeAlias` with a named condition silently skips that validation in the generated Python. Addressing this requires either generating a constrained wrapper type (analogous to Pydantic's `Annotated[str, ...]` with a custom validator) or implementing the Rune Path so that the alias and its conditions are preserved in the output.

### Codelist Validation

In CDM, the validation of codelist values is driven by the `FpMLCodingScheme` type alias condition described above, not by `[metadata scheme]`. The Java implementation works as follows:

- `ValidateFpMLCodingSchemeDomain` calls `LoadCodeList`, a `[codeImplementation]` function.
- `CdmRuntimeModule` binds it to `LoadCodeListImpl`, which reads `codelist/json/<domain>-N-M.json` from the classpath and caches it.
- The generated data rule `FpMLCodingSchemeIsValidCodingScheme` runs only through `RosettaTypeValidator`. Construction, serialization and deserialization never run it.
- The `@scheme` value on the attribute is ignored. A value is always checked against the fixed CDM list for that alias's domain.

**CDM usage**: CDM defines 15 aliases on `FpMLCodingScheme`, one per FpML codelist domain. Only `BusinessCenter` and `SettlementRateOption` are currently used as attribute types. The others are superseded by a parallel enum (for example, `CreditSeniorityEnum`), shadowed by a CDM type with the same name (`FloatingRateIndex`), or appear only in FpML ingestion. The CDM codelist authors plan to migrate further reference-data enums to codelists, so the number of affected attributes is expected to grow well beyond the current 15 domains.

The Python output has three separate gaps:

1. **The condition is not generated.** See `typeAlias` Named Condition Blocks above.
2. **`LoadCodeList` has no Python implementation.** The generated stub calls `rune_execute_native(...)`. No native module is registered, so calling it, or calling `ValidateFpMLCodingSchemeDomain`, raises `NotImplementedError`. An application can register its own implementation with `rune_register_native()`.
3. **The codelist JSON files are not packaged** in the Python distribution.

**Impact**: Python validation accepts any string for a codelist-typed attribute, including on `rune_serialize`/`rune_deserialize` with their default validation. Java reports a `DATA_RULE` failure, but only when the validator is run. The gap is small today, but every enum migrated to a codelist moves a field from "bad values rejected at construction and on read" (the Python enum behaviour) to "bad values accepted". So the parity gap widens with each migration unless all three gaps are closed. Fixing gap 1 alone would cause validation to raise `NotImplementedError` until gaps 2 and 3 are addressed.

---

## Rosetta Expression Support

All Rosetta/Rune expression types are supported by the Python code generator except `super` calls (`RosettaSuperCall`; see Core Rosetta Syntax). The table below is provided as a reference for contributors.

| Feature | Handled | Description |
| :--- | :---: | :--- |
| **ArithmeticOperation** | ✅ | Binary arithmetic (+, -, *, /) over two expressions. |
| **AsOperation** | ✅ | The `as` operator (Rune DSL v10). Choice narrowing emits `rune_resolve_attr(arg, "OptionName")` (or `rune_resolve_attr_values(arg, "OptionName")` for multi-valued arguments); data type narrowing emits an `isinstance` check via the walrus operator, returning the narrowed value or `None` (or a filtered list comprehension for multi-valued arguments). |
| **AsKeyOperation** | ✅ | Emits `Reference(arg)` (or a list comprehension for multi-valued arguments). The DSL restricts `as-key` to attributes annotated with `[metadata reference]`, and the operand is always a keyed complex type (`[metadata key]`) — never a bare primitive. `Reference(obj)` calls `get_or_create_key()` on the live object, which is the correct runtime behaviour. `as-key` is purely assignment-side syntax (store by reference, not by value); the operand is always already in scope, so no graph-walking is needed. |
| **ChoiceOperation** | ✅ | Handled within type conditions (via `rune_check_one_of`). |
| **ComparisonOperation** | ✅ | Binary comparisons (e.g., <, <=, >, >=). |
| **DefaultOperation** | ✅ | Binary fallback (use right when left is missing/empty). |
| **DistinctOperation** | ✅ | Unary list operation removing duplicates. Filters nulls and propagates `None`. |
| **EqualityOperation** | ✅ | Binary equality/inequality (==, !=). Supports `any` and `all` cardinality modifiers for list operands. |
| **FilterOperation** | ✅ | Filter elements from a list. Supported for both explicit and implicit closure parameters. |
| **FirstOperation** | ✅ | Returns the first non-null element. Returns `None` for empty or null inputs. |
| **FlattenOperation** | ✅ | Unary list operation flattening nested collections. Filters nulls and handles `COWList`. |
| **JoinOperation** | ✅ | Binary operation joining strings (via `str.join`). |
| **LastOperation** | ✅ | Returns the last non-null element. Returns `None` for empty or null inputs. |
| **ListLiteral** | ✅ | Literal list (e.g., `[1, 2, 3]`). An empty `ListLiteral` (the `empty` keyword) generates `None` to represent an absent optional value. |
| **LogicalOperation** | ✅ | Binary logical operations (`and`, `or`). |
| **MapOperation** | ✅ | Projection or mapping of values (via `extract` keyword). Supported for both explicit and implicit parameters. |
| **MaxOperation** | ✅ | Aggregation with null-filtering. Propagates `None` if input is `None`. |
| **MinOperation** | ✅ | Aggregation with null-filtering. Propagates `None` if input is `None`. |
| **OneOfOperation** | ✅ | `x one-of`: exactly one attribute of `x` is set. Generates `rune_check_one_of` over all attributes of the argument's type, taken at generation time. Supported in type conditions and as a general expression in functions. |
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
| **ConditionalExpression** | ✅ | If-then-else logic (via `if_cond_fn`). At the top level of a function body, branches are emitted as pre-hoisted closures (`_then_fnN`, `_else_fnN`). Inside a `map`/`extract`/`filter` lambda, inline `lambda:` expressions are used so the iteration variable is captured correctly. |
| **ConstructorExpression** | ✅ | Constructs an object or dict from key-value pairs. Scalar values assigned to multi-cardinality (`0..*` / `1..*`) attributes are automatically wrapped in a list. |
| **ContainsExpression** | ✅ | Binary "contains" assertion (via `rune_contains`). |
| **CountExpression** | ✅ | Counts list size (via `rune_count`). |
| **DeepFeatureCall** | ✅ | Deep path `x ->> attr` across choice options, via `rune_resolve_deep_attr` on the generated receiver (`item` in a condition on the type itself). |
| **DisjointOperation** | ✅ | Binary "no common elements" assertion (via `rune_disjoint`). |
| **EnumValueReference** | ✅ | Reference to an enumeration value. |
| **ExistsExpression** | ✅ | Unary "exists" assertion (supports `single`/`multiple` modifiers). |
| **FeatureCall** | ✅ | Attribute access (via `rune_resolve_attr`). |
| **ImplicitVariable** | ✅ | Reference to the implicit variable `item`. |
| **LiteralValues** | ✅ | Support for Boolean, Int, Number, String literals. |
| **OnlyElement** | ✅ | Unary list operation (via `rune_get_only_element`). |
| **OnlyExistsExpression** | ✅ | True when exactly the listed attributes are set on the parent object and every other attribute of its type, including inherited ones, is absent (Java `onlyExists`). The parent is the receiver of the first path, or the implicit variable for a bare attribute. Generated inline from `rune_attr_exists`; may move to a runtime helper (#346). |
| **SymbolReference** | ✅ | References to Data types, Enums, Attributes, and shortcuts. |
| **WithMetaOperation** | ✅ | Attaches metadata to a value. A basic type is wrapped in a new `*WithMeta` instance (e.g. `StrWithMeta(value, scheme=...)`), an enum goes through `EnumWithMetaMixin.deserialize` with the `@`-prefixed keys, and a data type has the metadata set in place with `set_meta`. |
| **ReduceOperation** | ✅ | List reduction. Supported for both explicit and implicit closure parameters (with fallback naming). |
