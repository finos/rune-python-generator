# Branch Separation Strategy

This document outlines the strategy for separating the recent changes in the `feature/function_support` branch into two isolated categories: 
1. **Refactoring & Stability:** Changes made to improve the generator's organization, stability, and handling of existing types/DAG dependencies.
2. **New Features:** Additions made specifically to support Python function generation, native external functions, and expanded expression operator generation.

The intention is to create a clean `refactoring-stability` branch that merges solely the refactoring commits (and corresponding tests) into the main line, leaving the function and advanced expression capabilities in the current branch.

---

## 🏗️ Category 1: Refactoring & Stability
These code elements will be moved into the new refactoring branch. They address DAG topological sorting, attribute processing defects, code quality, and testing robustness.

### Java Source Code
- **Model & Attribute Refactoring:**
  - `src/main/java/com/regnosys/rosetta/generator/python/object/PythonModelObjectGenerator.java` (DAG topological sorting and object dependency fixes)
  - `src/main/java/com/regnosys/rosetta/generator/python/object/PythonAttributeProcessor.java` (Refactored logic to group annotations and attribute rebuilds)
  - `src/main/java/com/regnosys/rosetta/generator/python/object/AttributeProcessingResult.java` (New data structure for attribute processing)
- **Enum Metadata Wrappers:**
  - `src/main/java/com/regnosys/rosetta/generator/python/enums/PythonEnumGenerator.java` (Added metadata and validation wrappers)
- **Generator Core & Utilities:**
  - `src/main/java/com/regnosys/rosetta/generator/python/PythonCodeGenerator.java` (Reordering of objects and removing `functions` path segment)
  - `src/main/java/com/regnosys/rosetta/generator/python/util/RuneToPythonMapper.java` (Centralized strict type formatting and numeric typing)
  - `src/main/java/com/regnosys/rosetta/generator/python/util/PythonCodeGeneratorUtil.java`
  - `src/main/java/com/regnosys/rosetta/generator/python/util/PythonCodeWriter.java`

### Tests & Infrastructure
- **Refactored JUnit Infrastructure:**
  - `src/test/java/com/regnosys/rosetta/generator/python/object/PythonCircularDependencyTest.java`
  - `src/test/java/com/regnosys/rosetta/generator/python/object/PythonCircularReferenceImplementationTest.java`
  - `src/test/resources/junit-platform.properties`
- **Shell Scripts & Python Testing Environment:**
  - `test/common.sh` (Centralized common bash functions)
  - `test/python_setup/setup_python_env.sh` (Environment stability)
  - `test/python_unit_tests/run_python_unit_tests.sh`
  - `test/cdm_tests/*` (CDM build condition scripts)

---

## ✨ Category 2: New Features
These code elements introduce function generation, external native handling, and complex expression expansions. They will **remain** in the current branch (or be isolated into their own standalone feature branch).

### Java Source Code
- **Function Generation:**
  - `src/main/java/com/regnosys/rosetta/generator/python/functions/PythonFunctionGenerator.java` (Core translation from `.rosetta` functions to Python `def`)
  - `src/main/java/com/regnosys/rosetta/generator/python/functions/PythonFunctionDependencyProvider.java` (Dependency traversing for functions)
- **Expressions & State Scope:**
  - `src/main/java/com/regnosys/rosetta/generator/python/expressions/PythonExpressionGenerator.java` (Implementation of `reduce`, `with-meta`, new operators, and block companion returns)
  - `src/main/java/com/regnosys/rosetta/generator/python/expressions/PythonExpressionScope.java` (State management, expression levels, and symbol shadowing)
  - `src/main/java/com/regnosys/rosetta/generator/python/object/PythonChoiceAliasProcessor.java` 
- **Native / Plugin Wiring:**
  - `src/main/java/com/regnosys/rosetta/generator/python/DefaultExternalGeneratorsProvider.java`
  - `src/main/java/com/regnosys/rosetta/generator/python/PythonCodeGeneratorCLI.java`

### Tests & Infrastructure
- **Function/Expression JUnit Tests:**
  - `src/test/java/com/regnosys/rosetta/generator/python/functions/*`
  - `src/test/java/com/regnosys/rosetta/generator/python/expressions/*`
- **Python Unit Test Workspaces:**
  - `test/python_unit_tests/features/functions/*`
  - `test/python_unit_tests/features/expressions/*`
  - `test/python_unit_tests/native_function/*`
- **Generated Feature Documentation:**
  - `docs/function-support-development-issues.md`
  - `docs/ROSETTA_LANGUAGE_GAPS.md`

---

## 🛠️ Step-by-Step Instructions to Create the Refactoring Branch

To construct the isolated refactoring branch without affecting the new features, execute the following commands in the root of the generator project:

1. **Verify your working tree is clean:**
   ```bash
   git status
   ```

2. **Checkout the `main` branch and pull the latest changes:**
   ```bash
   git checkout main
   git pull origin main
   ```

3. **Create the new branch targeting only refactoring:**
   ```bash
   git checkout -b refactoring-stability
   ```

4. **Selectively checkout the refactored files/folders from your feature branch into the index:**
   ```bash
   # Generator Core & Objects
   git checkout feature/function_support -- src/main/java/com/regnosys/rosetta/generator/python/object/
   git checkout feature/function_support -- src/main/java/com/regnosys/rosetta/generator/python/enums/PythonEnumGenerator.java
   git checkout feature/function_support -- src/main/java/com/regnosys/rosetta/generator/python/PythonCodeGenerator.java
   
   # Utils
   git checkout feature/function_support -- src/main/java/com/regnosys/rosetta/generator/python/util/
   
   # Note: Be sure to revert the ChoiceAliasProcessor if it was accidentally checked out from the object folder.
   git reset HEAD src/main/java/com/regnosys/rosetta/generator/python/object/PythonChoiceAliasProcessor.java
   git checkout -- src/main/java/com/regnosys/rosetta/generator/python/object/PythonChoiceAliasProcessor.java

   # Refactoring Tests & Env
   git checkout feature/function_support -- src/test/java/com/regnosys/rosetta/generator/python/object/
   git checkout feature/function_support -- test/python_setup/
   git checkout feature/function_support -- test/cdm_tests/
   git checkout feature/function_support -- test/common.sh
   git checkout feature/function_support -- test/python_unit_tests/run_python_unit_tests.sh
   git checkout feature/function_support -- src/test/resources/junit-platform.properties
   ```
   *(Note: You will need to carefully resolve any compilation dependencies in the refactored files that might rely on new feature files, manually excising them if necessary).*

5. **Run the core build and tests (ensure stability without the new features):**
   ```bash
   mvn clean test
   ```

6. **Once validation passes, commit your changes to establish the pure refactoring branch:**
   ```bash
   git add .
   git commit -m "refactor: isolate model object DAG updates, attribute processing fixes, and test environment stability"
   ```
