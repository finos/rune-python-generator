# Python Unit Test Alignment Release Notes

Date: 2026-03-23

## Overview

This release corrects naming and structural misalignments between Rosetta source files, their namespaces, and the corresponding Python test scripts. The goal is that any element can be located by navigating from the Rune name to the Python test without needing to know project-specific exceptions.

The guiding rule applied throughout: **the Rosetta filename (CamelCase), the declared namespace (snake_case segments), and the Python test filename (`test_<snake_case>.py`) must all agree**.

---

## Changes

### 1. Namespace corrected in `EnumQualifiedPathName.rosetta`

| | Before | After |
|---|---|---|
| Rosetta file | `EnumQualifiedPathName.rosetta` | `EnumQualifiedPathName.rosetta` (unchanged) |
| Namespace | `rosetta_dsl.test.semantic.ENUM` | `rosetta_dsl.test.semantic.enum_qualified_path_name` |
| Python test | *(missing)* | `test_enum_qualified_path_name.py` |

The namespace `ENUM` (uppercase) did not match the filename and violated the lowercase snake_case convention used everywhere else. Renamed to `enum_qualified_path_name` so it derives directly from the Rosetta filename. A corresponding test file was added.

---

### 2. `ChoiceDeepPath.rosetta` renamed to `DeepPath.rosetta`

| | Before | After |
|---|---|---|
| Rosetta file | `ChoiceDeepPath.rosetta` | `DeepPath.rosetta` |
| Namespace | `rosetta_dsl.test.semantic.deep_path` | `rosetta_dsl.test.semantic.deep_path` (unchanged) |
| Python test | `test_deep_path.py` | `test_deep_path.py` (unchanged) |

The `Choice` prefix in the filename was not reflected in the namespace or the test file name, making the file harder to find. The filename now matches the namespace.

---

### 3. `CardinalityTests.rosetta` renamed to `Cardinality.rosetta`

| | Before | After |
|---|---|---|
| Rosetta file | `CardinalityTests.rosetta` | `Cardinality.rosetta` |
| Namespace | `rosetta_dsl.test.semantic.cardinality` | `rosetta_dsl.test.semantic.cardinality` (unchanged) |
| Python test | `test_cardinality.py` | `test_cardinality.py` (unchanged) |

The `Tests` suffix on the Rosetta filename is not reflected in the namespace or test name, and is inconsistent with all other Rosetta files in the suite (none of which carry a `Tests` suffix). Removed.

---

### 4. `ConditionsTests.rosetta` renamed to `Conditions.rosetta`

| | Before | After |
|---|---|---|
| Rosetta file | `ConditionsTests.rosetta` | `Conditions.rosetta` |
| Namespace | `rosetta_dsl.test.semantic.conditions` | `rosetta_dsl.test.semantic.conditions` (unchanged) |
| Python test | `test_conditions.py` | `test_conditions.py` (unchanged) |

Same issue as `CardinalityTests.rosetta`: the `Tests` suffix was inconsistent with the namespace and test filename. Removed.

---

### 5. `test_entity_reuse.py` renamed to `test_reuse_type.py`

| | Before | After |
|---|---|---|
| Rosetta file | `ReuseType.rosetta` | `ReuseType.rosetta` (unchanged) |
| Namespace | `rosetta_dsl.test.model.reuse_type` | `rosetta_dsl.test.model.reuse_type` (unchanged) |
| Python test | `test_entity_reuse.py` | `test_reuse_type.py` |

`entity_reuse` is semantically unrelated to the Rosetta name `ReuseType` / namespace `reuse_type`. Renamed the test file to match.

---

### 6. `test_class_member_access_operator.py` renamed to `test_class_member_access.py`

| | Before | After |
|---|---|---|
| Rosetta file | `ClassMemberAccess.rosetta` | `ClassMemberAccess.rosetta` (unchanged) |
| Namespace | `rosetta_dsl.test.model.class_member_access` | `rosetta_dsl.test.model.class_member_access` (unchanged) |
| Python test | `test_class_member_access_operator.py` | `test_class_member_access.py` |

The `_operator` suffix was not present in the Rosetta filename or namespace. Removed to restore alignment.

---

### 7. `test_multiline.py` added for `Multiline.rosetta`

`Multiline.rosetta` had no corresponding test file. Added `test_multiline.py` as a smoke test confirming that a type with a multiline Rosetta docstring compiles to valid, instantiable Python.

---

### 8. Distinguishing comments added to switch test files

Two separate test areas cover the `switch` keyword:

- `language/Switch.rosetta` / `language/test_switch_operator.py` — tests `switch` used as a **condition guard** inside a type definition (literal, choice, and enum guards)
- `expressions/SwitchOp.rosetta` / `expressions/test_switch_op.py` — tests `switch` used as a **value-returning expression** inside a function

A short comment was added to the module docstring of each file to make this distinction visible at a glance.

---

## Files changed

| File | Change |
|---|---|
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
