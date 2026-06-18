# `as` Operator — Impact Analysis

> **Audience**: Generator contributors.
> This document defines the semantics of the `as` operator introduced in Rune DSL v10, analyses its impact on the Python runtime and generator, and specifies the changes required to implement it.

---

## 1. Semantics

The `as` operator narrows an expression to a more specific type. It has two distinct modes that share the same syntax but differ in what the left-hand argument is.

### 1.1 Choice narrowing

When the left-hand argument is a **choice type**, `as` extracts the value of one specific option:

- If the choice currently holds the named option, the value of that option is returned.
- If the choice holds a different option, `None` is returned.
- On a list, the operator filters to only those elements that hold the named option, returning their values.

```
# single — returns the Bar value or None
foo as Bar -> barAttr

# list — filters to Foos holding a Bar, then maps
[foo1, foo2, foo3] as Bar -> barAttr sum
```

Metadata attached to the choice option is preserved through the narrowing.

### 1.2 Data type narrowing

When the left-hand argument is a **data type** (a class in an inheritance hierarchy), `as` performs a runtime `isinstance` check against a target subtype:

- If the object is an instance of the target type, it is returned as that type.
- Otherwise, `None` is returned.
- On a list, the operator filters to only those elements that are instances of the target type.

```
# single — Bar is a subtype of Foo
(if condition then Bar { barAttr: 42 } else Foo {}) as Bar -> barAttr   =>  42

# list — filters to instances of Bar, then sums barAttr
[Bar { barAttr: 1 }, Qux { quxAttr: 2 }, Bar { barAttr: 3 }] as Bar -> barAttr sum   =>  4
```

### 1.3 Chaining

The `as` operator associates left-to-right and at the same precedence level as `->`. Attribute access after `as` operates on the narrowed type:

```
foo -> bar as Qux -> attr   ≡   ((foo -> bar) as Qux) -> attr
```

---

## 2. Runtime impact

**No runtime changes are required.** The runtime already provides everything needed to implement both narrowing modes:

| Mode | Required primitive | Already available? |
| :--- | :--- | :---: |
| Choice narrowing (single) | `rune_resolve_attr(obj, attr_name)` — returns `None` if attr is unset | ✅ |
| Choice narrowing (list) | List comprehension with `rune_resolve_attr` guard | ✅ |
| Data narrowing (single) | `isinstance(obj, TargetType)` | ✅ |
| Data narrowing (list) | `[x for x in lst if isinstance(x, TargetType)]` | ✅ |

No new utility functions are needed in `rune-python-runtime/src/rune/runtime/utils.py`.

---

## 3. Generator impact

### 3.1 Gap

`AsOperation` is entirely absent from `PythonExpressionGenerator.java`. The main `switch` statement in `generateExpression()` has no `case AsOperation` branch. At runtime, any Rune expression containing `as` will cause `PythonExpressionGenerator` to throw `UnsupportedOperationException`.

This is the **only file that requires a code change**.

### 3.2 Required change

Add a `case AsOperation` branch to `PythonExpressionGenerator.java`. The branch must:

1. Determine the RType of the argument — `RChoiceType` vs `RDataType`.
2. Determine cardinality — single vs multi (list).
3. Emit Python accordingly (see Section 4).

The type and cardinality information is already available through the same type-provider and context mechanisms used by `SwitchOperation`, which is the closest existing analogue.

---

## 4. Generated Python patterns

### 4.1 Choice narrowing — single

The `_CHOICE_ALIAS_MAP` already records the attribute path for each option on a choice type (populated by `PythonChoiceAliasProcessor`). For `as`, walk that path using `rune_resolve_attr`:

```python
# Rune:  foo as Bar
rune_resolve_attr(foo, 'bar')
```

If the alias path is more than one attribute deep (nested choice), the existing deep-resolution helpers apply.

### 4.2 Choice narrowing — multi

```python
# Rune:  [foo1, foo2] as Bar
[_v for _x in [foo1, foo2] if (_v := rune_resolve_attr(_x, 'bar')) is not None]
```

### 4.3 Data type narrowing — single

```python
# Rune:  expr as Bar
(_x if isinstance(_x := expr, Bar) else None)
```

### 4.4 Data type narrowing — multi

```python
# Rune:  [expr1, expr2, expr3] as Bar
[_x for _x in [expr1, expr2, expr3] if isinstance(_x, Bar)]
```

---

## 5. Implementation checklist

- [x] Add `case AsOperation` to `PythonExpressionGenerator.generateExpression()`, implementing Sections 4.1–4.4
- [x] Add unit tests in the generator test suite covering:
  - Choice narrowing, single
  - Choice narrowing, multi
  - Data type narrowing, single
  - Data type narrowing, multi
  - Chained `as` followed by `->` attribute access

  (`PythonAsOperationTest`; codegen-only tests, so the "match"/"no match" runtime
  distinction collapses into a single generated-code shape per cardinality/mode.)
- [x] Update `RUNE_LANGUAGE_GAPS.md` to mark `as` as supported once implemented
