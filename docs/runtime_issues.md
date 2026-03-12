# Runtime Issues

## BROKEN: Equality Comparison for COW Objects

### Description
The `_COWObject` proxy does not implement `__eq__`. When comparing a COW-wrapped object to another object (common in `filter` or `contains` operations), the comparison always returns `False` unless the objects are identical by memory address.

### Example Failure
In `FilterQuantity`, the expression `item -> unit = unit` fails because the input `unit` is wrapped in a COW proxy, and `proxy == original_unit` results in `False`.

### Required Fix
Implement `__eq__` in `_COWBase` or `_COWObject` to delegate equality checks to the underlying unwrapped object.

## BROKEN: Pydantic Type Validation with COW Proxies

### Description
Because `_COWObject` is a proxy class and not a subclass of the generated Rune model classes (which inherit from `BaseDataClass`), Pydantic's `@validate_call` and model field validation fail when a COW-wrapped object is passed as an argument.

### Error Traceback
```python
E   pydantic_core._pydantic_core.ValidationError: 1 validation error for rosetta_dsl_test_functions_ContainerObject
E   baseObject
E     Input should be a valid dictionary or instance of rosetta_dsl_test_functions_BaseObject [type=model_type, input_value=COWObject(...), input_type=_COWObject]
```

### Impact
This makes it impossible to use COW-wrapped objects as inputs to other functions or as values for model fields within a function body.

### Potential Fix
The COW proxies need to be recognized by Pydantic as valid instances of the wrapped type, or the runtime must unwrap them before passing them to Pydantic-validated constructors/functions.

## BROKEN: Existence Check (`is absent`/`is present`) for Empty COW Lists

### Description
The `rune_attr_exists` utility function in the runtime checks for attribute existence by explicitly comparing against `None` and an empty list `[]`. However, when an empty list is wrapped in a `_COWList` proxy, it does not compare as equal to `[]` because it lacks an `__eq__` implementation.

### Example Failure
In `IsAbsentList`, the expression `list is absent` fails for an empty list `[]` because:
1. `list` is wrapped in `_COWList`.
2. `rune_attr_exists(_COWList([]))` returns `True` (it is not `None` and `_COWList([]) == []` is `False`).
3. `is absent` (which is `not rune_attr_exists`) therefore returns `False` instead of `True`.

### Error Traceback
```python
E       assert False is True
E        +  where False = IsAbsentList(list=[])
```

### Affected Location
**File**: `rune/runtime/utils.py`
**Function**: `rune_attr_exists`

### Required Fix
Change `rune_attr_exists` to use truthiness or handle COW wrappers, for example:
```python
def rune_attr_exists(val: Any) -> bool:
    if val is None or not val: # Using truthiness handles COWList([]) and []
        return False
    return True
```
Alternatively, implement `__eq__` in `_COWList`.
