/*
 * Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
 * SPDX-License-Identifier: Apache-2.0
 */
package com.regnosys.rosetta.generator.python.object;

import jakarta.inject.Inject;
import com.regnosys.rosetta.tests.RosettaInjectorProvider;
import com.regnosys.rosetta.generator.python.PythonGeneratorTestUtils;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.assertFalse;
import java.util.Map;

@ExtendWith(InjectionExtension.class)
@InjectWith(RosettaInjectorProvider.class)
@SuppressWarnings("LineLength")
public class PythonStandaloneStructureTest {

    /**
     * Test utils for generating Python.
     */
    @Inject
    private PythonGeneratorTestUtils testUtils;

    /**
     * Generated Python.
     */
    private Map<String, CharSequence> python = null;

    /**
     * Returns the generated Python.
     */
    private Map<String, CharSequence> getPython() {
        if (python == null) {
            python = testUtils.generatePythonFromString(
                """
                namespace test.generated_syntax.basic : <"generate Python unit tests from Rosetta.">

                typeAlias ParameterisedNumberType:
                    number(digits: 18, fractionalDigits: 2)

                typeAlias ParameterisedStringType:
                    string(minLength: 1, maxLength: 20, pattern: "[a-zA-Z]")

                type BasicSingle:
                    booleanType boolean (1..1)
                    numberType number (1..1)
                    parameterisedNumberType ParameterisedNumberType (1..1)
                    parameterisedStringType ParameterisedStringType (1..1)
                    stringType string (1..1)
                    timeType time (1..1)

                type BasicList:
                    booleanTypes boolean (1..*)
                    numberTypes number (1..*)
                    parameterisedNumberTypes ParameterisedNumberType (1..*)
                    parameterisedStringTypes ParameterisedStringType (1..*)
                    stringTypes string (1..*)
                    timeTypes time (1..*)

                type Root:
                    [rootType]
                    basicSingle BasicSingle (0..1)
                    basicList BasicList (0..1)
                """);
        }
        return python;
    }

    /**
     * BasicSingle is acyclic — standalone. The file contains the class directly.
     */
    @Test
    public void testBasicSingleStandalone() {
        Map<String, CharSequence> gf = getPython();
        testUtils.assertGeneratedContainsExpectedString(
            gf.get("src/test/generated_syntax/basic/BasicSingle.py").toString(),
            "class BasicSingle(BaseDataClass):");
    }

    /**
     * BasicList is acyclic — standalone. The file contains the class directly.
     */
    @Test
    public void testBasicListStandalone() {
        Map<String, CharSequence> gf = getPython();
        testUtils.assertGeneratedContainsExpectedString(
            gf.get("src/test/generated_syntax/basic/BasicList.py").toString(),
            "class BasicList(BaseDataClass):");
    }

    /**
     * Root is acyclic — standalone. The file contains the class directly.
     */
    @Test
    public void testRootStandalone() {
        Map<String, CharSequence> gf = getPython();
        testUtils.assertGeneratedContainsExpectedString(
            gf.get("src/test/generated_syntax/basic/Root.py").toString(),
            "class Root(BaseDataClass):");
    }

    /**
     * Test case for bundle existence (bundle is still generated, even when mostly empty).
     */
    @Test
    public void testBundleExists() {
        Map<String, CharSequence> gf = getPython();
        assertFalse(gf.containsKey("src/test/_bundle.py"), "No bundle should be generated for a standalone-only model");
    }

    /**
     * Test case for BasicSingle standalone class fields.
     */
    @Test
    public void testExpectedBasicSingle() {
        Map<String, CharSequence> gf = getPython();
        String generatedPython = gf.get("src/test/generated_syntax/basic/BasicSingle.py").toString();
        String expectedBasicSingle = """
            class BasicSingle(BaseDataClass):
                booleanType: bool = Field(..., description='')
                numberType: Decimal = Field(..., description='')
                parameterisedNumberType: Decimal = Field(..., description='', max_digits=18, decimal_places=2)
                parameterisedStringType: str = Field(..., description='', min_length=1, pattern=r'^[a-zA-Z]*$', max_length=20)
                stringType: str = Field(..., description='')
                timeType: datetime.time = Field(..., description='')
            """;
        testUtils.assertGeneratedContainsExpectedString(generatedPython, expectedBasicSingle);
    }

    /**
     * Test case for BasicList standalone class fields.
     */
    @Test
    public void testExpectedBasicList() {
        Map<String, CharSequence> gf = getPython();
        String generatedPython = gf.get("src/test/generated_syntax/basic/BasicList.py").toString();
        String expectedBasicList = """
            class BasicList(BaseDataClass):
                booleanTypes: list[bool | None] = Field(..., description='', min_length=1)
                numberTypes: list[Decimal | None] = Field(..., description='', min_length=1)
                parameterisedNumberTypes: list[Annotated[Decimal, Field(max_digits=18, decimal_places=2)] | None] = Field(..., description='', min_length=1)
                parameterisedStringTypes: list[Annotated[str, Field(min_length=1, pattern=r'^[a-zA-Z]*$', max_length=20)] | None] = Field(..., description='', min_length=1)
                stringTypes: list[str | None] = Field(..., description='', min_length=1)
                timeTypes: list[datetime.time | None] = Field(..., description='', min_length=1)
            """;
        testUtils.assertGeneratedContainsExpectedString(generatedPython, expectedBasicList);
    }

    /**
     * Root depends on BasicSingle and BasicList (both standalone). Attributes
     * are referenced directly with short names — no Phase 2/3 needed.
     */
    @Test
    public void testExpectedRoot() {
        Map<String, CharSequence> gf = getPython();
        String generatedPython = gf.get("src/test/generated_syntax/basic/Root.py").toString();
        String expectedRoot = """
            class Root(BaseDataClass):
                basicSingle: Optional[BasicSingle] = Field(None, description='')
                basicList: Optional[BasicList] = Field(None, description='')
            """;
        testUtils.assertGeneratedContainsExpectedString(generatedPython, expectedRoot);
    }
}
