# Development Documentation: Rune to Python Generation Issues

## Issue: Circular Dependencies and Type Checking
Circular dependencies in the generated Python code (e.g., Type A depends on Type B, and B depends on A) cause `NameError` definition issues if strict class references are used.

### Approach 1: Lazy Resolution (Deprecated)
*   **Mechanism**: fields were defined as `Annotated[Any, LazyValidator('Type')]`.
*   **Pros**: Solved definition order crashes.
*   **Cons**: Resulted in the loss of static type safety (`Any`), lack of IDE autocomplete, and relied on custom runtime validators.

### Approach 2: String Forward References + Model Rebuild (Selected)
*   **Mechanism**: Types are emitted as string literals (e.g., `field: "Type"`) inside the shared `_bundle.py`. At the end of the module, `Class.model_rebuild()` is called for every defined class.
*   **Rationale**:
    *   **Strict Type Checking**: String references are valid Pydantic/MyPy annotations that resolve to the actual class. This restores full static analysis capabilities.
    *   **Performance**: Computation is shifted to import time (rebuild) rather than access time (lazy validation), resulting in faster runtime performance.
    *   **Standardization**: Aligns with standard Pydantic v2 patterns for self-referencing models.

### Comparison
*   **Current Baseline**: Relies on distinct wrapper classes for metadata (e.g., `ReferenceWithMeta`). This results in a complex web of cross-references between plain and wrapped types, which frequently causes resolution failures in circular models.
*   **Proposed**: Leverages Type Unification and a module-level `model_rebuild()` phase. This allows all self-referencing and metadata-aware types to resolve natively within a single pass.

### Implementation Details
The `_bundle.py` files now contain:
1.  Class definitions with string-quoted types for complex attributes.
2.  Fields with Rune metadata/references use a **Pydantic-native Union** (e.g., `Union['Type', Reference, UnresolvedReference]`) to allow reference objects without losing type safety for the base object.
3.  Function signatures with specific type hints (quoted for complex objects, unquoted for basics and enums) instead of `Any`.
4.  A footer section calling `.model_rebuild()` for every class to resolve all forward references.
5.  All generated files include `from __future__ import annotations` to support modern type hinting semantics.
6.  Complete removal of `LazyValidator` and `LazySerializer` usage.

---

## Issue: Metadata Architecture (Type Unification)

### Context
In the **Current Baseline**, entity reuse is handled by generating two separate Python classes for the same Rune type: a "Plain" version and a "Metadata" version (e.g., `ReferenceWithMetaBaseEntity`). 

### Proposed
To simplify the user experience and ensure consistency across the model, this generator uses **Type Unification**. 

*   **Mechanism**: Every data class inherits from `BaseDataClass`, which uses `BaseMetaDataMixin` to store metadata (like `@key` or `@ref`) dynamically in the object's `__dict__`.
*   **Pros**: 
    *   **Cleaner API**: Users don't have to worry about whether they are holding a "plain" or "meta" version of an object. 
    *   **Deep Path Consistency**: Simplifies operations like `set` and `extract`, as the internal structure of the model remains uniform regardless of metadata content.
    *   **Standard Pydantic**: The models remain valid Pydantic models with predictable field types.
*   **Cons/Trade-offs**:
    *   **Validation**: Since the types are unified, Pydantic's standard type validation cannot distinguish between an object that *has* a key and one that *doesn't*. 
    *   **Dynamic Enforcement**: Constraints (e.g., ensuring a field without `[metadata reference]` doesn't receive an object with a `@key`) must be enforced by the runtime (`BaseDataClass`) rather than by the Pydantic type system itself.

### Comparison
*   **Current Baseline**: Generates two distinct classes for every type (e.g., `BaseType` and `ReferenceWithMetaBaseType`). This is structurally safe but leads to massive code bloat and "type mismatch" errors when moving data between plain and metadata versions.
*   **Proposed**: Single unified class with metadata stored in `__dict__`. Results in a cleaner, more intuitive API at the cost of moving metadata validation to the runtime.

---

## Issue: Inconsistent Numeric Types

### Context
Rune `number` and `int` are often handled inconsistently across different languages and generators.

### Comparison
*   **Current Baseline**: Rune `number` is sometimes mapped to `float`, which causes precision loss (e.g., `0.1 + 0.2 != 0.3`).
*   **Proposed**: Rune `number` is **strictly** mapped to `Decimal`. Rune `int` is mapped to `int`. This ensures financial accuracy and matches the expectations of the Rune runtime.

---

## Issue: Enum Metadata Support

### Context
In Rune, even Enumerations can carry metadata in some models.

### Comparison
*   **Current Baseline**: Maps Rune enums to standard Python `Enum` classes. These cannot hold metadata.
*   **Proposed**: Uses a runtime `_EnumWrapper` that encapsulates the Enum value but provides the same `BaseMetaDataMixin` as data classes, allowing enums to have keys and references.

## Issue: Function Signature Type Hinting

### Context
Functions in Rune can have complex objects as inputs and outputs. If these objects are defined in a way that creates circular dependencies, the Python generator must decide how to type-hint them.

### Comparison
*   **Current Baseline**: Often defaults to `Any` for complex type parameters to avoid `NameError` or `ImportError` in circular dependency scenarios. This results in the loss of IDE autocomplete, static type safety for function calls, and prevents runtime validation of arguments.
*   **Proposed**: Uses **Specific String-Quoted Type Hints** (e.g., `def func(arg: 'ComplexType')`) combined with **Deferred Function Generation**.
    *   **Mechanism**: The generator strictly orders the `_bundle.py` file:
        1.  All Data Classes are defined.
        2.  `model_rebuild()` is called on all classes to resolve string forward references into actual types.
        3.  Functions are defined *after* the rebuild phase.
    *   **Benefit**: This allows Pydantic's `@validate_call` decorator to immediately resolve (and validate) the fully constructed types, providing strong runtime guarantees and perfect IDE support without circularity crashes.

---

## Issue: Partial Object Construction and Pydantic Validation

### Description
Rune allows partial initialization of objects using multiple `set` operations. For example:
```rosetta
func TestComplexTypeInputs:
    # ...
    output:
        c ComplexTypeC (1..1)
    set c->valueA:
        a->valueA
    set c->valueB:
        b->valueB
```

The Proposed implementation translates this into:
```python
    c = ComplexTypeC(valueA=...)
    c = set_rune_attr(c, 'valueB', ...)
```

If `ComplexTypeC` has `valueB` as a required field (cardinality 1..1), the first line fails with a Pydantic `ValidationError` because `valueB` is missing at construction time.

### Encountered Failures
- `test_create_incomplete_object_succeeds_in_python`: Fails because required fields are missing in constructor.
- `test_create_incomplete_object_succeeds`: Fails because required fields are missing in constructor.
- `test_complex_type_inputs`: Fails because the first `set` operation only provides one of multiple required fields.
- `testAliasWithTypeOutput` (Java): Fails because `com_rosetta_test_model_C` constructor is called with only one of its required fields.

### Potential Solutions (to be debated)
1. Use `model_construct(**kwargs)` for initial object creation to skip validation.
2. Generate all fields as `Optional` in Pydantic models, even if they are required in Rune.
3. Defer object construction until all `set` operations for that object are collected.

---

## Issue: Redundant Logic Generation
*   **Description**: The `PythonFunctionGenerator` previously contained redundant methods (like `generateIfBlocks`) that duplicated logic generated by `generateAlias` and `generateOperations`.
*   **Status**: Fixed. `generateIfBlocks` was removed, preventing duplicate Python code for conditional logic.

## Issue: Inconsistent Numeric Types
*   **Description**: Mapping of Rune `number` was inconsistent (sometimes `int`, `float`, or `Decimal`), causing precision loss and test failures.
*   **Status**: Fixed. `RuneToPythonMapper` strictly enforces mapping `number` to `Decimal`.

## Issue: Constructor Keyword Arguments SyntaxError
*   **Description**: Constructor expressions were generating duplicate `unknown` keyword arguments for null Rune keys.
*   **Status**: Fixed. The generator now uses unique fallback keys (`unknown_0`, `unknown_1`, etc.).

## Issue: Missing Runtime Support
*   **Description**: Support for `rune_with_meta` and `rune_default` was needed in the runtime to support `WithMeta` and `default` operators.
*   **Status**: Fixed.

*   **Description**: `_get_rune_object` failed with `KeyError` for non-imported classes.
*   **Status**: Partially addressed by moving towards direct constructor calls.

## Runtime Library Requirements

The **Proposed** architecture relies on specific features in the `rune-runtime` library (v1.0.19+). Below are the code updates required in the runtime to support these features.

### 1. Type Unification Support

*   **Requirement**: `BaseDataClass` must inherit from `BaseMetaDataMixin`.
*   **Purpose**: Allows any data object to dynamically store metadata (like `@key` or `@ref`) in `__dict__` without polluting the Pydantic model fields or requiring a separate "WithMeta" class.

**Before (Separate Hierarchy):**
```python
class BaseDataClass(BaseModel):
    # Standard Pydantic model behavior
    pass

class ReferenceWithMeta(BaseDataClass):
    # Separate class for metadata
    globalReference: str | None = None
    externalReference: str | None = None
    address: Any | None = None
```

**After (Unified Mixin):**
```python
class BaseMetaDataMixin:
    """Mixin to handle dynamic metadata storage in __dict__."""
    def _set_metadata(self, key: str, value: Any):
        self.__dict__[key] = value

    @property
    def meta(self):
        return self.__dict__.get("meta", {})

class BaseDataClass(BaseModel, BaseMetaDataMixin):
    # Now ALL data classes can natively hold metadata
    pass
```

### 2. Operator Support

*   **Requirement**: `rune_with_meta` and `rune_default` helper functions.
*   **Purpose**: `rune_with_meta` allows attaching metadata to an object (returning the same object). `rune_default` safely returns a default value if an optional field is None.

**Before (Missing or Ad-hoc):**
*   *Functionality did not exist or was handled by direct attribute manipulation in generated code.*

**After (Standardized Operators):**
```python
def rune_with_meta(obj: Any, metadata: dict) -> Any:
    """Attaches metadata to an object's internal dict and returns the object."""
    if obj is None:
        return None
    if hasattr(obj, "__dict__"):
        # For Pydantic models / Unified types
        for k, v in metadata.items():
            obj.__dict__[k] = v
    elif isinstance(obj, Enum):
        # Enums require wrapping to hold state (see below)
        return _EnumWrapper(obj, metadata)
    return obj

def rune_default(obj: Any, default_val: Any) -> Any:
    """Returns default_val if obj is None, otherwise obj."""
    return default_val if obj is None else obj
```

### 3. Enum Wrappers

*   **Requirement**: Runtime support for wrapping Python Enums to attach metadata.
*   **Purpose**: Python `Enum` members are singletons and cannot have per-instance attributes. To support metadata on specific usage of an enum value (e.g. `Scheme` info), we must wrap it.

**Before (No Support):**
*   *Enums could not accept metadata. Attempts to set attributes on Enums raised AttributeError.*

**After (Wrapper Proxy):**
```python
class _EnumWrapper(BaseMetaDataMixin):
    """Wraps an Enum to allow attaching metadata (keys/schemes)."""
    def __init__(self, enum_val: Enum, metadata: dict = None):
        self._value = enum_val
        if metadata:
            self.__dict__.update(metadata)

    def __getattr__(self, name):
        # Proxy access to the underlying Enum value
        return getattr(self._value, name)

    def __eq__(self, other):
        # Allow equality checks against the raw Enum
        if isinstance(other, type(self._value)):
            return self._value == other
        return self._value == other

    def __str__(self):
        return str(self._value)
```


---

## Summary of Generator Component Changes (CDM Support Refactor)

### `PythonAttributeProcessor.java`
*   Generates field definitions inside Python data classes.
*   Updated to support metadata-wrapped types (e.g., `'StrWithMeta'`) and ensures consistent formatting for testing.

### `PythonFunctionGenerator.java`
*   Generates signatures, aliases, and logic.
*   Removed duplicate logic generation.
*   Updated docstrings and signatures to use string forward references.
*   Wrapped `switch` logic in closures to prevent namespace pollution.

### `RuneToPythonMapper.java`
*   Centralizes DSL-to-Python name and type mapping.
*   Distinguishes between basic and complex types for quoting/forward reference decisions.
*   Enforces `Decimal` for all numeric mappings.

---

## List of Changed Classes (Feb 2026 Refactor)

### Generator Classes (src/main/java)

#### `com.regnosys.rosetta.generator.python.PythonCodeGenerator`
*   **Responsibility**: Orchestrates the generation of the Python package structure and the `_bundle.py` file.
*   **Changes**:
    *   **Generation Order**: Modified `processDAG` to strictly enforce the order: (1) Class/Model Definitions -> (2) `model_rebuild()` -> (3) Function Definitions. This fixes `PydanticUndefinedAnnotation` errors by ensuring types are fully resolved before being used in `@validate_call` decorators.
    *   **Stub Generation**: Added `generateStub` helper method to reduce code duplication when generating stub files for classes and functions.
    *   **Runtime Guarantees**: Ensures `sys.modules[__name__].__class__` guard is applied to function modules to intercept attribute access.

#### `com.regnosys.rosetta.generator.python.functions.PythonFunctionGenerator`
*   **Responsibility**: Generates Python functions from Rune function definitions.
*   **Changes**:
    *   **Dependency Management**: Updated to use `PythonFunctionDependencyProvider.addDependencies` to correctly collect and emit necessary imports (including Enums) for function bodies.
    *   **Refactoring**: Cleaned up legacy `enumImports` handling.

#### `com.regnosys.rosetta.generator.python.functions.PythonFunctionDependencyProvider`
*   **Responsibility**: Analyzes function bodies to determine required imports.
*   **Changes**:
    *   **Recursion Fix**: Fixed `addDependencies` to properly recurse into complex expressions to find nested dependencies.
    *   **Enum Resolution**: Added logic to correctly identify and import `REnumType` references used within function logic.

#### `com.regnosys.rosetta.generator.python.util.RuneToPythonMapper`
*   **Responsibility**: utility class for mapping Rune types to Python types.
*   **Changes**:
    *   **Centralization**: Consolidated logic for generating type strings (quoted vs unquoted) to ensure consistent handling of forward references.
    *   **Normalization**: Added standard methods for normalizing names to avoiding conflicts with Python keywords and built-ins.

### Test Classes (src/test/java)

#### `com.regnosys.rosetta.generator.python.functions.PythonFunctionsTest`
*   **Responsibility**: Unit tests for generated Python functions.
*   **Changes**:
    *   **Compilation Fix**: Resolved `PrintStream.println` method signature mismatch.
    *   **Updates**: Adapted tests to match the new `_bundle.py` structure and import patterns.

#### `com.regnosys.rosetta.generator.python.rule.PythonDataRuleGeneratorTest`
*   **Responsibility**: Unit tests for validation rules.
*   **Changes**:
    *   **Updates**: Updated expected output to reflect changes in how rules interact with the unified metadata structures.
