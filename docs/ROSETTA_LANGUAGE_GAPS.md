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
| **FilterOperation** | ⚠️ | Partial. Supported for explicit parameters, but fails for implicit parameters (no named args in closure). |
| **FirstOperation** | ✅ | Unary list operation returning the first element (`[0]`). |
| **FlattenOperation** | ✅ | Unary list operation flattening a list of lists. |
| **JoinOperation** | ✅ | Binary operation joining strings (via `str.join`). |
| **LastOperation** | ✅ | Unary list operation returning the last element (`[-1]`). |
| **ListLiteral** | ✅ | Literal list (e.g., `[1, 2, 3]`). |
| **LogicalOperation** | ✅ | Binary logical operations (`and`, `or`). |
| **MapOperation** | ⚠️ | Partial. Mapped to `extract` keyword. Supported for explicit parameters, but fails for implicit parameters. Extensively used in DRR (implicit parameters). |
| **MaxOperation** | ⚠️ | Partial. Standard `max(arg)` is supported, but closure-based keys (`max [ item -> ... ]`) used in CDM/DRR are currently ignored. |
| **MinOperation** | ⚠️ | Partial. Standard `min(arg)` is supported, but closure-based keys used in CDM/DRR are currently ignored. |
| **OneOfOperation** | ⚠️ | Partial. Supported in type conditions, but unsupported as a general expression in functions (used in CDM/DRR). |
| **ReverseOperation** | ✅ | Unary list operation reversing element order (via `reversed()`). |
| **SortOperation** | ⚠️ | Partial. Standard `sorted(arg)` is supported, but closure-based sorting (`sort [ item -> ... ]`) used in CDM/DRR is ignored. |
| **SumOperation** | ✅ | Unary list operation summing numeric elements (via `sum()`). |
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
| **ReduceOperation** | ⚠️ | Partial. Supported for explicit parameters (e.g., `reduce a, b [ a + b ]`), but fails for implicit closures (unnamed args). |

### Outstanding Expression Gaps

1. **Closures in Aggregations (CDM/DRR Gap)**: Operations like `sort`, `max`, and `min` frequently use closure blocks (e.g., `sort [ item -> unit -> currency ]`) in CDM and DRR (`digital-regulatory-reporting`). The generator currently ignores these blocks and produces standard Python `sorted()`/`max()` calls.
   ```rosetta
   // Exposes gap in SortOperation/MaxOperation
   func SortItems:
       inputs: items Item (0..*)
       output: sortedItems Item (0..*)
       set sortedItems:
           items sort [ item -> price -> amount ] // Generator ignores the [ ... ] block
   ```

2. **Implicit Closure Parameters**: Rosetta allows `extract [ item -> attr ]` or `reduce [ a + b ]` without naming parameters. The current generator expects parameters to be explicitly declared in the `InlineFunction` and fails with an error if they are missing.
   ```rosetta
   // Exposes gap in MapOperation/ReduceOperation
   func ImplicitClosure:
       inputs: items int (0..*)
       output: result int (0..1)
       set result:
           items 
               extract [ item * 2 ] // Fails: expects explicit 'item' parameter declaration
               reduce [ a + b ]     // Fails: expects explicit 'a, b' parameters
   ```

3. **General `one-of` Expressions**: In CDM, `one-of` is used as a stand-alone expression in functions and shortcuts. The generator only implements this behavior within type conditions.
   ```rosetta
   // Exposes gap in OneOfOperation (Expression)
   func CheckOneOf:
       inputs: msg Message (1..1)
       output: isValid boolean (1..1)
       set isValid:
           msg -> fieldA or msg -> fieldB one-of // Fails: Unsupported general expression
   ```

4. **`as-key` Full Semantics**: Current implementation is a dictionary literal placeholder. Full integration with the CDM workflow (where `as-key` signals persistence or cross-referencing) requires runtime support.
   ```rosetta
   // Exposes semantic gap in AsKeyOperation
   func MarkAsKey:
       inputs: trade Trade (1..1)
       output: tradeKey Trade (1..1)
       set tradeKey:
           trade as-key // Currently generates {trade: True}, missing reference semantics
   ```

5. **Standard Library Refinements**: While `sum` etc. are implemented using Python built-ins, handling of empty lists vs "nothing" in Rosetta may require custom `rune_*` protective wrappers.

