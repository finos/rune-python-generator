# Unit Testing Changes

Date: 2026-02-10

This documents a shift in strategy for unit testing the `rune-python-generator` project.

Previously, all tests were "full code" tests, meaning they would generate the entire Python code for a given Rune model and then assert that broad sections of the generated code were exactly as expected. This approach was brittle and difficult to maintain, as even small changes in the generated code would break the tests.

The new strategy is to use a combination of "full code" tests and "targeted" tests. "Full code" tests are used to confirm that the structure and content of the generated code is correct. "Targeted" tests are used when we want to assert that specific parts of the generated code are exactly as expected. This approach is more maintainable and less brittle, as it allows us to make small changes to the generated code without breaking all of the tests.

Tests are categorized by **Test Type**:

- **Logic**: Focuses on the generator's internal logic, such as complex branching, nested if-else structures, and rule accumulation.
- **Functional**: Verifies specific Rosetta-to-Python operations (e.g., `count`, `distinct`, `filter`) and their mapping to Python/Rune library calls.
- **Component**: Validates fundamental building blocks of the generation system, such as Enums or Inheritance hierarchies.
- **Anchor**: "Gold Standard" tests that verify the overall architectural pattern (Phase 1-2-3 structure) in a complete, end-to-end scenario.

All of the tests as of 2026-02-10 are categorized below and have been revised to use the new strategy.

## Complete Code Tests

| JUnit Class | Test Function | Test Type | Notes |
| :--- | :--- | :--- | :--- |
| `PythonBasicGeneratorTest` | `testExpectedBundleBasic` | Logic | Phase 1-2-3. |
| `PythonBasicGeneratorTest` | `testExpectedBundleList` | Logic | |
| `PythonBasicGeneratorTest` | `testExpectedBundleRoot` | Logic | |
| `PythonBasicTypeGeneratorTest` | `testExpectedBundle` | Logic | Updated to reflect Phases 1, 2, and 3 for Rosetta type references. |
| `PythonBasicTypeGeneratorTest` | `testExpectedBundleList` | Logic | |
| `PythonBasicTypeGeneratorTest` | `testExpectedBundleOptional` | Logic | |
| `PythonCircularDependencyTest` | `testCircularDependencyFailure` | Anchor | Verifies the broken cycle standard with Phase 2/3 blocks. |
| `PythonCircularReferenceImplementationTest` | `testCircularDependencyImplementation` | Anchor | Gold Standard. Verifies the full Phase 1-2-3 structure for a complex cycle. |
| `PythonEnumGeneratorTest` | `testGenerateEnums` | Component | Enabled and updated to reflect Phases 1, 2, and 3. |
| `PythonEnumGeneratorTest` | `testEnumWithDisplayName` | Component | |
| `PythonFunctionAccumulationTest` | `testFunctionAccumulation` | Logic | Logic focused on function body. |
| `PythonFunctionBasicTest` | `testFunction` | Logic | Logic focused on function body. |
| `PythonFunctionBasicTest` | `testFunctionWithOutput` | Logic | Logic focused on function body. |
| `PythonFunctionBasicTest` | `testFunctionWithInputs` | Logic | Logic focused on function body. |
| `PythonFunctionBasicTest` | `testFunctionWithFunctionCallingFunction` | Logic | Logic focused on function body. |
| `PythonInheritanceGeneratorTest` | `testGenerateTypesExtends` | Component | Enabled and updated to verify full Phase 1-2-3 structure. |
| `PythonInheritanceGeneratorTest` | `testGenerateTypesExtends2` | Component | |
| `PythonInheritanceGeneratorTest` | `testGenerateTypesExtends3` | Component | |
| `PythonInheritanceGeneratorTest` | `testExtendATypeWithSameAttribute` | Component | |
| `PythonInheritanceGeneratorTest` | `testSetAttributesOnEmptyClassWithInheritance` | Component | |
| `PythonInheritanceGeneratorTest` | `testInheritanceWithDelayedUpdates` | Component | |
| `PythonObjectConditionGeneratorTest` | `testGenerateTypesWithCondition` | Anchor | Updated to reflect Phases 1, 2, and 3 including delayed updates. |
| `PythonObjectConditionGeneratorTest` | `testGenerateTypesWithMultipleConditions` | Anchor | |
| `PythonObjectConditionGeneratorTest` | `testConditionWithAttributeReference` | Anchor | |
| `PythonObjectConditionGeneratorTest` | `testConditionWithMultipleAttributes` | Anchor | |
| `RosettaMathOperationTest` | `testAdd` | Functional | Focused on expression-to-python logic. |
| `RosettaMathOperationTest` | `testSubtract` | Functional | |
| `RosettaMathOperationTest` | `testMultiply` | Functional | |
| `RosettaMathOperationTest` | `testDivide` | Functional | |
| `RosettaShortcutTest` | `testFunctionAlias` | Logic | Mostly logic. |
| `RosettaSwitchExpressionTest` | `testSwitchExpression` | Functional | Focused on expression-to-python logic. |

## Targeted Tests

| JUnit Class | Test Function | Test Type | Notes |
| :--- | :--- | :--- | :--- |
| `PythonBasicGeneratorTest` | `testBasicSingleProxy` | Logic | |
| `PythonBasicGeneratorTest` | `testBasicListProxy` | Logic | |
| `PythonBasicGeneratorTest` | `testRootProxy` | Logic | |
| `PythonBasicGeneratorTest` | `testBundleExists` | Logic | |
| `PythonDataRuleGeneratorTest` | `shouldGenerateConditionWithIfElseIf` | Logic | Focus on the `def rule_...` logic. |
| `PythonDataRuleGeneratorTest` | `shouldGenerateConditionWithNestedIfElseIf` | Logic | |
| `PythonDataRuleGeneratorTest` | `quoteExists` | Logic | |
| `PythonDataRuleGeneratorTest` | `nestedAnds` | Logic | |
| `PythonDataRuleGeneratorTest` | `numberAttributeisHandled` | Logic | |
| `PythonDataRuleGeneratorTest` | `dataRuleWithDoIfAndFunction` | Logic | |
| `PythonDataRuleGeneratorTest` | `dataRuleWithDoIfAndFunctionAndElse` | Logic | |
| `PythonDataRuleGeneratorTest` | `dataRuleCoinHead` | Logic | |
| `PythonDataRuleGeneratorTest` | `dataRuleCoinTail` | Logic | |
| `PythonDataRuleGeneratorTest` | `dataRuleCoinEdge` | Logic | |
| `PythonDataRuleGeneratorTest` | `conditionCount` | Logic | |
| `PythonDataRuleGeneratorTest` | `checkConditionWithInheritedAttribute` | Logic | |
| `PythonDataRuleGeneratorTest` | `shouldCheckInheritedCondition` | Logic | |
| `PythonMetaDataGeneratorTest` | `testAProxy` | Logic | |
| `PythonMetaDataGeneratorTest` | `testNodeRefProxy` | Logic | |
| `PythonMetaDataGeneratorTest` | `testAttributeRefProxy` | Logic | |
| `PythonMetaDataGeneratorTest` | `testRootProxy` | Logic | |
| `PythonMetaDataGeneratorTest` | `testBundleExists` | Logic | |
| `PythonMetaDataGeneratorTest` | `testExpectedBundleA` | Logic | |
| `PythonMetaDataGeneratorTest` | `testExpectedBundleAttributeRef` | Logic | |
| `PythonMetaDataGeneratorTest` | `testExpectedBundleNodeRef` | Logic | |
| `PythonMetaDataGeneratorTest` | `testExpectedBundleRoot` | Logic | |
| `PythonMetaDataGeneratorTest` | `testExpectedBundleScheme` | Logic | |
| `PythonMetaKeyRefGeneratorTest` | `testGeneration` | Logic | Fragile tests; verify specific key/ref constraints and Phase 2 updates only. |
| `PythonWithMetaTest` | `testFunctionWithMeta` | Functional | Verifies `rune_with_meta` mapping logic, not class structure. |
| `PythonWithMetaTest` | `testFunctionWithMetaEnumDependency` | Functional | |
| `RosettaConstructorExpressionTest` | `testConstructorExpression` | Functional | |
| `RosettaContainsOperationTest` | `testContains` | Functional | |
| `RosettaCountOperationTest` | `testGenerateCountCondition` | Functional | |
| `RosettaDistinctOperationTest` | `testDistinct` | Functional | |
| `RosettaFilterOperationTest` | `testFilter` | Functional | |
| `RosettaFlattenOperationTest` | `testFlatten` | Functional | |
| `RosettaMapOperationTest` | `testMap` | Functional | |

## Detailed Revision Examples

### 1. `PythonInheritanceGeneratorTest.java` (Example: `testGenerateTypesExtends2`)

**Current fragile assertion:**
Expections include full class bodies with Rosetta type references (e.g., `unit: Optional[com_rosetta_test_model_UnitType]`).

**Revised approach:**
Use `assertTrue(bundle.contains(...))` to verify the clean class body separately from the delayed update.

```java
// Phase 1: Clean Body
assertTrue(bundle.contains("unit: Optional[com_rosetta_test_model_UnitType] = Field(None, ..."));

// Phase 2: Delayed Update
assertTrue(bundle.contains("com_rosetta_test_model_MeasureBase.__annotations__[\"unit\"] = Optional[Annotated[...]"));
```

### 2. `PythonMetaDataGeneratorTest.java`

**Current fragile assertion:**
Expects `Annotated[str, MetaData("scheme")]` inline inside the class.

**Revised approach:**
If the attribute is a native type with metadata (e.g., `str`), it is currently NOT delayed, so the test might stay **Y**.
If it is a Rosetta type with metadata, it will be **N**.

## Guidelines for Revising Tests

1. **Avoid Exact Class Body Matches**: Do not compare the entire `class X:` block if it contains attributes referring to other Rosetta types.
2. **Use Anchors**: Assert on specific lines that are unique to the test case.
3. **Phase Verification**:
    - Verify the **Clean attribute** exists in the class body.
    - Verify the **Delayed annotation** exists elsewhere in the bundle (if it's a Rosetta type).
    - Verify the **Model rebuild** exists at the end of the bundle.
4. **Keep "Complete" reference tests**: At least one or two tests (like `PythonCircularReferenceImplementationTest`) should be considered "gold standard" and verify the entire output structure.
