# Python Package Load Performance Optimization

This document outlines the strategy for resolving the substantial load time (~2 minutes for CDM 6) in the generated Python package.

## Problem Statement

The current generator forces all `Data` types into a single `_bundle.py` and calls Pydantic's `model_rebuild()` on every class at import time. This results in a massive performance bottleneck as the number of classes grows.

## Strategy: Heterogeneous Generation

The proposed solution partitions the model into **Standalone** (Acyclic) and **Bundled** (Cyclic) components to achieve near-instantaneous load times for the majority of the classes.

### Comparison Table

| Feature | Current State | Proposed State |
| :--- | :--- | :--- |
| **1. Structure** | **Homogeneous**: Forced into one `_bundle.py`. | **Heterogeneous**: Standalones in individual files; Cycles in `_bundle.py`. |
| **2. Graph Type** | `DirectedAcyclicGraph` (Ignores cycles). | **Directed Graph**: `DefaultDirectedGraph` to represent true dependencies. |
| **3. Detection** | Topological Sort (Approximate). | **SCC Algorithm**: Identify Strong Connectivity (True Cycles). |
| **4. Emission** | All Data classes in `_bundle.py`. | **Dual Path**: <br>• Standalones: Direct `.py` file definition.<br>• Bundled: `_bundle.py` with Phase 1/2/3. |
| **5. (Optional)** | `model_rebuild()` on every class. | **Lazy Strings**: Skip/Defer `model_rebuild()` if bundle is still slow. |

### Implementation Details

The implementation transition from the current "one-size-fits-all" bundle to a heterogeneous model is executed via a **Two-Phase Generation Process**:

#### Phase 1: Global Discovery (Scan)
Instead of generating Python code strings during individual file processing, the generator first performs a full scan of the model space:
1.  **Rosetta Data Capture**: Collect all `Data` type definitions from across all Rosetta resources into a single context.
2.  **Global Graph Construction**: Build a complete, directed dependency graph (`DefaultDirectedGraph`) covering all inter-file attributes and inheritance relationships.
3.  **Cycle Analysis**: Execute the SCC (Strongly Connected Components) algorithm to identify the "Cycle Kernels" (SCC size > 1) and the "Standalone Tree" (SCC size = 1, no self-loops).

#### Phase 2: Partitioned Emission (Process)
Once the global structure is analyzed, the generator performs the actual code emission in a second pass:
1.  **Standalone Classes**: For acyclic types, emit a "Normal" Python class body directly into a dedicated `.py` file. These skip all Phase 2/3 unhooking and `model_rebuild()` overhead.
2.  **Cyclic Kernel (Bundled)**: For classes participating in a cycle, emit a Phase 1/2/3 "unhooked" definition into the `_bundle.py`.
3.  **Cross-File Wiring**: The generator determines the correct imports (e.g., a standalone class importing its cyclic dependencies from `_bundle.py`).
4.  **Meta-Topological Ordering**: The topological sort is executed at the SCC level to ensure all files are emitted in the correct dependency order.
5.  **(Optional Lever) Lazy Resolution**: If the cyclic kernel remains large, transition to String Forward References in the bundle to skip `model_rebuild()` at import time entirely.

## Expected Outcomes

*   **Load Time Reduction**: Expected reduction from minutes to seconds (or sub-second if the optional lazy resolution is applied).
*   **Standard Python Structure**: Generated code will feel more native to Python by residing in individual files wherever cycles are not present.

## CDM 6.0 Case Study: Cycle Analysis

An analysis of the **CDM 6.0 Rosetta sources** (973 core types) yielded the following metrics for the partitioning strategy:

| Metric | Measured Value |
| :--- | :--- |
| **Total Rosetta Types** | **973** |
| **Types in Cycles (Bundled)** | **62** (~6.4%) |
| **Acyclic Types (Standalone)** | **911** (~93.6%) |
| **Number of SCCs (Cycle Kernels)** | **8** |
| **Biggest SCC Size** | **44** (`Trade`, `Product`, `EconomicTerms`, `Payout`, etc.) |

### Impact Analysis
The "Miserable" 2-minute load time is due to the cumulative cost of calling `model_rebuild()` on 2,000+ classes (including metadata wrappers). By moving **95%** of the model to standalone files, we eliminate this overhead for nearly the entire codebase, leaving only a small "kernel" in the cycle-bundle.

## Impact on Java Unit Testing

The transition from a homogeneous bundle to a heterogeneous multi-file approach will increase the complexity of the generator's JUnit tests:

1.  **File Location Assertions**: Many tests currently hard-code `src/test/_bundle.py` as the source of truth. These must be updated to find classes in their dynamic location (Standalone vs. Bundled).
2.  **Import Correctness**: New test cases are required to verify the cross-file import logic (e.g., verifying that Standalone classes correctly import cyclic dependencies from `_bundle.py`).
3.  **Cycle Validation**: The generator must be unit-tested to ensure the SCC algorithm correctly identifies and bundles true cycles to prevent runtime `ImportError` or `NameError` in the emitted Python.
4.  **Test Utility Refactoring**: `PythonGeneratorTestUtils` will need to be enhanced with a workspace-aware lookup helper to support these more sophisticated multi-file assertions.

## Technical Implementation Plan

The follow class-level changes are required to support the heterogeneous strategy:

### 1. `PythonCodeGeneratorContext.java`
- **Graph Update**: Change `dependencyDAG` from `DirectedAcyclicGraph` to `DefaultDirectedGraph` to accurately represent all Rosetta model dependencies.
- **Partitioning State**: Add fields to track which classes have been identified as acyclic during the scan phase.

### 2. `PythonCodeGenerator.java`
- **Global Scan**: Update the logic to scan all resources first, building the full dependency graph before proceeding to the code emission phase.
- **SCC Analysis**: Integrate the JGraphT `StrongConnectivityInspector` to identify Strongly Connected Components (SCCs).
- **Meta-DAG Sorting**: Construct and topologically sort a meta-DAG of SCCs to determine the global emission order.
- **Import Wiring**: Implement logic to correctly manage cross-file imports between Standalone files and the `_bundle.py`.

### 3. `PythonModelObjectGenerator.java`
- **Mode Dispatch**: Pass a "Standalone" vs. "Bundled" flag down to the code-generation logic.
- **Collection Strategy**: For Standalone classes, continue to return the generated code string to the central `classObjects` map in the `PythonCodeGeneratorContext`. The generator remains filesystem-agnostic.

### 4. `PythonAttributeProcessor.java`
- **Primary Mode (Standalone)**: New logic to emit attributes directly in the class body (`attr: Annotated[Type, metadata] = ...`) for standalone classes.
- **Delayed Mode (Bundled)**: Preserve the "unhooked" Phase 1 field emission for classes participating in cycles.

### 5. `PythonGeneratorTestUtils.java`
- **Workspace-Aware Assertions**: Refactor helpers like `assertGeneratedContainsExpectedString` to optionally search across the `classObjects` map in the context to verify both standalone and bundled definitions.
