package com.regnosys.rosetta.generator.python.object;

import com.regnosys.rosetta.generator.python.PythonGeneratorTestUtils;
import com.regnosys.rosetta.tests.RosettaInjectorProvider;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import jakarta.inject.Inject;
import java.util.Map;
import org.junit.jupiter.api.Disabled;

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
     */
    @Test
    public void testEnumWithMetadataId() {
        // This test captures the "Unresolved Issue" where enums with [metadata id]
        // fail to generate the necessary metadata handling infrastructure.
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

        String bundle = gf.get("src/test/_bundle.py").toString();

        // Assert that the 'currency' field in CashTransfer is using a metadata
        // wrapper with the correct @key tags.
        testUtils.assertGeneratedContainsExpectedString(bundle,
                "currency: Annotated[test.metadata.CurrencyEnum.CurrencyEnum, test.metadata.CurrencyEnum.CurrencyEnum.serializer(), test.metadata.CurrencyEnum.CurrencyEnum.validator(('@key', '@key:external'))]");

        // Also verify the key-ref constraints
        testUtils.assertGeneratedContainsExpectedString(bundle,
                "'currency': {'@key', '@key:external'}");
    }

    /**
     * Test case for enum without metadata.
     */
    @Test
    @Disabled("Blocked by Enum Wrapper implementation - see Backlog")
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
                        """);

        String bundle = gf.get("src/test/_bundle.py").toString();

        // Enums should be consistently wrapped in Annotated to support metadata
        // during deserialization even if not explicitly required in Rosetta.
        testUtils.assertGeneratedContainsExpectedString(bundle,
                "currency: Annotated[test.metadata.CurrencyEnum.CurrencyEnum, test.metadata.CurrencyEnum.CurrencyEnum.serializer(), test.metadata.CurrencyEnum.CurrencyEnum.validator()]");
    }
}
