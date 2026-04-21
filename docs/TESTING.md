# Testing Guide for Contributors

This document describes the two test suites used to verify the Rune Python Generator: the **JUnit generator tests** and the **Python unit tests**. Both suites must pass before any change is merged.

For build and run instructions, see [BUILD_AND_TEST.md](BUILD_AND_TEST.md).

---

## 1. JUnit Generator Tests

### What They Test

The JUnit suite verifies that the generator produces correct Python output from a given Rune model. Each test:

1. Defines a Rune model as an inline Java string.
2. Invokes the generator via `PythonGeneratorTestUtils`.
3. Asserts that the generated Python contains (or does not contain) expected strings.

These tests run during `mvn clean package` and are the first line of defence — they catch regressions in the generation logic before any Python is executed.

### How Tests Are Structured

#### Parsing and Generation

`PythonGeneratorTestUtils` provides two generation methods:

| Method | Returns | Use when |
| :--- | :--- | :--- |
| `generatePythonFromString(model)` | `Map<String, CharSequence>` (file path → content) | Asserting on a specific generated file |
| `generatePythonAndExtractBundle(model)` | `String` (all files concatenated) | Asserting across all generated output at once |

A typical test uses `generatePythonFromString` and then looks up an individual file by its generated path:

```java
Map<String, CharSequence> gf = testUtils.generatePythonFromString(modelString);
String classFile = gf.get("src/test/my_namespace/MyType.py").toString();
testUtils.assertGeneratedContainsExpectedString(classFile, "class MyType(BaseDataClass):");
```

#### Assertion Helpers

`PythonGeneratorTestUtils` provides the following helper methods:

| Method | Purpose |
| :--- | :--- |
| `assertGeneratedContainsExpectedString(generated, expected)` | Asserts the output contains the expected substring |
| `assertGeneratedDoesNotContain(generated, unexpected)` | Asserts the output does not contain the substring |
| `assertImportAppearsExactlyOnce(generated, import)` | Guards against duplicate import statements |
| `assertAppearsAfter(generated, first, second)` | Asserts ordering — `first` precedes `second` in the output |
| `assertBundleContainsExpectedString(model, expected)` | Concatenates all generated files and searches across them |

#### Test Styles

There are two complementary test styles. Both are present in the suite.

**Targeted tests** assert that a specific snippet appears in a specific file. They are precise, resistant to unrelated changes, and are the preferred style for new tests:

```java
// Asserts the Phase 1 placeholder is correct
testUtils.assertGeneratedContainsExpectedString(bundleFile,
    "CollateralIssuerType: None = Field(None, ...)");

// Asserts the Phase 2 annotation update is correct
testUtils.assertGeneratedContainsExpectedString(bundleFile,
    "MyClass.__annotations__[\"attr\"] = Optional[Annotated[...]]");
```

**Complete code tests** (also called "Anchor" or "gold standard" tests) assert on a larger, structural slice of the output — for example, verifying that a full Phase 1/2/3 bundle is assembled correctly. These are fewer in number and are used where overall structure matters more than individual lines:

```java
String allOutput = testUtils.generatePythonAndExtractBundle(model);
testUtils.assertGeneratedContainsExpectedString(allOutput, expectedFullStructure);
```

#### Test Categories

Each test is informally categorised by what it verifies:

| Category | Focus |
| :--- | :--- |
| **Logic** | Generator branching, nesting, accumulation rules |
| **Functional** | Specific Rune→Python operation mappings (e.g., `count`, `filter`) |
| **Component** | Building blocks: Enums, Inheritance hierarchies |
| **Anchor** | "Gold standard" end-to-end structure — Phase 1/2/3 pattern, full type bundles |

### Package Organization

The JUnit tests are organized under `src/test/java/.../python/` into three packages:

```
python/
├── object/         # Data type generation (classes, conditions, metadata, enums, inheritance)
├── expressions/    # Expression generation (arithmetic, collections, conditionals, etc.)
└── functions/      # Function generation (definitions, dispatch, native functions, etc.)
```

#### `object/` Package

| Class | What it tests |
| :--- | :--- |
| `PythonStandaloneStructureTest` | Standalone file structure for acyclic types |
| `PythonScalarTypeTest` | Scalar and parameterised type hints |
| `PythonInheritanceTest` | Type inheritance and attribute override |
| `PythonEnumTest` | Enum generation and display names |
| `PythonCircularDependencyTest` | Phase 1/2/3 bundle structure for cyclic types |
| `PythonPartitioningTest` | Standalone vs. bundled class partitioning |
| `PythonConditionTest` | Data rules and object conditions |
| `PythonMetadataTest` | Metadata annotations (keys, references, schemes) |
| `PythonTypeAliasTest` | `typeAlias` generation |
| `PythonNameCollisionTest` | Field-name / type-name aliasing to avoid Pydantic conflicts |

#### `expressions/` Package

| Class | What it tests |
| :--- | :--- |
| `PythonArithmeticExpressionTest` | Math operations and type conversions |
| `PythonCollectionExpressionTest` | List operations (`filter`, `map`, `count`, `flatten`, `distinct`, `join`, …) |
| `PythonConditionalExpressionTest` | `if`/`else` and `switch` expressions |
| `PythonExistsExpressionTest` | `exists` and `only exists` assertions |
| `PythonObjectExpressionTest` | Constructor expressions, `as-key`, shortcuts |

#### `functions/` Package

| Class | What it tests |
| :--- | :--- |
| `PythonFunctionDefinitionTest` | Basic function structure, inputs, outputs, aliases |
| `PythonFunctionOutputTest` | `add` operations and list-valued outputs |
| `PythonFunctionConditionTest` | Pre- and post-conditions on functions |
| `PythonFunctionDispatchTest` | Enum-based function overloading and ordering |
| `PythonFunctionExtensionsTest` | Native functions (`[codeImplementation]`) and `with-meta` |

### Writing a New JUnit Test

1. Choose the appropriate package (`object/`, `expressions/`, or `functions/`).
2. Add a `@Test` method to the relevant class, or create a new class if the topic does not fit any existing class.
3. Define the minimal Rune model needed to exercise the feature.
4. Use **targeted assertions** (`assertGeneratedContainsExpectedString` on a specific file) unless you need to verify the overall structural pattern, in which case use a complete-code assertion.
5. Avoid asserting on large verbatim blocks — they are brittle and break on unrelated formatting changes. Instead, assert on the key lines that are unique to the feature under test.
6. If a test spans the Phase 1/2/3 pattern, add separate assertions for:
   - The **Phase 1** clean attribute (in the class body)
   - The **Phase 2** `__annotations__` update (after all class definitions)
   - The **Phase 3** `model_rebuild()` call (at the end of the bundle)

---

## 2. Python Unit Tests

### What They Test

The Python unit tests verify that the **generated Python code** is correct and executable. They test runtime behavior: object construction, field access, function invocation, serialization, null propagation, and so on.

Unlike the JUnit tests, which inspect generated source text, the Python unit tests actually *run* the generated code.

### How Tests Work

The `run_python_unit_tests.sh` script handles the full pipeline:

1. **Generate** — invokes the CLI generator on all `.rosetta` files in `python-test/unit-tests/`, emitting Python to `target/python-tests/unit_tests/`.
2. **Package** — builds the generated code as a wheel (`python_rosetta_dsl-0.0.0-py3-none-any.whl`).
3. **Install** — sets up a fresh virtual environment and installs the wheel and its dependencies.
4. **Native functions** — finds and runs any `build_and_install.sh` scripts under `python-test/unit-tests/` to install native function implementations.
5. **Test** — runs `pytest` over the target directory.

The generation step always processes *all* Rune sources (not just those for the tests being run) to ensure the complete model is available. The `pytest` step can be scoped to a subdirectory.

### Feature Directory Organization

Tests are grouped into feature directories under `python-test/unit-tests/features/`:

```
features/
├── collections/     # List and collection behavior
├── conversions/     # Type conversion (to-string, to-int, to-number, etc.)
├── expressions/     # Expression evaluation (closures, conditionals, switch, etc.)
├── functions/       # Function invocation, outputs, native functions
├── language/        # Language constructs (conditions, enums, switch guards, etc.)
├── model-structure/ # Data types, inheritance, metadata, key/ref
├── operators/       # Arithmetic, comparison, logical, and boolean operators
├── robustness/      # Null handling and edge cases
└── serialization/   # Date, time, and zoned-datetime serialization
```

Each feature area contains one or more pairs of files:

| File | Role |
| :--- | :--- |
| `CamelCase.rosetta` | Rune model definition |
| `test_snake_case.py` | pytest-compatible test functions |

### Naming Convention

The Rosetta filename, declared namespace, and Python test filename must all agree:

| Element | Convention | Example |
| :--- | :--- | :--- |
| Rosetta file | `CamelCase.rosetta` | `FilterOperation.rosetta` |
| Declared namespace (leaf) | `snake_case` | `filter_operation` |
| Python test file | `test_<leaf>.py` | `test_filter_operation.py` |

The test file imports from the generated package using the full dotted namespace path. For example, a type `MyType` in namespace `rosetta_dsl.test.expressions.filter_operation` is imported as:

```python
from rosetta_dsl.test.expressions.filter_operation.MyType import MyType
```

### Writing a Python Unit Test

1. Add or identify the `.rosetta` file in the appropriate `features/` subdirectory.
2. Ensure the namespace leaf matches the filename in `snake_case`.
3. Create `test_<snake_case>.py` with standard pytest functions (`def test_...():`).
4. Import types and functions directly from the generated namespace.
5. Write assertions against the runtime behavior — instantiation, field values, function results.

A minimal example (from `features/functions/`):

```python
from rosetta_dsl.test.functions.add_operation.UnitType import UnitType
from rosetta_dsl.test.functions.add_operation.functions.FilterQuantity import FilterQuantity

def test_add_operation():
    fx_eur = UnitType(currency="EUR")
    fx_jpy = UnitType(currency="JPY")
    result = FilterQuantity(quantities=[...], unit=fx_jpy)
    assert len(result) == 1
    assert result[0].unit.currency == "JPY"
```

### Expected Failures

Files with the suffix `.manifest_fail` (e.g., `CircularFailure.rosetta.manifest_fail`) are Rune sources or test scripts that are expected to fail at generation or test time. They document known limitations and are excluded from normal test runs.

### Running a Subset of Tests

```sh
# Run only expression tests
python-test/unit-tests/run_python_unit_tests.sh features/expressions

# Run with an existing virtual environment (faster for repeated runs)
python-test/unit-tests/run_python_unit_tests.sh --reuse-env

# Keep the virtual environment active after the run
python-test/unit-tests/run_python_unit_tests.sh --no-clean

# Show all options
python-test/unit-tests/run_python_unit_tests.sh --help
```
