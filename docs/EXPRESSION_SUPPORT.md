
# _List of Supported and Unsupported Expressions_

## as of Oct 31 2025

| Feature                                | Handled | Description | Rosetta link |
|----------------------------------------|---------|-------------|--------------|
| ArithmeticOperation                    | ✓       | Binary arithmetic over two expressions |[RosettaExpression.xcore#L208](https://github.com/finos/rune-dsl/blob/38dd20e8f3f8145588de7a9586972c84caa23951/rosetta-lang/model/RosettaExpression.xcore#L208) |
| AsKeyOperation                         | ✓       | Unary operation treating its argument as a key |[RosettaExpression.xcore#L324](https://github.com/finos/rune-dsl/blob/38dd20e8f3f8145588de7a9586972c84caa23951/rosetta-lang/model/RosettaExpression.xcore#L324) |
| CanHandleListOfLists                   |         | Interface intended to tag unary operations that are compatible with nested lists | [RosettaExpression.xcore#L283](https://github.com/finos/rune-dsl/blob/38dd20e8f3f8145588de7a9586972c84caa23951/rosetta-lang/model/RosettaExpression.xcore#L283) |
| ChoiceOperation                        | ✓       | Unary operation selecting among specified attributes with a given necessity |[RosettaExpression.xcore#L334](https://github.com/finos/rune-dsl/blob/38dd20e8f3f8145588de7a9586972c84caa23951/rosetta-lang/model/RosettaExpression.xcore#L334) |
| ClosureParameter                       |         | Models parameters of an inline function used by operations like map or filter | [RosettaExpression.xcore#L431](https://github.com/finos/rune-dsl/blob/450fa8d9c85511718e0f241f6b83e4985559b962/rune-lang/model/RosettaExpression.xcore#L431) |
| ComparingFunctionalOperation           |         | Interface for unary functional operations that perform comparisons | [RosettaExpression.xcore#L461](https://github.com/finos/rune-dsl/blob/450fa8d9c85511718e0f241f6b83e4985559b962/rune-lang/model/RosettaExpression.xcore#L461) |
| ComparisonOperation                    | ✓       | Binary comparison (e.g., <, <=, >, >=) |[RosettaExpression.xcore#L230](https://github.com/finos/rune-dsl/blob/38dd20e8f3f8145588de7a9586972c84caa23951/rosetta-lang/model/RosettaExpression.xcore#L230) |
| DefaultOperation                       | ✓       | Binary fallback (use right when left is missing/empty) |[RosettaExpression.xcore#L211](https://github.com/finos/rune-dsl/blob/38dd20e8f3f8145588de7a9586972c84caa23951/rosetta-lang/model/RosettaExpression.xcore#L211) |
| DistinctOperation                      | ✓       | Unary list operation removing duplicates |[RosettaExpression.xcore#L309](https://github.com/finos/rune-dsl/blob/38dd20e8f3f8145588de7a9586972c84caa23951/rosetta-lang/model/RosettaExpression.xcore#L309) |
| EqualityOperation                      | ✓       | Binary equality/inequality assertion |[RosettaExpression.xcore#L227](https://github.com/finos/rune-dsl/blob/38dd20e8f3f8145588de7a9586972c84caa23951/rosetta-lang/model/RosettaExpression.xcore#L227) |
| FilterOperation                        | ✓       | Unary functional list operation keeping elements that satisfy a predicate |[RosettaExpression.xcore#L450](https://github.com/finos/rune-dsl/blob/38dd20e8f3f8145588de7a9586972c84caa23951/rosetta-lang/model/RosettaExpression.xcore#L450) |
| FirstOperation                         | ✓       | Unary list operation returning the first element | [RosettaExpression.xcore#L315](https://github.com/finos/rune-dsl/blob/38dd20e8f3f8145588de7a9586972c84caa23951/rosetta-lang/model/RosettaExpression.xcore#L315)|
| FlattenOperation                       | ✓       | Unary list operation flattening a list of lists |[RosettaExpression.xcore#L306](https://github.com/finos/rune-dsl/blob/38dd20e8f3f8145588de7a9586972c84caa23951/rosetta-lang/model/RosettaExpression.xcore#L306) |
| HasGeneratedInput                      |         | Interface for expressions that, if no input is provided, it'll auto‑use the implicit item (the current element). If an input is required, validation ensures you either provided one or item exists, otherwise it errors | [RosettaExpression.xcore#L33](https://github.com/finos/rune-dsl/blob/450fa8d9c85511718e0f241f6b83e4985559b962/rune-lang/model/RosettaExpression.xcore#L33) |
| InlineFunction                         |         | Base function with optional parameters used by functional operations |[RosettaExpression.xcore#L432](https://github.com/finos/rune-dsl/blob/38dd20e8f3f8145588de7a9586972c84caa23951/rosetta-lang/model/RosettaExpression.xcore#L432) |
| JoinOperation                          | ✓       | Binary operation joining items|[RosettaExpression.xcore#L239](https://github.com/finos/rune-dsl/blob/38dd20e8f3f8145588de7a9586972c84caa23951/rosetta-lang/model/RosettaExpression.xcore#L239) |
| LastOperation                          | ✓       | Unary list operation returning the last element | [RosettaExpression.xcore#L318](https://github.com/finos/rune-dsl/blob/38dd20e8f3f8145588de7a9586972c84caa23951/rosetta-lang/model/RosettaExpression.xcore#L318)|
| ListLiteral                            | ✓       | Literal list composed of RosettaElements |[RosettaExpression.xcore#L112](https://github.com/finos/rune-dsl/blob/38dd20e8f3f8145588de7a9586972c84caa23951/rosetta-lang/model/RosettaExpression.xcore#L112) |
| ListOperation                          |         | Interface for lists |[RosettaExpression.xcore#L280](https://github.com/finos/rune-dsl/blob/38dd20e8f3f8145588de7a9586972c84caa23951/rosetta-lang/model/RosettaExpression.xcore#L280) |
| LogicalOperation                       |  ✓       | Binary logical operation (e.g., and/or) |[RosettaExpression.xcore#L214](https://github.com/finos/rune-dsl/blob/38dd20e8f3f8145588de7a9586972c84caa23951/rosetta-lang/model/RosettaExpression.xcore#L214) |
| MandatoryFunctionalOperation           |         | Interface for functional operations that require an inline function |[RosettaExpression.xcore#L441](https://github.com/finos/rune-dsl/blob/38dd20e8f3f8145588de7a9586972c84caa23951/rosetta-lang/model/RosettaExpression.xcore#L441) |
| MapOperation                           | ✓       | Unary list operation transforming each element via a function |[RosettaExpression.xcore#L453](https://github.com/finos/rune-dsl/blob/38dd20e8f3f8145588de7a9586972c84caa23951/rosetta-lang/model/RosettaExpression.xcore#L453) |
| MaxOperation                           | ✓       | Comparing list operation selecting the maximum element |[RosettaExpression.xcore#L468](https://github.com/finos/rune-dsl/blob/38dd20e8f3f8145588de7a9586972c84caa23951/rosetta-lang/model/RosettaExpression.xcore#L468) |
| MinOperation                           | ✓       | Comparing list operation selecting the minimum element |[RosettaExpression.xcore#L465](https://github.com/finos/rune-dsl/blob/38dd20e8f3f8145588de7a9586972c84caa23951/rosetta-lang/model/RosettaExpression.xcore#L465) |
| ModifiableBinaryOperation              | ✓       | Binary operation that also carries a modifier |[RosettaExpression.xcore#L223](https://github.com/finos/rune-dsl/blob/38dd20e8f3f8145588de7a9586972c84caa23951/rosetta-lang/model/RosettaExpression.xcore#L223) |
| OneOfOperation                         | ✓       | Enforces one-of constraint on its argument |[RosettaExpression.xcore#L327](https://github.com/finos/rune-dsl/blob/38dd20e8f3f8145588de7a9586972c84caa23951/rosetta-lang/model/RosettaExpression.xcore#L327) |
| ParseOperation                         |         | Interface for parsing operations|[RosettaExpression.xcore#L342](https://github.com/finos/rune-dsl/blob/38dd20e8f3f8145588de7a9586972c84caa23951/rosetta-lang/model/RosettaExpression.xcore#L342) |
| ReduceOperation                        |         | Functional list operation reducing elements via a function |[RosettaExpression.xcore#L447](https://github.com/finos/rune-dsl/blob/38dd20e8f3f8145588de7a9586972c84caa23951/rosetta-lang/model/RosettaExpression.xcore#L447) |
| ReverseOperation                       |         | Unary list operation reversing element order |[RosettaExpression.xcore#L312](https://github.com/finos/rune-dsl/blob/38dd20e8f3f8145588de7a9586972c84caa23951/rosetta-lang/model/RosettaExpression.xcore#L312) |
| SortOperation                          | ✓       | Comparing functional list operation sorting elements |[RosettaExpression.xcore#L462](https://github.com/finos/rune-dsl/blob/38dd20e8f3f8145588de7a9586972c84caa23951/rosetta-lang/model/RosettaExpression.xcore#L462) |
| SumOperation                           | ✓       | Unary list operation summing numeric elements |[RosettaExpression.xcore#L321](https://github.com/finos/rune-dsl/blob/38dd20e8f3f8145588de7a9586972c84caa23951/rosetta-lang/model/RosettaExpression.xcore#L321) |
| SwitchOperation                        | ✓       | Selecting among cases (switch with guards and expressions) |[RosettaExpression.xcore#L367](https://github.com/finos/rune-dsl/blob/38dd20e8f3f8145588de7a9586972c84caa23951/rosetta-lang/model/RosettaExpression.xcore#L367) |
| ThenOperation                          | ✓       | Unary functional pipeline step applying a function to the argument |[RosettaExpression.xcore#L456](https://github.com/finos/rune-dsl/blob/38dd20e8f3f8145588de7a9586972c84caa23951/rosetta-lang/model/RosettaExpression.xcore#L456) |
| ToDateOperation                        | ✓       | Parse to date |[RosettaExpression.xcore#L358](https://github.com/finos/rune-dsl/blob/38dd20e8f3f8145588de7a9586972c84caa23951/rosetta-lang/model/RosettaExpression.xcore#L358) |
| ToDateTimeOperation                    | ✓       | Parse to date-time |[RosettaExpression.xcore#L361](https://github.com/finos/rune-dsl/blob/38dd20e8f3f8145588de7a9586972c84caa23951/rosetta-lang/model/RosettaExpression.xcore#L361) |
| ToEnumOperation                        | ✓       | Parse to an enum |[RosettaExpression.xcore#L354](https://github.com/finos/rune-dsl/blob/38dd20e8f3f8145588de7a9586972c84caa23951/rosetta-lang/model/RosettaExpression.xcore#L354) |
| ToIntOperation                         | ✓       | Parse to integer |[RosettaExpression.xcore#L348](https://github.com/finos/rune-dsl/blob/38dd20e8f3f8145588de7a9586972c84caa23951/rosetta-lang/model/RosettaExpression.xcore#L348) |
| ToStringOperation                      | ✓       | Convert to string |[RosettaExpression.xcore#L339](https://github.com/finos/rune-dsl/blob/38dd20e8f3f8145588de7a9586972c84caa23951/rosetta-lang/model/RosettaExpression.xcore#L339) |
| ToTimeOperation                        | ✓       | Parse to time object|[RosettaExpression.xcore#L351](https://github.com/finos/rune-dsl/blob/38dd20e8f3f8145588de7a9586972c84caa23951/rosetta-lang/model/RosettaExpression.xcore#L351) |
| ToZonedDateTimeOperation               | ✓       | Parse to zoned date-time object |[RosettaExpression.xcore#L364](https://github.com/finos/rune-dsl/blob/38dd20e8f3f8145588de7a9586972c84caa23951/rosetta-lang/model/RosettaExpression.xcore#L364) |
| UnaryFunctionalOperation               |         | Interface for functional operations with a single argument and an inline function |[RosettaExpression.xcore#L444](https://github.com/finos/rune-dsl/blob/38dd20e8f3f8145588de7a9586972c84caa23951/rosetta-lang/model/RosettaExpression.xcore#L444) |
| RosettaAbsentOperation                 | ✓       | Unary “is absent” assertion on the argument |[RosettaExpression.xcore#L297](https://github.com/finos/rune-dsl/blob/38dd20e8f3f8145588de7a9586972c84caa23951/rosetta-lang/model/RosettaExpression.xcore#L297) |
| RosettaBinaryOperation                 | ✓       | Base interface for binary operations with `left` and `right` |[RosettaExpression.xcore#L183](https://github.com/finos/rune-dsl/blob/38dd20e8f3f8145588de7a9586972c84caa23951/rosetta-lang/model/RosettaExpression.xcore#L183) |
| RosettaBooleanLiteral                  | ✓       | Boolean literal value |[RosettaExpression.xcore#L51](https://github.com/finos/rune-dsl/blob/38dd20e8f3f8145588de7a9586972c84caa23951/rosetta-lang/model/RosettaExpression.xcore#L51) |
| RosettaConditionalExpression           | ✓       | If-then-else expression|[RosettaExpression.xcore#L159](https://github.com/finos/rune-dsl/blob/38dd20e8f3f8145588de7a9586972c84caa23951/rosetta-lang/model/RosettaExpression.xcore#L159) |
| RosettaConstructorExpression           | ✓       | Constructs a value from key–value pairs |[RosettaExpression.xcore#L167](https://github.com/finos/rune-dsl/blob/38dd20e8f3f8145588de7a9586972c84caa23951/rosetta-lang/model/RosettaExpression.xcore#L167) |
| RosettaContainsExpression              | ✓       | Binary “contains” assertion |[RosettaExpression.xcore#L113](https://github.com/finos/rune-dsl/blob/38dd20e8f3f8145588de7a9586972c84caa23951/rosetta-lang/model/RosettaExpression.xcore#L113) |
| RosettaCountExpression                 | ✓       | Counts elements (list size) |[RosettaExpression.xcore#L303](https://github.com/finos/rune-dsl/blob/38dd20e8f3f8145588de7a9586972c84caa23951/rosetta-lang/model/RosettaExpression.xcore#L303) |
| RosettaDeepFeatureCall                 | ✓       | Accesses a nested attribute on a receiver |[RosettaExpression.xcore#L154](https://github.com/finos/rune-dsl/blob/38dd20e8f3f8145588de7a9586972c84caa23951/rosetta-lang/model/RosettaExpression.xcore#L154) |
| RosettaDisjointOperation               | ✓       | Binary "no common elements" assertion |[RosettaExpression.xcore#L236](https://github.com/finos/rune-dsl/blob/38dd20e8f3f8145588de7a9586972c84caa23951/rosetta-lang/model/RosettaExpression.xcore#L236) |
| RosettaEnumValueReference              | ✓       | Reference to an enumeration value | |
| RosettaExistsExpression                | ✓       | Unary “exists” assertion |[RosettaExpression.xcore#L293](https://github.com/finos/rune-dsl/blob/38dd20e8f3f8145588de7a9586972c84caa23951/rosetta-lang/model/RosettaExpression.xcore#L293) |
| RosettaFeatureCall                     | ✓       | Accesses a feature on a receiver |[RosettaExpression.xcore#L149](https://github.com/finos/rune-dsl/blob/38dd20e8f3f8145588de7a9586972c84caa23951/rosetta-lang/model/RosettaExpression.xcore#L149) |
| RosettaFunctionalOperation             |         | Interface for an inline function |[RosettaExpression.xcore#L437](https://github.com/finos/rune-dsl/blob/38dd20e8f3f8145588de7a9586972c84caa23951/rosetta-lang/model/RosettaExpression.xcore#L437) |
| RosettaImplicitVariable                |         | Reference to the implicit variable `item` (current element) |[RosettaExpression.xcore#L146](https://github.com/finos/rune-dsl/blob/38dd20e8f3f8145588de7a9586972c84caa23951/rosetta-lang/model/RosettaExpression.xcore#L146) |
| RosettaIntLiteral                      | ✓       | Integer literal value |[RosettaExpression.xcore#L84](https://github.com/finos/rune-dsl/blob/38dd20e8f3f8145588de7a9586972c84caa23951/rosetta-lang/model/RosettaExpression.xcore#L84) |
| RosettaNumberLiteral                   | ✓       | Decimal numeric literal value |[RosettaExpression.xcore#L73](https://github.com/finos/rune-dsl/blob/38dd20e8f3f8145588de7a9586972c84caa23951/rosetta-lang/model/RosettaExpression.xcore#L73) |
| RosettaOnlyElement                     | ✓       | Unary list operation asserting the only element |[RosettaExpression.xcore#L300](https://github.com/finos/rune-dsl/blob/38dd20e8f3f8145588de7a9586972c84caa23951/rosetta-lang/model/RosettaExpression.xcore#L300) |
| RosettaOnlyExistsExpression            | ✓       | Checks that only the listed expressions exist |[RosettaExpression.xcore#L247](https://github.com/finos/rune-dsl/blob/38dd20e8f3f8145588de7a9586972c84caa23951/rosetta-lang/model/RosettaExpression.xcore#L247) |
| RosettaReference                       | ✓       | Abstract base for reference expressions |[RosettaExpression.xcore#L123](https://github.com/finos/rune-dsl/blob/38dd20e8f3f8145588de7a9586972c84caa23951/rosetta-lang/model/RosettaExpression.xcore#L123) |

## Proposed Changes to ExpressionGeneration

The following changes are proposed to enhance the `ExpressionGeneration` functionality and address identified gaps:

1. **Direct Function Calling Support**: Implement support for `RosettaCallableWithArgs` to allow direct execution of other Rosetta functions within an expression.
2. **Date/Time & Math Standards**:  Create explicit tests and potentially refine implementation for `ToDate`, `ToDateTime`, `Min`, `Max`, `Sort`, and `Reverse` operations to ensure full compatibility with Python's standard library.
3. **List Operations**: Add comprehensive tests for `JoinOperation` and `ReverseOperation`.
4. **Refined Existence Logic**:  Investigate and correctly implement/test the `single exists` and `multiple exists` operators if present in the Rosetta DSL, ensuring they map correctly to Python logic (possibly extending `rune_attr_exists` or `rune_count`).
5. **Clean up `RosettaExistsExpressionTest`**:  Enable the disabled tests, add assertions, and migrate relevant test cases to the main test suite if they represent core functionality.

## Evaluation of Current Testing

### PythonExpressionGeneratorTest

- **Coverage**:  Strong coverage for arithmetic, boolean logic, basic list operations (`count`, `flatten`), `if-then-else`, and `switch`.
- **Gaps**:  Missing specific tests for `To*` conversions (Date/Time), `Min/Max/Sort`, and nested function calls.

### RosettaExistsExpressionTest

- **Status**:  Currently disabled (`@Disabled`) and non-functional (no assertions).
- **Analysis**:  Contains valuable DSL examples for complex `exists` logic (`only exists`, `single exists`, `multiple exists` combined with `or`/`and`).
- **Plan**:  Migrate valid DSL cases to `PythonExpressionGeneratorTest`. Specifically, the complex boolean logic combinations should be asserted against expected Python output to verify that `rune_attr_exists` interacts correctly with Python's `and`/`or` operators. The `single/multiple` exists concepts need to be verified against the `PythonExpressionGenerator` implementation (or lack thereof) to decide if they are supported features or need implementation.
