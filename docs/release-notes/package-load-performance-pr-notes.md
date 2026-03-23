# PR Notes: `feature/package-load-performance` → `feature/function_support`

Date: 2026-03-23

---

## Summary

This PR delivers a significant CDM package load time improvement, a set of generator refactors driven by a formal code review, cleanup of the `_FQRTN` internal attribute, a JUnit test suite reorganisation, and Python unit test naming alignment.

---

## 1. Package Load Performance — Standalone vs Bundled Emission

**Problem**: The generator previously placed every `Data` type into a single `_bundle.py` file, regardless of whether it actually participated in a circular dependency. Every class called `model_rebuild()` at import time. For CDM 6.0 (973 core types plus metadata wrappers), this produced a load time of approximately 2 minutes.

**Change**: Types are now partitioned by a Kosaraju SCC analysis of the full dependency graph:

- **Standalone** (acyclic, ~94%): Each type is emitted to its own `.py` file. Annotations are written directly in the class body. No `model_rebuild()` at import time.
- **Bundled** (cyclic, ~6%): Types that form a mutual circular dependency are grouped into `_bundle.py` and continue to use the Phase 1/2/3 deferred-annotation pattern with `model_rebuild()`.

**CDM 6.0 partition metrics**:

| Metric | Value |
|---|---|
| Total Rosetta Types | 973 |
| Standalone (acyclic) | 911 (~93.6%) |
| Bundled (cyclic) | 62 (~6.4%) |
| Cycle groups (SCCs) | 8 |
| Largest cycle group | 44 types (`Trade`, `Product`, `EconomicTerms`, `Payout`, …) |

**Result**: Load time reduced from ~120 seconds to ~15 seconds.

**Generator files changed**: `PythonCodeGenerator.java`, `PythonCodeGeneratorContext.java`, `PythonModelObjectGenerator.java`, `PythonAttributeProcessor.java`, `PythonFunctionGenerator.java`, `PythonGeneratorTestUtils.java`, `PartitioningMetricsCLI.java` (new diagnostic tool).

Full details: [package-load-performance-release-notes.md](package-load-performance-release-notes.md)

---

## 2. Generator Refactoring (Code Review Findings)

A code review of the generator (`docs/code-review.md`) identified organisational, efficiency, and duplication issues. Changes made:

- Removed dead code and internal helper duplication across `PythonCodeGenerator.java`, `PythonCodeGeneratorContext.java`, `PythonAttributeProcessor.java`, and `PythonModelObjectGenerator.java`.
- Improved separation of concerns between the scan pass and the emit pass.
- Added `PythonCodeWriter.java` utility to consolidate output formatting.
- Updated `PythonCircularDependencyTest.java` and `PythonPartitioningTest.java` to reflect the refactored internal structure.

---

## 3. `_FQRTN` Cleanup

`_FQRTN` (the fully-qualified Rune type name attribute) was previously emitted into every generated class so that `rune_serialize` could write the `@type` field in serialised JSON. With standalone emission, this attribute is now only needed inside `_bundle.py` — standalone classes are placed in modules whose dotted path already equals the Rune type name.

**Changes**:
- `PythonModelObjectGenerator.java`: stopped emitting `_FQRTN` for standalone classes.
- `PythonCircularDependencyTest.java`, `PythonPartitioningTest.java`: updated assertions accordingly.

Full details: [cdm-import-fixes-release-notes.md](cdm-import-fixes-release-notes.md)

---

## 4. JUnit Test Suite Reorganisation

The JUnit test suite was reorganised from **54 classes** down to **22 classes**.

Key consolidations:

| Area | Before | After |
|---|---|---|
| `expressions/` | 22 single-operation classes (`Rosetta*`) | 5 classes (`Python*ExpressionTest`) |
| `functions/` | 11 classes | 5 classes |
| `object/` | Mixed naming, split packages | 10 consistently named classes |
| `rule/`, `syntax/` | Orphaned sub-packages | Dissolved; classes moved into `object/` |

Full details: [unit-test-refactoring.md](unit-test-refactoring.md)

---

## 5. Python Unit Test Naming Alignment

Naming inconsistencies between Rosetta source files, declared namespaces, and Python test scripts were corrected so that each element can be located by navigating from the Rune name without project-specific knowledge.

| Change | Detail |
|---|---|
| `EnumQualifiedPathName.rosetta` namespace | `ENUM` → `enum_qualified_path_name` |
| `ChoiceDeepPath.rosetta` | Renamed → `DeepPath.rosetta` |
| `CardinalityTests.rosetta` | Renamed → `Cardinality.rosetta` |
| `ConditionsTests.rosetta` | Renamed → `Conditions.rosetta` |
| `test_entity_reuse.py` | Renamed → `test_reuse_type.py` |
| `test_class_member_access_operator.py` | Renamed → `test_class_member_access.py` |
| `test_enum_qualified_path_name.py` | **New** — was missing for `EnumQualifiedPathName.rosetta` |
| `test_multiline.py` | **New** — smoke test for multiline docstring generation |
| Switch test files | Comments added distinguishing condition-guard vs expression usage |

Full details: [python-unit-test-alignment.md](python-unit-test-alignment.md)

---

## Files Changed (summary)

| Area | Insertions | Deletions |
|---|---|---|
| Generator (Java) | ~2,400 | ~1,200 |
| JUnit tests (Java) | ~3,100 | ~3,300 |
| Python unit tests | ~200 | ~10 |
| Documentation | ~1,100 | ~93 |
| **Total** | **~6,300** | **~4,600** |

91 files changed across generator source, tests, and documentation.
