package com.regnosys.rosetta.generator.python.syntax;

import jakarta.inject.Inject;
import com.regnosys.rosetta.tests.RosettaInjectorProvider;
import com.regnosys.rosetta.generator.python.PythonGeneratorTestUtils;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import java.util.Map;

@ExtendWith(InjectionExtension.class)
@InjectWith(RosettaInjectorProvider.class)
@SuppressWarnings("LineLength")
public class PythonKeyRefTest {

    /**
     * Test utils for generating Python.
     */
    @Inject
    private PythonGeneratorTestUtils testUtils;

    /**
     * Test case for generating key ref.
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
        String generatedPython = gf.get("src/com/_bundle.py").toString();
        String expectedClass = """
            class com_rosetta_test_model_RefEntity(BaseDataClass):
                _FQRTN = 'com.rosetta.test.model.RefEntity'
                ke: com_rosetta_test_model_KeyEntity | BaseReference = Field(..., description='')
            """;
        testUtils.assertGeneratedContainsExpectedString(generatedPython, expectedClass);

        String expectedAnnotation = "com_rosetta_test_model_RefEntity.__annotations__[\"ke\"] = Annotated[com_rosetta_test_model_KeyEntity | BaseReference, com_rosetta_test_model_KeyEntity.serializer(), com_rosetta_test_model_KeyEntity.validator(('@key', '@key:external', '@ref', '@ref:external'))]";
        testUtils.assertGeneratedContainsExpectedString(generatedPython, expectedAnnotation);
    }
}
