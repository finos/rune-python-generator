package com.regnosys.rosetta.generator.python.generated_syntax;

import jakarta.inject.Inject;
import com.regnosys.rosetta.tests.RosettaInjectorProvider;
import com.regnosys.rosetta.generator.python.PythonGeneratorTestUtils;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Map;

@ExtendWith(InjectionExtension.class)
@InjectWith(RosettaInjectorProvider.class)
@SuppressWarnings("LineLength")
public class PythonBasicGeneratorTest {

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
     * Test case for BasicSingle proxy.
     */
    @Test
    public void testBasicSingleProxy() {
        Map<String, CharSequence> generatedPython = getPython();
        testUtils.assertGeneratedContainsExpectedString(
                generatedPython.get("src/test/generated_syntax/basic/BasicSingle.py").toString(),
                """
                        # pylint: disable=unused-import
                        from test._bundle import test_generated_syntax_basic_BasicSingle as BasicSingle

                        # EOF
                        """);
    }

    /**
     * Test case for BasicList proxy.
     */
    @Test
    public void testBasicListProxy() {
        Map<String, CharSequence> generatedPython = getPython();
        testUtils.assertGeneratedContainsExpectedString(
                generatedPython.get("src/test/generated_syntax/basic/BasicList.py").toString(),
                """
                        # pylint: disable=unused-import
                        from test._bundle import test_generated_syntax_basic_BasicList as BasicList

                        # EOF
                        """);
    }

    /**
     * Test case for Root proxy.
     */
    @Test
    public void testRootProxy() {
        Map<String, CharSequence> generatedPython = getPython();
        testUtils.assertGeneratedContainsExpectedString(
                generatedPython.get("src/test/generated_syntax/basic/Root.py").toString(),
                """
                        # pylint: disable=unused-import
                        from test._bundle import test_generated_syntax_basic_Root as Root

                        # EOF
                        """);
    }

    /**
     * Test case for bundle.
     */
    @Test
    public void testBundleExists() {
        Map<String, CharSequence> generatedPython = getPython();
        assertTrue(generatedPython.containsKey("src/test/_bundle.py"), "The bundle should be in the generated Python");
    }

    /**
     * Test case for bundle.
     */
    @Test
    public void testExpectedBundleBasic() {
        Map<String, CharSequence> generatedPython = getPython();
        String bundle = generatedPython.get("src/test/_bundle.py").toString();
        String expectedBasicSingle = """
                class test_generated_syntax_basic_BasicSingle(BaseDataClass):
                    _FQRTN = 'test.generated_syntax.basic.BasicSingle'
                    booleanType: bool = Field(..., description='')
                    numberType: Decimal = Field(..., description='')
                    parameterisedNumberType: Decimal = Field(..., description='', max_digits=18, decimal_places=2)
                    parameterisedStringType: str = Field(..., description='', min_length=1, pattern=r'^[a-zA-Z]*$', max_length=20)
                    stringType: str = Field(..., description='')
                    timeType: datetime.time = Field(..., description='')
                """;
        testUtils.assertGeneratedContainsExpectedString(bundle, expectedBasicSingle);
    }

    /**
     * Test case for bundle.
     */
    @Test
    public void testExpectedBundleList() {
        Map<String, CharSequence> generatedPython = getPython();
        String bundle = generatedPython.get("src/test/_bundle.py").toString();
        String expectedBasicList = """
                class test_generated_syntax_basic_BasicList(BaseDataClass):
                    _FQRTN = 'test.generated_syntax.basic.BasicList'
                    booleanTypes: list[bool] = Field(..., description='', min_length=1)
                    numberTypes: list[Decimal] = Field(..., description='', min_length=1)
                    parameterisedNumberTypes: list[Annotated[Decimal, Field(max_digits=18, decimal_places=2)]] = Field(..., description='', min_length=1)
                    parameterisedStringTypes: list[Annotated[str, Field(min_length=1, pattern=r'^[a-zA-Z]*$', max_length=20)]] = Field(..., description='', min_length=1)
                    stringTypes: list[str] = Field(..., description='', min_length=1)
                    timeTypes: list[datetime.time] = Field(..., description='', min_length=1)
                """;
        testUtils.assertGeneratedContainsExpectedString(bundle, expectedBasicList);
    }

    /**
     * Test case for bundle.
     */
    @Test
    public void testExpectedBundleRoot() {
        Map<String, CharSequence> generatedPython = getPython();
        String bundle = generatedPython.get("src/test/_bundle.py").toString();

        // Phase 1, 2, and 3 for Root should be tested as a complete logical sequence
        String expectedRootFull = """
                class test_generated_syntax_basic_Root(BaseDataClass):
                    _FQRTN = 'test.generated_syntax.basic.Root'
                    basicSingle: Optional[test_generated_syntax_basic_BasicSingle] = Field(None, description='')
                    basicList: Optional[test_generated_syntax_basic_BasicList] = Field(None, description='')


                # Phase 2: Delayed Annotation Updates
                test_generated_syntax_basic_Root.__annotations__["basicSingle"] = Annotated[Optional[test_generated_syntax_basic_BasicSingle], test_generated_syntax_basic_BasicSingle.serializer(), test_generated_syntax_basic_BasicSingle.validator()]
                test_generated_syntax_basic_Root.__annotations__["basicList"] = Annotated[Optional[test_generated_syntax_basic_BasicList], test_generated_syntax_basic_BasicList.serializer(), test_generated_syntax_basic_BasicList.validator()]


                # Phase 3: Rebuild
                test_generated_syntax_basic_Root.model_rebuild()
                """;
        testUtils.assertGeneratedContainsExpectedString(bundle, expectedRootFull);
    }
}
