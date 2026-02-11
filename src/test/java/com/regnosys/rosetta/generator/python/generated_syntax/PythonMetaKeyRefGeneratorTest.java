package com.regnosys.rosetta.generator.python.generated_syntax;

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
public class PythonMetaKeyRefGeneratorTest {

        /**
         * Test utils for generating Python.
         */
        @Inject
        private PythonGeneratorTestUtils testUtils;

        /**
         * Test case for generating meta key ref.
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

                // check proxies
                String proxyKeyRef = python.get("src/test/generated_syntax/meta_key_ref/KeyRef.py").toString();
                assertNotNull(proxyKeyRef, "KeyRef.py was not found");
                testUtils.assertGeneratedContainsExpectedString(proxyKeyRef,
                                "from test._bundle import test_generated_syntax_meta_key_ref_KeyRef as KeyRef");

                String proxyScopedKeyRef = python.get("src/test/generated_syntax/meta_key_ref/ScopedKeyRef.py")
                                .toString();
                assertNotNull(proxyScopedKeyRef, "ScopedKeyRef.py was not found");
                testUtils.assertGeneratedContainsExpectedString(proxyScopedKeyRef,
                                "from test._bundle import test_generated_syntax_meta_key_ref_ScopedKeyRef as ScopedKeyRef");

                String bundle = python.get("src/test/_bundle.py").toString();
                assertNotNull(bundle, "src/test/_bundle.py was not found");

                // KeyRef checks
                testUtils.assertGeneratedContainsExpectedString(bundle,
                                "class test_generated_syntax_meta_key_ref_KeyRef(BaseDataClass):");
                testUtils.assertGeneratedContainsExpectedString(bundle,
                                "'fieldA': {'@ref', '@ref:external', '@key', '@key:external'}");
                // Check if delayed or inline (StrWithMeta is currently inline as it's a basic
                // type)
                testUtils.assertGeneratedContainsExpectedString(bundle,
                                "fieldA: Annotated[StrWithMeta | BaseReference, StrWithMeta.serializer(), StrWithMeta.validator(('@ref', '@ref:external', '@key', '@key:external'))] = Field(..., description='')");

                // ScopedKeyRef checks
                testUtils.assertGeneratedContainsExpectedString(bundle,
                                "class test_generated_syntax_meta_key_ref_ScopedKeyRef(BaseDataClass):");
                testUtils.assertGeneratedContainsExpectedString(bundle, "'fieldA': {'@key:scoped', '@ref:scoped'}");
                testUtils.assertGeneratedContainsExpectedString(bundle,
                                "fieldA: Annotated[StrWithMeta | BaseReference, StrWithMeta.serializer(), StrWithMeta.validator(('@key:scoped', '@ref:scoped'))] = Field(..., description='')");
        }
}
