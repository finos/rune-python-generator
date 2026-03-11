# Rosetta Language Gaps in Rune-Python Generator

This document tracks implementation gaps and status for various Rune language features within the Python code generator.

## Annotation Support

The following table tracks support for Rosetta/Rune annotations.

| Annotation Type | Description | Needs Generator Support? | Status | Implementation Details |
| :--- | :--- | :---: | :---: | :--- |
| **`metadata`** | Assigns special metadata behaviors (e.g., references, schemes) to fields or types. | **Y** | ✅ | Generates `_ALLOWED_METADATA`, `_KEY_REF_CONSTRAINTS`, and `Annotated[...]` types for Pydantic. |
| **`codeImplementation`** | Indicates the function logic is implemented externally in the host language. | **Y** | ✅ | Redirects function generation to `rune_execute_native` and registers in `native_registry`. Extensively used in DRR. |
| `calculation` | Marks a function as performing mathematical or financial calculations. | **N** | ❌ | Marker for informational purposes. Logic is generated via standard function patterns. Extensively used in DRR. |
| `deprecated` | Indicates that a type, attribute, or function is slated for removal. | **Y** | ❌| Should ideally map to `@deprecated` decorator or Pydantic `Field(deprecated=True)`. |
| `rootType` | Identifies a type as a primary entry point or top-level document in the model. | **?** | ❌ | May require global registration or different serialization entry points. |
| `qualification` | Marks a function used to classify or identify a specific product or event. | **?** | ❌ | Purpose marker (CDM). May require registration in a qualification engine. Extensively used in DRR. |
| `ingest` | Marks a function used for parsing or ingesting external data formats. | **?** | ❌ | Purpose marker (CDM). May require registration in an ingestion registry. Extensively used in DRR. |
| `projection` | Marks a function intended to project data into reporting or external formats. | **?** | ❌ | Purpose marker (CDM). May require registration in a projection registry. Extensively used in DRR. |
| `enrich` | Marks a function used to add or derive additional data for an existing object. | **?** | ❌ | Purpose marker (CDM). Extensively used in DRR. |
| `synonym` | Maps a type or attribute to an equivalent concept in an external standard (e.g., FpML). | **?** | ❌ | Mapping to external formats. Often ignored in the core logic path. |
| `ruleReference` | Links an element to a specific regulatory or institutional business rule. | **N** | ❌ | Primarily for traceability/reporting. Could be added to docstrings. |
| `docReference` | Links an element to standard documentation (e.g., ISDA definitions). | **N** | ❌ | Primarily for documentation. Could be added to docstrings. |
| `creation` | Marks a function involved in state transitions or the creation of new business events. | **N** | ❌ | CDM-specific workflow marker. |

**Legend:**
- ✅ : Fully supported and integrated into generated code.
- ❌ : Currently ignored by the generator.
- **Y** : Critical for functionality or code correctness.
- **N** : Informational only; doesn't affect Python runtime logic.
- **?** : Requirement depends on future runtime integration architecture.

## Core Rosetta Syntax

The following table tracks implementation status for core Rosetta language features.

| Syntax Feature | Needs Generator Support? | Status | Implementation Details |
| :--- | :---: | :---: | :--- |
| **`extends`** (Functions) | **Y** | ❌ | Inherits shortcuts, logic, and structure from a base function. Java implementation uses compositional approaches and shortcut inheritance. |
| **`super`** (Calls) | **Y** | ❌ | Invoking logic from a parent function within the current scope. Used in DRR. |

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
| **AsKeyOperation** | ⚠️ | Partial. Treat an argument as a key. Currently maps to `{expr: True}`. CDM integration for full key/reference lifecycle is pending. Present in DRR. |
| **ChoiceOperation** | ✅ | Handled within type conditions (via `rune_check_one_of`). |
| **ComparisonOperation** | ✅ | Binary comparisons (e.g., <, <=, >, >=). |
| **DefaultOperation** | ✅ | Binary fallback (use right when left is missing/empty). |
| **DistinctOperation** | ✅ | Unary list operation removing duplicates (via `set()`). |
| **EqualityOperation** | ✅ | Binary equality/inequality (==, !=). |
| **FilterOperation** | ✅ | Filter elements from a list. Supported for both explicit and implicit closure parameters. |
| **FirstOperation** | ⚠️ | Partial. Returns the first element (`[0]`). Currently crashes on empty or null inputs. |
| **FlattenOperation** | ✅ | Unary list operation flattening a list of lists. |
| **JoinOperation** | ✅ | Binary operation joining strings (via `str.join`). |
| **LastOperation** | ⚠️ | Partial. Returns the last element (`[-1]`). Currently crashes on empty or null inputs. |
| **ListLiteral** | ✅ | Literal list (e.g., `[1, 2, 3]`). |
| **LogicalOperation** | ✅ | Binary logical operations (`and`, `or`). |
| **MapOperation** | ✅ | Projection or mapping of values (via `extract` keyword). Supported for both explicit and implicit parameters. |
| **MaxOperation** | ⚠️ | Partial. Supported for closure-based keys, but crashes if the input list contains `None` (nothing). |
| **MinOperation** | ⚠️ | Partial. Supported for closure-based keys, but crashes if the input list contains `None` (nothing). |
| **OneOfOperation** | ✅ | Supported both in type conditions and as a general expression in functions (Dynamic attribute detection). |
| **ReverseOperation** | ✅ | Unary list operation reversing element order (via `reversed()`). |
| **SortOperation** | ⚠️ | Partial. Supported for closure-based keys, but crashes if the input list contains `None` (nothing). |
| **SumOperation** | ⚠️ | Partial. Returns `0` for empty/null lists (may mismatch Rosetta `nothing`) and crashes on lists containing `None`. |
| **SwitchOperation** | ✅ | Conditional selection among cases (via helper functions). |
| **ThenOperation** | ✅ | Unary functional pipeline step (via lambda). |
| **ToDateOperation** | ✅ | Conversion/Parse to date. |
| **ToDateTimeOperation** | ✅ | Conversion/Parse to date-time. |
| **ToEnumOperation** | ✅ | Conversion/Parse to an enum. |
| **ToIntOperation** | ✅ | Conversion to integer. |
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

### Outstanding Expression Gaps

1. **`as-key` Full Semantics**: Current implementation is a dictionary literal placeholder. Full integration with the CDM workflow (where `as-key` signals persistence or cross-referencing) requires runtime support.
   ```rosetta
   // Exposes semantic gap in AsKeyOperation
   func MarkAsKey:
       inputs: trade Trade (1..1)
       output: tradeKey Trade (1..1)
       set tradeKey:
           trade as-key // Currently generates {trade: True}, missing reference semantics
   ```

2. **Null-Safety & "Nothing" Propagation**: While `sum`, `max`, and `min` are implemented using Python built-ins, the generator currently lacks robust handling for Rosetta's "nothing" (null) semantics as defined in the [Rune Modelling Components](https://github.com/finos/rune-dsl/blob/master/docs/rune-modelling-component.md) specification. 

    *Note: Structural definitions in `Rosetta.xtext` and `RosettaExpression.xcore` align with the documentation, but the behavioral expectations are enforced at the type and generation layers.*

    *   **Collection Nulls**: Python built-ins like `sum()`, `max()`, and `sorted()` crash if a list contains `None`. 
        *   **Rune Expectation (Heading: [`Other List Operator`](https://github.com/finos/rune-dsl/blob/master/docs/rune-modelling-component.md#L1409))**: Aggregations and sorts must filter out nulls and operate over the remaining elements.
    *   **Empty Accessors**: `first` and `last` operations currently use index access (`[0]` and `[-1]`) which crash on empty collections. 
        *   **Rune Expectation (Heading: [`Other List Operator`](https://github.com/finos/rune-dsl/blob/master/docs/rune-modelling-component.md#L1409))**: Return "nothing" (None) when the collection is empty.
    *   **Scalar Propagation**: The `sum` of "nothing" (null input) currently returns `0` via `sum(arg or [])`. 
        *   **Rune Expectation (Headings: [`Null`](https://github.com/finos/rune-dsl/blob/master/docs/rune-modelling-component.md#L922), [`List`](https://github.com/finos/rune-dsl/blob/master/docs/rune-modelling-component.md#L1194))**: Propagate "nothing" (None) for all scalar results where the input collection is null/nothing. An empty sum is only `0` if the collection exists but has no elements after filtering.

3. **List Comparison Semantics**: Several implementation details in `rune-python-runtime` violate the formal comparison rules:
    *   **The `_ntoz` (None to Zero) Violation**: The runtime converts `None` to `0` for comparisons.
        *   **Rune Expectation (Heading: [`Comparison Operator and Null`](https://github.com/finos/rune-dsl/blob/master/docs/rune-modelling-component.md#L990))**: `null = any value` must be `false` (including `null = null`). Python currently returns `true` for `None == None` and `None == 0`.
    *   **Pairwise Inequality (`<>`)**: The generator currently calls `rune_any_elements` for list inequality, which performs a **segment-wise Cartesian product** check (`any(x != y for ...)`).
        *   **Rune Expectation (Heading: [`List Comparison`](https://github.com/finos/rune-dsl/blob/master/docs/rune-modelling-component.md#L1382))**: `<>` must return `true` if lists have different lengths OR if any pairwise items differ. `rune_any_elements` ignores list length and relative order entirely.

