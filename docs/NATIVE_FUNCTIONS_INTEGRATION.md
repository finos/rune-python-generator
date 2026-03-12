# Native Functions Integration

This document describes how to integrate custom Python logic into generated Rune code using **Native Functions**.

## Overview
Native functions allow developers to provide custom Python implementations for Rune functions that either cannot be expressed in the Rosetta DSL or require optimized low-level logic.

**Audience**: Rune developers and Python engineers.

**Contents**:
- Definitions of native functions.
- Mechanism of registration and execution.
- Implementation guidelines and troubleshooting.
- Comprehensive end-to-end example.

---

## 1. What are Native Functions
A function is treated as "Native" by the Python generator if:
- It is explicitly annotated with `[codeImplementation]`. (**Recommended**)
- It has no operations (assignment or `add` statements) in its body.

### Exclusions
**Enum Dispatchers** (functions that act as switchboards) are excluded from the implicit "no operations" rule. Dispatcher headers are always generated as `match` statements in Python and will not be treated as native unless explicitly annotated with `[codeImplementation]`.

---

## 2. Integration Mechanism

### Registration and Discovery
The generated code includes a call to `rune_attempt_register_native_functions` during module initialization. This mechanism:
1.  Identifies all functions flagged as native during generation.
2.  Attempts to dynamically import these functions from a specific package prefix (default: `rune.native`).
3.  Registers successfully imported callables in a global registry.

### The Runtime Hook
At runtime, the generated code uses `rune_execute_native`. If a function is called but no implementation was successfully registered, it will raise a `NotImplementedError` listing the available functions.

---

## 3. Implementation Guidelines

### Naming Contract
The Python module must follow the **Fully Qualified Name (FQN)** of the Rune definition, prefixed with `rune.native`.

*   **Rune Namespace**: `rosetta_dsl.test.functions`
*   **Rune Function**: `RoundToNearest`
*   **Expected Python Import Path**: `rune.native.rosetta_dsl.test.functions.RoundToNearest`

### Signature Matching
The Python function must accept exactly the same parameters as defined in Rune, in the same order. Use Python type hints where possible to maintain type safety.

### Troubleshooting
If your native function is not being called:
1.  **Check Logs**: The initialization logic logs a `WARNING` if a module import fails or if the attribute found is not callable.
2.  **Verify Package**: Ensure your native code is installed in the current environment (e.g., via `pip install -e`).
3.  **Implicit Init**: Ensure all directories in your path contain an `__init__.py` file to be treated as a valid Python package.

---

## 4. End-to-End Example

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
Place your logic in the directory structure matching the namespace.

**File**: `src/rune/native/rosetta_dsl/test/functions/RoundToNearest.py`
```python
from decimal import Decimal
from rosetta_dsl.test.functions.RoundingModeEnum import RoundingModeEnum

def RoundToNearest(value: Decimal, digits: int, roundingMode: RoundingModeEnum) -> Decimal:
    """
    Implementation using Python's Decimal quantization.
    """
    decimal_mode = "ROUND_UP" if roundingMode == RoundingModeEnum.UP else "ROUND_DOWN"
    quantifier = Decimal("1").scaleb(-digits)
    return value.quantize(quantifier, rounding=decimal_mode)
```

### C. Packaging and Installation
Use a standard Python packaging tool (like `setuptools`) to make the `rune.native` namespace discoverable.

**File**: `pyproject.toml`
```toml
[project]
name = "rosetta-dsl-native-functions"
version = "0.1.0"

[tool.setuptools.packages.find]
where = ["src"]
```

**Installation Command**:
```bash
python -m pip install -e .
```

### D. Invocation and Generated Code

When Rune generates code for a native function, it creates a wrapper that handles the bridge to your Python implementation using `rune_execute_native`.  

**Note**: The exmaples are mocked up and do not include the complete code.

**Generated Wrapper for `RoundToNearest`**:
```python
@replaceable
@validate_call
def RoundToNearest(value: Decimal, digits: int, roundingMode: RoundingModeEnum) -> Decimal:
    # ... initialization ...
    roundedValue = rune_execute_native('rosetta_dsl.test.functions.RoundToNearest', value, digits, roundingMode)
    
    return roundedValue
```

Other generated functions call this wrapper just like any other function:

**Generated Code for `RoundUp`**:
```python
@replaceable
@validate_call
def RoundUp(value: Decimal, digits: int) -> Decimal:
    # ...
    # Calls the native wrapper defined above
    roundedValue = RoundToNearest(value, digits, RoundingModeEnum.UP)
    
    return roundedValue
```

#### Test Execution
Once installed, any Rune code calling `RoundUp` will automatically route through the native bridge to your Python logic.
