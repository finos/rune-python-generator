package com.regnosys.rosetta.generator.python.generated_syntax;

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
public class PythonKeyRefTest {
    /**
     * Test utils for generating Python.
     */
    @Inject
    private PythonGeneratorTestUtils testUtils;

    /**
     * Test case for KeyRef.
     */
    @Test
    public void testKeyRef() {
        Map<String, CharSequence> gf = testUtils.generatePythonFromString(
                """
                        type KeyEntity:
                            [metadata key]
                            value int (1..1)

                        type RefEntity:
                            ke KeyEntity (1..1)
                                [metadata reference]
                        """);
        String generated = gf.get("src/com/_bundle.py").toString();
        String expectedClass = """
                class com_rosetta_test_model_RefEntity(BaseDataClass):
                    _FQRTN = 'com.rosetta.test.model.RefEntity'
                    ke: com_rosetta_test_model_KeyEntity | BaseReference = Field(..., description='')

                    _KEY_REF_CONSTRAINTS = {
                        'ke': {'@key', '@key:external', '@ref', '@ref:external'}
                    }
                """;
        String expectedPhase2 = "com_rosetta_test_model_RefEntity.__annotations__[\"ke\"] = Annotated[com_rosetta_test_model_KeyEntity | BaseReference, com_rosetta_test_model_KeyEntity.serializer(), com_rosetta_test_model_KeyEntity.validator(('@key', '@key:external', '@ref', '@ref:external'))]";
        testUtils.assertGeneratedContainsExpectedString(generated, expectedClass);
        testUtils.assertGeneratedContainsExpectedString(generated, expectedPhase2);
    }
}
