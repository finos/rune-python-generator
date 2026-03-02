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
| **`Function Overloading`** | **Y** | ✅ | Specialized versions of functions with dispatch via `match` (supported for enums). |
| **`Circular References`** | **Y** | ✅ | Handled with Pydantic forward references (string-based) in models. |

## Rosetta Expression Support

The following table tracks support for Rosetta/Rune expressions within the Python code generator.

| Feature | Handled | Description |
| :--- | :---: | :--- |
| **ArithmeticOperation** | ✅ | Binary arithmetic (+, -, *, /) over two expressions. |
| **AsKeyOperation** | ✅ | Treat an argument as a key (generates a key dict). |
| **ChoiceOperation** | ✅ | Handled within type conditions (via `rune_check_one_of`). |
| **ComparisonOperation** | ✅ | Binary comparisons (e.g., <, <=, >, >=). |
| **DefaultOperation** | ✅ | Binary fallback (use right when left is missing/empty). |
| **DistinctOperation** | ✅ | Unary list operation removing duplicates (via `set()`). |
| **EqualityOperation** | ✅ | Binary equality/inequality (==, !=). |
| **FilterOperation** | ✅ | Unary list operation keeping elements that satisfy a predicate. |
| **FirstOperation** | ✅ | Unary list operation returning the first element (`[0]`). |
| **FlattenOperation** | ✅ | Unary list operation flattening a list of lists. |
| **JoinOperation** | ✅ | Binary operation joining strings (via `str.join`). |
| **LastOperation** | ✅ | Unary list operation returning the last element (`[-1]`). |
| **ListLiteral** | ✅ | Literal list (e.g., `[1, 2, 3]`). |
| **LogicalOperation** | ✅ | Binary logical operations (`and`, `or`). |
| **MapOperation** | ✅ | Unary list operation transforming each element via a function. |
| **MaxOperation** | ✅ | Selecting the maximum element (via `max()`). |
| **MinOperation** | ✅ | Selecting the minimum element (via `min()`). |
| **OneOfOperation** | ✅ | Handled within type conditions (via `rune_check_one_of`). |
| **ReverseOperation** | ✅ | Unary list operation reversing element order (via `reversed()`). |
| **SortOperation** | ✅ | Sorting list elements (via `sorted()`). |
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
| **ReduceOperation** | ✅ | Functional list operation reducing elements via a function (via `functools.reduce`). |

### Outstanding Expression Gaps

1. **Standard Library Refinements**: While `min`/`max`/`sum` are implemented using Python built-ins, full Rosetta compatibility (e.g. handling of empty lists vs nothing) might require custom `rune_*` wrappers.
2. **Complex Nested lambdas**: While supported, performance and readability impact of heavy lambda nesting in pipelines should be monitored.
