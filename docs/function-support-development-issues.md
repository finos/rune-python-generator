# Development Documentation: Function Support Dev Issues

## Unresolved Issues and Proposals

Title: Fragile Object Building (Direct Constructors)

### Problem Description

The generator relies on a magic `_get_rune_object` helper which bypasses IDE checks and is hard to debug. This helper attempts to resolve model classes from the global namespace, which is fragile and leads to isolation issues when multiple bundles are present.

### Steps to Reproduce

Example Rune code: (See JUnit Test `testComplexSetConstructors`)

```rosetta
type ObservationIdentifier:
    observable Observable (1..1)
    observationDate date (1..1)

func ResolveObservation:
    inputs: date date (1..1)
    output: identifiers ObservationIdentifier (1..1)
    set identifiers -> observationDate:
        date
```

1. Define a Rune function (in a `.rosetta` file) that creates a new object (e.g., using `set`).
2. Observe the generated code calling `_get_rune_object`.

**Test Case:**

* JUnit: `PythonFunctionTypeTest.testComplexSetConstructors` (Disabled) ([Link](https://github.com/finos/rune-python-generator/blob/feature/function_support/src/test/java/com/regnosys/rosetta/generator/python/functions/PythonFunctionTypeTest.java))

### Expected Result

The generator should emit direct Python constructor calls (e.g., `MyClass(attr=val)`) or standard Pydantic methods, providing full IDE support and transparency.

### Actual Result

Generated code uses `_get_rune_object(base_model, attribute, value)`, which is opaque to static analysis.

### Additional Context

Improving the generator to use direct constructors will provide full IDE support and better transparency.

---

Title: Partial Object Construction (Stepwise Initialization)

### Problem Description

Rune functions (defined in `.rosetta` files) often populate an output object through multiple `set` operations. Pydantic's default constructor (used by the legacy `_get_rune_object`) enforces validation of all required fields immediately. If the object is missing other required fields during the first `set` operation, Pydantic throws a `ValidationError`. Additionally, `_get_rune_object` fails with a `KeyError` because it cannot resolve classes from the generated `_bundle` module's namespace.

### Steps to Reproduce

Example Rune code: [IncompleteObjects.rosetta](https://github.com/finos/rune-python-generator/blob/feature/function_support/test/python_unit_tests/features/language/IncompleteObjects.rosetta)

```rosetta
type A:
    a1 int (1..1)
    a2 int (1..1)

func CreateA:
    inputs:
        a1 int (1..1)
    output:
        a A (1..1)
    set a->a1: a1
    set a->a2: a1 * 2
```

1. Define a Rune type with multiple required fields.
2. Define a Rune function that sets these fields one by one.
3. Call the function in Python.

**Test Case:**

* Python: `test/python_unit_tests/features/language/test_incomplete_objects.py` ([Link](https://github.com/finos/rune-python-generator/blob/feature/function_support/test/python_unit_tests/features/language/test_incomplete_objects.py))
* Rosetta: `test/python_unit_tests/features/language/IncompleteObjects.rosetta` ([Link](https://github.com/finos/rune-python-generator/blob/feature/function_support/test/python_unit_tests/features/language/IncompleteObjects.rosetta))

### Expected Result

The object should be constructed successfully in steps, with validation deferred until the object is fully populated or explicitly validated.

### Actual Result

* **KeyError**: `_get_rune_object` cannot find the model class in the `rune.runtime.utils` namespace.
* **ValidationError**: If the class is found, Pydantic fails immediately on the first `set` because required fields are still `None`.

### Additional Context

Currently partially addressed via `model_construct()`, but full stepwise support is pending.

---

Title: Circular Dependencies / Out-of-Order Definitions (The "Topological Sort" Limitation)

### Problem Description

The generator uses a Directed Acyclic Graph (DAG) to order definitions in `_bundle.py`. However, the current implementation only includes edges for **inheritance (SuperTypes)**. It ignores **Attribute types**, leading to out-of-order definitions. Furthermore, Rune (following the `.rosetta` standard) allows recursive/circular types (A has B, B has A), which a DAG cannot resolve by design. Inheritance-based cycles are particularly fatal as base classes must be defined before children.

### Steps to Reproduce

1. Define two types that refer to each other as attributes.
2. Define a child type that inherits from a parent, while the parent refers to the child.
3. Import the generated classes.

* **Attribute Cycle**: [CircularFailure.rosetta](https://github.com/finos/rune-python-generator/blob/feature/function_support/test/python_unit_tests/features/model_structure/CircularFailure.rosetta.manifest_fail)

```rosetta
type CircularA:
    b CircularB (1..1)

type CircularB:
    a CircularA (1..1)
```

**Test Cases:**

* JUnit: `PythonCircularDependencyTest.testAttributeCircularDependency` ([testAttributeCircularDependency](https://github.com/finos/rune-python-generator/blob/feature/function_support/src/test/java/com/regnosys/rosetta/generator/python/object/PythonCircularDependencyTest.java))
* Python: `test_inheritance_cycle.py.manifest_fail` ([test_inheritance_cycle](https://github.com/finos/rune-python-generator/blob/feature/function_support/test/python_unit_tests/features/model_structure/test_inheritance_cycle.py.manifest_fail))

* **Inheritance Cycle**: [InheritanceCycle.rosetta](https://github.com/finos/rune-python-generator/blob/feature/function_support/test/python_unit_tests/features/model_structure/InheritanceCycle.rosetta.manifest_fail)

```rosetta
type Parent:
    child Child (0..1)

type Child extends Parent:
    val int (1..1)
```

**Test Cases:**

* JUnit: `PythonCircularDependencyTest.testInheritanceCircularDependency` ([testInheritanceCircularDependency](https://github.com/finos/rune-python-generator/blob/feature/function_support/src/test/java/com/regnosys/rosetta/generator/python/object/PythonCircularDependencyTest.java))
* Python: `test_circular_failure.py.manifest_fail` ([test_circular_failure](https://github.com/finos/rune-python-generator/blob/feature/function_support/test/python_unit_tests/features/model_structure/test_circular_failure.py.manifest_fail))

### Expected Result

Definitions should be correctly ordered or forward-referenced using String type hints to allow successful import of the bundle.

### Actual Result

`NameError: name '...Parent' is not defined` occurs because the generator emitted `Child` before `Parent`.

### Additional Context

Inheritance cycles are the most critical manifestation of this issue. One potential solution to enable lazy model construction and resolve these cycles is the emission of string-based forward references.

---

Title: Bundle Loading Performance (Monolithic `_bundle.py`)

### Problem Description

The current architecture generates all model types (classes, enums, functions) into a single `_bundle.py` file. For large models like the Common Domain Model (CDM), this results in a monolithic module that must be fully parsed and executed by Python upon any import. This causes significant startup latency and high memory overhead even when only a small subset of the model is needed.

### Steps to Reproduce

1. Generate the full CDM using the Python generator.
2. Attempt to import any type from the model (e.g., `from cdm.base.datetime.AdjustableDate import AdjustableDate`).
3. Measure the time spent in the `import` statement and peak memory usage.

### Expected Result

The model should support granular or lazy loading, allowing sub-packages to be imported independently without parsing the entire universe of types.

### Actual Result

Every import triggers the execution of the entire `_bundle.py`, leading to multi-second delays for large models.

### Additional Context

Resolving this is functionally blocked by the **Circular Dependencies** issue.

---

Title: FQN Type Hints for Clean API (Dots vs. Underscores)

### Problem Description

The current generator uses "bundle-mangled" names with underscores (e.g., `cdm_base_datetime_AdjustableDate`) in function and constructor signatures to avoid collisions. This makes the generated Python API feel non-standard compared to the Rune/Rosetta FQN model.

### Steps to Reproduce

Example Rune code: Standard Rosetta type definition.

```rosetta
namespace cdm.base.datetime

type AdjustableDate:
   unadjustedDate date (1..1)
```

1. Generate code for a function taking a complex Rune type.
2. Inspect the generated Python function signature.

**Test Case:**

* Observation: Check generated `_bundle.py` for underscore-separated class names vs dotted FQNs.

### Expected Result

Type hints should use fully qualified, period-delimited names (as strings) that match the Rune namespaces: `val: "cdm.base.datetime.AdjustableDate"`.

### Actual Result

Function signature uses `val: cdm_base_datetime_AdjustableDate`.

### Additional Context

Depends on the implementation of forward-referencing.

---

Title: Unmapped External Function Calls ([codeImplementation])

### Problem Description

The Rune standard allows functions (defined in `.rosetta` files) to be marked with `[codeImplementation]`, indicating logic provided by the host language. The Python generator does not yet emit the syntax to delegate these calls to Python external implementations.

### Steps to Reproduce

Example Rune code: [import-overlap.rosetta](https://github.com/finos/rune-dsl/blob/feature/function_support/rune-integration-tests/src/test/resources/generation-regression-tests/name-escaping/model/import-overlap.rosetta)

```rosetta
func MyFunc:
  [codeImplementation]
  output:
    result int (1..1)
```

1. Define a Rune function with the `[codeImplementation]` annotation.
2. Attempt to generate Python code for this function.

### Expected Result

The generator should emit a call to a standard Python registry or dispatcher where the user can provide the implementation.

### Actual Result

The generator produces an empty body or fails to map the implementation.

### Additional Context

Critical for CDM validation functions like `ValidateFpMLCodingSchemeDomain`.

---

Title: Conditions on Basic Types (Strings)

### Problem Description

Rune (using `.rosetta` files) allows attaching conditions directly to basic type aliases (e.g., `typeAlias BusinessCenter: string condition IsValid`). The Python generator does not yet support wrapping simple types to trigger these validators upon assignment.

### Steps to Reproduce

Example Rune code: (Type alias with condition)

```rosetta
typeAlias Currency:
    string

    condition C:
        item count = 3
```

1. Define a `typeAlias` on a basic type (string, number, etc.) with an attached `condition`.
2. Generate Python code and check the Pydantic field definition for the alias.

### Expected Result

The assignment should be intercepted and validated according to the condition.

### Actual Result

No validation occurs as the type is treated as a plain Python `str`.

### Additional Context

This is a missing feature in the current Python model generation.

---

## Resolved Issues

### 1. Inconsistent Numeric Types (Literals and Constraints)

Rune `number` and `int` were handled inconsistently, leading to precision loss and `TypeError` when interacting with Python `Decimal` fields in financial models.

* **Resolution**: Strictly mapped Rune `number` to Python `Decimal`. Literals and constraints are now explicitly wrapped in `Decimal('...')` during generation.
* **Status**: Fixed ([`fe798c1`](https://github.com/finos/rune-python-generator/commit/fe798c1))
* **Summary of Impact**:
  * **Precision**: Ensures that calculations involving monetary amounts maintain exact precision, avoiding the pitfalls of floating-point arithmetic.
  * **Type Safety**: Prevents runtime crashes when Pydantic models expect `Decimal` but receive `float` or `int`.

### 2. Redundant Logic Generation

Expression logic was previously duplicated between different generator components, leading to maintenance overhead and potential diverging implementations of the same DSL logic.

* **Resolution**: Centralized all expression logic within `PythonExpressionGenerator`. Removed `generateIfBlocks` from the higher-level function generator to prevent duplicate emission of conditional statements.
* **Status**: Fixed ([`2fb276d`](https://github.com/finos/rune-python-generator/commit/2fb276d))
* **Summary of Impact**:
  * **Maintainability**: Logic changes (like the Switch fix) only need to be implemented in one place.
  * **Code Quality**: The generated Python is cleaner and follows a predictable pattern for side-effecting blocks.

### 3. Missing Function Dependencies (Recursion & Enums)

The dependency provider failed to identify imports for types deeply nested in expressions or those referenced via `REnumType`.

* **Resolution**: Implemented recursive tree traversal in `PythonFunctionDependencyProvider` and added explicit handling for Enum types to ensure they are captured in the import list.
* **Status**: Fixed ([`4878326`](https://github.com/finos/rune-python-generator/commit/4878326))
* **Summary of Impact**:
  * **Runtime Stability**: Resolves `NameError` exceptions in generated code where functions used types that were not imported at the top of the module.
  * **Enum Integration**: Functions can now safely use Rosetta-defined enums in conditions and assignments.

### 4. Robust Dependency Management (DAG Population)

The generator's dependency graph (DAG) previously failed to capture attribute-level dependencies and function-internal dependencies, leading to `NameError` exceptions when a class or function was defined after it was referenced.

* **Resolution**: Implemented recursive dependency extraction in `PythonModelObjectGenerator` and `PythonFunctionGenerator`. The DAG now includes edges for all class attributes, function inputs/outputs, and symbol references, guaranteeing a topologically correct definition order in `_bundle.py` for acyclic dependencies.
* **Status**: Fixed ([`b813ff0`](https://github.com/finos/rune-python-generator/commit/b813ff0))
* **Verification**: `PythonFunctionOrderTest` (Java) confirms strict ordering for linear dependencies.
* **Summary of Impact**:
  * **Generation Stability**: Eliminates `NameError` causing build failures.
  * **Correctness**: Ensures that the generated Python code adheres to the "define-before-use" principle required by the language.

### 5. Mapping of [metadata id] for Enums

Enums with metadata constraints failed to support validation and key referencing because they were generated as plain Python `Enum` classes, which cannot carry metadata payloads or validators.

* **Resolution**: Enums with explicit metadata (e.g., `[metadata id]`, `[metadata reference]`) are now wrapped in `Annotated[Enum, serializer(), validator()]`. The `serializer` and `validator` methods are provided by the `EnumWithMetaMixin` runtime class.
* **Status**: Fixed ([`b813ff0`](https://github.com/finos/rune-python-generator/commit/b813ff0))
* **Summary of Impact**:
  * **Feature Parity**: Brings Enums up to par with complex types regarding metadata support.
  * **Validation**: Enables `@key` and `@ref` validation for Enum fields.

### 6. Inconsistent Type Mapping (Centralization)

Type string generation was scattered across multiple classes, making it impossible to implement global features like string forward-referencing or custom cardinality formatting.

* **Resolution**: Centralized type mapping and formatting in `RuneToPythonMapper`, adding a flexible `formatPythonType` method that handles both legacy and modern typing styles.
* **Status**: Fixed ([`fe798c1`](https://github.com/finos/rune-python-generator/commit/fe798c1))
* **Summary of Impact**:
  * **Extensibility**: Enabled the groundwork for "String Forward References".
  * **Cardinality Control**: Standardized how `list[...]` and `Optional[...]` are generated.

### 7. Switch Expression Support

`generateSwitchOperation` returned a block of statements instead of a single expression, causing `SyntaxError` during variable assignment.

* **Resolution**: Encapsulated switch logic within a unique helper function closure (`_switch_fn_0`) and returned a call to that function.
* **Status**: Fixed ([`4d394c5`](https://github.com/finos/rune-python-generator/commit/4d394c5))

---
Title: Standard Library for External Data (Codelist Support)

### Problem Description

Rune models often rely on external data sources, such as FpML coding schemes or ISO code lists, for validating field values. In the Java implementation, this is handled by a standard `CodelistLoader`. The Python generator and runtime currently lack an equivalent mechanism to load, cache, and query these external data sources during validation.

### Steps to Reproduce

Example Rune code: (Business Center with metadata scheme)

```rosetta
type BusinessCenters:
    businessCenter string (1..*)
        [metadata scheme]
```

1. Define a Rune field that refers to an external scheme (e.g., `[metadata scheme]`).
2. Generate Python code and attempt to validate an instance with an external value.
3. Observe that no validation logic is emitted, or it fails due to the missing library.

### Expected Result

A standard Python library or runtime utility should be available to handle external data loading, allowing the generator to emit consistent validation calls.

### Actual Result

There is no "Standard Library" for external data in the Python runtime, leaving external scheme validation unhandled.

### Additional Context

This has a major impact on the **Common Domain Model (CDM)**, which uses external schemes for validating dozens of critical fields (e.g., Business Centers, Currencies, Instrument Identifiers). Without this support, the Python-generated CDM cannot achieve full validation parity with the Java implementation.

---

## Backlog: Proposed Architectural / Runtime Changes

* **Uniform Enum Metadata Wrapper**: Implement a global runtime proxy for all Enums to allow attaching metadata (keys/references) consistently.

### Problem Description

Plain Enums in the current implementation do not have a uniform wrapper at runtime. This leads to inconsistent behavior when trying to attach metadata dynamically to any enum instance, whereas complex types always have a metadata dictionary.

### Steps to Reproduce

Example Rune code: [EnumMetadata.rosetta](https://github.com/finos/rune-python-generator/blob/feature/function_support/test/python_unit_tests/features/model_structure/EnumMetadata.rosetta)

```rosetta
enum CurrencyEnum:
    USD
    EUR
    GBP

type CashTransfer:
    amount number (1..1)
    currency CurrencyEnum (1..1)
        [metadata id]
```

1. Define an Enum without explicit metadata.
2. Attempt to treat it as a metadata-carrying object at runtime.

### Expected Result

All enums should behave consistently at runtime, ideally wrapped in a proxy that can hold a metadata payload.

### Actual Result

Only Enums with explicit metadata annotations are wrapped; plain ones remain as standard Python `Enum` members.

**Test Case:**

* JUnit: `PythonEnumMetadataTest.testEnumWithoutMetadata` (Disabled) ([Link](https://github.com/finos/rune-python-generator/blob/feature/function_support/src/test/java/com/regnosys/rosetta/generator/python/object/PythonEnumMetadataTest.java))
* Python: `test_enum_metadata.py::test_enum_metadata_behavior` (Skipped) ([Link](https://github.com/finos/rune-python-generator/blob/feature/function_support/test/python_unit_tests/features/model_structure/test_enum_metadata.py))

### Additional Context

Necessary for metadata handling consistency across types and enums.
