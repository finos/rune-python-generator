# CDM Import Fixes Release Notes

This document describes fixes to the Python generator that resolve `ImportError` and
`ArbitraryTypeWarning` failures encountered when loading CDM types such as `TradeState`.

---

## 1. Runtime Change: Robust `_FQRTN` Lookup in `rune_serialize`

### What Is the Issue

The `rune_serialize` method in `BaseDataClass` uses the `_FQRTN` class attribute to record
the fully-qualified Rune type name in the serialised JSON `@type` field. The generator only
emits `_FQRTN` for bundled classes (those that live inside `_bundle.py`). Standalone classes
— which have their own module file — do not have `_FQRTN` defined. Calling `rune_serialize`
on an instance of a standalone class therefore raises:

```
AttributeError: _FQRTN
```

### Proposed Change to the Runtime

Add a `try/except` fallback in `rune_serialize` so that when `_FQRTN` is absent the method
falls back to `type(self).__module__`:

```python
try:
    fqrtn = self._FQRTN
except AttributeError:
    fqrtn = type(self).__module__
```

### Why This Addresses the Issue

Standalone classes are named and placed in modules whose dotted path matches the Rune
fully-qualified type name (e.g., `cdm.event.common.TradeState`). Using `type(self).__module__`
therefore produces an equivalent `@type` value without requiring the generator to emit a
redundant `_FQRTN` attribute in every standalone class file.

---
## 2. Lazy Proxy Stubs via PEP 562 `__getattr__` [FIXED]

### What Is the Issue

Each bundled class has a proxy stub file at its fully-qualified path (e.g.,
`cdm/event/common/TradeState.py`). Previously the stub used a direct top-level import:

```python
from cdm._bundle import cdm_event_common_TradeState as TradeState
```

This import executes unconditionally the moment the stub module is loaded. When a
downstream package imports `TradeState` before the bundle has fully initialised (for
example, during the bundle's own loading of a standalone dependency that in turn imports
`TradeState`), Python encounters a partially-initialised `cdm._bundle` module in
`sys.modules` and raises:

```
ImportError: cannot import name 'cdm_event_common_TradeState' from partially
initialized module 'cdm._bundle'
```

A related problem occurs inside `_bundle.py` itself: imports of standalone types were
placed in the bundle header, before the bundled class definitions. If a standalone
module triggers any further imports from the bundle during its own loading, the required
bundled names are not yet defined.

### What Changed in the Emitted Code

**Proxy stubs** now use a PEP 562 module-level `__getattr__` to defer the bundle import
until the first time the exported name is accessed:

```python
def __getattr__(name: str):
    if name == 'TradeState':
        import cdm._bundle as _b
        _v = _b.cdm_event_common_TradeState
        globals()['TradeState'] = _v
        return _v
    raise AttributeError(name)

# EOF
```

The stub module itself loads instantly (no bundle import at module scope). The bundle is
imported only when a caller first accesses `TradeState`, by which time the bundle is
fully initialised.

**Standalone-class imports in `_bundle.py`** are now emitted as a deferred block after
all bundled class definitions rather than in the file header:

```python
# Standalone type imports (deferred to avoid circular import at bundle load time)
from cdm.observable.asset.Index import Index
from cdm.product.collateral.CollateralIssuerType import CollateralIssuerType
# …
```

### Why This Addresses the Issue

With eager proxy stubs, any import of a stub module during bundle loading immediately
triggers a recursive bundle import, which fails because the bundle is not yet in
`sys.modules`. The PEP 562 `__getattr__` breaks this cycle: the stub loads without
touching the bundle, and the bundle import is only attempted after the bundle is
already cached in `sys.modules`.

Deferring standalone-class imports to after all class definitions ensures that when a
standalone module is loaded it can safely import bundled names (they are already
defined in the bundle's module globals).

### Generator Changes

- **`PythonCodeGenerator.java` — proxy stub generation**: Replaced the direct
  `from <namespace>._bundle import <BundleClass> as <ShortName>` top-level import with
  a PEP 562 `__getattr__` function that lazily imports the bundle and caches the
  attribute in `globals()`.

- **`PythonCodeGenerator.java` — `_bundle.py` assembly**: Standalone-class import
  statements that previously appeared in the bundle header are now collected into a
  `deferredStandaloneImports` list and emitted after all bundled class definitions, under
  the comment `# Standalone type imports (deferred to avoid circular import at bundle
  load time)`. Exception: a standalone type used as a **direct base class** of a bundled
  type cannot be deferred — Python evaluates base-class expressions immediately at
  class-definition time (unlike attribute annotations). Such imports remain in the
  header via `standaloneSupertypesOfBundled` detection.

---

## 3. Choice-Type Normalization in the Dependency DAG [FIXED]

### What Is the Issue

The dependency graph used by the Kosaraju SCC analysis to detect circular dependencies
did not handle Rune choice-type attributes. When a class `A` had an attribute of a
choice type `C`, the edge `A → C` was missing from the DAG. If `C` in turn referenced
`A`, the cycle `A → C → A` was invisible to SCC analysis. Both `A` and `C` were
classified as standalone, and the generated standalone files tried to import each other
at module level, causing a circular import error at runtime.

### What Changed in the Emitted Code

Types that would previously be incorrectly classified as standalone (because their
circular dependency ran through a choice type) are now correctly placed in the bundle
with Phase 1/2/3 treatment, eliminating the circular standalone import.

### Why This Addresses the Issue

The SCC analysis only detects cycles it can see in the graph. Once choice-type
attributes contribute their edges, previously invisible cycles become visible and the
affected types are bundled rather than made standalone.

### Generator Changes

- **`PythonModelObjectGenerator.java` — dependency graph construction**: When iterating
  attributes to build the dependency DAG, `RChoiceType` values are now normalized to
  their underlying `RDataType` via `rChoiceType.asRDataType()`, mirroring the existing
  handling in `getImportsFromAttributes`. This makes choice-type dependencies visible
  to the SCC analysis.

---

## 4. Cross-Namespace Bundle Imports [FIXED]

### What Is the Issue

When CDM types referenced types from another namespace (e.g., fpml), the generator emitted
imports that reached inside the foreign package's private `_bundle` module:

```python
from fpml._bundle import fpml_consolidated_asset_Asset
```

The `_bundle` module is a private implementation detail of each generated package. Other
packages must never import from it directly. When fpml types are internally bundled (i.e.,
they form a circular reference cycle within fpml), the flattened bundle name
`fpml_consolidated_asset_Asset` does not exist as a public export and the import fails with
an `ImportError`.

### What Changed in the Emitted Code

External-namespace types are now always accessed via their fully-qualified proxy stub path:

```python
from fpml.consolidated.asset.Asset import Asset
```

The proxy stub at that path uses a lazy `__getattr__` (PEP 562) to delegate to the fpml
bundle internally. CDM code never needs to know whether fpml's `Asset` is bundled or
standalone.

### Why This Addresses the Issue

Each generated package owns its own `_bundle`. The proxy stubs at fully-qualified paths are
the stable public API. Using them ensures that CDM never depends on fpml's internal
structure, and the import succeeds regardless of how fpml organises its bundle.

### Generator Changes

- **`PythonCodeGenerator.java` — `partitionClasses`**: After Kosaraju SCC analysis,
  all vertices whose namespace does not belong to the current model are unconditionally added
  to `standaloneClasses`. This forces their imports to use the fully-qualified proxy stub
  path rather than the bundle.

- **`PythonFunctionDependencyProvider.java` — `addDependencies` (both overloads)**: The
  cross-namespace branch that previously emitted `from <root>._bundle import <flattened>`
  now emits `from <fullNamespace>.<Name> import <Name>` for both `RosettaNamed` and `RType`
  inputs.

---

## 5. Phase 1 Placeholder for Bundled Classes [FIXED]

### What Is the Issue

In the generated `_bundle.py`, all bundled class definitions (Phase 1) are emitted before
any standalone type imports. This ordering is required to avoid circular imports within the
bundle. However, pydantic processes each class at definition time and adds `vars(cls)` to the
annotation evaluation local namespace.

For bundled classes such as `cdm_product_collateral_CollateralCriteria`:

```python
class cdm_product_collateral_CollateralCriteria(BaseDataClass):
    CollateralIssuerType: Optional[CollateralIssuerType] = Field(None, ...)
```

At class definition time, `CollateralIssuerType` has not yet been imported into the bundle's
module globals (its import appears thousands of lines later). Pydantic evaluates
`Optional[CollateralIssuerType]` and finds `CollateralIssuerType = Field(None, ...)` (a
`FieldInfo` instance) in `vars(cls)`. Pydantic accepts `FieldInfo` as an arbitrary type and
emits:

```
ArbitraryTypeWarning: FieldInfo(annotation=NoneType, ...) is not a Python type.
Pydantic will allow any object with no validation.
```

Although the Phase 2 annotation update and subsequent `model_rebuild()` call correct the
schema, the warnings indicate that pydantic's initial processing is incorrect.

### What Changed in the Emitted Code

Phase 1 field declarations in bundled classes now use `None` as the type placeholder:

```python
class cdm_product_collateral_CollateralCriteria(BaseDataClass):
    CollateralIssuerType: None = Field(None, ...)   # placeholder
```

The Phase 2 annotation update (already emitted after all class definitions) is unchanged and
sets the correct type at module level, where `vars(cls)` is not in scope:

```python
cdm_product_collateral_CollateralCriteria.__annotations__["CollateralIssuerType"] = \
    Annotated[Optional[CollateralIssuerType], CollateralIssuerType.serializer(), ...]
```

`model_rebuild()` (already emitted) then rebuilds the schema from this correct annotation.

### Why This Addresses the Issue

`None` (i.e., `type(None)` = `NoneType`) is a valid Python type and does not match any field
name. Pydantic processes Phase 1 without seeing a `FieldInfo` in the annotation, so no
`ArbitraryTypeWarning` is emitted. The Phase 2 update and `model_rebuild()` produce the
correct final schema with proper type validation.

### Generator Changes

- **`PythonAttributeProcessor.java` — `createAttributeResult`**: For bundled delayed fields
  (`isDelayed && !isStandalone`), `pythonType` is set to the literal string `"None"` instead
  of `hint.build(false)`. The Phase 2 annotation update string (`hint.build(true)`) and the
  `model_rebuild()` call are unchanged.

- **`PythonCircularDependencyTest.java`** and **`PythonCircularReferenceImplementationTest.java`**:
  Updated Phase 1 assertion strings from flattened type names (e.g.,
  `com_rosetta_test_model_CircularB`) to `None` to reflect the new placeholder.

---

## 6. Naming Conflict Aliasing for Standalone Classes [FIXED]

### What Is the Issue

Pydantic v2 adds `vars(cls)` to the local namespace when evaluating annotation strings at
class definition time (see `_namespace_utils.py`, `NsResolver.types_namespace`). This means
that a field whose name matches its type name shadows the imported class with the field's
`FieldInfo` default.

For example, in the standalone class `Index`:

```python
from cdm.observable.asset.InterestRateIndex import InterestRateIndex

class Index(BaseDataClass):
    InterestRateIndex: Annotated[Optional[InterestRateIndex],
                                 InterestRateIndex.serializer(), ...] = Field(None, ...)
```

When pydantic evaluates `Annotated[Optional[InterestRateIndex], InterestRateIndex.serializer(), ...]`,
`InterestRateIndex` resolves to the `FieldInfo` default (from `vars(cls)`) rather than the
imported class. Calling `.serializer()` on a `FieldInfo` instance raises:

```
AttributeError: 'FieldInfo' object has no attribute 'serializer'
```

This exception during bundle load leaves `cdm._bundle` in a broken partial state in
`sys.modules`. Subsequent proxy stub imports (e.g., `from cdm.event.common.TradeState import
TradeState`) silently receive the broken module and raise `ImportError`.

### What Changed in the Emitted Code

For standalone classes where a field name matches its type name, the import is aliased and
the alias is used in the annotation:

```python
from cdm.observable.asset.InterestRateIndex import InterestRateIndex as _InterestRateIndex

class Index(BaseDataClass):
    InterestRateIndex: Annotated[Optional[_InterestRateIndex],
                                 _InterestRateIndex.serializer(), ...] = Field(None, ...)
```

The alias `_InterestRateIndex` is a module-level name. Pydantic finds it in `globalns`
rather than in `vars(cls)`, so it resolves to the actual class.

This aliasing is applied **only for standalone source classes**. Bundled classes use Phase 2
annotation updates evaluated at module level, where `vars(cls)` is not in scope, so no alias
is needed there.

### Why This Addresses the Issue

The aliased name does not appear in the class body as a field attribute, so `vars(cls)` does
not contain it. Pydantic's annotation evaluator finds the alias in module globals and
correctly identifies it as the class, enabling `.serializer()` and `.validator()` to be
called.

### Generator Changes

- **`PythonAttributeProcessor.java` — `deriveTypeHint`**: The naming-conflict check
  (`attrPythonName.equals(rt.getName())`) and alias prefix (`"_" + rt.getName()`) are now
  applied only when `isSourceStandalone = true`. Bundled source classes continue to use the
  plain type name in Phase 2 annotation updates (module-level, no `vars(cls)` conflict).

- **`PythonAttributeProcessor.java` — `getImportsFromAttributes`**: The aliased import
  (`from X.Y import Z as _Z`) is generated only when `includeBundledTypes = true`
  (standalone file context). Bundle header imports (`includeBundledTypes = false`) always use
  the plain form because Phase 2 is evaluated at module level.

---

## 7. Self-Import Guard for Self-Referential Types [FIXED]

### What Is the Issue

A type that references itself as an attribute type (e.g., via `[metadata reference]`)
caused the generator to emit a self-import in the standalone class file:

```python
# In Node.py
from com.rosetta.test.model.Node import Node   # ← imports the same file
```

When Python partially initialises a module and then encounters an import of that same
module, it returns the partially-initialised module object. The `Node` name is not yet
defined in it, so the import raises:

```
ImportError: cannot import name 'Node' from partially initialized module
'com.rosetta.test.model.Node'
```

### What Changed in the Emitted Code

The self-import line is no longer emitted. The attribute annotation uses the class name
directly (it is already in scope as the class being defined):

```python
class Node(BaseDataClass):
    parent: Optional[FieldWithMeta[Node]] = Field(None, ...)
```

### Why This Addresses the Issue

A class can always refer to itself by name in its own body without an import — the name
is resolved in the module's own global namespace where it is already being defined.
Removing the self-import eliminates the circular-import error.

### Generator Changes

- **`PythonAttributeProcessor.java` — `getImportsFromAttributes`**: Before emitting a
  standalone-class import, the method now computes `rcFqn` (the fully-qualified name of
  the class that owns the attributes) and skips any attribute whose resolved type FQN
  equals `rcFqn`.
