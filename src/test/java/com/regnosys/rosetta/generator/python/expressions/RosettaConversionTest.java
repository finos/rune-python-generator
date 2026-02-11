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
public class RosettaConversionTest {

    /**
     * Test utils for generating Python.
     */
    @Inject
    private PythonGeneratorTestUtils testUtils;

    /**
     * Test case for basic conversions.
     */
    @Test
    public void testBasicConversions() {
        testUtils.assertBundleContainsExpectedString("""
                type TestConv:
                    val int (1..1)
                    s string (1..1)
                    condition ConvCheck:
                        val to-string = "1" and
                        s to-int = 1
                """,
                """
                        class com_rosetta_test_model_TestConv(BaseDataClass):
                            _FQRTN = 'com.rosetta.test.model.TestConv'
                            val: int = Field(..., description='')
                            s: str = Field(..., description='')

                            @rune_condition
                            def condition_0_ConvCheck(self):
                                item = self
                                return (rune_all_elements(rune_str(rune_resolve_attr(self, "val")), "=", "1") and rune_all_elements(int(rune_resolve_attr(self, "s")), "=", 1))""");
    }

    /**
     * Test case for date conversions.
     */
    @Test
    public void testDateConversions() {
        testUtils.assertBundleContainsExpectedString("""
                type TestDateConv:
                    s string (1..1)
                    condition DateConvCheck:
                        s to-date = "2023-11-20" to-date and
                        s to-date-time = "2023-11-20 12:00:00" to-date-time and
                        s to-time = "12:00:00" to-time
                """,
                """
                        class com_rosetta_test_model_TestDateConv(BaseDataClass):
                            _FQRTN = 'com.rosetta.test.model.TestDateConv'
                            s: str = Field(..., description='')

                            @rune_condition
                            def condition_0_DateConvCheck(self):
                                item = self
                                return ((rune_all_elements(datetime.datetime.strptime(rune_resolve_attr(self, "s"), "%Y-%m-%d").date(), "=", datetime.datetime.strptime("2023-11-20", "%Y-%m-%d").date()) and rune_all_elements(datetime.datetime.strptime(rune_resolve_attr(self, "s"), "%Y-%m-%d %H:%M:%S"), "=", datetime.datetime.strptime("2023-11-20 12:00:00", "%Y-%m-%d %H:%M:%S"))) and rune_all_elements(datetime.datetime.strptime(rune_resolve_attr(self, "s"), "%H:%M:%S").time(), "=", datetime.datetime.strptime("12:00:00", "%H:%M:%S").time()))""");
    }

    /**
     * Test case for enum conversion.
     */
    @Test
    public void testEnumConversion() {
        testUtils.assertBundleContainsExpectedString("""
                enum MyEnum:
                    Value1
                type TestEnumConv:
                    s string (1..1)
                    condition EnumConvCheck:
                        s to-enum MyEnum = MyEnum -> Value1
                """,
                """
                        class com_rosetta_test_model_TestEnumConv(BaseDataClass):
                            _FQRTN = 'com.rosetta.test.model.TestEnumConv'
                            s: str = Field(..., description='')

                            @rune_condition
                            def condition_0_EnumConvCheck(self):
                                item = self
                                return rune_all_elements(MyEnum(rune_resolve_attr(self, "s")), "=", com.rosetta.test.model.MyEnum.MyEnum.VALUE_1)""");
    }
}
