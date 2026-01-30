package com.regnosys.rosetta.generator.python.functions;

import com.regnosys.rosetta.generator.python.PythonGeneratorTestUtils;
import com.regnosys.rosetta.tests.RosettaInjectorProvider;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import jakarta.inject.Inject;
import java.util.Map;

@ExtendWith(InjectionExtension.class)
@InjectWith(RosettaInjectorProvider.class)
public class PythonFunctionsTest {

    @Inject
    private PythonGeneratorTestUtils testUtils;

    // Test generating an Abs function
    @Test
    public void testGeneratedAbsFunction() {

        Map<String, CharSequence> gf = testUtils.generatePythonFromString(
                """
                        func Abs: <"Returns the absolute value of a number. If the argument is not negative, the argument is returned. If the argument is negative, the negation of the argument is returned.">
                            inputs:
                                arg number (1..1)
                            output:
                                result number (1..1)
                            set result:
                                if arg < 0 then -1 * arg else arg
                        """);
        String expectedStub = """
                from com._bundle import com_rosetta_test_model_functions_Abs as Abs

                sys.modules[__name__].__class__ = create_module_attr_guardian(sys.modules[__name__].__class__)


                # EOF
                """;

        testUtils.assertGeneratedContainsExpectedString(
                gf.get("src/com/rosetta/test/model/functions/Abs.py").toString(), expectedStub);
        String expectedBundle = """
                @replaceable
                @validate_call
                def com_rosetta_test_model_functions_Abs(arg: Decimal) -> Decimal:
                    \"""
                    Returns the absolute value of a number. If the argument is not negative, the argument is returned. If the argument is negative, the negation of the argument is returned.

                    Parameters\s
                    ----------
                    arg : Decimal

                    Returns
                    -------
                    result : Decimal

                    \"""
                    self = inspect.currentframe()


                    def _then_fn0():
                        return (-1 * rune_resolve_attr(self, "arg"))

                    def _else_fn0():
                        return rune_resolve_attr(self, "arg")

                    result =  if_cond_fn(rune_all_elements(rune_resolve_attr(self, "arg"), "<", 0), _then_fn0, _else_fn0)


                    return result
                """;

        testUtils.assertGeneratedContainsExpectedString(gf.get("src/com/_bundle.py").toString(), expectedBundle);
    }

    // Test generating a function referencing a type
    @Test
    public void testGeneratedFunctionReferencingType() {
        Map<String, CharSequence> gf = testUtils.generatePythonFromString(
                """
                        type A : <"A type">
                            a number (1..1)

                        func TestAbsType: <"Returns the absolute value of a number. If the argument is not negative, the argument is returned. If the argument is negative, the negation of the argument is returned.">
                            inputs:
                                arg  A (1..1)
                            output:
                                result number (1..1)
                            set result:
                                if arg->a < 0 then -1 * arg->a else arg->a
                        """);
        System.out.println(gf.toString());
    }

    // Test generating an AppendToList function
    @Disabled
    @Test
    public void testGeneratedAppendToListFunction() {
        String pythonString = testUtils.generatePythonFromString(
                """
                        func AppendToList: <"Append a single value to a list of numbers.">
                            inputs:
                                list number (0..*) <"Input list.">
                                value number (1..1) <"Value to add to a list.">
                            output:
                                result number (0..*) <"Resulting list.">

                            add result: list
                            add result: value
                        """).toString();

        String expected = """
                @replaceable
                def AppendToList(list: list[Decimal] | None, value: Decimal) -> list[Decimal]:
                    \"\"\"
                    Append a single value to a list of numbers.

                    Parameters\s
                    ----------
                    list : number
                    Input list.

                    value : number
                    Value to add to a list.

                    Returns
                    -------
                    result : number

                    \"\"\"
                    self = inspect.currentframe()


                    result =  rune_resolve_attr(self, "list")
                    result.add_rune_attr(self, rune_resolve_attr(self, "value"))


                    return result

                sys.modules[__name__].__class__ = create_module_attr_guardian(sys.modules[__name__].__class__)
                """;
        testUtils.assertGeneratedContainsExpectedString(pythonString, expected);
    }

    // Test generating a function to add two numbers
    /*
     * @Test
     * public void testGeneratedAddTwoNumbersFunction() {
     * String python = testUtils.generatePythonFromString(
     * """
     * func AddTwoNumbers: <"Add two numbers together.">
     * inputs:
     * number1 number (1..1) <"The first number to add.">
     * number2 number (1..1) <"The second number to add.">
     * output:
     * sum number (1..1)
     * set sum: number1
     * add sum: number2
     * 
     * """)
     * .toString();
     * 
     * String expected = """
     * 
     * @replaceable
     * def AddTwoNumbers(number1: Decimal, number2: Decimal) -> Decimal:
     * \"""
     * Add two numbers together.
     * 
     * Parameters\s
     * ----------
     * number1 : number
     * The first number to add.
     * 
     * number2 : number
     * The second number to add.
     * 
     * Returns
     * -------
     * sum : number
     * The sum of the two numbers.
     * 
     * \"""
     * _pre_registry = {}
     * self = inspect.currentframe()
     * 
     * # conditions
     * 
     * @rune_local_condition(_pre_registry)
     * def condition_0_CurrencyOrFinancialUnitExists(self):
     * return (rune_attr_exists(rune_resolve_attr(self, "number1")) and
     * rune_attr_exists(rune_resolve_attr(self, "number2")))
     * # Execute all registered conditions
     * rune_execute_local_conditions(_pre_registry, 'Pre-condition')
     * 
     * sum = set_rune_attr(rune_resolve_attr(self, 'sum'), 'number2',
     * rune_resolve_attr(self, "number2"))
     * 
     * 
     * return sum
     * 
     * sys.modules[__name__].__class__ =
     * create_module_attr_guardian(sys.modules[__name__].__class__)
     * """;
     * testUtils.assertGeneratedContainsExpectedString(python, expected);
     * }
     */
    @Disabled
    @Test
    public void testFilterOperation() {
        String python = testUtils.generatePythonFromString(
                """
                        func FilterQuantity: <"Filter list of quantities based on unit type.">
                                    inputs:
                                        quantities Quantity (0..*) <"List of quantities to filter.">
                                        unit UnitType (1..1) <"Currency unit type.">
                                    output:
                                        filteredQuantities Quantity (0..*)

                                    add filteredQuantities:
                                        quantities
                                            filter quantities -> unit all = unit
                                type Quantity: <"Specifies a quantity as a single value to be associated to a financial product, for example a transfer amount resulting from a trade. This data type extends QuantitySchedule and requires that only the single amount value exists.">
                                    value number (0..1) <"Specifies the value of the measure as a number. Optional because in a measure vector or schedule, this single value may be omitted.">
                                    unit UnitType (0..1) <"Qualifies the unit by which the amount is measured. Optional because a measure may be unit-less (e.g. when representing a ratio between amounts in the same unit).">
                                  type UnitType: <"Defines the unit to be used for price, quantity, or other purposes">
                                      value int (1..1)
                        """)
                .toString();

        String expected = """
                @replaceable
                def FilterQuantity(quantities: list[Quantity] | None, unit: UnitType) -> Quantity:
                    \"""
                    Filter list of quantities based on unit type.

                    Parameters\s
                    ----------
                    quantities : Quantity
                    List of quantities to filter.

                    unit : UnitType
                    Currency unit type.

                    Returns
                    -------
                    filteredQuantities : Quantity

                    \"""
                    self = inspect.currentframe()


                    filteredQuantities = rune_filter(rune_resolve_attr(self, "quantities"), lambda item: rune_all_elements(rune_resolve_attr(rune_resolve_attr(self, "quantities"), "unit"), "=", rune_resolve_attr(self, "unit")))


                    return filteredQuantities

                sys.modules[__name__].__class__ = create_module_attr_guardian(sys.modules[__name__].__class__)
                """;
        testUtils.assertGeneratedContainsExpectedString(python, expected);

    }

    // Test generation with an enum
    @Disabled
    @Test
    public void testWithEnumAttr() {

        Map<String, CharSequence> python = testUtils.generatePythonFromString(
                """
                        enum ArithmeticOperationEnum: <"An arithmetic operator that can be passed to a function">
                            Add <"Addition">
                            Subtract <"Subtraction">
                            Multiply <"Multiplication">
                            Divide <"Division">
                            Max <"Max of 2 values">
                            Min <"Min of 2 values">

                        func ArithmeticOperation:
                            inputs:
                                n1 number (1..1)
                                op ArithmeticOperationEnum (1..1)
                                n2 number (1..1)
                            output:
                                result number (1..1)

                            set result:
                                if op = ArithmeticOperationEnum -> Add then
                                    n1 + n2
                                else if op = ArithmeticOperationEnum -> Subtract then
                                    n1 - n2
                                else if op = ArithmeticOperationEnum -> Multiply then
                                    n1 * n2
                                else if op = ArithmeticOperationEnum -> Divide then
                                    n1 / n2
                                else if op = ArithmeticOperationEnum -> Max then
                                    Max( n1, n2 )
                                else if op = ArithmeticOperationEnum -> Min then
                                    Min( n1, n2 )
                        """);
        String generatedFunction = python.get("src/com/rosetta/test/model/functions/ArithmeticOperation.py").toString();

        String expected = """
                @replaceable
                def ArithmeticOperation(n1: Decimal, op: ArithmeticOperationEnum, n2: Decimal) -> Decimal:
                    \"""

                    Parameters\s
                    ----------
                    n1 : number

                    op : ArithmeticOperationEnum

                    n2 : number

                    Returns
                    -------
                    result : number

                    \"""
                    self = inspect.currentframe()


                    def _then_fn5():
                        return Min(rune_resolve_attr(self, "n1"), rune_resolve_attr(self, "n2"))

                    def _else_fn5():
                        return True

                    def _then_fn4():
                        return Max(rune_resolve_attr(self, "n1"), rune_resolve_attr(self, "n2"))

                    def _else_fn4():
                        return if_cond_fn(rune_all_elements(rune_resolve_attr(self, "op"), "=", com.rosetta.test.model.ArithmeticOperationEnum.ArithmeticOperationEnum.MIN), _then_fn5, _else_fn5)

                    def _then_fn3():
                        return (rune_resolve_attr(self, "n1") / rune_resolve_attr(self, "n2"))

                    def _else_fn3():
                        return if_cond_fn(rune_all_elements(rune_resolve_attr(self, "op"), "=", com.rosetta.test.model.ArithmeticOperationEnum.ArithmeticOperationEnum.MAX), _then_fn4, _else_fn4)

                    def _then_fn2():
                        return (rune_resolve_attr(self, "n1") * rune_resolve_attr(self, "n2"))

                    def _else_fn2():
                        return if_cond_fn(rune_all_elements(rune_resolve_attr(self, "op"), "=", com.rosetta.test.model.ArithmeticOperationEnum.ArithmeticOperationEnum.DIVIDE), _then_fn3, _else_fn3)

                    def _then_fn1():
                        return (rune_resolve_attr(self, "n1") - rune_resolve_attr(self, "n2"))

                    def _else_fn1():
                        return if_cond_fn(rune_all_elements(rune_resolve_attr(self, "op"), "=", com.rosetta.test.model.ArithmeticOperationEnum.ArithmeticOperationEnum.MULTIPLY), _then_fn2, _else_fn2)

                    def _then_fn0():
                        return (rune_resolve_attr(self, "n1") + rune_resolve_attr(self, "n2"))

                    def _else_fn0():
                        return if_cond_fn(rune_all_elements(rune_resolve_attr(self, "op"), "=", com.rosetta.test.model.ArithmeticOperationEnum.ArithmeticOperationEnum.SUBTRACT), _then_fn1, _else_fn1)

                    result =  if_cond_fn(rune_all_elements(rune_resolve_attr(self, "op"), "=", com.rosetta.test.model.ArithmeticOperationEnum.ArithmeticOperationEnum.ADD), _then_fn0, _else_fn0)


                    return result
                """;
        testUtils.assertGeneratedContainsExpectedString(generatedFunction, expected);
    }

    @Disabled
    @Test
    public void testFilterOperation2() {
        String python = testUtils.generatePythonFromString(
                """
                        func FilterQuantityByCurrencyExists: <"Filter list of quantities based on unit type.">
                            inputs:
                                quantities QuantitySchedule (0..*) <"List of quantities to filter.">
                            output:
                                filteredQuantities QuantitySchedule (0..*)

                            add filteredQuantities:
                                quantities
                                    filter item -> unit -> currency exists
                        type QuantitySchedule: <"Specifies a quantity as a single value to be associated to a financial product, for example a transfer amount resulting from a trade. This data type extends QuantitySchedule and requires that only the single amount value exists.">
                                value number (0..1) <"Specifies the value of the measure as a number. Optional because in a measure vector or schedule, this single value may be omitted.">
                                unit UnitType (0..1) <"Qualifies the unit by which the amount is measured. Optional because a measure may be unit-less (e.g. when representing a ratio between amounts in the same unit).">
                        type UnitType: <"Defines the unit to be used for price, quantity, or other purposes">
                                  currency string(0..1)
                        """)
                .toString();

        String expected = """
                @replaceable
                def FilterQuantityByCurrencyExists(quantities: list[QuantitySchedule] | None) -> QuantitySchedule:
                    \"""
                    Filter list of quantities based on unit type.

                    Parameters\s
                    ----------
                    quantities : QuantitySchedule
                    List of quantities to filter.

                    Returns
                    -------
                    filteredQuantities : QuantitySchedule

                    \"""
                    self = inspect.currentframe()


                    filteredQuantities = rune_filter(rune_resolve_attr(self, "quantities"), lambda item: rune_attr_exists(rune_resolve_attr(rune_resolve_attr(item, "unit"), "currency")))


                    return filteredQuantities

                sys.modules[__name__].__class__ = create_module_attr_guardian(sys.modules[__name__].__class__)
                """;
        testUtils.assertGeneratedContainsExpectedString(python, expected);

    }

    @Disabled
    @Test
    public void testAlias1() {

        String python = testUtils.generatePythonFromString(
                """
                        func testAlias:
                            inputs:
                                inp1 number(1..1)
                                inp2 number(1..1)
                            output:
                                result number(1..1)
                            alias Alias:
                                if inp1 < 0 then inp1

                            set result:
                                Alias
                        """).toString();

        String expected = """
                @replaceable
                def testAlias(inp1: Decimal, inp2: Decimal) -> Decimal:
                    \"""

                    Parameters\s
                    ----------
                    inp1 : number

                    inp2 : number

                    Returns
                    -------
                    result : number

                    \"""
                    self = inspect.currentframe()


                    def _then_fn0():
                        return rune_resolve_attr(self, "inp1")

                    def _else_fn0():
                        return True

                    Alias = if_cond_fn(rune_all_elements(rune_resolve_attr(self, "inp1"), "<", 0), _then_fn0, _else_fn0)
                    result =  rune_resolve_attr(self, "Alias")


                    return result

                sys.modules[__name__].__class__ = create_module_attr_guardian(sys.modules[__name__].__class__)
                """;
        testUtils.assertGeneratedContainsExpectedString(python, expected);

    }

    // Test alias with basemodels inputs
    @Disabled
    @Test
    public void testAlias2() {

        String python = testUtils.generatePythonFromString(
                """
                        type A:
                            valueA number(1..1)

                        type B:
                            valueB number(1..1)

                        type C:
                            valueC number(1..1)

                        func testAlias:
                            inputs:
                                a A (1..1)
                                b B (1..1)
                            output:
                                c C (1..1)
                            alias Alias1:
                                a->valueA
                            alias Alias2:
                                b->valueB
                            set c->valueC:
                                Alias1*Alias2
                        """).toString();

        String expected = """
                @replaceable
                def testAlias(a: A, b: B) -> C:
                    \"""

                    Parameters\s
                    ----------
                    a : A

                    b : B

                    Returns
                    -------
                    c : C

                    \"""
                    self = inspect.currentframe()


                    Alias1 = rune_resolve_attr(rune_resolve_attr(self, "a"), "valueA")
                    Alias2 = rune_resolve_attr(rune_resolve_attr(self, "b"), "valueB")
                    c = _get_rune_object('C', 'valueC', (rune_resolve_attr(self, "Alias1") * rune_resolve_attr(self, "Alias2")))


                    return c

                sys.modules[__name__].__class__ = create_module_attr_guardian(sys.modules[__name__].__class__)
                """;

        testUtils.assertGeneratedContainsExpectedString(python, expected);

    }

    @Disabled
    @Test
    public void testComplexSetConstructors() {

        String pythonString = testUtils.generatePythonFromString(
                """
                        type InterestRatePayout: <" A class to specify all of the terms necessary to define and calculate a cash flow based on a fixed, a floating or an inflation index rate. The interest rate payout can be applied to interest rate swaps and FRA (which both have two associated interest rate payouts), credit default swaps (to represent the fee leg when subject to periodic payments) and equity swaps (to represent the funding leg). The associated globalKey denotes the ability to associate a hash value to the InterestRatePayout instantiations for the purpose of model cross-referencing, in support of functionality such as the event effect and the lineage.">
                            [metadata key]
                            rateSpecification RateSpecification (0..1) <"The specification of the rate value(s) applicable to the contract using either a floating rate calculation, a single fixed rate, a fixed rate schedule, or an inflation rate calculation.">

                        type RateSpecification: <" A class to specify the fixed interest rate, floating interest rate or inflation rate.">
                            floatingRate FloatingRateSpecification (0..1) <"The floating interest rate specification, which includes the definition of the floating rate index. the tenor, the initial value, and, when applicable, the spread, the rounding convention, the averaging method and the negative interest rate treatment.">

                        type FloatingRateSpecification: <"A class defining a floating interest rate through the specification of the floating rate index, the tenor, the multiplier schedule, the spread, the qualification of whether a specific rate treatment and/or a cap or floor apply.">
                            [metadata key]

                            rateOption FloatingRateOption (0..1)

                        type FloatingRateOption: <"Specification of a floating rate option as a floating rate index and tenor.">
                            value int(1..1)

                        type ObservationIdentifier: <"Defines the parameters needed to uniquely identify a piece of data among the population of all available market data.">
                            observable Observable (1..1) <"Represents the asset or rate to which the observation relates.">
                            observationDate date (1..1) <"Specifies the date value to use when resolving the market data.">

                        type Observable: <"Specifies the object to be observed for a price, it could be an asset or a reference.">
                            [metadata key]

                            rateOption FloatingRateOption (0..1) <"Specifies a floating rate index and tenor.">

                        func ResolveInterestRateObservationIdentifiers: <"Defines which attributes on the InterestRatePayout should be used to locate and resolve the underlier's price, for example for the reset process.">
                            inputs:
                                payout InterestRatePayout (1..1)
                                date date (1..1)
                            output:
                                identifiers ObservationIdentifier (1..1)

                            set identifiers -> observable -> rateOption:
                                payout -> rateSpecification -> floatingRate -> rateOption
                            set identifiers -> observationDate:
                                date
                        """)
                .toString();

        String expected = """
                @replaceable
                def ResolveInterestRateObservationIdentifiers(payout: InterestRatePayout, date: datetime.date) -> ObservationIdentifier:
                    \"""
                    Defines which attributes on the InterestRatePayout should be used to locate and resolve the underlier's price, for example for the reset process.

                    Parameters\s
                    ----------
                    payout : InterestRatePayout

                    date : date

                    Returns
                    -------
                    identifiers : ObservationIdentifier

                    \"""
                    self = inspect.currentframe()


                    identifiers = _get_rune_object('ObservationIdentifier', 'observable', _get_rune_object('Observable', 'rateOption', rune_resolve_attr(rune_resolve_attr(rune_resolve_attr(rune_resolve_attr(self, "payout"), "rateSpecification"), "floatingRate"), "rateOption")))
                    identifiers = set_rune_attr(rune_resolve_attr(self, 'identifiers'), 'observationDate', rune_resolve_attr(self, "date"))


                    return identifiers

                sys.modules[__name__].__class__ = create_module_attr_guardian(sys.modules[__name__].__class__)
                """;
        testUtils.assertGeneratedContainsExpectedString(pythonString, expected);

    }

    @Disabled
    @Test
    public void testCondition() {
        String python = testUtils.generatePythonFromString(
                """
                        func RoundToNearest:
                            inputs:
                                value number (1..1)
                                nearest number (1..1)
                                roundingMode RoundingModeEnum (1..1)
                            output:
                                roundedValue number (1..1)
                            condition PositiveNearest:
                                nearest > 0
                        enum RoundingModeEnum:
                            Down
                            Up
                        """).toString();

        String expected = """
                @replaceable
                def RoundToNearest(value: Decimal, nearest: Decimal, roundingMode: RoundingModeEnum) -> Decimal:
                    \"""

                    Parameters\s
                    ----------
                    value : number

                    nearest : number

                    roundingMode : RoundingModeEnum

                    Returns
                    -------
                    roundedValue : number

                    \"""
                    _pre_registry = {}
                    self = inspect.currentframe()

                    # conditions

                    @rune_local_condition(_pre_registry)
                    def condition_0_PositiveNearest(self):
                        return rune_all_elements(rune_resolve_attr(self, "nearest"), ">", 0)
                    # Execute all registered conditions
                    rune_execute_local_conditions(_pre_registry, 'Pre-condition')

                    roundedValue = rune_resolve_attr(self, "roundedValue")


                    return roundedValue

                sys.modules[__name__].__class__ = create_module_attr_guardian(sys.modules[__name__].__class__)
                """;
        testUtils.assertGeneratedContainsExpectedString(python, expected);
    }

    @Disabled
    @Test
    public void testMultipleConditions() {
        String python = testUtils.generatePythonFromString(
                """
                        func RoundToNearest:
                            inputs:
                                value number (1..1)
                                nearest number (1..1)
                                roundingMode RoundingModeEnum (1..1)
                            output:
                                roundedValue number (1..1)
                            condition PositiveNearest:
                                nearest > 0
                            condition valueNegative:
                                value < 0
                        enum RoundingModeEnum:
                            Down
                            Up
                        """).toString();

        String expected = """
                @replaceable
                def RoundToNearest(value: Decimal, nearest: Decimal, roundingMode: RoundingModeEnum) -> Decimal:
                    \"""

                    Parameters\s
                    ----------
                    value : number

                    nearest : number

                    roundingMode : RoundingModeEnum

                    Returns
                    -------
                    roundedValue : number

                    \"""
                    _pre_registry = {}
                    self = inspect.currentframe()

                    # conditions

                    @rune_local_condition(_pre_registry)
                    def condition_0_PositiveNearest(self):
                        return rune_all_elements(rune_resolve_attr(self, "nearest"), ">", 0)

                    @rune_local_condition(_pre_registry)
                    def condition_1_valueNegative(self):
                        return rune_all_elements(rune_resolve_attr(self, "value"), "<", 0)
                    # Execute all registered conditions
                    rune_execute_local_conditions(_pre_registry, 'Pre-condition')

                    roundedValue = rune_resolve_attr(self, "roundedValue")


                    return roundedValue

                sys.modules[__name__].__class__ = create_module_attr_guardian(sys.modules[__name__].__class__)
                """;
        testUtils.assertGeneratedContainsExpectedString(python, expected);
    }

    @Disabled
    @Test
    public void testPostCondition() {
        String python = testUtils.generatePythonFromString(
                """
                        func NewFloatingPayout: <"Function specification to create the interest rate (floating) payout part of an Equity Swap according to the 2018 ISDA CDM Equity Confirmation template.">
                            inputs: masterConfirmation EquitySwapMasterConfirmation2018 (0..1)
                            output: interestRatePayout InterestRatePayout (1..1)

                            post-condition InterestRatePayoutTerms: <"Interest rate payout must inherit terms from the Master Confirmation Agreement when it exists.">
                                if masterConfirmation exists then
                                //interestRatePayout -> calculationPeriodDates = masterConfirmation -> equityCalculationPeriod and
                                interestRatePayout -> paymentDates = masterConfirmation -> equityCashSettlementDates
                        type EquitySwapMasterConfirmation2018:
                            equityCashSettlementDates PaymentDates (1..1)
                        type PaymentDates:
                            date date(0..1)
                        type InterestRatePayout:
                            paymentDates PaymentDates(0..1)
                        """)
                .get("src/com/rosetta/test/model/functions/NewFloatingPayout.py").toString();

        String expected = """
                @replaceable
                def NewFloatingPayout(masterConfirmation: EquitySwapMasterConfirmation2018 | None) -> InterestRatePayout:
                    \"""
                    Function specification to create the interest rate (floating) payout part of an Equity Swap according to the 2018 ISDA CDM Equity Confirmation template.

                    Parameters\s
                    ----------
                    masterConfirmation : EquitySwapMasterConfirmation2018

                    Returns
                    -------
                    interestRatePayout : InterestRatePayout

                    \"""
                    _post_registry = {}
                    self = inspect.currentframe()


                    interestRatePayout = rune_resolve_attr(self, "interestRatePayout")

                    # post-conditions

                    @rune_local_condition(_post_registry)
                    def condition_0_InterestRatePayoutTerms(self):
                        \"""
                        Interest rate payout must inherit terms from the Master Confirmation Agreement when it exists.
                        \"""
                        def _then_fn0():
                            return rune_all_elements(rune_resolve_attr(rune_resolve_attr(self, "interestRatePayout"), "paymentDates"), "=", rune_resolve_attr(rune_resolve_attr(self, "masterConfirmation"), "equityCashSettlementDates"))

                        def _else_fn0():
                            return True

                        return if_cond_fn(rune_attr_exists(rune_resolve_attr(self, "masterConfirmation")), _then_fn0, _else_fn0)
                    # Execute all registered post-conditions
                    rune_execute_local_conditions(_post_registry, 'Post-condition')

                    return interestRatePayout

                sys.modules[__name__].__class__ = create_module_attr_guardian(sys.modules[__name__].__class__)
                """;
        testUtils.assertGeneratedContainsExpectedString(python, expected);

    }

    @Disabled
    @Test
    public void functionCallTest() {
        String python = testUtils.generatePythonFromString(
                """
                        type InterestRatePayout: <" A class to specify all of the terms necessary to define and calculate a cash flow based on a fixed, a floating or an inflation index rate. The interest rate payout can be applied to interest rate swaps and FRA (which both have two associated interest rate payouts), credit default swaps (to represent the fee leg when subject to periodic payments) and equity swaps (to represent the funding leg). The associated globalKey denotes the ability to associate a hash value to the InterestRatePayout instantiations for the purpose of model cross-referencing, in support of functionality such as the event effect and the lineage.">
                                            [metadata key]
                                            rateSpecification RateSpecification (0..1) <"The specification of the rate value(s) applicable to the contract using either a floating rate calculation, a single fixed rate, a fixed rate schedule, or an inflation rate calculation.">

                                        type RateSpecification: <" A class to specify the fixed interest rate, floating interest rate or inflation rate.">
                                            floatingRate FloatingRateSpecification (0..1) <"The floating interest rate specification, which includes the definition of the floating rate index. the tenor, the initial value, and, when applicable, the spread, the rounding convention, the averaging method and the negative interest rate treatment.">

                                        type FloatingRateSpecification: <"A class defining a floating interest rate through the specification of the floating rate index, the tenor, the multiplier schedule, the spread, the qualification of whether a specific rate treatment and/or a cap or floor apply.">
                                            [metadata key]

                                            rateOption FloatingRateOption (0..1)

                                        type FloatingRateOption: <"Specification of a floating rate option as a floating rate index and tenor.">
                                            value int(1..1)
                                func FixedAmount:
                                  [calculation]
                                  inputs:
                                    interestRatePayout InterestRatePayout (1..1)
                                    date date (1..1)
                                  output:
                                    fixedAmount number (1..1)

                                  alias dayCountFraction: DayCountFraction(interestRatePayout, date)
                                func DayCountFraction:
                                     inputs:
                                         interestRatePayout InterestRatePayout (1..1)
                                         date date(1..1)
                                     output:
                                         a number(1..1)
                        """)
                .toString();

        String expected = """
                @replaceable
                def DayCountFraction(interestRatePayout: InterestRatePayout, date: datetime.date) -> Decimal:
                    \"""

                    Parameters\s
                    ----------
                    interestRatePayout : InterestRatePayout

                    date : date

                    Returns
                    -------
                    a : number

                    \"""
                    self = inspect.currentframe()


                    a = rune_resolve_attr(self, "a")


                    return a""";

        testUtils.assertGeneratedContainsExpectedString(python, expected);
    }
}
