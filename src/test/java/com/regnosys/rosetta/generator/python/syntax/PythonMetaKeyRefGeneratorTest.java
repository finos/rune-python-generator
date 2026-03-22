package com.regnosys.rosetta.generator.python.syntax;

import jakarta.inject.Inject;
import com.regnosys.rosetta.tests.RosettaInjectorProvider;
import com.regnosys.rosetta.generator.python.PythonGeneratorTestUtils;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.util.Map;

@ExtendWith(InjectionExtension.class)
@InjectWith(RosettaInjectorProvider.class)
@SuppressWarnings("LineLength")
public class PythonMetaKeyRefGeneratorTest {

    /**
     * Test utils for generating Python.
     */
    @Inject
    private PythonGeneratorTestUtils testUtils;

    /**
     * Test case for generating meta key ref.
     * KeyRef and ScopedKeyRef are acyclic — both standalone. Classes are
     * written directly to their FQ-path files; metadata annotations are
     * inline in the class body (no Phase 2/3 needed).
     */
    @Test
    public void testGeneration() {
        Map<String, CharSequence> python = testUtils.generatePythonFromString(
            """
            namespace test.generated_syntax.meta_key_ref : <"generate Python unit tests from Rosetta.">

            type KeyRef:
                fieldA string (1..1)
                [metadata id]
                [metadata reference]

            type ScopedKeyRef:
                fieldA string (1..1)
                [metadata location]
                [metadata address]
            """);

        // Standalone files contain classes directly (not proxy stubs)
        String keyRefPython = python.get("src/test/generated_syntax/meta_key_ref/KeyRef.py").toString();
        assertNotNull(keyRefPython, "KeyRef.py was not found");
        testUtils.assertGeneratedContainsExpectedString(keyRefPython,
            "class KeyRef(BaseDataClass):");

        String scopedKeyRefPython = python.get("src/test/generated_syntax/meta_key_ref/ScopedKeyRef.py").toString();
        assertNotNull(scopedKeyRefPython, "ScopedKeyRef.py was not found");
        testUtils.assertGeneratedContainsExpectedString(scopedKeyRefPython,
            "class ScopedKeyRef(BaseDataClass):");

        // KeyRef checks — annotations are inline in the standalone file
        testUtils.assertGeneratedContainsExpectedString(keyRefPython,
            "'fieldA': {'@ref', '@ref:external', '@key', '@key:external'}");
        // StrWithMeta is a basic type — annotation is always inline
        testUtils.assertGeneratedContainsExpectedString(keyRefPython,
            "fieldA: Annotated[StrWithMeta | BaseReference, StrWithMeta.serializer(), StrWithMeta.validator(('@ref', '@ref:external', '@key', '@key:external'))] = Field(..., description='')");

        // ScopedKeyRef checks
        testUtils.assertGeneratedContainsExpectedString(scopedKeyRefPython,
            "'fieldA': {'@key:scoped', '@ref:scoped'}");
        testUtils.assertGeneratedContainsExpectedString(scopedKeyRefPython,
            "fieldA: Annotated[StrWithMeta | BaseReference, StrWithMeta.serializer(), StrWithMeta.validator(('@key:scoped', '@ref:scoped'))] = Field(..., description='')");
    }
}
