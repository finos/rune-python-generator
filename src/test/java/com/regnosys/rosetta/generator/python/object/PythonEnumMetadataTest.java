package com.regnosys.rosetta.generator.python.object;

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
@SuppressWarnings("LineLength")
public class PythonEnumMetadataTest {

    /**
     * Test utils for generating Python.
     */
    @Inject
    private PythonGeneratorTestUtils testUtils;

    /**
     * Test case for enum with metadata id.
     * CashTransfer is acyclic — standalone. The class is in CashTransfer.py.
     * Enums use module-style imports so the reference remains fully qualified.
     */
    @Test
    public void testEnumWithMetadataId() {
        Map<String, CharSequence> gf = testUtils.generatePythonFromString(
                """
                namespace test.metadata : <"test">

                enum CurrencyEnum:
                    USD
                    EUR
                    GBP

                type CashTransfer:
                    amount number (1..1)
                    currency CurrencyEnum (1..1)
                        [metadata id]
                """);

        // CashTransfer is standalone — class is in its own file
        String generatedPython = gf.get("src/test/metadata/CashTransfer.py").toString();

        // Assert that the 'currency' field in CashTransfer is using a metadata
        // wrapper with the correct @key tags. Enums use module-style import so
        // the reference stays fully qualified as namespace.EnumName.EnumName.
        testUtils.assertGeneratedContainsExpectedString(generatedPython,
                "currency: Annotated[test.metadata.CurrencyEnum.CurrencyEnum, test.metadata.CurrencyEnum.CurrencyEnum.serializer(), test.metadata.CurrencyEnum.CurrencyEnum.validator(('@key', '@key:external'))]");

        // Also verify the key-ref constraints
        testUtils.assertGeneratedContainsExpectedString(generatedPython,
                "'currency': {'@key', '@key:external'}");
    }

    /**
     * Test case for enum without metadata.
     */
    @Test
    public void testEnumWithoutMetadata() {
        Map<String, CharSequence> gf = testUtils.generatePythonFromString(
                """
                        namespace test.metadata : <"test">

                        enum CurrencyEnum:
                            USD
                            EUR
                            GBP

                        type CashTransfer:
                            amount number (1..1)
                            currency CurrencyEnum (1..1)
                                [metadata id]
                        """);

        // CashTransfer is standalone — class is in its own file
        String generatedPython = gf.get("src/test/metadata/CashTransfer.py").toString();

        // Enums should be consistently wrapped in Annotated to support metadata
        // during deserialization even if not explicitly required in Rosetta.
        testUtils.assertGeneratedContainsExpectedString(generatedPython,
                "currency: Annotated[test.metadata.CurrencyEnum.CurrencyEnum, test.metadata.CurrencyEnum.CurrencyEnum.serializer(), test.metadata.CurrencyEnum.CurrencyEnum.validator(('@key', '@key:external'))] = Field(..., description='')");
    }
}
