package com.regnosys.rosetta.generator.python.object;

import jakarta.inject.Inject;
import com.regnosys.rosetta.tests.RosettaInjectorProvider;
import com.regnosys.rosetta.generator.python.PythonGeneratorTestUtils;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests for basic Python type generation using the standalone partitioning strategy.
 */
@ExtendWith(InjectionExtension.class)
@InjectWith(RosettaInjectorProvider.class)
@SuppressWarnings("checkstyle:LineLength")
public class PythonScalarTypeTest {

    @Inject
    private PythonGeneratorTestUtils testUtils;

    @Test
    public void testGenerateBasicTypeString() {
        testUtils.assertBundleContainsExpectedString(
            """
            type Tester:
                one string (0..1)
                list string (1..*)
            """,
            """
            class Tester(BaseDataClass):
                one: Optional[str] = Field(None, description='')
                list: list[str | None] = Field(..., description='', min_length=1)""");
    }

    @Test
    public void testGenerateBasicTypeInt() {
        testUtils.assertBundleContainsExpectedString(
            """
            type Tester:
                one int (0..1)
                list int (1..*)
            """,
            """
            class Tester(BaseDataClass):
                one: Optional[int] = Field(None, description='')
                list: list[int | None] = Field(..., description='', min_length=1)""");
    }

    @Test
    public void testGenerateBasicTypeNumber() {
        testUtils.assertBundleContainsExpectedString(
            """
            type Tester:
                one number (0..1)
                list number (1..*)
            """,
            """
            class Tester(BaseDataClass):
                one: Optional[Decimal] = Field(None, description='')
                list: list[Decimal | None] = Field(..., description='', min_length=1)""");
    }

    @Test
    public void testGenerateBasicTypeBoolean() {
        testUtils.assertBundleContainsExpectedString(
            """
            type Tester:
                one boolean (0..1)
                list boolean (1..*)
            """,
            """
            class Tester(BaseDataClass):
                one: Optional[bool] = Field(None, description='')
                list: list[bool | None] = Field(..., description='', min_length=1)""");
    }

    @Test
    public void testGenerateBasicTypeDate() {
        testUtils.assertBundleContainsExpectedString(
            """
            type Tester:
                one date (0..1)
                list date (1..*)
            """,
            """
            class Tester(BaseDataClass):
                one: Optional[datetime.date] = Field(None, description='')
                list: list[datetime.date | None] = Field(..., description='', min_length=1)""");
    }

    @Test
    public void testGenerateBasicTypeDateTime() {
        testUtils.assertBundleContainsExpectedString(
            """
            type Tester:
                one dateTime (0..1)
                list dateTime (1..*)
                zoned zonedDateTime (0..1)
            """,
            """
            class Tester(BaseDataClass):
                one: Optional[datetime.datetime] = Field(None, description='')
                list: list[datetime.datetime | None] = Field(..., description='', min_length=1)
                zoned: Optional[datetime.datetime] = Field(None, description='')""");
    }

    @Test
    public void testGenerateBasicTypeTime() {
        testUtils.assertBundleContainsExpectedString(
            """
            type Tester:
                one time (0..1)
                list time (1..*)
            """,
            """
            class Tester(BaseDataClass):
                one: Optional[datetime.time] = Field(None, description='')
                list: list[datetime.time | None] = Field(..., description='', min_length=1)""");
    }

    @Test
    public void testOmitGlobalKeyAnnotationWhenNotDefined() {
        testUtils.assertBundleContainsExpectedString(
            """
            type AttributeGlobalKeyTest:
                withoutGlobalKey string (1..1)
            """,
            """
            class AttributeGlobalKeyTest(BaseDataClass):
                withoutGlobalKey: str = Field(..., description='')""");
    }

    @Test
    public void testGenerateRosettaCalculationTypeAsString() {
        testUtils.assertBundleContainsExpectedString(
            """
            type Foo:
                bar calculation (0..1)
            """,
            """
            class Foo(BaseDataClass):
                bar: Optional[str] = Field(None, description='')""");
    }

    @Test
    public void testComplexTypeHierarchy() {
        String model = """
            type TestType: <"Test type description.">
                testTypeValue1 string (1..1)
                testTypeValue2 string (0..1)
                testTypeValue3 string (1..*)
                testTypeValue4 TestType2 (1..1)
                testEnum TestEnum (0..1)

            type TestType2:
                testType2Value1 number(1..*)
                testType2Value2 date(0..1)
                testEnum TestEnum (0..1)

            enum TestEnum:
                TestEnumValue1
                TestEnumValue2
            """;
        testUtils.assertBundleContainsExpectedString(model,
            """
            class TestType2(BaseDataClass):
                testType2Value1: list[Decimal | None] = Field(..., description='', min_length=1)
                testType2Value2: Optional[datetime.date] = Field(None, description='')
                testEnum: Optional[com.rosetta.test.model.TestEnum.TestEnum] = Field(None, description='')
            """);

        testUtils.assertBundleContainsExpectedString(model,
            """
            class TestType(BaseDataClass):
                \"\"\"
                Test type description.
                \"\"\"
                testTypeValue1: str = Field(..., description='')
                testTypeValue2: Optional[str] = Field(None, description='')
                testTypeValue3: list[str | None] = Field(..., description='', min_length=1)
                testTypeValue4: TestType2 = Field(..., description='')
                testEnum: Optional[com.rosetta.test.model.TestEnum.TestEnum] = Field(None, description='')
            """);
    }

    @Test
    public void testComplexTypeHierarchyWithInheritance() {
        String model = """
            type UnitType:
                currency string (0..1)

            type MeasureBase:
                amount number (1..1)
                unitOfAmount UnitType (1..1)

            type Quantity extends MeasureBase:
                multiplier number (0..1)
                multiplierUnit UnitType (0..1)
            """;

        testUtils.assertBundleContainsExpectedString(model,
            """
            class UnitType(BaseDataClass):
                currency: Optional[str] = Field(None, description='')
            """);

        testUtils.assertBundleContainsExpectedString(model,
            """
            class MeasureBase(BaseDataClass):
                amount: Decimal = Field(..., description='')
                unitOfAmount: UnitType = Field(..., description='')
            """);

        testUtils.assertBundleContainsExpectedString(model,
            """
            class Quantity(MeasureBase):
                multiplier: Optional[Decimal] = Field(None, description='')
                multiplierUnit: Optional[UnitType] = Field(None, description='')
            """);
    }

    @Test
    public void testMultilineAttributeDefinition() {
        testUtils.assertBundleContainsExpectedString(
            """
            type MultilineDefinition:
                field string (1..1) <"This is a multiline
                    definition for a field.">
            """,
            """
            class MultilineDefinition(BaseDataClass):
                field: str = Field(..., description='This is a multiline definition for a field.')""");
    }
}
