# Circular Reference Support Release Notes

This document describes the implementation of support for circular type references in the Python generator.

## 1. Support for Circular Type Dependencies [FIXED]

Rosetta models often contain circular references (e.g., `CircularA` has an attribute of type `CircularB`, and `CircularB` has an attribute of type `CircularA`). Previously, these would cause `NameError` during Python module import because one class would inevitably refer to another that hadn't been defined yet.

### Solution: Three-Phase Generation

We have implemented a three-phase generation strategy within each Python bundle to resolve these cycles:

1.  **Phase 1: Bare Class Definitions**: Classes are first defined with "clean" type hints that use bare names. This allows the Python interpreter to register the class names in the module namespace without immediately requiring the full type information of their attributes.
2.  **Phase 2: Delayed Annotation Injection**: After all classes in a bundle are defined, the generator emits explicit updates to the `__annotations__` dictionary of each class. This phase injects the full `Annotated[...]` metadata, including the Rosetta-specific `serializer()` and `validator()` methods.
3.  **Phase 3: Pydantic Model Rebuilding**: Finally, `model_rebuild()` is called on every class. This forces Pydantic to re-evaluate the updated `__annotations__` and compile the final validation and serialization logic now that all referenced types are fully available.

### Benefits:
- **Model Integrity**: Complex models like the **Common Domain Model (CDM)** can now be fully generated and imported without structural errors.
- **Metadata Support**: Serialization and validation metadata are correctly applied even to types involved in reference cycles.
- **Native Pydantic Alignment**: Leverages Pydantic's built-in mechanism for resolving forward references.

## 2. Robust Topological Sorting [FIXED]

The generator's internal Directed Acyclic Graph (DAG) has been improved to manage dependencies more accurately.

### Changes:
- **Inheritance vs. Attribute Edges**: The DAG now distinguishes between hard dependencies (inheritance) and soft dependencies (attributes).
- **Cycle Detection**: When the DAG detects a cycle that cannot be resolved via linear ordering, it automatically triggers the three-phase "Delayed Annotation" logic for the involved types.
