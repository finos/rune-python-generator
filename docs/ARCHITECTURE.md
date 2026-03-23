# Generator Architecture

> **Audience**: Generator contributors.
> This document describes how the Rune Python Generator is structured internally, the key architectural patterns used in the generated code, and the design decisions behind unimplemented features.

---

## 1. Overview

The generator is a standalone Java CLI that interprets the produces Python from Rune defined model definitions. The generated code depends on the [rune-python-runtime](https://github.com/finos/rune-python-runtime) library and uses [Pydantic v2](https://docs.pydantic.dev/) for validation and serialization.

### Key Java Classes

| Class | Responsibility |
| :--- | :--- |
| `PythonCodeGeneratorCLI` | Invokes Python generation based on input Rune file(s) or directories.  Invokes xText validation on all input models ignoring any that are deemed to be errors |
| `PythonCodeGenerator` | Orchestrates the generation pipeline: collection, scan, partition, emit, and assembly |
| `PythonCodeGeneratorContext` | Holds shared state: dependency graph, SCC results, standalone/bundled partition |
| `PythonModelObjectGenerator` | Generates data type files (standalone or bundled) |
| `PythonAttributeProcessor` | Generates field declarations and `Annotated[...]` type hints |
| `PythonFunctionGenerator` | Generates function files |
| `PythonExpressionGenerator` | Generates expression code; instantiated fresh per function or class body |
| `PythonFunctionDependencyProvider` | Computes import statements for function files |
| `PythonCodeWriter` | Utility for output formatting and indentation |

---

## 2. Generation Pipeline

The generator is invoked by the Rune DSL framework via three lifecycle callbacks. All substantive work happens in `afterAllGenerate`.

### `beforeAllGenerate`

Clears the per-run `contexts` map. No model processing or output occurs here.

### `generate` (called once per model resource)

Collects elements into a per-namespace `PythonCodeGeneratorContext`. For each model file, `Data` types, `Function` types, and `RosettaEnumeration` values are added to the context for their namespace. No Python is generated yet — this method always returns an empty map.

### `afterAllGenerate`

All code generation happens here, in four phases per namespace:

#### Phase 1 — Scan

`pojoGenerator.scan()` and `functionGenerator.scan()` walk the collected types and build the dependency graph (`DefaultDirectedGraph`) stored in the context. Each edge represents a type dependency: inheritance edges and attribute-type edges are both recorded.

#### Phase 2 — Partition (`partitionClasses`)

**Kosaraju SCC analysis** (`KosarajuStrongConnectivityInspector`) is run on the dependency graph to identify strongly-connected components (cycle groups). Types are partitioned:

- **Standalone** (acyclic): a type whose SCC has size 1 and no self-loop. Each gets its own `.py` file.
- **Bundled** (cyclic): a type that shares an SCC with another type. All members of a cycle group are emitted into `_bundle.py` using the Phase 1/2/3 deferred-annotation pattern (see §3).

Types belonging to other namespaces are unconditionally marked standalone, so cross-namespace imports always use the fully-qualified proxy stub path rather than a foreign `_bundle`.

#### Phase 3 — Emit

`pojoGenerator.generate()` and `functionGenerator.generate()` produce Python source text for each type, returning it into maps in the context (`classObjects`, `functionObjects`). At this point the standalone/bundled partition is known, so each generator emits the appropriate form.

#### Phase 4 — Assemble (`processDAG`)

The SCC dependency graph is condensed and topologically sorted. Types are then walked in topological order and routed to one of two destinations:

- **Standalone types**: written directly to individual `src/<namespace>/<TypeName>.py` files.
- **Bundled types**: appended to accumulating writers for Phase 1 class bodies, Phase 2 annotation updates, and Phase 3 `model_rebuild()` calls. A lazy PEP 562 proxy stub is also created at the type's fully-qualified path so the public import API is the same as for standalone types.

Finally, `assembleBundleFile` concatenates:
1. Bundle header (standard imports, inline standalone-supertype imports)
2. Phase 1 bundled class bodies
3. Deferred standalone-class imports (after all class definitions, to avoid circular-import at bundle load time)
4. Phase 2 `__annotations__` updates
5. Phase 3 `model_rebuild()` calls
6. Bundled function bodies

into `src/<namespace>/_bundle.py`.

### CDM 6.0 Partition Metrics - For Reference

| Metric | Value |
| :--- | :--- |
| Total types | 973 |
| Standalone (acyclic) | 911 (~93.6%) |
| Bundled (cyclic) | 62 (~6.4%) |
| Cycle groups (SCCs) | 8 |
| Largest cycle group | 44 types (`Trade`, `Product`, `EconomicTerms`, `Payout`, …) |

This partitioning reduced CDM 6.0 load time from ~120 seconds to ~15 seconds by limiting `model_rebuild()` calls to the 62 bundled types.

---

## 3. Phase 1/2/3 Deferred Annotation Pattern

Types that participate in circular dependencies cannot be fully defined in a single pass — a class body that references another class must be parseable before that class is defined. The generator resolves this with a three-phase strategy within each `_bundle.py`.

### Objective

Enable `Annotated[Type, serializer(), validator()]` type hints for all attributes, including those involved in reference cycles, without causing `NameError` or `AttributeError` during module import.

### Background

In the generated code, attributes are declared using:

```python
Annotated[Type, Type.serializer(), Type.validator()]
```

If `Type` is part of a cycle, it may not yet be defined when another class in the cycle refers to it. Calling `.serializer()` or `.validator()` on an undefined name raises `NameError`.

### Three-Phase Strategy

**Phase 1 — Bare class definitions**

Classes are defined with `None` as the type placeholder for all attributes that reference other Rune types. This allows Pydantic to accept the class definition without needing the full type information of attributes.

```python
class cdm_product_collateral_CollateralCriteria(BaseDataClass):
    CollateralIssuerType: None = Field(None, ...)   # placeholder
```

Using `None` (rather than a forward-reference string) avoids a Pydantic `ArbitraryTypeWarning` that was triggered when the attribute's `FieldInfo` default shadowed the type name in `vars(cls)`.

**Phase 2 — Delayed annotation injection**

After all classes in the bundle are defined, the generator emits explicit updates to each class's `__annotations__` dictionary with the full `Annotated[...]` metadata. At this point all class names are in scope, so `.serializer()` and `.validator()` resolve correctly.

```python
cdm_product_collateral_CollateralCriteria.__annotations__["CollateralIssuerType"] = \
    Annotated[Optional[CollateralIssuerType], CollateralIssuerType.serializer(), ...]
```

**Phase 3 — Model rebuilding**

`model_rebuild()` is called on every class in the bundle. This forces Pydantic to re-evaluate the updated `__annotations__` and compile the final validation and serialization logic.

```python
cdm_product_collateral_CollateralCriteria.model_rebuild()
```

### Scope of Delayed Annotations

- **Rune Data type attributes**: always use Phase 2 delayed updates.
- **Native type attributes** (`str`, `int`, `Decimal`, etc.) and **Enum attributes**: their `Annotated[...]` metadata is written directly in the Phase 1 class body, because these types are always available at class-definition time.

### Implementation

| Class | Role |
| :--- | :--- |
| `PythonAttributeProcessor` | Produces two outputs per attribute: the Phase 1 placeholder and the Phase 2 `__annotations__` update string |
| `PythonModelObjectGenerator` | Appends Phase 1 code to the class body; stores Phase 2 updates in the generation context |
| `PythonCodeGenerator` | Maintains two writers (`bodyWriter` for Phase 1, `updateWriter` for Phase 2/3); appends Phase 2/3 content after all class definitions |

### Complete Example

```python
from __future__ import annotations
from typing import Annotated, Optional
from pydantic import Field
from rune.runtime.base_data_class import BaseDataClass

# Phase 1: bare class definitions
class rosetta_dsl_test_CircularB(BaseDataClass):
    a: None = Field(None, description='')

class rosetta_dsl_test_CircularA(BaseDataClass):
    b: None = Field(None, description='')

# Phase 2: delayed annotation updates
rosetta_dsl_test_CircularB.__annotations__["a"] = Optional[Annotated[
    rosetta_dsl_test_CircularA,
    rosetta_dsl_test_CircularA.serializer(),
    rosetta_dsl_test_CircularA.validator()
]]
rosetta_dsl_test_CircularA.__annotations__["b"] = Annotated[
    rosetta_dsl_test_CircularB,
    rosetta_dsl_test_CircularB.serializer(),
    rosetta_dsl_test_CircularB.validator()
]

# Phase 3: rebuild
rosetta_dsl_test_CircularB.model_rebuild()
rosetta_dsl_test_CircularA.model_rebuild()
```

---

## 4. Expression Generator

### Isolation

`PythonFunctionGenerator` and `PythonModelObjectGenerator` obtain `PythonExpressionGenerator` instances via a Guice `Provider<PythonExpressionGenerator>`. A fresh instance is created for every function or class body.

This isolation eliminates the bug-prone pattern of sharing a single stateful `ifCondBlocks` list across nested generation steps, which previously caused logic bleeding between sibling expressions.

### ExpressionResult

The expression generator does not accumulate output into a shared buffer. Instead, every generation call returns an `ExpressionResult` record containing:

- **`expression`**: the primary generated expression string.
- **`companionBlocks`**: any supporting statements that must precede the expression in the output (e.g., intermediate `if`-condition functions that are defined before the expression that calls them).

The caller (function or object generator) assembles these into the final output, maintaining full control over scoping and ordering.

---

## 5. Null-Safety and "Nothing" Propagation

### Rune Semantics

In the Rune specification, `null` ("nothing") is semantically equivalent to an empty list in a collection context. Two rules govern this:

- **Flattening rule**: chained path expressions are automatically flattened; `nothing` contributes zero elements.
- **Propagation rule**: operations that receive a `None` input must propagate `None` for scalar results rather than returning a default like `0` or `[]`.

### Java Reference: `MapperC`

The Python generator's null-handling logic is aligned with the Java reference implementation's `MapperC` (Mapper for Collections). In Java, all collection operations use a `nonErrorItems()` filter that excludes "nothing" items before processing:

```java
protected Stream<MapperItem<? extends T,?>> nonErrorItems() {
    return items.stream().filter(i -> !i.isError());
}
```

### Python Implementation

The generator implements these semantics using generator expressions and lambda wrapping.

**Collection filtering** — a comprehension filters `None` values before any operation acts on a list:

```python
(x for x in (items or []) if x is not None)
```

**Scalar propagation** — aggregations are lambda-wrapped to return `None` when the input collection itself is `None`:

```python
# Rosetta: sum(myList)
(lambda items: sum(x for x in (items or []) if x is not None)
               if items is not None else None)(myList)
```

### Behaviour by Operation Type

| Operation type | Null behaviour |
| :--- | :--- |
| **Aggregations** (`sum`, `count`) | Return `0` for all-null or empty input; return `None` if the collection itself is `None` |
| **Selection aggregators** (`min`, `max`, `first`, `last`, `only-element`) | Return `None` for all-null, empty, or `None` input |
| **Collection result operators** (`flatten`, `distinct`, `sort`, `filter`) | Return `[]` for all-null or empty input; return `None` if the collection itself is `None` |

---

## 6. Design Decision: `typeAlias` Generation

### Problem

The Rune language supports `typeAlias`, which gives a domain-relevant name to an existing type (e.g., `typeAlias ISIN: string`). The generator must decide how to represent these aliases in Python.

### Option A — Complete Replacement ("The Java Path")

The generator expands every `typeAlias` to its underlying base type at generation time. A field declared as `ISIN` in Rune becomes `str` in Python.

| Pros | Cons |
| :--- | :--- |
| Simpler generator — no alias dependency tracking | Loss of domain meaning (`ISIN` → `str`) |
| Zero runtime overhead | Opaque Pydantic/JSON schemas |
| Fewer circular-dependency edge cases | Generated code diverges visibly from the Rune source |

### Option B — Replication ("The Rune Path")

The generator emits a Python assignment (`ISIN = str`) and preserves the alias name in type hints. IDE tooltips and JSON schemas show `ISIN`, not `str`.

| Pros | Cons |
| :--- | :--- |
| High fidelity — generated code mirrors the Rune model | Requires DAG ordering to ensure the base type precedes the alias |
| Better developer experience (domain names in IDE and schemas) | More complex cross-namespace import management |
| Aligned with Rune's design philosophy of preserving model metadata | Adds symbols to top-level modules |

### Current Implementation: Option A (Java Path)

The generator currently implements the Java Path. Every `typeAlias` is expanded to its underlying base type at generation time. The alias name does not appear in the output — fields, function parameters, and return types all use the resolved base type. This behaviour is verified and locked in by `PythonTypeAliasTest`, which asserts that alias names are absent from the generated output:

```python
# Rune: val MyNumber (1..1)  where  typeAlias MyNumber: number
val: Decimal = Field(..., description='')   # alias stripped; domain name lost
```

The same stripping applies in function signatures:

```python
# Rune: inputs: inParam AliasedNum(1..1)  where  typeAlias AliasedNum: number
def MyTestFunc(inParam: Decimal) -> str:   # alias stripped
```

### Aspired Direction: Option B (Rune Path)

The Rune Path is the preferred long-term direction. Switching to it requires:

1. **Alias assignments**: scan models for `RosettaTypeAlias` and emit `MyAlias = BaseType` in the appropriate module.
2. **Preserve hint names**: modify `PythonAttributeProcessor` to retain the alias name in field type hints rather than substituting the base type.
3. **DAG ordering**: ensure the base type is topologically ordered before its alias in the emission sequence.
4. **Cross-namespace imports**: when a function or type uses an alias from another namespace, emit the correct `from <namespace>.<Alias> import <Alias>` statement.
5. **Update `PythonTypeAliasTest`**: the existing tests assert the Java Path behaviour and will need to be revised to assert the opposite.
