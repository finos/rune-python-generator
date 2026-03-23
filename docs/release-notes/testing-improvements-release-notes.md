# Testing Improvements Release Notes

This document consolidates all improvements made to the JUnit generator test suite and the Python unit test suite across recent releases. It covers:

1. [JUnit test strategy: targeted vs. complete-code tests](#1-junit-test-strategy)
2. [JUnit test suite reorganization: 54 → 22 classes](#2-junit-test-suite-reorganization)
3. [Python unit test naming alignment](#3-python-unit-test-naming-alignment)

---

## 1. JUnit Test Strategy

**Date**: 2026-02-10

### Motivation

The original JUnit tests were all "complete code" tests: the generator produced Python from a Rune model and the test asserted that a large verbatim section of the output matched exactly. This approach had a significant maintenance cost:

- Any change to the generator — even a whitespace or formatting adjustment — could break dozens of tests simultaneously, even when the semantic output was correct.
- The tests were difficult to read: the expected strings were hundreds of lines of generated Python embedded in Java source.
- Tests were not meaningfully distinguishable: they all asserted on large blobs, making it hard to identify which feature a failing test was exercising.

### Changes

A mixed strategy was introduced, combining "complete code" (Anchor) tests with smaller, focused (Targeted) tests.

**Complete code / Anchor tests** retain full output comparisons for scenarios where overall structural correctness matters — in particular, verifying that the Phase 1/2/3 deferred-annotation pattern is assembled correctly for circular dependencies. These tests are few in number and are treated as the "gold standard" for the generation architecture.

**Targeted tests** assert that specific, semantically meaningful lines appear in the generated output — for example, that a specific Phase 2 `__annotations__` update is present, or that a specific import statement appears exactly once. These tests are unaffected by unrelated changes elsewhere in the output.

### Test Categories

Tests are informally classified by purpose:

| Category | Focus |
| :--- | :--- |
| **Logic** | Generator branching, nesting, and accumulation rules |
| **Functional** | Specific Rune→Python operation mappings (e.g., `count`, `filter`, `distinct`) |
| **Component** | Fundamental building blocks: Enums, Inheritance hierarchies |
| **Anchor** | "Gold standard" end-to-end tests verifying the full Phase 1/2/3 structure |

### Guidelines for Writing Tests

1. **Prefer targeted assertions**: use `assertGeneratedContainsExpectedString` on a specific generated file rather than on the full concatenated output.
2. **Avoid exact class-body comparisons**: do not compare the entire `class X:` block if it references other Rune types — those lines are subject to change as the Phase 1/2/3 pattern evolves.
3. **Use anchors sparingly**: at least one or two tests (such as `PythonCircularDependencyTest`) should be "gold standard" and verify the entire output structure. New tests should not default to this style.
4. **Phase verification** (for bundled types): assert separately on:
   - The **Phase 1** clean attribute in the class body
   - The **Phase 2** `__annotations__` update after all class definitions
   - The **Phase 3** `model_rebuild()` call at the end of the bundle

---

## 2. JUnit Test Suite Reorganization

### Motivation

Over several development cycles the JUnit test suite had grown to 54 classes with a number of structural problems that made it difficult to navigate and maintain:

- **Inconsistent naming**: expression and operation tests used a `Rosetta*` prefix while everything else used `Python*`, with no meaningful distinction.
- **Redundant suffixes**: many class names included `Generator` (e.g., `PythonEnumGeneratorTest`, `PythonInheritanceGeneratorTest`) even though every class in the suite tests the generator.
- **Fragmented expression tests**: each list or collection operation had its own single-test class (`RosettaJoinOperationTest`, `RosettaDistinctOperationTest`, …), producing 22 classes in the `expressions/` package alone.
- **Overlapping names**: `PythonObjectInheritanceTest` and `PythonInheritanceGeneratorTest` both tested inheritance with no visible distinction between them. Similarly, `PythonCircularDependencyTest` and `PythonCircularReferenceImplementationTest` both tested circular dependencies.
- **Scattered metadata tests**: metadata concerns were spread across four classes using three different naming conventions.
- **Orphaned sub-packages**: `syntax/` and `rule/` each contained a small number of classes that logically belonged in `object/`.
- **Inconsistent method naming**: several methods in `PythonDataRuleGeneratorTest` used `should*`, `check*`, or bare verb names instead of the `test*` convention.

### Changes

The test suite was reorganized from **54 classes** to **22 classes**, with all tests passing. All `Rosetta*` prefixes were replaced with `Python*`, the `Generator` suffix was removed throughout, and the `syntax/` and `rule/` sub-packages were dissolved into `object/`.

#### `object/` Package

| Old class(es) | New class | Change |
| :--- | :--- | :--- |
| `PythonBasicGeneratorTest` | `PythonStandaloneStructureTest` | Renamed to reflect what it actually tests — standalone file structure |
| `PythonBasicTypeGeneratorTest` | `PythonScalarTypeTest` | Renamed: dropped "Generator"; "Basic" → "Scalar" |
| `PythonObjectInheritanceTest` + `PythonInheritanceGeneratorTest` | `PythonInheritanceTest` | Merged two classes covering the same topic |
| `PythonEnumGeneratorTest` + `PythonEnumMetadataTest` | `PythonEnumTest` | Merged; dropped "Generator" |
| `PythonGeneratedStructureTest` + `PythonBundleImportsTest` | `PythonPartitioningTest` | Merged; both test standalone/bundle partitioning |
| `PythonCircularDependencyTest` + `PythonCircularReferenceImplementationTest` | `PythonCircularDependencyTest` | Merged; "ReferenceImplementation" did not distinguish itself |
| `PythonDataRuleGeneratorTest` + `PythonObjectConditionGeneratorTest` + `PythonChoiceGeneratorTest` | `PythonConditionTest` | Merged all condition tests; moved into `object/` |
| `PythonMetaDataGeneratorTest` + `PythonKeyRefTest` + `PythonMetaKeyRefGeneratorTest` | `PythonMetadataTest` | Consolidated all metadata tests; fixed "MetaData" → "Metadata" |
| `PythonTypeAliasTest` | `PythonTypeAliasTest` | Unchanged |
| `PythonNameCollisionTest` | `PythonNameCollisionTest` | Unchanged |

#### `expressions/` Package

22 single-operation classes consolidated into 5:

| Old class(es) | New class |
| :--- | :--- |
| `RosettaExistsExpressionTest` + `RosettaOnlyExistsExpressionTest` | `PythonExistsExpressionTest` |
| `RosettaConditionalExpressionTest` + `RosettaSwitchExpressionTest` + `RosettaChoiceExpressionTest` | `PythonConditionalExpressionTest` |
| `RosettaListOperationTest` + `RosettaFilterOperationTest` + `RosettaMapOperationTest` + `RosettaFlattenOperationTest` + `RosettaDistinctOperationTest` + `RosettaCountOperationTest` + `RosettaAnyOperationTest` + `RosettaContainsOperationTest` + `RosettaDisjointOperationTest` + `RosettaJoinOperationTest` + `RosettaOnlyElementTest` + `PythonReduceOperationTest` | `PythonCollectionExpressionTest` |
| `RosettaMathOperationTest` + `RosettaConversionTest` | `PythonArithmeticExpressionTest` |
| `RosettaConstructorExpressionTest` + `RosettaAsKeyOperationTest` + `RosettaShortcutTest` | `PythonObjectExpressionTest` |

#### `functions/` Package

11 classes consolidated into 5:

| Old class(es) | New class |
| :--- | :--- |
| `PythonFunctionBasicTest` + `PythonFunctionTypeTest` + `PythonFunctionControlFlowTest` + `PythonFunctionAliasTest` | `PythonFunctionDefinitionTest` |
| `PythonFunctionAddOperationTest` + `PythonFunctionListTest` | `PythonFunctionOutputTest` |
| `PythonFunctionConditionTest` | `PythonFunctionConditionTest` (unchanged) |
| `PythonFunctionOverloadingTest` + `PythonFunctionOrderTest` | `PythonFunctionDispatchTest` |
| `PythonFunctionNativeTest` + `PythonFunctionWithMetaTest` | `PythonFunctionExtensionsTest` |

#### Method Renames in `PythonConditionTest` (formerly `PythonDataRuleGeneratorTest`)

| Old name | New name |
| :--- | :--- |
| `shouldGenerateConditionWithIfElseIf` | `testIfElseIfCondition` |
| `shouldGenerateConditionWithNestedIfElseIf` | `testNestedIfElseIfCondition` |
| `shouldCheckInheritedCondition` | `testConditionOnInheritedAttribute` |
| `checkConditionWithInheritedAttribute` | `testConditionReferencingInheritedAttribute` |
| `nestedAnds` | `testNestedAndCondition` |
| `numberAttributeisHandled` | `testNumberAttributeCondition` |
| `dataRuleCoinHead` | `testBooleanConditionTrue` |
| `dataRuleCoinTail` | `testBooleanConditionFalse` |
| `dataRuleCoinEdge` | `testBooleanConditionDefault` |
| `dataRuleWithDoIfAndFunction` | `testConditionWithFunctionCall` |
| `dataRuleWithDoIfAndFunctionAndElse` | `testConditionWithFunctionCallAndElse` |
| `conditionCount` | `testCountCondition` |
| `testExists` | `testExistsCondition` |

#### Method Renames in Other Merged Classes

| Old class | Old method | New class | New method |
| :--- | :--- | :--- | :--- |
| `PythonBasicTypeGeneratorTest` | `testGenerateTypes` | `PythonScalarTypeTest` | `testComplexTypeHierarchy` |
| `PythonBasicTypeGeneratorTest` | `testGenerateTypesMethod2` | `PythonScalarTypeTest` | `testComplexTypeHierarchyWithInheritance` |
| `PythonMetaDataGeneratorTest` | `testAStandalone` | `PythonMetadataTest` | `testKeyMetadata` |
| `PythonMetaDataGeneratorTest` | `testNodeRefStandalone` | `PythonMetadataTest` | `testReferenceMetadata` |
| `PythonMetaDataGeneratorTest` | `testAttributeRefStandalone` | `PythonMetadataTest` | `testAttributeWithDateMetadata` |
| `PythonMetaDataGeneratorTest` | `testRootStandalone` | `PythonMetadataTest` | `testRootWithMetadataDependency` |
| `PythonMetaDataGeneratorTest` | `testSchemeTestStandalone` | `PythonMetadataTest` | `testSchemeMetadata` |
| `PythonMetaKeyRefGeneratorTest` | `testGeneration` | `PythonMetadataTest` | `testIdReferenceLocationMetadata` |
| `PythonChoiceGeneratorTest` | `testGeneration` | `PythonConditionTest` | `testOneOfChoiceCondition` |

---

## 3. Python Unit Test Naming Alignment

**Date**: 2026-03-23

### Motivation

The Python unit tests are organized around the principle that the Rune filename (CamelCase), the declared namespace (snake_case segments), and the Python test file (`test_<snake_case>.py`) must all agree. This makes tests discoverable by navigating from the Rune source — no project-specific knowledge should be required.

Several files in the test suite had grown out of alignment with this convention, making it harder to find the test corresponding to a given Rune source and vice versa.

### Changes

#### 1. Namespace corrected in `EnumQualifiedPathName.rosetta`

| | Before | After |
| :--- | :--- | :--- |
| Namespace | `rosetta_dsl.test.semantic.ENUM` | `rosetta_dsl.test.semantic.enum_qualified_path_name` |
| Python test | *(missing)* | `test_enum_qualified_path_name.py` |

The namespace `ENUM` (uppercase) did not match the filename and violated the `snake_case` convention used throughout the suite. Renamed to derive directly from the Rosetta filename. A corresponding test file was added.

#### 2. `ChoiceDeepPath.rosetta` renamed to `DeepPath.rosetta`

| | Before | After |
| :--- | :--- | :--- |
| Rosetta file | `ChoiceDeepPath.rosetta` | `DeepPath.rosetta` |
| Namespace | `rosetta_dsl.test.semantic.deep_path` | unchanged |

The `Choice` prefix appeared only in the filename — not in the namespace or the test file. Removed to restore alignment.

#### 3. `CardinalityTests.rosetta` renamed to `Cardinality.rosetta`

| | Before | After |
| :--- | :--- | :--- |
| Rosetta file | `CardinalityTests.rosetta` | `Cardinality.rosetta` |
| Namespace | `rosetta_dsl.test.semantic.cardinality` | unchanged |

The `Tests` suffix is inconsistent with all other Rosetta files in the suite (none of which carry it). Removed.

#### 4. `ConditionsTests.rosetta` renamed to `Conditions.rosetta`

| | Before | After |
| :--- | :--- | :--- |
| Rosetta file | `ConditionsTests.rosetta` | `Conditions.rosetta` |
| Namespace | `rosetta_dsl.test.semantic.conditions` | unchanged |

Same issue as `CardinalityTests.rosetta`. Removed the `Tests` suffix.

#### 5. `test_entity_reuse.py` renamed to `test_reuse_type.py`

| | Before | After |
| :--- | :--- | :--- |
| Rosetta file | `ReuseType.rosetta` | unchanged |
| Python test | `test_entity_reuse.py` | `test_reuse_type.py` |

`entity_reuse` has no relationship to the Rune name `ReuseType` or the namespace `reuse_type`. Renamed to match.

#### 6. `test_class_member_access_operator.py` renamed to `test_class_member_access.py`

| | Before | After |
| :--- | :--- | :--- |
| Rosetta file | `ClassMemberAccess.rosetta` | unchanged |
| Python test | `test_class_member_access_operator.py` | `test_class_member_access.py` |

The `_operator` suffix does not appear in the Rosetta filename or namespace. Removed.

#### 7. `test_multiline.py` added for `Multiline.rosetta`

`Multiline.rosetta` had no corresponding test file. `test_multiline.py` was added as a smoke test confirming that a type with a multiline Rosetta docstring generates valid, instantiable Python.

#### 8. Distinguishing comments added to switch test files

Two separate test areas both cover the `switch` keyword:

- `language/Switch.rosetta` / `language/test_switch_operator.py` — tests `switch` used as a **condition guard** inside a type definition (literal, choice, and enum guards).
- `expressions/SwitchOp.rosetta` / `expressions/test_switch_op.py` — tests `switch` used as a **value-returning expression** inside a function.

A short comment was added to the module docstring of each file to make this distinction visible at a glance.

### Files Changed

| File | Change |
| :--- | :--- |
| `features/language/EnumQualifiedPathName.rosetta` | Namespace renamed |
| `features/language/test_enum_qualified_path_name.py` | **New file** |
| `features/language/test_multiline.py` | **New file** |
| `features/language/test_switch_operator.py` | Comment added |
| `features/expressions/test_switch_op.py` | Comment added |
| `features/model-structure/ChoiceDeepPath.rosetta` | **Renamed** → `DeepPath.rosetta` |
| `features/model-structure/CardinalityTests.rosetta` | **Renamed** → `Cardinality.rosetta` |
| `features/model-structure/test_entity_reuse.py` | **Renamed** → `test_reuse_type.py` |
| `features/model-structure/test_class_member_access_operator.py` | **Renamed** → `test_class_member_access.py` |
| `features/language/ConditionsTests.rosetta` | **Renamed** → `Conditions.rosetta` |
