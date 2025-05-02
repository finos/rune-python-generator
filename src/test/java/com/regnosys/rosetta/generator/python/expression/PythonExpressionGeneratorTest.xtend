package com.regnosys.rosetta.generator.python.expression

// todo fix enum tests
import com.google.inject.Inject
import com.regnosys.rosetta.tests.RosettaInjectorProvider
import com.regnosys.rosetta.generator.python.PythonGeneratorTestUtils
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.extensions.InjectionExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.^extension.ExtendWith
import org.junit.jupiter.api.Disabled

@ExtendWith(InjectionExtension)
@InjectWith(RosettaInjectorProvider)

class PythonExpressionGeneratorTest {

    @Inject PythonGeneratorTestUtils testUtils


    @Test
    def void testArithmeticOperator () {
        val pythonString = testUtils.generatePythonFromString (
            '''
            type ArithmeticTest:
                a int (1..1)
                b int (1..1)
                condition Test:
                    if a + b = 3 then True
                    else False
            '''
            ).toString()
        testUtils.assertGeneratedContainsExpectedString (
            pythonString, 
            '''
            class com_rosetta_test_model_ArithmeticTest(BaseDataClass):
                _FQRTN = 'com.rosetta.test.model.ArithmeticTest'
                a: int = Field(..., description='')
                b: int = Field(..., description='')
                
                @rune_condition
                def condition_0_Test(self):
                    item = self
                    def _then_fn0():
                        return True
                    
                    def _else_fn0():
                        return False
                    
                    return if_cond_fn(rune_all_elements((rune_resolve_attr(self, "a") + rune_resolve_attr(self, "b")), "=", 3), _then_fn0, _else_fn0)
            '''
        )
    }
    @Test
    def void testGenerateSwitch() {
        testUtils.assertBundleContainsExpectedString(
            '''type FooTest:
            a int (1..1) <"Test field a">
            condition Test:
                a switch
                    1 then True,
                    2 then True,
                    default False
            ''',
            '''
            class com_rosetta_test_model_FooTest(BaseDataClass):
                _FQRTN = 'com.rosetta.test.model.FooTest'
                a: int = Field(..., description='Test field a')
                """
                Test field a
                """
                
                @rune_condition
                def condition_0_Test(self):
                    item = self
                    def _then_1():
                        return True
                    def _then_2():
                        return True
                    def _then_default():
                        return False
                    switchAttribute = rune_resolve_attr(self, "a")
                    if switchAttribute == 1:
                        return _then_1()
                    elif switchAttribute == 2:
                        return _then_2()
                    else:
                        return _then_default()
            '''
        )
    }

    @Test
    def void testGenerateChoiceCondition() {
        testUtils.assertBundleContainsExpectedString(
            '''type Test1:<"Test choice condition.">
            field1 string (0..1) <"Test string field 1">
            field2 string (0..1) <"Test string field 2">
            field3 string (0..1) <"Test string field 3">
            condition TestChoice: optional choice field1, field2, field3
            ''',
            '''
            class com_rosetta_test_model_Test1(BaseDataClass):
                """
                Test choice condition.
                """
                _FQRTN = 'com.rosetta.test.model.Test1'
                field1: Optional[str] = Field(None, description='Test string field 1')
                """
                Test string field 1
                """
                field2: Optional[str] = Field(None, description='Test string field 2')
                """
                Test string field 2
                """
                field3: Optional[str] = Field(None, description='Test string field 3')
                """
                Test string field 3
                """
                
                @rune_condition
                def condition_0_TestChoice(self):
                    item = self
                    return rune_check_one_of(self, 'field1', 'field2', 'field3', necessity=False)'''
        )
    }
    
    @Test
    def void testGenerateOneOfCondition() {
        testUtils.assertBundleContainsExpectedString(
            '''type Test1:<"Test one-of condition.">
                field1 string (0..1) <"Test string field 1">
                condition OneOf: one-of
            ''',
            '''
            class com_rosetta_test_model_Test1(BaseDataClass):
                _CHOICE_ALIAS_MAP ={"field1":[]}
                """
                Test one-of condition.
                """
                _FQRTN = 'com.rosetta.test.model.Test1'
                field1: Optional[str] = Field(None, description='Test string field 1')
                """
                Test string field 1
                """
                
                @rune_condition
                def condition_0_OneOf(self):
                    item = self
                    return rune_check_one_of(self, 'field1', necessity=True)'''
        )
    }

    @Test
    def void testGenerateIfThenCondition() {
        testUtils.assertBundleContainsExpectedString(
            '''type Test1: <"Test if-then condition.">
                field1 string (0..1) <"Test string field 1">
                field2 number (0..1) <"Test number field 2">
                condition TestCond: <"Test condition">
                    if field1 exists
                        then field2=0
            ''',
            '''
            class com_rosetta_test_model_Test1(BaseDataClass):
                """
                Test if-then condition.
                """
                _FQRTN = 'com.rosetta.test.model.Test1'
                field1: Optional[str] = Field(None, description='Test string field 1')
                """
                Test string field 1
                """
                field2: Optional[Decimal] = Field(None, description='Test number field 2')
                """
                Test number field 2
                """
                
                @rune_condition
                def condition_0_TestCond(self):
                    """
                    Test condition
                    """
                    item = self
                    def _then_fn0():
                        return rune_all_elements(rune_resolve_attr(self, "field2"), "=", 0)
                    
                    def _else_fn0():
                        return True
                    
                    return if_cond_fn(rune_attr_exists(rune_resolve_attr(self, "field1")), _then_fn0, _else_fn0)'''
        )
    }
    
    @Test
    def void testGenerateIfThenElseCondition() {
        testUtils.assertBundleContainsExpectedString(
            '''
            type Test1: <"Test if-then-else condition.">
            field1 string (0..1) <"Test string field 1">
            field2 number (0..1) <"Test number field 2">
            condition TestCond: <"Test condition">
                if field1 exists
                    then field2=0
                    else field2=1
            ''',
            '''
            class com_rosetta_test_model_Test1(BaseDataClass):
                """
                Test if-then-else condition.
                """
                _FQRTN = 'com.rosetta.test.model.Test1'
                field1: Optional[str] = Field(None, description='Test string field 1')
                """
                Test string field 1
                """
                field2: Optional[Decimal] = Field(None, description='Test number field 2')
                """
                Test number field 2
                """
                
                @rune_condition
                def condition_0_TestCond(self):
                    """
                    Test condition
                    """
                    item = self
                    def _then_fn0():
                        return rune_all_elements(rune_resolve_attr(self, "field2"), "=", 0)
                    
                    def _else_fn0():
                        return rune_all_elements(rune_resolve_attr(self, "field2"), "=", 1)
                    
                    return if_cond_fn(rune_attr_exists(rune_resolve_attr(self, "field1")), _then_fn0, _else_fn0)'''
        )
    }
    
    @Test
    def void testGenerateBooleanCondition() {
        testUtils.assertBundleContainsExpectedString(
            '''
            type Test1: <"Test boolean condition.">
            field1 boolean (1..1) <"Test booelan field 1">
            field2 number (0..1) <"Test number field 2">
            condition TestCond: <"Test condition">
                if field1= True
                    then field2=0
                    else field2=5
            ''',
            '''
            class com_rosetta_test_model_Test1(BaseDataClass):
                """
                Test boolean condition.
                """
                _FQRTN = 'com.rosetta.test.model.Test1'
                field1: bool = Field(..., description='Test booelan field 1')
                """
                Test booelan field 1
                """
                field2: Optional[Decimal] = Field(None, description='Test number field 2')
                """
                Test number field 2
                """
                
                @rune_condition
                def condition_0_TestCond(self):
                    """
                    Test condition
                    """
                    item = self
                    def _then_fn0():
                        return rune_all_elements(rune_resolve_attr(self, "field2"), "=", 0)
                    
                    def _else_fn0():
                        return rune_all_elements(rune_resolve_attr(self, "field2"), "=", 5)
                    
                    return if_cond_fn(rune_all_elements(rune_resolve_attr(self, "field1"), "=", True), _then_fn0, _else_fn0)'''
        )
    }
    
    @Test
    def void testGenerateAbsentCondition() {
        testUtils.assertBundleContainsExpectedString(
            '''
            type Test1: <"Test absent condition.">
            field1 boolean (1..1) <"Test booelan field 1">
            field2 number (0..1) <"Test number field 2">
            condition TestCond: <"Test condition">
                if field1= True
                    then field2=0
                    else field2 is absent
            ''',
            '''
            class com_rosetta_test_model_Test1(BaseDataClass):
                """
                Test absent condition.
                """
                _FQRTN = 'com.rosetta.test.model.Test1'
                field1: bool = Field(..., description='Test booelan field 1')
                """
                Test booelan field 1
                """
                field2: Optional[Decimal] = Field(None, description='Test number field 2')
                """
                Test number field 2
                """
                
                @rune_condition
                def condition_0_TestCond(self):
                    """
                    Test condition
                    """
                    item = self
                    def _then_fn0():
                        return rune_all_elements(rune_resolve_attr(self, "field2"), "=", 0)
                    
                    def _else_fn0():
                        return (not rune_attr_exists(rune_resolve_attr(self, "field2")))
                    
                    return if_cond_fn(rune_all_elements(rune_resolve_attr(self, "field1"), "=", True), _then_fn0, _else_fn0)'''
        )
    }
    
    @Test
    def void testGenerateOnlyElementCondition() {
        val pythonString = testUtils.generatePythonFromString (
            '''
            enum TestEnum: <"Enum to test">
            TestEnumValue1 <"Test enum value 1">
            TestEnumValue2 <"Test enum value 2">
            type Test1: <"Test only-element condition.">
                field1 TestEnum (0..1) <"Test enum field 1">
                field2 number (0..1) <"Test number field 2">
                condition TestCond: <"Test condition">
                    if field1 only-element= TestEnum->TestEnumValue1 
                        then field2=0
            ''').toString();
            val expectedTestEnum = 
            '''
            class TestEnum(rune.runtime.metadata.EnumWithMetaMixin, Enum):
                """
                Enum to test
                """
                TEST_ENUM_VALUE_1 = "TestEnumValue1"
                """
                Test enum value 1
                """
                TEST_ENUM_VALUE_2 = "TestEnumValue2"
                """
                Test enum value 2
                """'''
            val expectedTest1 = 
            '''
            class com_rosetta_test_model_Test1(BaseDataClass):
                """
                Test only-element condition.
                """
                _FQRTN = 'com.rosetta.test.model.Test1'
                field1: Optional[com.rosetta.test.model.TestEnum.TestEnum] = Field(None, description='Test enum field 1')
                """
                Test enum field 1
                """
                field2: Optional[Decimal] = Field(None, description='Test number field 2')
                """
                Test number field 2
                """
                
                @rune_condition
                def condition_0_TestCond(self):
                    """
                    Test condition
                    """
                    item = self
                    def _then_fn0():
                        return rune_all_elements(rune_resolve_attr(self, "field2"), "=", 0)
                    
                    def _else_fn0():
                        return True
                    
                    return if_cond_fn(rune_all_elements(rune_get_only_element(rune_resolve_attr(self, "field1")), "=", com.rosetta.test.model.TestEnum.TestEnum.TEST_ENUM_VALUE_1), _then_fn0, _else_fn0)'''
            testUtils.assertGeneratedContainsExpectedString (pythonString, expectedTestEnum)
            testUtils.assertGeneratedContainsExpectedString (pythonString, expectedTest1)
    }

    @Test
    def void testGenerateOnlyExistsCondition() {
        val pythonString = testUtils.generatePythonFromString(
            '''
            type A: <"Test type">
            field1 number (0..1) <"Test number field 1">

            type Test: <"Test only exists condition">
                aValue A (1..1) <"Test A type aValue">

                condition TestCond: <"Test condition">
                    if aValue -> field1 exists 
                        then aValue -> field1 only exists
            '''
        ).toString();
        testUtils.assertGeneratedContainsExpectedString(
            pythonString,
            '''
            class com_rosetta_test_model_Test(BaseDataClass):
                """
                Test only exists condition
                """
                _FQRTN = 'com.rosetta.test.model.Test'
                aValue: Annotated[com_rosetta_test_model_A, com_rosetta_test_model_A.serializer(), com_rosetta_test_model_A.validator()] = Field(..., description='Test A type aValue')
                """
                Test A type aValue
                """
                
                @rune_condition
                def condition_0_TestCond(self):
                    """
                    Test condition
                    """
                    item = self
                    def _then_fn0():
                        return rune_check_one_of(self, rune_resolve_attr(rune_resolve_attr(self, "aValue"), "field1"))
                    
                    def _else_fn0():
                        return True
                    
                    return if_cond_fn(rune_attr_exists(rune_resolve_attr(rune_resolve_attr(self, "aValue"), "field1")), _then_fn0, _else_fn0)'''
        )
        testUtils.assertGeneratedContainsExpectedString(
            pythonString,
            '''
            class com_rosetta_test_model_A(BaseDataClass):
                """
                Test type
                """
                _FQRTN = 'com.rosetta.test.model.A'
                field1: Optional[Decimal] = Field(None, description='Test number field 1')
                """
                Test number field 1
                """'''
        )
    }
    
    @Test
    def void testGenerateCountCondition() {
        val pythonString = testUtils.generatePythonFromString (
            '''
            type A: <"Test type">
                field1 int (0..*) <"Test int field 1">
                field2 int (1..*) <"Test int field 2">
                field3 int (1..3) <"Test int field 3">
                field4 int (0..3) <"Test int field 4">

            type Test: <"Test count operation condition">
                aValue A (1..*) <"Test A type aValue">
                
                condition TestCond: <"Test condition">
                    if aValue -> field1 count <> aValue -> field2 count 
                        then True
                    else False
            '''
            ).toString()
        testUtils.assertGeneratedContainsExpectedString(
            pythonString,
            '''
            class com_rosetta_test_model_Test(BaseDataClass):
                """
                Test count operation condition
                """
                _FQRTN = 'com.rosetta.test.model.Test'
                aValue: list[Annotated[com_rosetta_test_model_A, com_rosetta_test_model_A.serializer(), com_rosetta_test_model_A.validator()]] = Field(..., description='Test A type aValue', min_length=1)
                """
                Test A type aValue
                """
                
                @rune_condition
                def condition_0_TestCond(self):
                    """
                    Test condition
                    """
                    item = self
                    def _then_fn0():
                        return True
                    
                    def _else_fn0():
                        return False
                    
                    return if_cond_fn(rune_any_elements(rune_count(rune_resolve_attr(rune_resolve_attr(self, "aValue"), "field1")), "<>", rune_count(rune_resolve_attr(rune_resolve_attr(self, "aValue"), "field2"))), _then_fn0, _else_fn0)
            '''
        )
        testUtils.assertGeneratedContainsExpectedString(
            pythonString,
            '''
            class com_rosetta_test_model_A(BaseDataClass):
                """
                Test type
                """
                _FQRTN = 'com.rosetta.test.model.A'
                field1: Optional[list[int]] = Field(None, description='Test int field 1')
                """
                Test int field 1
                """
                field2: list[int] = Field(..., description='Test int field 2', min_length=1)
                """
                Test int field 2
                """
                field3: list[int] = Field(..., description='Test int field 3', min_length=1, max_length=3)
                """
                Test int field 3
                """
                field4: Optional[list[int]] = Field(None, description='Test int field 4', max_length=3)
                """
                Test int field 4
                """
            '''
        )
    }
    
    @Test
    def void testGenerateAnyCondition() {
        testUtils.assertBundleContainsExpectedString(
            '''
            type Test: <"Test any operation condition">
                field1 string (1..1) <"Test string field1">
                field2 string (1..1) <"Test boolean field2">
                condition TestCond: <"Test condition">
                    if field1="A" 
                    then ["B", "C", "D"] any = field2
            ''',
            '''
            class com_rosetta_test_model_Test(BaseDataClass):
                """
                Test any operation condition
                """
                _FQRTN = 'com.rosetta.test.model.Test'
                field1: str = Field(..., description='Test string field1')
                """
                Test string field1
                """
                field2: str = Field(..., description='Test boolean field2')
                """
                Test boolean field2
                """
                
                @rune_condition
                def condition_0_TestCond(self):
                    """
                    Test condition
                    """
                    item = self
                    def _then_fn0():
                        return rune_all_elements(["B", "C", "D"], "=", rune_resolve_attr(self, "field2"))
                    
                    def _else_fn0():
                        return True
                    
                    return if_cond_fn(rune_all_elements(rune_resolve_attr(self, "field1"), "=", "A"), _then_fn0, _else_fn0)'''
        )
    }
    
    @Test
    def void testGenerateDistinctCondition() {
        val pythonString = testUtils.generatePythonFromString (
            '''
            type A: <"Test type">
                field1 int (1..*) <"Test int field 1">
                field2 int (1..*) <"Test int field 2">
                    
            type Test: <"Test distinct operation condition">
                aValue A (1..*) <"Test A type aValue">
                field3 number (1..1)<"Test number field 3">
                condition TestCond: <"Test condition">
                    if aValue -> field1 distinct count = 1
                        then field3=0
                    else field3=1
            '''
        ).toString()
        testUtils.assertGeneratedContainsExpectedString(
            pythonString,
            '''
            class com_rosetta_test_model_Test(BaseDataClass):
                """
                Test distinct operation condition
                """
                _FQRTN = 'com.rosetta.test.model.Test'
                aValue: list[Annotated[com_rosetta_test_model_A, com_rosetta_test_model_A.serializer(), com_rosetta_test_model_A.validator()]] = Field(..., description='Test A type aValue', min_length=1)
                """
                Test A type aValue
                """
                field3: Decimal = Field(..., description='Test number field 3')
                """
                Test number field 3
                """
                
                @rune_condition
                def condition_0_TestCond(self):
                    """
                    Test condition
                    """
                    item = self
                    def _then_fn0():
                        return rune_all_elements(rune_resolve_attr(self, "field3"), "=", 0)
                    
                    def _else_fn0():
                        return rune_all_elements(rune_resolve_attr(self, "field3"), "=", 1)
                    
                    return if_cond_fn(rune_all_elements(rune_count(set(rune_resolve_attr(rune_resolve_attr(self, "aValue"), "field1"))), "=", 1), _then_fn0, _else_fn0)'''
        )
        testUtils.assertGeneratedContainsExpectedString(
            pythonString,
            '''
            class com_rosetta_test_model_A(BaseDataClass):
                """
                Test type
                """
                _FQRTN = 'com.rosetta.test.model.A'
                field1: list[int] = Field(..., description='Test int field 1', min_length=1)
                """
                Test int field 1
                """
                field2: list[int] = Field(..., description='Test int field 2', min_length=1)
                """
                Test int field 2
                """'''
        )
    }
    
    @Test
    def void testGenerateBinContainsCondition() {
        val pythonString = testUtils.generatePythonFromString(
            '''
            enum C: <"Test type C">
                field4 <"Test enum field 4">
                field5 <"Test enum field 5">
            type A: <"Test type">
                field1 int (1..*) <"Test int field 1">
                cValue C (1..*) <"Test C type cValue">
            type B: <"Test type B">
                field2 int (1..*) <"Test int field 2">
                aValue A (1..*) <"Test A type aValue">
            type Test: <"Test filter operation condition">
                bValue B (1..*) <"Test B type bValue">
                field3 boolean (0..1) <"Test bool type field3">
                condition TestCond: <"Test condition">
                    if field3=True
                    then bValue->aValue->cValue contains C->field4
            ''').toString()
        val expectedC =
        '''
        class C(rune.runtime.metadata.EnumWithMetaMixin, Enum):
            """
            Test type C
            """
            FIELD_4 = "field4"
            """
            Test enum field 4
            """
            FIELD_5 = "field5"
            """
            Test enum field 5
            """'''
        val expectedA =
        '''
        class com_rosetta_test_model_A(BaseDataClass):
            """
            Test type
            """
            _FQRTN = 'com.rosetta.test.model.A'
            field1: list[int] = Field(..., description='Test int field 1', min_length=1)
            """
            Test int field 1
            """
            cValue: list[com.rosetta.test.model.C.C] = Field(..., description='Test C type cValue', min_length=1)
            """
            Test C type cValue
            """'''
        val expectedB =
        '''
        class com_rosetta_test_model_B(BaseDataClass):
            """
            Test type B
            """
            _FQRTN = 'com.rosetta.test.model.B'
            field2: list[int] = Field(..., description='Test int field 2', min_length=1)
            """
            Test int field 2
            """
            aValue: list[Annotated[com_rosetta_test_model_A, com_rosetta_test_model_A.serializer(), com_rosetta_test_model_A.validator()]] = Field(..., description='Test A type aValue', min_length=1)
            """
            Test A type aValue
            """'''
        val expectedTest = 
        '''
        class com_rosetta_test_model_Test(BaseDataClass):
            """
            Test filter operation condition
            """
            _FQRTN = 'com.rosetta.test.model.Test'
            bValue: list[Annotated[com_rosetta_test_model_B, com_rosetta_test_model_B.serializer(), com_rosetta_test_model_B.validator()]] = Field(..., description='Test B type bValue', min_length=1)
            """
            Test B type bValue
            """
            field3: Optional[bool] = Field(None, description='Test bool type field3')
            """
            Test bool type field3
            """
            
            @rune_condition
            def condition_0_TestCond(self):
                """
                Test condition
                """
                item = self
                def _then_fn0():
                    return rune_contains(rune_resolve_attr(rune_resolve_attr(rune_resolve_attr(self, "bValue"), "aValue"), "cValue"), com.rosetta.test.model.C.C.FIELD_4)
                
                def _else_fn0():
                    return True
                
                return if_cond_fn(rune_all_elements(rune_resolve_attr(self, "field3"), "=", True), _then_fn0, _else_fn0)'''
        testUtils.assertGeneratedContainsExpectedString (pythonString, expectedC)
        testUtils.assertGeneratedContainsExpectedString (pythonString, expectedA)
        testUtils.assertGeneratedContainsExpectedString (pythonString, expectedB)
        testUtils.assertGeneratedContainsExpectedString (pythonString, expectedTest)
    }
    
    @Test
    def void testGenerateBinDisjointCondition() {
        testUtils.assertBundleContainsExpectedString(
            '''
            type Test: <"Test disjoint binary expression condition">
                field1 string (1..1) <"Test string field1">
                field2 string (1..1) <"Test string field2">
                field3 boolean (1..1) <"Test boolean field3">
                condition TestCond: <"Test condition">
                    if field3=False 
                    then if ["B", "C", "D"] any = field2 and ["A"] disjoint field1 
                    then field3=True
            ''',
            '''
            class com_rosetta_test_model_Test(BaseDataClass):
                """
                Test disjoint binary expression condition
                """
                _FQRTN = 'com.rosetta.test.model.Test'
                field1: str = Field(..., description='Test string field1')
                """
                Test string field1
                """
                field2: str = Field(..., description='Test string field2')
                """
                Test string field2
                """
                field3: bool = Field(..., description='Test boolean field3')
                """
                Test boolean field3
                """
                
                @rune_condition
                def condition_0_TestCond(self):
                    """
                    Test condition
                    """
                    item = self
                    def _then_fn1():
                        return rune_all_elements(rune_resolve_attr(self, "field3"), "=", True)
                    
                    def _else_fn1():
                        return True
                    
                    def _then_fn0():
                        return if_cond_fn((rune_all_elements(["B", "C", "D"], "=", rune_resolve_attr(self, "field2")) and rune_disjoint(["A"], rune_resolve_attr(self, "field1"))), _then_fn1, _else_fn1)
                    
                    def _else_fn0():
                        return True
                    
                    return if_cond_fn(rune_all_elements(rune_resolve_attr(self, "field3"), "=", False), _then_fn0, _else_fn0)'''
        )
    }
    @Test
    def void testGenerateFlattenCondition() {
        val pythonString = testUtils.generatePythonFromString(
        '''
        type Bar:
            numbers int (0..*)
        type Foo: <"Test flatten operation condition">
            bars Bar (0..*) <"test bar">
            condition TestCondition: <"Test Condition">
                [1, 2, 3] = 
                (bars
                    extract numbers
                    then flatten)
        ''').toString()

        val expectedFoo = 
        '''
        class com_rosetta_test_model_Foo(BaseDataClass):
            """
            Test flatten operation condition
            """
            _FQRTN = 'com.rosetta.test.model.Foo'
            bars: Optional[list[Annotated[com_rosetta_test_model_Bar, com_rosetta_test_model_Bar.serializer(), com_rosetta_test_model_Bar.validator()]]] = Field(None, description='test bar')
            """
            test bar
            """
            
            @rune_condition
            def condition_0_TestCondition(self):
                """
                Test Condition
                """
                item = self
                return rune_all_elements([1, 2, 3], "=", (lambda item: rune_flatten_list(item))(list(map(lambda item: rune_resolve_attr(item, "numbers"), rune_resolve_attr(self, "bars")))))
        '''
        testUtils.assertGeneratedContainsExpectedString(pythonString, expectedFoo)
    }

    @Disabled
    @Test
    def void setUp() {
        /*
        val pythonString = testUtils.generatePythonFromString(
            '''
            type Foo:
                bar Bar (0..*)
                baz Baz (0..1)
            
            type Bar:
                before number (0..1)
                after number (0..1)
                other number (0..1)
                beforeWithScheme number (0..1)
                    [metadata scheme]
                afterWithScheme number (0..1)
                    [metadata scheme]
                beforeList number (0..*)
                afterList number (0..*)
                beforeListWithScheme number (0..*)
                    [metadata scheme]
                afterListWithScheme number (0..*)
                    [metadata scheme]
            
            type Baz:
                bazValue number (0..1)
                other number (0..1)
            
            func Exists:
                inputs: foo Foo (1..1)
                output: result boolean (1..1)
                set result:
                    foo -> bar -> before exists
            
            func SingleExists:
                inputs: foo Foo (1..1)
                output: result boolean (1..1)
                set result:
                    foo -> bar -> before single exists

            func MultipleExists:
                inputs: foo Foo (1..1)
                output: result boolean (1..1)
                set result:
                    foo -> bar -> before multiple exists

            func OnlyExists:
                inputs: foo Foo (1..1)
                output: result boolean (1..1)
                set result:
                    foo -> bar -> before only exists
            
            func OnlyExistsMultiplePaths:
                inputs: foo Foo (1..1)
                output: result boolean (1..1)
                set result:
                    ( foo -> bar -> before, foo -> bar -> after ) only exists

            func OnlyExistsPathWithScheme:
                inputs: foo Foo (1..1)
                output: result boolean (1..1)
                set result:
                    ( foo -> bar -> before, foo -> bar -> afterWithScheme ) only exists
            
            func OnlyExistsBothPathsWithScheme:
                inputs: foo Foo (1..1)
                output: result boolean (1..1)
                set result:
                    ( foo -> bar -> beforeWithScheme, foo -> bar -> afterWithScheme ) only exists

            func OnlyExistsListMultiplePaths:
                inputs: foo Foo (1..1)
                output: result boolean (1..1)
                set result:
                    ( foo -> bar -> before, foo -> bar -> afterList ) only exists

            func OnlyExistsListPathWithScheme:
                inputs: foo Foo (1..1)
                output: result boolean (1..1)
                set result:
                    ( foo -> bar -> before, foo -> bar -> afterListWithScheme ) only exists
            
            func OnlyExistsListBothPathsWithScheme:
                inputs: foo Foo (1..1)
                output: result boolean (1..1)
                set result:
                    ( foo -> bar -> beforeListWithScheme, foo -> bar -> afterListWithScheme ) only exists

«««            TODO tests compilation only, add unit test
            func MultipleSeparateOr_NoAliases_Exists:
                inputs: foo Foo (1..1)
                output: result boolean (1..1)
                set result:
                    foo -> bar -> before exists or foo -> bar -> after exists

«««         TODO tests compilation only, add unit test
            func MultipleOr_NoAliases_Exists:
                inputs: foo Foo (1..1)
                output: result boolean (1..1)
                set result:
                    foo -> bar -> before exists or foo -> bar -> after exists or foo -> baz -> other exists
            
«««         TODO tests compilation only, add unit test
            func MultipleOrBranchNode_NoAliases_Exists:
                inputs: foo Foo (1..1)
                output: result boolean (1..1)
                set result:
                    foo -> bar exists or foo -> baz exists
            
«««         TODO tests compilation only, add unit test
            func MultipleAnd_NoAliases_Exists:
                inputs: foo Foo (1..1)
                output: result boolean (1..1)
                set result:
                    foo -> bar -> before exists and foo -> bar -> after exists and foo -> baz -> other exists
            
«««            TODO tests compilation only, add unit test
            func MultipleOrAnd_NoAliases_Exists:
                inputs: foo Foo (1..1)
                output: result boolean (1..1)
                set result:
                    foo -> bar -> before exists or ( foo -> bar -> after exists and foo -> baz -> other exists )
            
«««            TODO tests compilation only, add unit test
            func MultipleOrAnd_NoAliases_Exists2:
                inputs: foo Foo (1..1)
                output: result boolean (1..1)
                set result:
                    (foo -> bar -> before exists and foo -> bar -> after exists) or foo -> baz -> other exists or foo -> baz -> bazValue exists
            
«««            TODO tests compilation only, add unit test
            func MultipleOrAnd_NoAliases_Exists3:
                inputs: foo Foo (1..1)
                output: result boolean (1..1)
                set result:
                    (foo -> bar -> before exists or foo -> bar -> after exists) or (foo -> baz -> other exists and foo -> baz -> bazValue exists)
            
«««            TODO tests compilation only, add unit test
            func MultipleExistsWithOrAnd:
                inputs: foo Foo (1..1)
                output: result boolean (1..1)
                set result:
                    foo -> bar -> before exists or ( foo -> baz -> other exists and foo -> bar -> after exists ) or foo -> baz -> bazValue exists
            ''').toString()
        */
    }  
}