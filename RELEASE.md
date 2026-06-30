# Release Notes

## Current Release

> **Runtime Dependency**: this release requires [rune-python-runtime](https://github.com/finos/rune-python-runtime) v2.0.0, which is available on PyPI. The generated `pyproject.toml` declares `rune.runtime>=2.0.0,<3.0.0`.


### Support for the `as` Operator (Rune DSL v10)

The `as` operator is now fully generated. It narrows an expression to a more specific type and has two distinct modes.

**Choice narrowing** — when the left-hand side is a choice type, `as` extracts the value of one specific option, returning `None` if the choice holds a different option. On a list, it filters to elements holding the named option and returns their values:

| Cardinality | Generated Python |
| :--- | :--- |
| Single | `rune_resolve_attr(arg, 'optionName')` |
| Multi | `[_v for _x in arg if (_v := rune_resolve_attr(_x, 'optionName')) is not None]` |

**Data type narrowing** — when the left-hand side is a data type, `as` performs a runtime `isinstance` check against a target subtype, returning the object if it matches or `None` otherwise. On a list, it filters to matching instances:

| Cardinality | Generated Python |
| :--- | :--- |
| Single | `(_x if isinstance(_x := arg, TargetType) else None)` |
| Multi | `[_x for _x in arg if isinstance(_x, TargetType)]` |

The operator associates left-to-right at the same precedence as `->`, so chained expressions such as `foo as Bar -> attr` are correctly parenthesised as `((foo as Bar) -> attr)`.

No changes to `rune-python-runtime` are required — the runtime already provides all necessary primitives (`rune_resolve_attr`, `isinstance`).

---

## Known Gaps

See [RUNE_LANGUAGE_GAPS.md](docs/RUNE_LANGUAGE_GAPS.md) for the full feature coverage matrix.
