# Rune Null Handling & Collection Flattening Analysis

This document outlines the expected behavior of the Rune DSL regarding "nothing" (null) values in collections and provides evidence from the reference Java implementation.

## 1. The "Nothing" Semantics

In Rune, `null` (nothing) is semantically equivalent to an **empty list** when encountered in a collection context. This is derived from the following core specification principles:

*   **Flattening Rule**: Chained path expressions and collections are automatically flattened. An empty list (`nothing`) contributes zero elements to a flattened result.
*   **Propagation Rule**: Operations that fail to find a valid input (because the collection is empty or only contained `nothing`) must result in `nothing`.

### Example Walkthrough: `[1, nothing, 2] sort`
1.  **Input**: `[1, nothing, 2]`
2.  **Rune Semantic**: The list is flattened. `nothing` (an empty list) is discarded.
3.  **Intermediate State**: `[1, 2]`
4.  **Sort Operation**: The elements are compared and reordered.
5.  **Result**: `[1, 2]`

---

## 2. Implementation Evidence (Java Runtime)

The reference Java implementation uses the `MapperC` (Mapper for Collections) class to enforce these semantics.

### Ingesting Nulls as "Errors"
When a list is converted into a `MapperC`, any `null` element is tagged with `isError = true`.

*Source: `com.rosetta.model.lib.mapper.MapperC.java`*
```java
public static <T> MapperC<T> of(List<? extends T> ts) {
    List<MapperItem<? extends T, ?>> items = new ArrayList<>();
    if (ts != null) {
        for (T ele : ts) {
            if (ele == null) {
                // Nulls are explicitly marked as errors (nothing)
                items.add(new MapperItem<>(ele, MapperPath.builder().addNull(), true, Optional.empty()));
            } else {
                items.add(new MapperItem<>(ele, MapperPath.builder().addRoot(ele.getClass()), false, Optional.empty()));
            }
        }
    }
    return new MapperC<T>(items);
}
```

### The "Flattening" Filter (`nonErrorItems`)
All functional operations (sort, sum, min, max) utilize the `nonErrorItems()` stream, which effectively "flattens" the collection by ignoring the nulls.

```java
protected Stream<MapperItem<? extends T,?>> nonErrorItems() {
    // This implements the "nothing propagation" filter
    return items.stream().filter(i -> !i.isError());
}
```

### Application to `sort()`
Because `sort()` operates only on `nonErrorItems()`, it is guaranteed to exclude nulls from the comparison logic.

```java
public MapperC<T> sort() {
    return MapperC.of(nonErrorItems()
            .map(MapperItem::getMappedObject) // Retrieves 1, 2 (ignores null)
            .sorted()                        // Standard Java sort
            .collect(Collectors.toList()));
}
```

---

## 3. Impact on Python Generation

The current Python generator lacks this intermediate filtering layer. When it encounters `[1, None, 2]`, it passes the list directly to Python's `sorted()` or `sum()` functions:

*   **Current (Failing)**: `sorted([1, None, 2])` -> `TypeError: '<' not supported between instances of 'NoneType' and 'int'`
*   **Proposed (Correct)**: `sorted(x for x in (items or []) if x is not None)`

To align with Rune, the Python generator must implement **Null-Filtered Comprehensions** for all collection-based operations.
