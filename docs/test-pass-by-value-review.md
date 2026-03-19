# Rosetta/Rune Definition Review: `TestPassByValue`

This document summarizes the invalidity of the `TestPassByValue` function defined in `FunctionTest.rosetta`.

## Function Definition

```rosetta
236: func TestPassByValue:
237:     inputs:
238:         baseObject BaseObject (1..1)
239:     output:
240:         result BaseObject (1..1)
241:     set baseObject->value1:
242:         baseObject->value1 * -1
243:     set result:
244:         baseObject
```

## Why it is Invalid

The current definition of `TestPassByValue` violates several fundamental principles of the Rosetta/Rune language semantics:

### 1. Mutation of Inputs
Rosetta/Rune functions are declarative and pure by design. **Inputs are immutable.** 
Line 241 attempts to "set" a value on an attribute of `baseObject`, which is an input parameter:
```rosetta
set baseObject->value1: ...
```
This is explicitly prohibited in the language specification.

### 2. Illegal `set` Target
The `set` keyword must target the function's `output` or a component/path within that output. 
In `TestPassByValue`, the output is named `result`. Any `set` operation must start from `result`. Targeting `baseObject` (an input) makes the operation an invalid assignment.

### 3. Imperative vs. Declarative Style
The logic in this function follows an imperative "mutate-and-return" pattern. Rosetta is a **declarative** DSL where functions define the output's construction from inputs. 

## Recommended Valid Alternatives

### Option A: Constructor Expression (Best Practice)
Use a constructor to create a new object instance for the output:
```rosetta
func TestPassByValue:
    inputs:
        baseObject BaseObject (1..1)
    output:
        result BaseObject (1..1)
    set result:
        BaseObject {
            value1: baseObject -> value1 * -1,
            value2: baseObject -> value2
        }
```

### Option B: Stepwise Output Modification
Assign the original object to the output first, and then modify the output path:
```rosetta
func TestPassByValue:
    inputs:
        baseObject BaseObject (1..1)
    output:
        result BaseObject (1..1)
    set result: baseObject 
    set result->value1: 
        baseObject->value1 * -1
```

## Note on Generator Implementation
While the `rune-python-generator` may currently generate Python code that "works" for this specific case (by using `rune_cow` to simulate local mutation), it is supporting a conceptually invalid Rosetta construct. Testing this edge case is likely the purpose of this specific function in the unit test suite.
