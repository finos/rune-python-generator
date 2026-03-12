# Implementation Detail: Null-Safety and "Nothing" Propagation

This document outlines the architectural approach and implementation details for handling "nothing" (null) values in expressions and collection operations within the Rune Python generator.

## 1. The "Nothing" Semantics

In Rune, `null` (nothing) is semantically equivalent to an **empty list** when encountered in a collection context. This is governed by two core principles from the [Rune Modeling Components](https://github.com/finos/rune-dsl/blob/master/docs/rune-modelling-component.md) specification:

*   **Flattening Rule**: Chained path expressions and collections are automatically flattened. An empty list (`nothing`) contributes zero elements to a flattened result.
*   **Propagation Rule**: Operations that fail to find a valid input (e.g., because the input collection is `None` or only contains `None` values) must propagate `None` for scalar results.

### Example: `[1, nothing, 2] sort`
1.  **Input**: `[1, nothing, 2]`
2.  **Semantic**: The list is treated as having two effective elements `[1, 2]`.
3.  **Result**: `[1, 2]`

## 2. Java Reference Implementation (`MapperC`)

To ensure parity, the Python generator's logic is aligned with the reference Java implementation's `MapperC` (Mapper for Collections) class.

### Null Filtering logic
In Java, all collection operations utilize the `nonErrorItems()` filter, which excludes items marked as "nothing" (errors) before processing.

```java
// Logic from com.rosetta.model.lib.mapper.MapperC
protected Stream<MapperItem<? extends T,?>> nonErrorItems() {
    return items.stream().filter(i -> !i.isError()); // Filters out "nothing"
}

public MapperC<T> sort() {
    return MapperC.of(nonErrorItems()
            .map(MapperItem::getMappedObject) 
            .sorted()                        
            .collect(Collectors.toList()));
}
```

## 3. Python Implementation: Null-Filtered Comprehensions

The Python generator implements these semantics natively using **generator expressions** and **lambda wrapping**.

### Collection Filtering
For any operation acting on a collection (e.g., `sort`, `sum`, `max`), the generator emits a comprehension that filters out `None` values:

```python
# Generated pattern
(x for x in (items or []) if x is not None)
```

### Scalar Propagation
Operations that return a single value (aggregations like `sum`, `max`, `min` or accessors like `count`, `first`, `last`) are wrapped in a lambda to ensure the **Propagation Rule** is satisfied.

If the input collection is itself `None`, the entire operation evaluates to `None` (propagating "nothing") rather than an empty default like `0` or `[]`.

#### Example: Sum Operation
```python
# Rosetta: sum(myList)
(lambda items: sum((x for x in (items or []) if x is not None)) if items is not None else None)(myList)
```

## 4. Summary of Supported Operations

| Operation | Implementation Strategy |
| :--- | :--- |
| **Aggregations** (`sum`, `max`, etc.) | Lambda-wrapped with `if items is not None` check + generator expression filter. |
| **Accessors** (`first`, `last`) | Uses `next(...)` with a `None` default on a filtered generator. |
| **Transformations** (`sort`, `distinct`) | Returns `None` if input is `None`, otherwise returns a new filtered and transformed list. |
| **Flattening** | Recursive lambda that handles nested iterables and `COWList` while filtering `None` at every level. |
