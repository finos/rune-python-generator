# Rosetta Language Gaps in Rune-Python Generator

This document tracks implementation gaps and status for various Rune language features within the Python code generator.

## Annotation Support

The following table tracks support for Rosetta/Rune annotations.

| Annotation Type | Needs Generator Support? | Status | Implementation Details |
| :--- | :---: | :---: | :--- |
| **`metadata`** | **Y** | ✅ | Generates `_ALLOWED_METADATA`, `_KEY_REF_CONSTRAINTS`, and `Annotated[...]` types for Pydantic. |
| **`codeImplementation`** | **Y** | ✅ | Redirects function generation to `rune_execute_native` and registers in `native_registry`. |
| `calculation` | **N** | ❌ | Marker for informational purposes. Logic is generated via standard function patterns. |
| `deprecated` | **Y** | ❌| Should ideally map to `@deprecated` decorator or Pydantic `Field(deprecated=True)`. |
| `rootType` | **?** | ❌ | May require global registration or different serialization entry points. |
| `qualification` | **?** | ❌ | Purpose marker (CDM). May require registration in a qualification engine. |
| `ingest` | **?** | ❌ | Purpose marker (CDM). May require registration in an ingestion registry. |
| `projection` | **?** | ❌ | Purpose marker (CDM). May require registration in a projection registry. |
| `enrich` | **?** | ❌ | Purpose marker (CDM). |
| `synonym` | **?** | ❌ | Mapping to external formats. Often ignored in the core logic path. |
| `ruleReference` | **N** | ❌ | Primarily for traceability/reporting. Could be added to docstrings. |
| `docReference` | **N** | ❌ | Primarily for documentation. Could be added to docstrings. |
| `creation` | **N** | ❌ | CDM-specific workflow marker. |

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
| **`super`** (Calls) | **Y** | ❌ | Invoking logic from a parent function within the current scope. |

## Rosetta Expression Support

The following table tracks support for Rosetta/Rune expressions within the Python code generator.

| Feature | Handled | Description |
| :--- | :---: | :--- |
| **ArithmeticOperation** | ✅ | Binary arithmetic (+, -, *, /) over two expressions. |
| **AsKeyOperation** | ⚠️ | Partial. Treat an argument as a key. Currently maps to `{expr: True}`. CDM integration for full key/reference lifecycle is pending. |
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
| **MapOperation** | ⚠️ | Partial. Mapped to `extract` keyword. Supported for explicit parameters, but fails for implicit parameters. |
| **MaxOperation** | ⚠️ | Partial. Standard `max(arg)` is supported, but closure-based keys (`max [ item -> ... ]`) used in CDM are currently ignored. |
| **MinOperation** | ⚠️ | Partial. Standard `min(arg)` is supported, but closure-based keys used in CDM are currently ignored. |
| **OneOfOperation** | ⚠️ | Partial. Supported in type conditions, but unsupported as a general expression in functions (used in CDM). |
| **ReverseOperation** | ✅ | Unary list operation reversing element order (via `reversed()`). |
| **SortOperation** | ⚠️ | Partial. Standard `sorted(arg)` is supported, but closure-based sorting (`sort [ item -> ... ]`) used in CDM is ignored. |
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

1. **Closures in Aggregations (CDM Gap)**: Operations like `sort`, `max`, and `min` frequently use closure blocks (e.g., `sort [ item -> unit -> currency ]`) in CDM. The generator currently ignores these blocks and produces standard Python `sorted()`/`max()` calls.
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

