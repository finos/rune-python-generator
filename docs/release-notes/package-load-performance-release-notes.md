# Package Load Performance Release Notes

---

## Heterogeneous Generation: Standalone and Bundled Classes [IMPLEMENTED]

### What Is the Issue

The generator previously placed every `Data` type into a single `_bundle.py` file,
regardless of whether it participated in a circular dependency. Every class called
pydantic's `model_rebuild()` at import time. For CDM 6.0 (973 core types, plus metadata
wrappers), this produced a load time of approximately 2 minutes — an unusable delay for
any interactive or production workload.

### What Changed in the Emitted Code

Types are now partitioned into two categories based on whether they participate in
circular dependencies:

- **Standalone** (acyclic): Each type gets its own `.py` file with a normal class
  definition. Annotations are written directly in the class body. No `model_rebuild()`
  is called at import time.

- **Bundled** (cyclic): Types that form a mutual circular dependency are grouped into
  `_bundle.py` and continue to use the Phase 1/2/3 deferred-annotation pattern with
  `model_rebuild()`.

For CDM 6.0, this partitioning yields:

| Metric | Value |
| :--- | :--- |
| Total Rosetta Types | 973 |
| Standalone (Acyclic) | 911 (~93.6%) |
| Bundled (Cyclic) | 62 (~6.4%) |
| Number of cycle groups (SCCs) | 8 |
| Largest cycle group | 44 types (`Trade`, `Product`, `EconomicTerms`, `Payout`, …) |

### Why This Addresses the Issue

`model_rebuild()` is now called only for the 62 bundled types rather than for all 973+
types (including metadata wrappers). The 911 standalone types load with no rebuild
overhead. The cumulative cost that caused the 2-minute delay is eliminated for
approximately 94% of the model.

### Generator Changes

- **`PythonCodeGeneratorContext.java`**: Changed `dependencyDAG` from
  `DirectedAcyclicGraph` to `DefaultDirectedGraph` to accurately represent all
  dependencies, including cycles. Added a `standaloneClasses` set populated after SCC
  analysis.

- **`PythonCodeGenerator.java`**: Replaced single-pass generation with a two-pass
  approach. The first pass scans all resources and builds the complete dependency graph.
  The second pass runs Kosaraju SCC analysis (`KosarajuStrongConnectivityInspector`) to
  identify cycle groups, then emits standalone files for acyclic types and `_bundle.py`
  entries for cyclic types. A meta-DAG of SCCs is topologically sorted to determine
  global emission order.

- **`PythonModelObjectGenerator.java`**: Accepts a standalone/bundled flag and dispatches
  to the appropriate code-generation path.

- **`PythonAttributeProcessor.java`**: In standalone mode, attributes are emitted
  directly in the class body with full `Annotated[...]` type hints. In bundled mode, the
  existing Phase 1 (`None` placeholder) / Phase 2 (annotation update) / Phase 3
  (`model_rebuild()`) pattern is preserved.

- **`PythonGeneratorTestUtils.java`**: Enhanced with workspace-aware lookup helpers so
  that tests can locate a generated class in either its standalone file or `_bundle.py`
  without hard-coding the path.
