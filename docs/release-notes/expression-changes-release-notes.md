# Expression Changes Release Notes

This document tracks fixes and improvements made to the Rune Python generator's expression handling.

## 1. Support for [FIXED] Implicit Closure Parameters

Rosetta allows `extract / filter [ item -> attr ]` and `reduce [ a + b ]` without naming parameters. Previously, the generator expected explicit parameter declarations and would fail or ignore the closure body.

### Changes:
- **`MapOperation` (extract)**: Automatically fallbacks to an implicit `item` parameter if none is provided.
- **`FilterOperation` (filter)**: Automatically fallbacks to an implicit `item` parameter if none is provided.
- **`ReduceOperation` (reduce)**: 
    - **Fallback Naming**: Defaults lambda arguments to `a` (accumulator) and `b` (item) when model parameters are missing.
    - **Scope Awareness**: Correctly identifies `b` (the item) as the default receiver and shadows the accumulator `a`, preventing `IndexOutOfBoundsException` and ensuring correct name resolution.
- **Receiver Management**: The expression scope now correctly identifies the implicit variable (`item` or user-defined) for all closure-based operations.

### Context:
These patterns are extensively used in **Digital Regulatory Reporting (DRR)** projects, where declarative navigation is preferred over explicit parameter naming.

## 2. Support for [FIXED] Closures in `Max`, `Min`, and `Sort` Operations

Rosetta uses closure blocks with aggregation operations (e.g., `sort [ item -> price -> amount ]`) to define the sorting or selection key.

### Changes:
- **`key=lambda` Generation**: The generator now correctly produces the Python `key=lambda` syntax for these operations.
- **Implicit Parameters**: Supports implicit `item` naming and receiver scoping for the closure block.

## 3. [FIXED] Robust Null-Safety & Scalar Propagation

In Rosetta/Rune DSL, "nothing" (null) values are common when navigation paths evaluate to empty lists. We have implemented full alignment with the Rune specification for collection aggregations and accessors.

### Changes:
- **Null-Filtering**: All collection operations (`sum`, `max`, `min`, `sort`, `reverse`, `distinct`, `count`) now automatically filter out `None` values prior to execution using generator expressions. 
- **Scalar Propagation**: Operations that return a scalar (`sum`, `max`, `min`, `count`) now wrap their logic in a lambda that checks if the input collection itself is `None`. If the input is `None`, the operation returns `None` (propagating "nothing") rather than `0` or an empty default.
- **Resilient Accessors**: 
    - `first` and `last` now use `next(...)` with a default of `None` to ensure they return "nothing" instead of raising `IndexError` on empty collections.
    - `flatten` now uses a universal lambda flattener that handles nested iterables and `COWList` while filtering nulls at every level.
- **Empty Collection Defaults**: If a collection exists but is empty after null-filtering, aggregations return their appropriate defaults (e.g., `None` for `max`/`min`, `0` for `sum`).

### Context:
These changes ensure the generated code strictly follows the [Rune Modeling Components](https://github.com/finos/rune-dsl/blob/master/docs/rune-modelling-component.md) specification regarding null behavior and "nothing" propagation.

## 4. Support for [FIXED] General `one-of` Expressions

The `one-of` operator can be used in Rosetta as a standalone boolean expression (outside of type/data conditions) to verify that exactly one attribute of a complex object is set.

### Changes:
- **Standalone Support**: The generator now treats `one-of` as a first-class expression in functions and shortcuts.
- **Dynamic Attribute Detection**: Utilizing `RosettaTypeProvider`, the generator resolves the argument's type at generation time and extracts all its attributes.
- **Runtime Validation**: Generates calls to `rune_check_one_of(arg, [...attributes])`, ensuring precise validation against the model's schema.
- **Choice Type Support**: Correctly handles `Choice` types by leveraging the `asRDataType()` view to identify all available options.

### Context:
This closes a gap between the CDM (Common Domain Model) usage and generator support, where `one-of` was previously only supported within data-level constraints.

## 5. Improved Generator Architecture (Safety)

To support the above features (which require services like `RosettaTypeProvider`), the generator's internal structure was refined.

- **Isolated Expression Generators**: `PythonFunctionGenerator` and `PythonModelObjectGenerator` now use a Guice `Provider<PythonExpressionGenerator>`.
- **State Isolation**: A fresh generator instance is created for every function or class body. This prevents clashing of stateful variables (like `generatedFunctionCounter` and `ifCondBlocks`) that could lead to naming collisions or incorrect logic when processing large models.
