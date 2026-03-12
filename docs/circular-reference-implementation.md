# Implementation Detail: Support for Circular Type References

## Objective

Enable support for circular dependencies between Rosetta types in generated Python code. This ensures that `Annotated` types referring to `serializer()` or `validator()` methods do not cause `NameError` or `AttributeError` during module import when part of a reference cycle.

## Background

In the generated code:
1. Attributes are defined using `Annotated[Type, Serializer, Validator]`.
2. `Serializer` and `Validator` are method calls on the referenced class (e.g., `MyClass.serializer()`).
3. If `MyClass` is part of a circular dependency, it may not be defined yet when another class in the cycle refers to it.

## Implementation Strategy: Delayed Annotation Updates

We leverage Pydantic's ability to "rebuild" models after they have been defined. This involves a three-phase generation process within each Python bundle:

1. **Phase 1: Bare Class Definitions**
   Generate class bodies using only "clean" type hints (e.g., `attr: Optional[OtherClass]`). This allows Pydantic to accept the class definition as long as forward references can be resolved as strings (which `from __future__ import annotations` handles).

2. **Phase 2: Post-Definition Annotation Injection**
   After all classes in the bundle have been defined, explicitly update each class's `__annotations__` dictionary with the full `Annotated[...]` metadata, including serializers and validators.
   - **Scope**: This applies to attributes referring to **Rosetta Data types**.
   - **Exclusions**: Native types (e.g., `str`, `int`, `Decimal`) and **Enums** keep their `Annotated` definitions inline within the class body as their dependencies are always available.

3. **Phase 3: Model Rebuilding**
   Call `model_rebuild()` on each model to force Pydantic to re-evaluate the updated annotations and compile the serialization/validation logic.

## Architecture

### 1. `PythonAttributeProcessor`
Separates the inline field definition from the metadata-heavy `Annotated` update statement. Returns an `AttributeProcessingResult` containing both the class body code and the delayed update strings.

### 2. `PythonModelObjectGenerator`
Integrates the results from the processor. It appends the clean attribute code to the class definition and stores the `__annotations__` updates in the generation context.

### 3. `PythonCodeGenerator`
Manages the bundle assembly. It uses a single topological order traversal but maintains two separate buffers:
- `bodyWriter`: For Phase 1 (Class/Function definitions).
- `updateWriter`: For Phase 2 and 3 (Annotation updates and rebuilds).

After processing all types in the bundle, the contents of `updateWriter` are appended to `bodyWriter`, ensuring all definitions precede their metadata updates.

## Example for `CircularDependency` Model

For a Rosetta model where `A` depends on `B` and `B` depends on `A`:

```python
from __future__ import annotations
from typing import Annotated, Optional
from pydantic import Field
from rune.runtime.base_data_class import BaseDataClass

# Phase 1: Clean definitions
class rosetta_dsl_test_language_CircularDependency_B(BaseDataClass):
    a: Optional[rosetta_dsl_test_language_CircularDependency_A] = Field(None, description='')

class rosetta_dsl_test_language_CircularDependency_A(BaseDataClass):
    b: rosetta_dsl_test_language_CircularDependency_B = Field(..., description='')

# Phase 2: Delayed Annotation Updates
rosetta_dsl_test_language_CircularDependency_B.__annotations__["a"] = Optional[Annotated[rosetta_dsl_test_language_CircularDependency_A, rosetta_dsl_test_language_CircularDependency_A.serializer(), rosetta_dsl_test_language_CircularDependency_A.validator()]]
rosetta_dsl_test_language_CircularDependency_A.__annotations__["b"] = Annotated[rosetta_dsl_test_language_CircularDependency_B, rosetta_dsl_test_language_CircularDependency_B.serializer(), rosetta_dsl_test_language_CircularDependency_B.validator()]

# Phase 3: Rebuild
rosetta_dsl_test_language_CircularDependency_B.model_rebuild()
rosetta_dsl_test_language_CircularDependency_A.model_rebuild()
```
