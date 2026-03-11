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

## 3. [FIXED] Defensive Null-Safety in Operations

In Rosetta/Rune DSL, "nothing" (null) values are common when navigation paths evaluate to empty lists. Previously, standard Python functions like `max()` or `sum()` would crash when encountering an empty or missing list.

### Changes:
- **Resilient Built-ins**: The generator now emits defensive patterns for all list-based operations:
    - `max(...) or [], default=None`: Returns `None` if the input is empty or null, instead of raising a `ValueError`.
    - `min(...) or [], default=None`: Returns `None` if the input is empty or null.
    - `sorted(...) or []`: Gracefully handles null inputs by return an empty list.
    - `sum(...) or []`: Gracefully handles null inputs.
    - `list(map(..., arg or []))`: Ensures `map` operations don't throw on null arguments.
- **Runtime Alignment**: These changes ensure the generated code is resilient when used with the standard Python environment, reducing dependency on custom null-handling runtime functions.

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
