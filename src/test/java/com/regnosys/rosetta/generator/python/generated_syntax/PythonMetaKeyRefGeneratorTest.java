package com.regnosys.rosetta.generator.python.generated_syntax;

import jakarta.inject.Inject;
import com.regnosys.rosetta.tests.RosettaInjectorProvider;
import com.regnosys.rosetta.generator.python.PythonGeneratorTestUtils;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Map;

@ExtendWith(InjectionExtension.class)
@InjectWith(RosettaInjectorProvider.class)
public class PythonMetaKeyRefGeneratorTest {

    @Inject
    private PythonGeneratorTestUtils testUtils;

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

        // check proxies
        String proxyKeyRef = python.get("src/test/generated_syntax/meta_key_ref/KeyRef.py").toString();
        assertNotNull(proxyKeyRef, "KeyRef.py was not found");
        assertTrue(proxyKeyRef.contains("from test._bundle import test_generated_syntax_meta_key_ref_KeyRef as KeyRef"),
                "KeyRef proxy import");

        String proxyScopedKeyRef = python.get("src/test/generated_syntax/meta_key_ref/ScopedKeyRef.py").toString();
        assertNotNull(proxyScopedKeyRef, "ScopedKeyRef.py was not found");
        assertTrue(
                proxyScopedKeyRef.contains(
                        "from test._bundle import test_generated_syntax_meta_key_ref_ScopedKeyRef as ScopedKeyRef"),
                "ScopedKeyRef proxy import");

        String bundle = python.get("src/test/_bundle.py").toString();
        assertNotNull(bundle, "src/test/_bundle.py was not found");

        // KeyRef checks
        assertTrue(bundle.contains("class test_generated_syntax_meta_key_ref_KeyRef(BaseDataClass):"),
                "KeyRef class definition");
        assertTrue(bundle.contains("'fieldA': {'@ref', '@ref:external', '@key', '@key:external'}"),
                "KeyRef constraints");
        // Check if delayed or inline (StrWithMeta is currently inline as it's a basic
        // type)
        assertTrue(bundle.contains(
                "fieldA: Annotated[StrWithMeta, StrWithMeta.serializer(), StrWithMeta.validator(('@ref', '@ref:external', '@key', '@key:external'))] = Field(..., description='')"),
                "KeyRef fieldA definition");

        // ScopedKeyRef checks
        assertTrue(bundle.contains("class test_generated_syntax_meta_key_ref_ScopedKeyRef(BaseDataClass):"),
                "ScopedKeyRef class definition");
        assertTrue(bundle.contains("'fieldA': {'@key:scoped', '@ref:scoped'}"), "ScopedKeyRef constraints");
        assertTrue(bundle.contains(
                "fieldA: Annotated[StrWithMeta, StrWithMeta.serializer(), StrWithMeta.validator(('@key:scoped', '@ref:scoped'))] = Field(..., description='')"),
                "ScopedKeyRef fieldA definition");

    }
}
