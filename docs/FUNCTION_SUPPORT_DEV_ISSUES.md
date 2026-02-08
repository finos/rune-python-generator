# Development Documentation: Function Support Dev Issues

## Resolved Issues

### 1. Inconsistent Numeric Types (Literals and Constraints)
Rune `number` and `int` were handled inconsistently, leading to precision loss and `TypeError` when interacting with Python `Decimal` fields in financial models.

*   **Resolution**: Strictly mapped Rune `number` to Python `Decimal`. Literals and constraints are now explicitly wrapped in `Decimal('...')` during generation.
*   **Status**: Fixed ([`fe798c1`](https://github.com/finos/rune-python-generator/commit/fe798c1))
*   **Summary of Impact**:
    *   **Precision**: Ensures that calculations involving monetary amounts maintain exact precision, avoiding the pitfalls of floating-point arithmetic.
    *   **Type Safety**: Prevents runtime crashes when Pydantic models expect `Decimal` but receive `float` or `int`.

### 2. Redundant Logic Generation
Expression logic was previously duplicated between different generator components, leading to maintenance overhead and potential diverging implementations of the same DSL logic.

*   **Resolution**: Centralized all expression logic within `PythonExpressionGenerator`. Removed `generateIfBlocks` from the higher-level function generator to prevent duplicate emission of conditional statements.
*   **Status**: Fixed ([`2fb276d`](https://github.com/finos/rune-python-generator/commit/2fb276d))
*   **Summary of Impact**:
    *   **Maintainability**: Logic changes (like the Switch fix) only need to be implemented in one place.
    *   **Code Quality**: The generated Python is cleaner and follows a predictable pattern for side-effecting blocks.

### 3. Missing Function Dependencies (Recursion & Enums)
The dependency provider failed to identify imports for types deeply nested in expressions or those referenced via `REnumType`.

*   **Resolution**: Implemented recursive tree traversal in `PythonFunctionDependencyProvider` and added explicit handling for Enum types to ensure they are captured in the import list.
*   **Status**: Fixed ([`4878326`](https://github.com/finos/rune-python-generator/commit/4878326))
*   **Summary of Impact**:
    *   **Runtime Stability**: Resolves `NameError` exceptions in generated code where functions used types that were not imported at the top of the module.
    *   **Enum Integration**: Functions can now safely use Rosetta-defined enums in conditions and assignments.

### 4. Inconsistent Type Mapping (Centralization)
Type string generation was scattered across multiple classes, making it impossible to implement global features like string forward-referencing or custom cardinality formatting.

*   **Resolution**: Centralized type mapping and formatting in `RuneToPythonMapper`, adding a flexible `formatPythonType` method that handles both legacy and modern typing styles.
*   **Status**: Fixed ([`fe798c1`](https://github.com/finos/rune-python-generator/commit/fe798c1))
*   **Summary of Impact**:
    *   **Extensibility**: Enabled the groundwork for "String Forward References" (quoted types). *Note: This is an optional feature whose widespread usage is currently still under consideration.*
    *   **Cardinality Control**: Standardized how `list[...]` and `Optional[...]` (or `| None`) are generated across all object and function attributes.

### 5. Switch Expression Support
`generateSwitchOperation` returned a block of statements (including `def` and `if/else`) instead of a single expression, causing a `SyntaxError` during variable assignment (e.g., `var = if x: ...`).

*   **Resolution**: Encapsulated switch logic within a unique helper function (closure) and returned a call to that function as the expression string (e.g., `_switch_fn_0()`). This ensures the output is always a valid Python expression.
*   **Status**: Fixed ([`4d394c5`](https://github.com/finos/rune-python-generator/commit/4d394c5))
*   **Summary of Impact**:
    *   **Aliases & Operations (`var = expr`)**: Now receive a valid function call string. The helper function definition is emitted *before* the assignment by the orchestrating generator.
    *   **Conditions (`return expr`)**: Now return the function call, with the definition emitted before the return statement.
    *   **Consistency**: Unifies the behavior of `SwitchOperation` with `IfThenElseOperation`, ensuring all complex logic blocks are handled via the `ifCondBlocks` side-effect mechanism.

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
*   **Recommended Code Changes**: Use an `AtomicInteger` for unique fallback keys in `PythonExpressionGenerator.java`.

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
    *   **Strict Type Checking**: String references are valid Pydantic/MyPy annotations that resolve to the actual class.
    *   **Performance**: Computation is shifted to import time (rebuild) rather than access time.
    *   **Standardization**: Aligns with standard Pydantic v2 patterns.

### Issue: Metadata Architecture (Type Unification)
**Problem**: Current code generates separate `WithMeta` classes which bloats the API and forces users to toggle between `.value` and raw access.
*   **Recommendation**: **Full Type Unification** using dynamic `__dict__` storage.
*   **Proposed Fix**:
    *   **Mechanism**: Every data class inherits from `BaseDataClass`, which uses `BaseMetaDataMixin` to store metadata dynamically in the object's `__dict__`.
    *   **Benefits**: Cleaner API, Deep Path Consistency, Standard Pydantic compatibility.

### Issue: Enum Metadata Support
**Problem**: Python `Enum` cannot hold metadata attributes natively.
*   **Recommendation**: Wrap enums in a proxy class.
*   **Implementation**: Create an `_EnumWrapper(BaseDataClass)` that holds the enum value and the metadata dictionary, allowing enums to have keys and references.

---

## Runtime Library Requirements

The **Proposed** architecture (from the parked changes) relies on specific features in the `rune-runtime` library (v1.0.19+).

### 1. Type Unification Support
*   **Requirement**: `BaseDataClass` must inherit from `BaseMetaDataMixin`.
*   **Purpose**: Allows any data object to dynamically store metadata without polluting Pydantic model fields.

### 2. Operator Support
*   **Requirement**: `rune_with_meta` and `rune_default` helper functions.

### 3. Enum Wrappers
*   **Requirement**: Runtime support for wrapping Python Enums to attach metadata.
