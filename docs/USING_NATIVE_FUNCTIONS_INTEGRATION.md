# Native Functions Integration

This document describes how to integrate custom Python logic into generated Rune code using **Native Functions**.

## Overview

Native functions allow developers to provide custom Python implementations for Rune functions that either cannot be expressed in the Rosetta DSL or require optimized low-level logic.

Native implementations are **copied directly into the generated package** by the CLI. They become part of the package rather than a separately-installed library.

**Audience**: Rune developers and Python engineers.

**Contents**:

- Definitions of native functions.
- Mechanism of registration and execution.
- Source directory layout and CLI flag.
- Writing imports inside native functions.
- Comprehensive end-to-end example.

---

## 1. What are Native Functions

A function is treated as "Native" by the Python generator if:

- It is explicitly annotated with `[codeImplementation]`. (**Recommended**)
- It has no operations (assignment or `add` statements) in its body.

### Exclusions

**Enum Dispatchers** (functions that act as switchboards) are excluded from the implicit "no operations" rule. Dispatcher headers are always generated as `match` statements in Python and will not be treated as native unless explicitly annotated with `[codeImplementation]`.

---

## 2. Source Directory Layout

Native implementations must be placed under a `rune/native/` sub-tree inside the directory passed to `--native-dir`. The path beneath `rune/native/` mirrors the Rune namespace with the function name as the module name.

```
<nativeDir>/
└── rune/
    ├── __init__.py          # optional — copied if present
    └── native/
        └── <namespace>/
            └── <FunctionName>.py
```

**Example** for function `RoundToNearest` in namespace `rosetta_dsl.test.functions`:

```
native-src/
└── rune/
    └── native/
        └── rosetta_dsl/
            └── test/
                └── functions/
                    └── RoundToNearest.py
```

### Output location

The CLI copies these files into the generated package under:

| Mode | Output path |
| :--- | :--- |
| No prefix | `src/rune/native/<namespace>/<FunctionName>.py` |
| With prefix (e.g. `finos`) | `src/finos/rune/native/<namespace>/<FunctionName>.py` |

`__init__.py` files are added to every directory in the output path. If a source `__init__.py` exists for a directory it is copied; otherwise an empty one is written. When a namespace prefix is configured, `src/<prefix>/rune/__init__.py` is also created.

### CLI flag

Pass the native source root to the generator with `--native-dir` / `-n`:

```bash
java -jar generator.jar \
  -s src/main/rosetta \
  -t build/python \
  -p my-project \
  -v 1.0.0 \
  -n path/to/native-src
```

---

## 3. Integration Mechanism

### Registration and Discovery

The generated package's top-level `__init__.py` calls `rune_attempt_register_native_functions` during module initialisation. This mechanism:

1. Identifies all functions flagged as native during generation.
2. Attempts to dynamically import each from its canonical path under `rune.native` (no prefix) or `<prefix>.rune.native` (with prefix).
3. Registers successfully imported callables in a global registry.

### The Runtime Hook

At runtime, generated wrappers call `rune_execute_native`. If a function is called but no implementation was successfully registered, it raises `NotImplementedError` listing the available functions.

---

## 4. Writing Imports Inside Native Functions

Native functions frequently need to import generated types (enums, data classes) from the same package. The import path depends on whether a namespace prefix is configured.

### The prefix variable

The top-level `__init__.py` of the generated package defines `rune_namespace_prefix`:

```python
# Without prefix — in rosetta_dsl/__init__.py
rune_namespace_prefix = None

# With prefix "finos" — in finos/__init__.py
rune_namespace_prefix = "finos"
```

### Dynamic imports for generated types

Because native function files live inside the package at a fixed relative location, but the package root changes with the prefix, use `importlib` to resolve the correct path at runtime:

```python
import importlib

def _import_class(module_path: str, class_name: str, prefix: str | None = None):
    # strip the rune.native prefix that appears in the file path but not the module path
    if module_path.startswith("rune.native."):
        module_path = module_path[len("rune.native."):]
    full_path = f"{prefix}.{module_path}" if prefix else module_path
    return getattr(importlib.import_module(full_path), class_name)
```

**Usage** — importing `RoundingModeEnum` from `rosetta_dsl.test.functions`:

```python
# At module level, read the prefix from the generated package root
try:
    from rosetta_dsl import rune_namespace_prefix
except ImportError:
    rune_namespace_prefix = None

RoundingModeEnum = _import_class(
    "rosetta_dsl.test.functions.RoundingModeEnum",
    "RoundingModeEnum",
    rune_namespace_prefix
)
```

The `try/except` on the prefix import handles the case where the package root differs (e.g. `finos` prefix means `rosetta_dsl` is not the root and the import would fail — but `rune_namespace_prefix` would have been set by the caller's package before any native function is invoked).

### Static imports (prefix-free deployments only)

If you know the generated package will never use a namespace prefix, you can use a plain static import:

```python
# Only safe when no namespace prefix is configured
from rosetta_dsl.test.functions.RoundingModeEnum import RoundingModeEnum
```

---

## 5. End-to-End Example

### A. Rune Definition

**File**: `NativeFunctionTest.rosetta`

```rosetta
namespace rosetta_dsl.test.functions

// This function is implemented in Python
func RoundToNearest:
    [codeImplementation]
    inputs:
        value number (1..1)
        digits int (1..1)
        roundingMode RoundingModeEnum (1..1)
    output:
        roundedValue number (1..1)

// This function is a regular Rune function that calls the native one
func RoundUp:
    inputs:
        value number (1..1)
        digits int (1..1)
    output:
        roundedValue number (1..1)
    set roundedValue:
        RoundToNearest(value, digits, RoundingModeEnum -> Up)
```

### B. Native Python Implementation

**File**: `native-src/rune/native/rosetta_dsl/test/functions/RoundToNearest.py`

```python
import importlib
from decimal import Decimal

def _import_class(module_path: str, class_name: str, prefix: str | None = None):
    if module_path.startswith("rune.native."):
        module_path = module_path[len("rune.native."):]
    full_path = f"{prefix}.{module_path}" if prefix else module_path
    return getattr(importlib.import_module(full_path), class_name)

try:
    from rosetta_dsl import rune_namespace_prefix
except ImportError:
    rune_namespace_prefix = None

RoundingModeEnum = _import_class(
    "rosetta_dsl.test.functions.RoundingModeEnum",
    "RoundingModeEnum",
    rune_namespace_prefix
)

def RoundToNearest(value: Decimal, digits: int, roundingMode: RoundingModeEnum) -> Decimal:
    decimal_mode = "ROUND_UP" if roundingMode == RoundingModeEnum.UP else "ROUND_DOWN"
    quantifier = Decimal("1").scaleb(-digits)
    return value.quantize(quantifier, rounding=decimal_mode)
```

### C. Build

```bash
java -jar target/python-0.0.0.main-SNAPSHOT.jar \
  -s src/main/rosetta \
  -t build/python \
  -p rosetta-dsl \
  -v 1.0.0 \
  -n native-src
```

The generator copies `RoundToNearest.py` into `build/python/src/rune/native/rosetta_dsl/test/functions/` and adds `__init__.py` to every directory in that path.

### D. Generated Code

**Generated wrapper for `RoundToNearest`**:

```python
@replaceable
@validate_call
def RoundToNearest(value: Decimal, digits: int, roundingMode: RoundingModeEnum) -> Decimal:
    roundedValue = rune_execute_native(
        'rosetta_dsl.test.functions.RoundToNearest', value, digits, roundingMode)
    return roundedValue
```

**Generated code for `RoundUp`**:

```python
@replaceable
@validate_call
def RoundUp(value: Decimal, digits: int) -> Decimal:
    roundedValue = RoundToNearest(value, digits, RoundingModeEnum.UP)
    return roundedValue
```

#### Runtime Behaviour

Once the generated package is installed, any Rune code calling `RoundUp` automatically routes through the native bridge to your Python logic.

### E. With a Namespace Prefix

If the package is generated with `-x finos`, the output location becomes:

```
build/python/src/finos/rune/native/rosetta_dsl/test/functions/RoundToNearest.py
```

The import path used by `rune_attempt_register_native_functions` becomes:

```
finos.rune.native.rosetta_dsl.test.functions.RoundToNearest
```

The `_import_class` helper in the native function handles this transparently via `rune_namespace_prefix`.

---

## 6. Troubleshooting

| Symptom | Likely cause | Fix |
| :--- | :--- | :--- |
| `NotImplementedError` at runtime | Native function not registered | Check logs for import errors at startup |
| Import error in native file | Wrong prefix assumption | Use the `_import_class` dynamic import helper |
| Missing `__init__.py` | Source dir structure incorrect | Ensure source lives under `<nativeDir>/rune/native/` |
| Function not copied | `--native-dir` not passed | Add `-n <nativeDir>` to the CLI invocation |
