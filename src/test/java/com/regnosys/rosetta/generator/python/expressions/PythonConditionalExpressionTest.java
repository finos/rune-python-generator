/*
 * Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
 * SPDX-License-Identifier: Apache-2.0
 */
package com.regnosys.rosetta.generator.python.expressions;

import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.regnosys.rosetta.generator.python.PythonGeneratorTestUtils;
import com.regnosys.rosetta.tests.RosettaInjectorProvider;

import jakarta.inject.Inject;

@ExtendWith(InjectionExtension.class)
@InjectWith(RosettaInjectorProvider.class)
@SuppressWarnings("LineLength")
public class PythonConditionalExpressionTest {

    /**
     * Test utils for generating Python.
     */
    @Inject
    private PythonGeneratorTestUtils testUtils;

    // -------------------------------------------------------------------------
    // From RosettaConditionalExpressionTest
    // -------------------------------------------------------------------------

    /**
     * Test case for if-then condition.
     */
    @Test
    public void testGenerateIfThenCondition() {
        testUtils.assertBundleContainsExpectedString(
            """
            type Test1: <"Test if-then condition.">
                field1 string (0..1) <"Test string field 1">
                field2 number (0..1) <"Test number field 2">
                condition TestCond: <"Test condition">
                    if field1 exists
                        then field2=0
            """,
            """
            class Test1(BaseDataClass):
                model_config = ConfigDict(defer_build=True)
                \"\"\"
                Test if-then condition.
                \"\"\"
                field1: Optional[str] = Field(None, description='Test string field 1')
                \"\"\"
                Test string field 1
                \"\"\"
                field2: Optional[Decimal] = Field(None, description='Test number field 2')
                \"\"\"
                Test number field 2
                \"\"\"

                @rune_condition
                def condition_0_TestCond(self):
                    \"\"\"
                    Test condition
                    \"\"\"
                    item = self
                    def _then_fn0():
                        return rune_all_elements(rune_resolve_attr(self, "field2"), "=", 0)

                    def _else_fn0():
                        return True

                    return if_cond_fn(rune_attr_exists(rune_resolve_attr(self, "field1")), _then_fn0, _else_fn0)""");
    }

    @Test
    public void testGenerateIfThenElseCondition() {
        testUtils.assertBundleContainsExpectedString(
            """
            type Test1: <"Test if-then-else condition.">
            field1 string (0..1) <"Test string field 1">
            field2 number (0..1) <"Test number field 2">
            condition TestCond: <"Test condition">
                if field1 exists
                    then field2=0
                    else field2=1
            """,
            """
            class Test1(BaseDataClass):
                model_config = ConfigDict(defer_build=True)
                \"\"\"
                Test if-then-else condition.
                \"\"\"
                field1: Optional[str] = Field(None, description='Test string field 1')
                \"\"\"
                Test string field 1
                \"\"\"
                field2: Optional[Decimal] = Field(None, description='Test number field 2')
                \"\"\"
                Test number field 2
                \"\"\"

                @rune_condition
                def condition_0_TestCond(self):
                    \"\"\"
                    Test condition
                    \"\"\"
                    item = self
                    def _then_fn0():
                        return rune_all_elements(rune_resolve_attr(self, "field2"), "=", 0)

                    def _else_fn0():
                        return rune_all_elements(rune_resolve_attr(self, "field2"), "=", 1)

                    return if_cond_fn(rune_attr_exists(rune_resolve_attr(self, "field1")), _then_fn0, _else_fn0)""");
    }

    @Test
    public void testGenerateBooleanCondition() {
        testUtils.assertBundleContainsExpectedString(
            """
            type Test1: <"Test boolean condition.">
            field1 boolean (1..1) <"Test booelan field 1">
            field2 number (0..1) <"Test number field 2">
            condition TestCond: <"Test condition">
                if field1= True
                    then field2=0
                    else field2=5
            """,
            """
            class Test1(BaseDataClass):
                model_config = ConfigDict(defer_build=True)
                \"\"\"
                Test boolean condition.
                \"\"\"
                field1: bool = Field(..., description='Test booelan field 1')
                \"\"\"
                Test booelan field 1
                \"\"\"
                field2: Optional[Decimal] = Field(None, description='Test number field 2')
                \"\"\"
                Test number field 2
                \"\"\"

                @rune_condition
                def condition_0_TestCond(self):
                    \"\"\"
                    Test condition
                    \"\"\"
                    item = self
                    def _then_fn0():
                        return rune_all_elements(rune_resolve_attr(self, "field2"), "=", 0)

                    def _else_fn0():
                        return rune_all_elements(rune_resolve_attr(self, "field2"), "=", 5)

                    return if_cond_fn(rune_all_elements(rune_resolve_attr(self, "field1"), "=", True), _then_fn0, _else_fn0)""");
    }

    @Test
    public void testGenerateAbsentCondition() {
        testUtils.assertBundleContainsExpectedString(
            """
            type Test1: <"Test absent condition.">
            field1 boolean (1..1) <"Test booelan field 1">
            field2 number (0..1) <"Test number field 2">
            condition TestCond: <"Test condition">
                if field1= True
                    then field2=0
                    else field2 is absent
            """,
            """
            class Test1(BaseDataClass):
                model_config = ConfigDict(defer_build=True)
                \"\"\"
                Test absent condition.
                \"\"\"
                field1: bool = Field(..., description='Test booelan field 1')
                \"\"\"
                Test booelan field 1
                \"\"\"
                field2: Optional[Decimal] = Field(None, description='Test number field 2')
                \"\"\"
                Test number field 2
                \"\"\"

                @rune_condition
                def condition_0_TestCond(self):
                    \"\"\"
                    Test condition
                    \"\"\"
                    item = self
                    def _then_fn0():
                        return rune_all_elements(rune_resolve_attr(self, "field2"), "=", 0)

                    def _else_fn0():
                        return (not rune_attr_exists(rune_resolve_attr(self, "field2")))

                    return if_cond_fn(rune_all_elements(rune_resolve_attr(self, "field1"), "=", True), _then_fn0, _else_fn0)""");
    }

    // -------------------------------------------------------------------------
    // From RosettaSwitchExpressionTest
    // -------------------------------------------------------------------------

    /**
     * Test case for switch basic.
     */
    @Test
    public void testGenerateSwitch() {
        testUtils.assertBundleContainsExpectedString(
            """
            type FooTest:
                a int (1..1) <"Test field a">
                condition Test:
                    a switch
                        1 then True,
                        2 then True,
                        default False
            """,
            """
            class FooTest(BaseDataClass):
                model_config = ConfigDict(defer_build=True)
                a: int = Field(..., description='Test field a')
                \"\"\"
                Test field a
                \"\"\"

                @rune_condition
                def condition_0_Test(self):
                    item = self
                    def _switch_fn_0():
                        def _then_1():
                            return True
                        def _then_2():
                            return True
                        def _then_default():
                            return False
                        switchAttribute = rune_resolve_attr(self, "a")
                        if switchAttribute is None:
                            return None
                        if switchAttribute == 1:
                            return _then_1()
                        elif switchAttribute == 2:
                            return _then_2()
                        else:
                            return _then_default()

                    return _switch_fn_0()
            """);
    }

    /**
     * A switch on a type with subtypes tests the value's type, and passes the matched value to
     * the case as item, so the case can use that type's attributes.
     */
    @Test
    public void testGenerateTypeSwitch() {
        String bundle = testUtils.generatePythonAndExtractBundle(
            """
            type Person:
                name string (0..1)

            type Employee extends Person:
                salary number (1..1)

            func Wage:
                inputs:
                    person Person (0..1)
                output:
                    wage number (0..1)
                set wage:
                    person switch
                        Employee then salary,
                        default 0
            """);
        testUtils.assertGeneratedContainsExpectedString(bundle, "def _then_1(item):\n            return rune_resolve_attr(item, \"salary\")");
        testUtils.assertGeneratedContainsExpectedString(bundle, "if switchAttribute is None:\n            return None");
        testUtils.assertGeneratedContainsExpectedString(bundle,
                "if isinstance(rune_unwrap(switchAttribute), Employee):\n            return _then_1(switchAttribute)");
    }

    /**
     * A switch on a choice follows the path through nested choices to the option, and passes the
     * option's value to the case as item.
     */
    @Test
    public void testGenerateNestedChoiceSwitch() {
        String bundle = testUtils.generatePythonAndExtractBundle(
            """
            choice PoweredVehicle:
                PetrolCar
                Motorcycle

            type PetrolCar:
                fuelCapacity number (1..1)

            type Motorcycle:
                fuelCapacity number (1..1)

            choice Vehicle:
                PoweredVehicle
                Bicycle

            type Bicycle:
                weight number (1..1)

            func Mileage:
                inputs:
                    vehicle Vehicle (1..1)
                output:
                    mileage number (1..1)
                set mileage:
                    vehicle switch
                        PetrolCar then 15 * fuelCapacity,
                        PoweredVehicle then 80,
                        Bicycle then 30
            """);
        testUtils.assertGeneratedContainsExpectedString(bundle,
                "if (caseValue := rune_resolve_attr(rune_resolve_attr(switchAttribute, \"PoweredVehicle\"), \"PetrolCar\")) is not None:\n            return _then_1(caseValue)");
        testUtils.assertGeneratedContainsExpectedString(bundle,
                "return (15 * rune_resolve_attr(item, \"fuelCapacity\"))");
    }

    // -------------------------------------------------------------------------
    // From RosettaChoiceExpressionTest
    // -------------------------------------------------------------------------

    /**
     * Test case for choice expression.
     */
    @Test
    public void testGenerateChoiceCondition() {
        testUtils.assertBundleContainsExpectedString("""
                type Test1:<"Test choice condition.">
                    field1 string (0..1) <"Test string field 1">
                    field2 string (0..1) <"Test string field 2">
                    field3 string (0..1) <"Test string field 3">
                    condition TestChoice: optional choice field1, field2, field3
                """,
                """
                class Test1(BaseDataClass):
                    model_config = ConfigDict(defer_build=True)
                    \"""
                    Test choice condition.
                    \"""
                    field1: Optional[str] = Field(None, description='Test string field 1')
                    \"""
                    Test string field 1
                    \"""
                    field2: Optional[str] = Field(None, description='Test string field 2')
                    \"""
                    Test string field 2
                    \"""
                    field3: Optional[str] = Field(None, description='Test string field 3')
                    \"""
                    Test string field 3
                    \"""

                    @rune_condition
                    def condition_0_TestChoice(self):
                        item = self
                        return rune_check_one_of(item, 'field1', 'field2', 'field3', necessity=False)""");
    }

    /**
     * Test case for one-of condition.
     */
    @Test
    public void testGenerateOneOfCondition() {
        testUtils.assertBundleContainsExpectedString("""
                type Test1:<"Test one-of condition.">
                    field1 string (0..1) <"Test string field 1">
                    condition OneOf: one-of
                """,
                """
                class Test1(BaseDataClass):
                    model_config = ConfigDict(defer_build=True)
                    _CHOICE_ALIAS_MAP ={"field1":[]}
                    \"""
                    Test one-of condition.
                    \"""
                    field1: Optional[str] = Field(None, description='Test string field 1')
                    \"""
                    Test string field 1
                    \"""

                    @rune_condition
                    def condition_0_OneOf(self):
                        item = self
                        return rune_check_one_of(item, 'field1', necessity=True)""");
    }
}
