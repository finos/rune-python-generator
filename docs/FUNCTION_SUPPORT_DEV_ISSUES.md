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

### 4. Robust Dependency Management (DAG Population)
The generator's dependency graph (DAG) previously failed to capture attribute-level dependencies and function-internal dependencies, leading to `NameError` exceptions when a class or function was defined after it was referenced.

*   **Resolution**: Implemented recursive dependency extraction in `PythonModelObjectGenerator` and `PythonFunctionGenerator`. The DAG now includes edges for all class attributes, function inputs/outputs, and symbol references, guaranteeing a topologically correct definition order in `_bundle.py` for acyclic dependencies.
*   **Status**: Fixed ([`b813ff0`](https://github.com/finos/rune-python-generator/commit/b813ff0))
*   **Verification**: `PythonFunctionOrderTest` (Java) confirms strict ordering for linear dependencies.
*   **Summary of Impact**:
    *   **Generation Stability**: Eliminates `NameError` causing build failures.
    *   **Correctness**: Ensures that the generated Python code adheres to the "define-before-use" principle required by the language.

### 5. Mapping of [metadata id] for Enums
Enums with metadata constraints failed to support validation and key referencing because they were generated as plain Python `Enum` classes, which cannot carry metadata payloads or validators.

*   **Resolution**: Enums with explicit metadata (e.g., `[metadata id]`, `[metadata reference]`) are now wrapped in `Annotated[Enum, serializer(), validator()]`. The `serializer` and `validator` methods are provided by the `EnumWithMetaMixin` runtime class.
*   **Status**: Fixed ([`b813ff0`](https://github.com/finos/rune-python-generator/commit/b813ff0))
*   **Summary of Impact**:
    *   **Feature Parity**: Brings Enums up to par with complex types regarding metadata support.
    *   **Validation**: Enables `@key` and `@ref` validation for Enum fields.

### 6. Inconsistent Type Mapping (Centralization)
Type string generation was scattered across multiple classes, making it impossible to implement global features like string forward-referencing or custom cardinality formatting.

*   **Resolution**: Centralized type mapping and formatting in `RuneToPythonMapper`, adding a flexible `formatPythonType` method that handles both legacy and modern typing styles.
*   **Status**: Fixed ([`fe798c1`](https://github.com/finos/rune-python-generator/commit/fe798c1))
*   **Summary of Impact**:
    *   **Extensibility**: Enabled the groundwork for "String Forward References".
    *   **Cardinality Control**: Standardized how `list[...]` and `Optional[...]` are generated.

### 7. Switch Expression Support
`generateSwitchOperation` returned a block of statements instead of a single expression, causing `SyntaxError` during variable assignment.

*   **Resolution**: Encapsulated switch logic within a unique helper function closure (`_switch_fn_0`) and returned a call to that function.
*   **Status**: Fixed ([`4d394c5`](https://github.com/finos/rune-python-generator/commit/4d394c5))

---

## Unresolved Issues and Proposals

### 1. Constructor-Related Issues

#### Issue: Fragile Object Building (Direct Constructors)
**Problem**: The generator relies on a magic `_get_rune_object` helper which bypasses IDE checks and is hard to debug.
*   **Recommendation**: Refactor `PythonFunctionGenerator` to use direct Python constructor calls (e.g., `MyClass(attr=val)`).
*   **Status**: **Unresolved**. The codebase currently uses `_get_rune_object`.

#### Issue: Constructor Keyword Arguments SyntaxError
**Problem**: Python forbids duplicate or invalid keyword arguments.
*   **Recommendation**: Use unique counters for missing/duplicate keys.
*   **Proposed Fix**: The generator should use unique fallback keys (`unknown_0`, `unknown_1`, etc.) when property names are missing or invalid.
*   **Recommended Code Changes**: Use an `AtomicInteger` for unique fallback keys in `PythonExpressionGenerator.java`.

#### Issue: Partial Object Construction (Required Fields)
**Problem**: Pydantic's default constructor enforces validation immediately, breaking multi-step `set` operations.
*   **Recommendation**: Use `model_construct()`.
*   **Proposed Solution**: Use `model_construct(**kwargs)` for initial object creation to skip validation, allowing the object to be filled via subsequent `set` calls before final consumption.

---

### 2. Bundle Generation and Dependency Issues

#### Issue: Circular Dependencies / Out-of-Order Definitions (The "Topological Sort" Limitation)
**Manifestation**: `NameError: name 'cdm_base_datetime_BusinessCenterTime' is not defined` during CDM import.

**Problem**: The generator uses a Directed Acyclic Graph (DAG) to order definitions in `_bundle.py`. However, the current implementation only adds edges for **inheritance (SuperTypes)**. It ignores **Attribute types**, leading to out-of-order definitions. Furthermore, Rosetta allows recursive/circular types (e.g., A has attribute B, B has attribute A), which a DAG cannot resolve by design.

**Reproducing Tests**:
*   **JUnit**: `PythonFunctionOrderTest.testFunctionDependencyOrder` (asserts ClassA defined before ClassB).
*   **Python**: `test_functions_order.py` (triggers NameError during Pydantic decorator execution).

**Proposed Alternatives & Recommendation**:
Use **String Forward References + `model_rebuild()`** (The official "Pydantic Way" for v2).
*   **The Hybrid DAG Strategy**: We will continue to use the DAG to organize the definition order of classes, but we will limit its scope to **Inheritance only** (`SuperType`). By using String Forward References for attributes, we eliminate the need for the DAG to handle the complex "web" of references, avoiding cycle detection failures while ensuring that parent classes are always defined before their children.

#### Issue: FQN Type Hints for Clean API (Dots vs. Underscores)
**Problem**: The current generator uses "bundle-mangled" names with underscores (e.g., `cdm_base_datetime_AdjustableDate`) in function and constructor signatures to avoid collisions. 

**Proposal**: Use fully qualified, period-delimited names (e.g., `"cdm.base.datetime.AdjustableDate"`) in all signatures. 
*   **Mechanism**: Utilize a `_type_registry` mapping at the end of the `_bundle.py` that links Rosetta FQNs to the bundled Python class definitions.
*   **Dependency**: This approach **requires** implementing the **String Forward Reference** solution for circular dependencies. 
*   **Benefits**: API Purity (matches CDM/Rosetta names exactly), Consistency, and Encapsulation.

#### Issue: Bundle Loading Performance (Implicit Overhead)
**Problem**: The current "Bundle" architecture results in significant "load-all-at-once" overhead for large models like CDM.

**Proposal**: Evolve the bundle architecture to support **Partitioned Loading** or **Lazy Rebuilds**.
*   **Status**: **Unresolved**. Prerequisite is the fix for Circular Dependencies via String Forward References.

---

---

## Backlog

### Enum Wrappers (Global Proposal)
*   **Problem**: While explicit metadata is now supported via `Annotated`, plain Enums do not have a uniform wrapper object at runtime. This leads to inconsistent behavior if code expects to attach metadata dynamically to any enum instance.
*   **Proposal**: Wrap *all* enums in a proxy class (`_EnumWrapper`) that holds the enum value and a metadata dictionary.
*   **Relevant Tests**:
    *   Java: `PythonEnumMetadataTest.testEnumWithoutMetadata` (Disabled)
    *   Python: `test_enum_metadata.py::test_enum_metadata_behavior` (Skipped)
*   **Usage Note**: This is a proposed architectural change, not a defect fix.

---

### General Support Suggestions

*   **Type Unification Support**: Evaluate if `BaseDataClass` inheriting from a metadata mixin provides a scalable way to handle metadata across various model sizes.
*   **Operator Support**: Consider standardizing helper functions like `rune_with_meta` and `rune_default` to simplify generated code.
