# Development Documentation: Rune to Python Generation Issues

## Resolved Issues

### 1. Inconsistent Numeric Types (Literals and Constraints)
Rune `number` and `int` were handled inconsistently, leading to precision loss and `TypeError` when interacting with Python `Decimal` fields.
*   **Resolution**: Strictly mapped Rune `number` to Python `Decimal`.
*   **Status**: Fixed.
*   **Changed Files**:
    *   `src/main/java/com/regnosys/rosetta/generator/python/util/RuneToPythonMapper.java`
    `RuneToPythonMapper` strictly enforces mapping `number` to `Decimal`. Literals and constraints are wrapped in `Decimal('...')`.

### 2. Redundant Logic Generation
Expression logic was previously duplicated between different generator components.
*   **Resolution**: Centralized all logic in `PythonExpressionGenerator`.
*   **Status**: Fixed. `generateIfBlocks` was removed from the function generator, preventing duplicate Python code for conditional logic.
*   **Changed Files**:
    *   `src/main/java/com/regnosys/rosetta/generator/python/functions/PythonFunctionGenerator.java`

### 3. Missing Function Dependencies (Recursion & Enums)
The dependency provider failed to find imports deeply nested in expressions or referenced via `REnumType`.
*   **Resolution**: Improved recursion in `PythonFunctionDependencyProvider` and added explicit `REnumType` handling.
*   **Status**: Fixed.
*   **Changed Files**:
    *   `src/main/java/com/regnosys/rosetta/generator/python/functions/PythonFunctionDependencyProvider.java`

### 4. Inconsistent Type Mapping (Centralization)
Type string generation was scattered and inconsistent, making it hard to implement features like forward referencing.
*   **Resolution**: Centralized type mapping logic in `RuneToPythonMapper` and added support for optional quoting.
*   **Status**: Fixed.
*   **Changed Files**:
    *   `src/main/java/com/regnosys/rosetta/generator/python/util/RuneToPythonMapper.java`

---

## Unresolved Issues and Proposals

### Issue: Fragile Object Building (Direct Constructors)
**Problem**: The generator relies on a magic `_get_rune_object` helper which bypasses IDE checks and is hard to debug.
*   **Recommendation**: Refactor `PythonFunctionGenerator` to use direct Python constructor calls (e.g., `MyClass(attr=val)`).
*   **Status**: **Unresolved**. The codebase currently uses `_get_rune_object`.

### Issue: Constructor Keyword Arguments SyntaxError
**Problem**: Python forbids duplicate or invalid keyword arguments.
*   **Recommendation**: Use unique counters for missing/duplicate keys.
*   **Proposed Fix**: The generator should use unique fallback keys (`unknown_0`, `unknown_1`, etc.) when property names are missing or invalid.
*   **Recommended Code Changes**:
    *   **In `PythonExpressionGenerator.java`**:
        ```java
        final java.util.concurrent.atomic.AtomicInteger unknownCounter = new java.util.concurrent.atomic.AtomicInteger(0);
        // ... inside the stream mapping:
        String k = (pair.getKey() == null || pair.getKey().getName() == null)
                ? "unknown_" + unknownCounter.getAndIncrement()
                : pair.getKey().getName();
        ```

### Issue: Partial Object Construction (Required Fields)
**Problem**: Pydantic's default constructor enforces validation immediately, breaking multi-step `set` operations.
*   **Recommendation**: Use `model_construct()`.
*   **Proposed Solution**: Use `model_construct(**kwargs)` for initial object creation to skip validation, allowing the object to be filled via subsequent `set` calls before final consumption.

### Issue: Circular Dependencies (The "Topological Sort" Limitation)
Circular dependencies in the generated Python code (e.g., Type A depends on Type B, and B depends on A) cause `NameError` definition issues if strict class references are used.

**Problem**: The generator uses a topological sort that fails on cyclic dependencies (e.g., recursive types), leading to `NameError`.
*   **Recommendation**: Use **String Forward References** and **`model_rebuild()`**.
*   **Mechanism**: Types are emitted as string literals (e.g., `field: "Type"`) inside the shared `_bundle.py`. At the end of the module, `Class.model_rebuild()` is called for every defined class.
*   **Rationale**:
    *   **Strict Type Checking**: String references are valid Pydantic/MyPy annotations that resolve to the actual class. This restores full static analysis capabilities.
    *   **Performance**: Computation is shifted to import time (rebuild) rather than access time (lazy validation), resulting in faster runtime performance.
    *   **Standardization**: Aligns with standard Pydantic v2 patterns for self-referencing models.

*   **Recommended Code Changes**:
    *   **In `PythonAttributeProcessor.java`**: Wrap type hints in quotes.
        ```java
        // Change from:
        lineBuilder.append(attrName).append(": ").append(pythonType);
        // To:
        lineBuilder.append(attrName).append(": ").append("\"").append(pythonType).append("\"");
        ```
    *   **In `PythonCodeGenerator.java`**: Collect all class names and trigger Pydantic's rebuild at the end of the `_bundle.py`.
        ```java
        for (String className : generatedClasses) {
            bundleWriter.appendLine(className + ".model_rebuild()");
        }
        ```

### Issue: Metadata Architecture (Type Unification)
**Problem**: Current code generates separate `WithMeta` classes which bloats the API and forces users to toggle between `.value` and raw access.
*   **Recommendation**: **Full Type Unification** using dynamic `__dict__` storage.
*   **Proposed Fix**:
    *   **Mechanism**: Every data class inherits from `BaseDataClass`, which uses `BaseMetaDataMixin` to store metadata (like `@key` or `@ref`) dynamically in the object's `__dict__`.
    *   **Benefits**: Cleaner API, Deep Path Consistency, Standard Pydantic compatibility.
*   **Recommended Code Changes**:
    *   **In `RuneToPythonMapper.java`**: Remove all `getAttributeTypeWithMeta` switching logic.
    *   **In `PythonAttributeProcessor.java`**: Simplify attribute generation to always use the base type, trusting the mixin to handle metadata.

### Issue: Enum Metadata Support
**Problem**: Python `Enum` cannot hold metadata attributes natively.
*   **Recommendation**: Wrap enums in a proxy class.
*   **Implementation**: Create an `_EnumWrapper(BaseDataClass)` that holds the enum value and the metadata dictionary, allowing enums to have keys and references.

---

## Runtime Library Requirements

The **Proposed** architecture (from the parked changes) relies on specific features in the `rune-runtime` library (v1.0.19+).

### 1. Type Unification Support
*   **Requirement**: `BaseDataClass` must inherit from `BaseMetaDataMixin`.
*   **Purpose**: Allows any data object to dynamically store metadata (like `@key` or `@ref`) in `__dict__` without polluting the Pydantic model fields or requiring a separate "WithMeta" class.

### 2. Operator Support
*   **Requirement**: `rune_with_meta` and `rune_default` helper functions.
*   **Purpose**: `rune_with_meta` allows attaching metadata to an object. `rune_default` safely returns a default value if an optional field is None.

### 3. Enum Wrappers
*   **Requirement**: Runtime support for wrapping Python Enums to attach metadata.
