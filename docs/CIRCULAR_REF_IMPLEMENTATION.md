# Implementation Proposal: Support for Circular Type References

## Objective
Enable support for circular dependencies between Rosetta types in generated Python code. The current implementation fails when `Annotated` types refer to `serializer()` or `validator()` methods of classes that are not yet fully defined or are part of a reference cycle.

## Background
In the current code generation:
1. Attributes are defined using `Annotated[Type, Serializer, Validator]`.
2. `Serializer` and `Validator` are method calls on the referenced class (e.g., `MyClass.serializer()`).
3. If `MyClass` is part of a circular dependency, it may not be defined yet when another class in the cycle refers to it, leading to a `NameError` or `AttributeError` at runtime during module import. We will refer to the class by its **bundle name** (e.g., `com_rosetta_test_model_MyClass`) in these updates.

## Proposed Strategy: Delayed Annotation Updates
We will leverage Pydantic's ability to "rebuild" models after they have been defined. This involves a three-phase generation process within each Python bundle:

1. **Phase 1: Clean Class Definitions**
   Generate class bodies using only "clean" type hints (e.g., `attr: Optional[OtherClass]`). This allows Pydantic to accept the class definition as long as forward references can be resolved as strings (which `from __future__ import annotations` handles).

2. **Phase 2: Post-Definition Annotation Injection**
   After all classes in the bundle have been defined, explicitly update each class's `__annotations__` dictionary (using its **bundle name**) with the full `Annotated[...]` metadata, including serializers and validators.
   - **Scope**: This only applies to attributes referring to **Rosetta Data types** (Rune-defined types).
   - **Exclusions**: Native types (e.g., `str`, `int`, `Decimal`) and **Enums** will keep their `Annotated` definitions (if any) inline within the class body. Since these types are either built-in or imported at the top of the module, their `serializer()` or `validator()` methods are always available at definition time.

3. **Phase 3: Model Rebuilding**
   Call `model_rebuild()` on each model to force Pydantic to re-evaluate the updated annotations and compile the serialization/validation logic.

## Proposed Code Changes

### 1. `PythonAttributeProcessor.java`
- Introduce a new result class (e.g., `AttributeProcessingResult`):
    ```java
    public class AttributeProcessingResult {
        private final String attributeCode;           // The "clean" class body attributes
        private final List<String> annotationUpdates; // The "__annotations__['x'] = ..." snippets
        // ... constructor and getters
    }
    ```
- Modify `generateAllAttributes` to return this `AttributeProcessingResult` instead of a raw `String`.
- **Refactoring**: Separation of the inline field definition from the metadata-heavy `Annotated` update statement.
- **Logic**:
    - For Rosetta `Data` types: Generate a clean type definition for the class body AND a delayed update string.
    - For Native types/Enums: Generate the full definition inline as before (no delayed update).

### 3. `PythonModelObjectGenerator.java`
- Receive the `AttributeProcessingResult` from the processor.
- Append the `attributeCode` to the class definition as usual.
- Group the `annotationUpdates` by class name and store them in the `PythonCodeGeneratorContext`.
- Prepend the class's **bundle name** to each update string during storage.

### 4. `PythonCodeGenerator.java`
- Update `processDAG` (specifically the bundle generation logic):
    - **Single-Loop, Two-Buffer Approach**:
        - Instead of multiple loops, use a single topological order traversal.
        - Use two separate `PythonCodeWriter` instances (or `StringBuilder`s) inside the loop:
            1. `bodyWriter`: For Phase 1 (Class/Function definitions).
            2. `updateWriter`: For Phase 2 and 3 (Annotation updates and rebuilds).
        - For each type in the DAG:
            - Append its class body to `bodyWriter`.
            - If it has entries in `postDefinitionUpdates`, append the `__annotations__` assignments AND the `.model_rebuild()` call to `updateWriter`.
    - **Final Assembly**:
        - After the loop finishes, append the contents of `updateWriter` to `bodyWriter`.
        - This ensures that all classes are defined (in `bodyWriter`) before any updates or rebuilds (in `updateWriter`) are executed by the Python interpreter.
        - This keeps the implementation efficient (single pass) while satisfying the structural requirements of the Python code.

## Example for `CircularDependency` Model

For the following Rosetta model:
```rosetta
namespace rosetta_dsl.test.language.CircularDependency

type A:
    b B (1..1)

type B:
    a A (0..1)
```

### Exact Contents of `postDefinitionUpdates` Map

**Key**: `rosetta_dsl.test.language.CircularDependency.A`
**Value**: 
1. `rosetta_dsl_test_language_CircularDependency_A.__annotations__["b"] = Annotated[rosetta_dsl_test_language_CircularDependency_B, rosetta_dsl_test_language_CircularDependency_B.serializer(), rosetta_dsl_test_language_CircularDependency_B.validator()]`

**Key**: `rosetta_dsl.test.language.CircularDependency.B`
**Value**: 
1. `rosetta_dsl_test_language_CircularDependency_B.__annotations__["a"] = Optional[Annotated[rosetta_dsl_test_language_CircularDependency_A, rosetta_dsl_test_language_CircularDependency_A.serializer(), rosetta_dsl_test_language_CircularDependency_A.validator()]]`

### Final Generated Python Structure in `_bundle.py`

```python
from __future__ import annotations
from typing import Annotated, Optional
from pydantic import Field
from rune.runtime.base_data_class import BaseDataClass

# Phase 1: Clean definitions
class rosetta_dsl_test_language_CircularDependency_B(BaseDataClass):
    _FQRTN = 'rosetta_dsl.test.language.CircularDependency.B'
    a: Optional[rosetta_dsl_test_language_CircularDependency_A] = Field(None, description='')

class rosetta_dsl_test_language_CircularDependency_A(BaseDataClass):
    _FQRTN = 'rosetta_dsl.test.language.CircularDependency.A'
    b: rosetta_dsl_test_language_CircularDependency_B = Field(..., description='')

# Phase 2: Delayed Annotation Updates
rosetta_dsl_test_language_CircularDependency_B.__annotations__["a"] = Optional[Annotated[rosetta_dsl_test_language_CircularDependency_A, rosetta_dsl_test_language_CircularDependency_A.serializer(), rosetta_dsl_test_language_CircularDependency_A.validator()]]
rosetta_dsl_test_language_CircularDependency_A.__annotations__["b"] = Annotated[rosetta_dsl_test_language_CircularDependency_B, rosetta_dsl_test_language_CircularDependency_B.serializer(), rosetta_dsl_test_language_CircularDependency_B.validator()]

# Phase 3: Rebuild
rosetta_dsl_test_language_CircularDependency_B.model_rebuild()
rosetta_dsl_test_language_CircularDependency_A.model_rebuild()
```

## Considerations
- **Inheritance**: `model_rebuild()` correctly handles inherited fields, but we should ensure parent classes are rebuilt if they also have cycles.
- **Performance**: Calling `model_rebuild()` adds a small overhead at import time, but it is necessary for resolving cycles.
- **Ordering**: Since all updates and rebuilds happen at the end of the `_bundle.py`, the order of rebuilds doesn't strictly matter as long as all classes are already defined in the namespace.

## Next Steps
Upon approval of this proposal, we will:
1.  Implement the changes in `PythonAttributeProcessor` and `PythonCodeGeneratorContext`.
2.  Update the `PythonModelObjectGenerator` and `PythonCodeGenerator` to emit the new code structure.
3.  Add a regression test `PythonCircularDependencyTest.java` to verify the fix.

## Appendix: Pydantic 2 Challenges and Findings

During implementation, we identified a critical behavior in Pydantic 2 that impacts this modular strategy:

### 1. Pydantic 2 Core Validation Cache
**Issue**: Pydantic 2 aggressively optimizes type resolution. If a class referred to in a type hint is already defined in the module (as often happens within a single bundle), Pydantic resolves it immediately and generates a "Core Validator". 

**Impact**: 
- Once a field is resolved, `model_rebuild()` **does not** re-evaluate `__annotations__` or even `model_fields` for that field. 
- Updates during Phase 2 are effectively ignored if the type was fully available during Phase 1.
- This breaks the "clean body" approach for any types that happen to be defined early in the bundle (like `KeyEntity` before `RefEntity`).

### 2. Evaluated Approaches (and why they failed)
- **Bare Names + `model_rebuild()`**: Failed because Pydantic resolved the "base" class immediately and cached the validator without our custom metadata.
- **`model_fields` internal update**: Tried explicitly replacing `model_fields[name].annotation` and even the entire `FieldInfo` object during Phase 2. This *still* failed to trigger a validator update in `model_rebuild()` because Pydantic's internal state machine considers the field "done" if it was resolved as a non-forward reference.

### 3. Recommendation: Forced Forward References (Quoting)
The only robust way to ensure that Phase 1 definitions do not "lock in" an incomplete validator is to force Pydantic into a **`ForwardRef`** state. Wrapping Phase 1 types in single quotes (e.g., `ke: 'KeyEntity'`) prevents immediate resolution and guarantees that Phase 3 correctly incorporates the Phase 2 metadata.

**Current Implementation Status**: The implementation currently follows the **Original Design** (Bare Names), which carrying the known limitation regarding metadata resolution for co-located types in Pydantic 2.
