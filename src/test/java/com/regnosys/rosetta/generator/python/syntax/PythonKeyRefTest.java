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
     * KeyEntity and RefEntity are acyclic — both standalone. RefEntity.py
     * holds the class directly with inline annotation; no Phase 2/3 needed.
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

        // RefEntity is standalone — class is in its own file, not the bundle
        String refEntityPython = gf.get("src/com/rosetta/test/model/RefEntity.py").toString();

        // Class declaration uses short name (no _FQRTN for standalone)
        testUtils.assertGeneratedContainsExpectedString(refEntityPython,
            "class RefEntity(BaseDataClass):");

        // Annotation is inline in the class body (no Phase 2 __annotations__ update)
        testUtils.assertGeneratedContainsExpectedString(refEntityPython,
            "ke: Annotated[KeyEntity | BaseReference, KeyEntity.serializer(), KeyEntity.validator(('@key', '@key:external', '@ref', '@ref:external'))] = Field(..., description='')");
    }
}
