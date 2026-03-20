package com.regnosys.rosetta.generator.python.functions;

import com.regnosys.rosetta.generator.python.PythonGeneratorTestUtils;
import com.regnosys.rosetta.tests.RosettaInjectorProvider;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import jakarta.inject.Inject;
import java.util.Map;

@ExtendWith(InjectionExtension.class)
@InjectWith(RosettaInjectorProvider.class)
@SuppressWarnings("checkstyle:LineLength")
public class PythonFunctionTypeTest {

    /**
     * Test utils for generating Python code.
     */
    @Inject
    private PythonGeneratorTestUtils testUtils;

    /**
     * Test case for generated function Abs.
     */
    @Test
    public void testGeneratedFunctionTypeAsInput() {
        Map<String, CharSequence> gf = testUtils.generatePythonFromString(
            """
            type AInput : <"A type">
                a number (1..1)

            func TestAbsType: <"Returns the absolute value of a number. If the argument is not negative, the argument is returned. If the argument is negative, the negation of the argument is returned.">
                inputs:
                    arg  AInput (1..1)
                output:
                    result number (1..1)
                set result:
                    if arg->a < 0 then -1 * arg->a else arg->a
            """);
        String expectedBundle = """
            @replaceable
            @validate_call
            def com_rosetta_test_model_functions_TestAbsType(arg: com_rosetta_test_model_AInput) -> Decimal:
                \"\"\"
                Returns the absolute value of a number. If the argument is not negative, the argument is returned. If the argument is negative, the negation of the argument is returned.

                Parameters
                ----------
                arg : com.rosetta.test.model.AInput

                Returns
                -------
                result : Decimal

                \"\"\"
                self = inspect.currentframe()

                arg = rune_cow(arg)


                def _then_fn0():
                    return (-1 * rune_resolve_attr(rune_resolve_attr(self, \"arg\"), \"a\"))

                def _else_fn0():
                    return rune_resolve_attr(rune_resolve_attr(self, \"arg\"), \"a\")

                result = if_cond_fn(rune_all_elements(rune_resolve_attr(rune_resolve_attr(self, \"arg\"), \"a\"), \"<\", 0), _then_fn0, _else_fn0)


                return result
            """;
        testUtils.assertGeneratedContainsExpectedString(gf.get("src/com/_bundle.py").toString(), expectedBundle);
    }

    /**
     * Test case for generated function Abs.
     */
    @Test
    public void testGeneratedFunctionTypeAsOutput() {
        Map<String, CharSequence> gf = testUtils.generatePythonFromString(
            """
            type AOutput : <"AOutput type">
                a number (1..1)

            func TestAbsOutputType: <"Returns the absolute value of a number. If the argument is not negative, the argument is returned. If the argument is negative, the negation of the argument is returned.">
                inputs:
                    arg number (1..1)
                output:
                    result AOutput (1..1)
                set result: AOutput {
                    a: if arg < 0 then arg * -1 else arg
                }
            """);

        String expectedBundle = """
            @replaceable
            @validate_call
            def com_rosetta_test_model_functions_TestAbsOutputType(arg: Decimal) -> com_rosetta_test_model_AOutput:
                \"\"\"
                Returns the absolute value of a number. If the argument is not negative, the argument is returned. If the argument is negative, the negation of the argument is returned.

                Parameters
                ----------
                arg : Decimal

                Returns
                -------
                result : com.rosetta.test.model.AOutput

                \"\"\"
                self = inspect.currentframe()

                arg = rune_cow(arg)


                def _then_fn0():
                    return (rune_resolve_attr(self, "arg") * -1)

                def _else_fn0():
                    return rune_resolve_attr(self, "arg")

                result = com_rosetta_test_model_AOutput(a=if_cond_fn(rune_all_elements(rune_resolve_attr(self, "arg"), "<", 0), _then_fn0, _else_fn0))


                return result
            """;
        testUtils.assertGeneratedContainsExpectedString(gf.get("src/com/_bundle.py").toString(), expectedBundle);
    }

    /**
     * Test case for numeric precision with decimals.
     */
    @Test
    public void testNumericPrecisionWithDecimals() {
        Map<String, CharSequence> gf = testUtils.generatePythonFromString(
            """
            func TestPrecision:
                output:
                    result number (1..1)
                set result:
                    0.1 + 0.2
            """);
        String generatedPython = gf.get("src/com/_bundle.py").toString();
        testUtils.assertGeneratedContainsExpectedString(generatedPython,
            "def com_rosetta_test_model_functions_TestPrecision() -> Decimal:");
        testUtils.assertGeneratedContainsExpectedString(generatedPython, "result = (Decimal('0.1') + Decimal('0.2'))");
        testUtils.assertGeneratedContainsExpectedString(generatedPython, "return result");
    }

    /**
     * Test case for generating function with enum.
     */
    @Test
    public void testGenerateFunctionWithEnum() {
        Map<String, CharSequence> gf = testUtils.generatePythonFromString(
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
                        [n1, n2] max
                    else if op = ArithmeticOperationEnum -> Min then
                        [n1, n2] min
            """);
        String expectedBundle = """
            @replaceable
            @validate_call
            def com_rosetta_test_model_functions_ArithmeticOperation(n1: Decimal, op: com.rosetta.test.model.ArithmeticOperationEnum.ArithmeticOperationEnum, n2: Decimal) -> Decimal:
                \"\"\"

                Parameters
                ----------
                n1 : Decimal

                op : com.rosetta.test.model.ArithmeticOperationEnum.ArithmeticOperationEnum

                n2 : Decimal

                Returns
                -------
                result : Decimal

                \"\"\"
                self = inspect.currentframe()

                n1 = rune_cow(n1)
                op = rune_cow(op)
                n2 = rune_cow(n2)


                def _then_fn5():
                    return (lambda items: min((x for x in (items or []) if x is not None), default=None) if items is not None else None)([rune_resolve_attr(self, "n1"), rune_resolve_attr(self, "n2")])

                def _else_fn5():
                    return True

                def _then_fn4():
                    return (lambda items: max((x for x in (items or []) if x is not None), default=None) if items is not None else None)([rune_resolve_attr(self, "n1"), rune_resolve_attr(self, "n2")])

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

                result = if_cond_fn(rune_all_elements(rune_resolve_attr(self, "op"), "=", com.rosetta.test.model.ArithmeticOperationEnum.ArithmeticOperationEnum.ADD), _then_fn0, _else_fn0)


                return result
            """;
        testUtils.assertGeneratedContainsExpectedString(gf.get("src/com/_bundle.py").toString(), expectedBundle);
    }

    /**
     * Test case for object creation from fields.
     */
    @Test
    public void testObjectCreationFromFields() {
        Map<String, CharSequence> gf = testUtils.generatePythonFromString(
            """
            type BaseObject:
                value1 int (1..1)
                value2 int (1..1)
            func TestObjectCreationFromFields:
                inputs:
                    baseObject BaseObject (1..1)
                output:
                    result BaseObject (1..1)
                set result:
                    BaseObject {
                        value1: baseObject->value1,
                        value2: baseObject->value2
                    }
            """);

        String expectedBundle = """
            @replaceable
            @validate_call
            def com_rosetta_test_model_functions_TestObjectCreationFromFields(baseObject: com_rosetta_test_model_BaseObject) -> com_rosetta_test_model_BaseObject:
                \"\"\"

                Parameters
                ----------
                baseObject : com.rosetta.test.model.BaseObject

                Returns
                -------
                result : com.rosetta.test.model.BaseObject

                \"\"\"
                self = inspect.currentframe()

                baseObject = rune_cow(baseObject)


                result = com_rosetta_test_model_BaseObject(value1=rune_resolve_attr(rune_resolve_attr(self, \"baseObject\"), \"value1\"), value2=rune_resolve_attr(rune_resolve_attr(self, \"baseObject\"), \"value2\"))


                return result
            """;
        testUtils.assertGeneratedContainsExpectedString(gf.get("src/com/_bundle.py").toString(), expectedBundle);
    }

    /**
     * Test case for complex set constructors.
     */
    @Test
    public void testComplexSetConstructors() {
        Map<String, CharSequence> gf = testUtils.generatePythonFromString(
            """
            type InterestRatePayout:
                rateSpecification RateSpecification (0..1)

            type RateSpecification:
                floatingRate FloatingRateSpecification (0..1)

            type FloatingRateSpecification:
                rateOption int (0..1)

            type ObservationIdentifier:
                rateOption int (0..1)
                observationDate date (1..1)

            func ResolveInterestRateObservationIdentifiers:
                inputs:
                    payout InterestRatePayout (1..1)
                    date date (1..1)
                output:
                    identifiers ObservationIdentifier (1..1)

                set identifiers -> rateOption:
                    payout -> rateSpecification -> floatingRate -> rateOption
                set identifiers -> observationDate:
                    date
            """);

        String expectedBundle = """
            @replaceable
            @validate_call
            def com_rosetta_test_model_functions_ResolveInterestRateObservationIdentifiers(payout: com_rosetta_test_model_InterestRatePayout, date: datetime.date) -> com_rosetta_test_model_ObservationIdentifier:
            """;
        String generatedPython = gf.get("src/com/_bundle.py").toString();
        // Check signature
        testUtils.assertGeneratedContainsExpectedString(generatedPython, expectedBundle);
        // Check ObjectBuilder usage
        testUtils.assertGeneratedContainsExpectedString(generatedPython, "identifiers = ObjectBuilder(com_rosetta_test_model_ObservationIdentifier)");
        testUtils.assertGeneratedContainsExpectedString(generatedPython, "identifiers.rateOption = rune_resolve_attr(rune_resolve_attr(rune_resolve_attr(rune_resolve_attr(self, \"payout\"), \"rateSpecification\"), \"floatingRate\"), \"rateOption\")");
        testUtils.assertGeneratedContainsExpectedString(generatedPython, "identifiers.observationDate = rune_resolve_attr(self, \"date\")");
    }

    /**
     * Test case for 'as-key' syntax.
     */
    @Test
    public void testAsKey() {
        Map<String, CharSequence> gf = testUtils.generatePythonFromString(
            """
            type Person:
                [metadata key]
                name string (1..1)

            type House:
                owner Person (1..1)
                    [metadata reference]

            func TestAsKey:
                inputs:
                    p Person (1..1)
                output:
                    h House (1..1)
                set h -> owner: p as-key
            """);
        String expectedBundle = """
                self = inspect.currentframe()

                p = rune_cow(p)


                h = ObjectBuilder(com_rosetta_test_model_House)
                h.owner = Reference(rune_resolve_attr(self, "p"))


                return h
            """;
        testUtils.assertGeneratedContainsExpectedString(gf.get("src/com/_bundle.py").toString(), expectedBundle);
    }

    /**
     * Test case for 'as-key' syntax with multiple cardinality.
     */
    @Test
    public void testAsKeyMulti() {
        Map<String, CharSequence> gf = testUtils.generatePythonFromString(
            """
            type Person:
                [metadata key]
                name string (1..1)

            type House:
                owners Person (1..*)
                    [metadata reference]

            func TestAsKeyMulti:
                inputs:
                    ps Person (0..*)
                output:
                    h House (1..1)
                set h -> owners: ps as-key
            """);
        String expectedBundle = """
                h = ObjectBuilder(com_rosetta_test_model_House)
                h.owners = [Reference(x) for x in (rune_resolve_attr(self, "ps") or []) if x is not None]


                return h
            """;
        testUtils.assertGeneratedContainsExpectedString(gf.get("src/com/_bundle.py").toString(), expectedBundle);
    }
}
