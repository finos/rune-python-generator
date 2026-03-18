# Function Support Release Notes

This document summarizes the significant improvements and architectural changes made to support Rosetta/Rune functions within the Python generator.

## 1. Direct Pydantic Constructors [FIXED]

Previously, the generator relied on a fragile `_get_rune_object` helper that attempted to resolve classes from the global namespace at runtime. This has been replaced with a robust, statically-analysable approach.

### Changes

- **Direct Instantiation**: The generator now emits direct constructor calls (e.g., `MyClass(...)`) for object creation.
- **IDE Transparency**: Modern IDEs can now provide full autocomplete and type-checking for objects constructed inside functions.
- **Namespace Safety**: Removes reliance on the global namespace, preventing collisions in multi-bundle environments.

## 2. Stepwise Object Construction [FIXED]

Rosetta functions often populate an output object through multiple `set` operations. Standard Pydantic constructors enforce validation immediately, which fails if required fields are missing during intermediate steps.

### Changes

- **`ObjectBuilder` Integration**: The generator now utilizes `ObjectBuilder` wrappers for partial object initialization.
- **Deferred Validation**: Objects can be populated field-by-field, or via deep nested paths (`set result -> a -> b`), without triggering `ValidationError` for missing required fields during intermediate steps.
- **Automatic Materialization**: The explicit `.to_model()` call has been removed from generated function bodies. The Rune runtime (`rune_finalize_return` via the `@replaceable` decorator) automatically unwraps and validates `ObjectBuilder` drafts into materialized models upon function return, resulting in cleaner generated code.

## 3. Support for Circular Dependencies [FIXED]

Circular references (e.g., Type A has Type B, and Type B has Type A) are common in complex domain models like the CDM. Previous generator versions were blocked by these cycles.

### Changes

- **String Forward References**: Implemented Pydantic-native string type hints (e.g., `attr: "TypeB"`) to allow Python to parse classes before their dependencies are defined.
- **Refined Topological Sort**: The generator’s DAG now correctly orders definitions based on inheritance and attribute types, falling back to forward references when cycles are detected.
- **`model_rebuild()`**: Automatically generates `model_rebuild()` calls at the end of the bundle to resolve all forward references lazily.

## 4. Native Function Support (`[codeImplementation]`) [FIXED]

Rosetta allows functions to be marked as externally implemented. The generator now provides a standard bridge to these Python implementations.

### Changes

- **`native_registry`**: A centralized registry allows users to map Rosetta function names to Python callables.
- **Dispatcher Logic**: The generator emits calls to `rune_execute_native`, which looks up the implementation and handles execution at runtime.
- **Standardized Signatures**: Native implementations receive standardized arguments, ensuring parity with generated logic.

## 5. Dotted FQN Type Hints [FIXED]

To match the Rosetta/Rune standard, function signatures and type hints have been improved for readability and consistency.

### Changes

- **Namespace Alignment**: Type hints now use fully qualified, period-delimited names as strings (e.g., `val: "cdm.base.datetime.AdjustableDate"`).
- **Cleaner API**: Removes "bundle-mangled" underscore names from public-facing function signatures.

## 6. Standardized Condition Execution [FIXED]

The execution of pre-conditions and post-conditions has been standardized to ensure consistent behavior across model types and standalone functions.

### Changes

- **Registry-Based Execution**: Conditions are registered in a local `_pre_registry` or `_post_registry` and executed via `rune_execute_local_conditions`.
- **Improved Scoping**: Uses `inspect.currentframe()` to provide a reliable `self` context for condition evaluation within function bodies.
