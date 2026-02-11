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
public class PythonMetaDataGeneratorTest {

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
         * Initialize generated Python.
         */

        private void initPython() {
                if (python == null) {
                        python = testUtils.generatePythonFromString(
                                        """
                                                        namespace test.generated_syntax.metadata : <"generate Python unit tests from Rosetta.">

                                                        type A:
                                                            [metadata key]
                                                            fieldA string (1..1)

                                                        type NodeRef:
                                                            typeA A (0..1)
                                                            aReference A (0..1)
                                                                [metadata reference]

                                                        type AttributeRef:
                                                            dateField date (0..1)
                                                                [metadata id]
                                                            dateReference date (0..1)
                                                                [metadata reference]

                                                        type Root:
                                                            [rootType]
                                                            nodeRef NodeRef (0..1)
                                                            attributeRef AttributeRef (0..1)

                                                        type SchemeTest:
                                                            [metadata scheme]
                                                            a string (1..1)
                                                        """);
                }
        }

        /**
         * Get generated Python.
         */
        private Map<String, CharSequence> getPython() {
                if (python == null) {
                        python = testUtils.generatePythonFromString(
                                        """
                                                        namespace test.generated_syntax.metadata : <"generate Python unit tests from Rosetta.">

                                                        type A:
                                                            [metadata key]
                                                            fieldA string (1..1)

                                                        type NodeRef:
                                                            typeA A (0..1)
                                                            aReference A (0..1)
                                                                [metadata reference]

                                                        type AttributeRef:
                                                            dateField date (0..1)
                                                                [metadata id]
                                                            dateReference date (0..1)
                                                                [metadata reference]

                                                        type Root:
                                                            [rootType]
                                                            nodeRef NodeRef (0..1)
                                                            attributeRef AttributeRef (0..1)

                                                        type SchemeTest:
                                                            [metadata scheme]
                                                            a string (1..1)
                                                        """);
                }
                return python;
        }

        /**
         * Test case for A proxy.
         */
        @Test
        public void testAProxy() {
                initPython();
                testUtils.assertGeneratedContainsExpectedString(
                                python.get("src/test/generated_syntax/metadata/A.py").toString(),
                                """
                                                # pylint: disable=unused-import
                                                from test._bundle import test_generated_syntax_metadata_A as A

                                                # EOF
                                                """);
        }

        /**
         * Test case for NodeRef proxy.
         */
        @Test
        public void testNodeRefProxy() {
                initPython();
                testUtils.assertGeneratedContainsExpectedString(
                                python.get("src/test/generated_syntax/metadata/NodeRef.py").toString(),
                                """
                                                # pylint: disable=unused-import
                                                from test._bundle import test_generated_syntax_metadata_NodeRef as NodeRef

                                                # EOF
                                                """);
        }

        /**
         * Test case for AttributeRef proxy.
         */
        @Test
        public void testAttributeRefProxy() {
                initPython();
                testUtils.assertGeneratedContainsExpectedString(
                                python.get("src/test/generated_syntax/metadata/AttributeRef.py").toString(),
                                """
                                                # pylint: disable=unused-import
                                                from test._bundle import test_generated_syntax_metadata_AttributeRef as AttributeRef

                                                # EOF
                                                """);
        }

        /**
         * Test case for Root proxy.
         */
        @Test
        public void testRootProxy() {
                initPython();
                testUtils.assertGeneratedContainsExpectedString(
                                python.get("src/test/generated_syntax/metadata/Root.py").toString(),
                                """
                                                # pylint: disable=unused-import
                                                from test._bundle import test_generated_syntax_metadata_Root as Root

                                                # EOF
                                                """);
        }

        /**
         * Test case for bundle existence.
         */
        @Test
        public void testBundleExists() {
                initPython();
                assertTrue(python.containsKey("src/test/_bundle.py"),
                                "The bundle should be in the generated Python");
        }

        /**
         * Test case for bundle A.
         */
        @Test
        public void testExpectedBundleA() {
                initPython();
                String bundle = python.get("src/test/_bundle.py").toString();

                // Native types are not delayed
                testUtils.assertGeneratedContainsExpectedString(bundle,
                                "class test_generated_syntax_metadata_A(BaseDataClass):");
                testUtils.assertGeneratedContainsExpectedString(bundle,
                                "_ALLOWED_METADATA = {'@key', '@key:external'}");
                testUtils.assertGeneratedContainsExpectedString(bundle, "fieldA: str = Field(..., description='')");
        }

        /**
         * Test case for bundle AttributeRef.
         */
        @Test
        public void testExpectedBundleAttributeRef() {
                initPython();
                String bundle = python.get("src/test/_bundle.py").toString();

                // Date is a basic type, so DateWithMeta is currently not delayed
                testUtils.assertGeneratedContainsExpectedString(bundle,
                                "class test_generated_syntax_metadata_AttributeRef(BaseDataClass):");
                testUtils.assertGeneratedContainsExpectedString(bundle,
                                "dateField: Optional[Annotated[DateWithMeta, DateWithMeta.serializer(), DateWithMeta.validator(('@key', '@key:external'))]] = Field(None, description='')");
                testUtils.assertGeneratedContainsExpectedString(bundle,
                                "dateReference: Optional[Annotated[DateWithMeta | BaseReference, DateWithMeta.serializer(), DateWithMeta.validator(('@ref', '@ref:external'))]] = Field(None, description='')");
        }

        /**
         * Test case for bundle NodeRef.
         */
        @Test
        public void testExpectedBundleNodeRef() {
                initPython();
                String bundle = python.get("src/test/_bundle.py").toString();

                // Phase 1: Clean Body
                testUtils.assertGeneratedContainsExpectedString(bundle,
                                "class test_generated_syntax_metadata_NodeRef(BaseDataClass):");
                testUtils.assertGeneratedContainsExpectedString(bundle,
                                "typeA: Optional[test_generated_syntax_metadata_A] = Field(None, description='')");
                testUtils.assertGeneratedContainsExpectedString(bundle,
                                "aReference: Optional[test_generated_syntax_metadata_A | BaseReference] = Field(None, description='')");

                // Phase 2: Delayed Update
                testUtils.assertGeneratedContainsExpectedString(bundle,
                                "test_generated_syntax_metadata_NodeRef.__annotations__[\"typeA\"] = Optional[Annotated[test_generated_syntax_metadata_A, test_generated_syntax_metadata_A.serializer(), test_generated_syntax_metadata_A.validator()]]");
                testUtils.assertGeneratedContainsExpectedString(bundle,
                                "test_generated_syntax_metadata_NodeRef.__annotations__[\"aReference\"] = Optional[Annotated[test_generated_syntax_metadata_A | BaseReference, test_generated_syntax_metadata_A.serializer(), test_generated_syntax_metadata_A.validator(('@key', '@key:external', '@ref', '@ref:external'))]]");

                // Phase 3: Rebuild
                testUtils.assertGeneratedContainsExpectedString(bundle,
                                "test_generated_syntax_metadata_NodeRef.model_rebuild()");
        }

        /**
         * Test case for bundle Root.
         */
        @Test
        public void testExpectedBundleRoot() {
                initPython();
                String bundle = python.get("src/test/_bundle.py").toString();

                // Phase 1: Clean Body
                testUtils.assertGeneratedContainsExpectedString(bundle,
                                "class test_generated_syntax_metadata_Root(BaseDataClass):");
                testUtils.assertGeneratedContainsExpectedString(bundle,
                                "nodeRef: Optional[test_generated_syntax_metadata_NodeRef] = Field(None, description='')");
                testUtils.assertGeneratedContainsExpectedString(bundle,
                                "attributeRef: Optional[test_generated_syntax_metadata_AttributeRef] = Field(None, description='')");

                // Phase 2: Delayed Update
                testUtils.assertGeneratedContainsExpectedString(bundle,
                                "test_generated_syntax_metadata_Root.__annotations__[\"nodeRef\"] = Optional[Annotated[test_generated_syntax_metadata_NodeRef, test_generated_syntax_metadata_NodeRef.serializer(), test_generated_syntax_metadata_NodeRef.validator()]]");
                testUtils.assertGeneratedContainsExpectedString(bundle,
                                "test_generated_syntax_metadata_Root.__annotations__[\"attributeRef\"] = Optional[Annotated[test_generated_syntax_metadata_AttributeRef, test_generated_syntax_metadata_AttributeRef.serializer(), test_generated_syntax_metadata_AttributeRef.validator()]]");

                // Phase 3: Rebuild
                testUtils.assertGeneratedContainsExpectedString(bundle,
                                "test_generated_syntax_metadata_Root.model_rebuild()");
        }

        /**
         * Test case for bundle SchemeTest.
         */
        @Test
        public void testExpectedBundleScheme() {
                initPython();
                String bundle = python.get("src/test/_bundle.py").toString();

                testUtils.assertGeneratedContainsExpectedString(bundle,
                                "class test_generated_syntax_metadata_SchemeTest(BaseDataClass):");
                testUtils.assertGeneratedContainsExpectedString(bundle, "_ALLOWED_METADATA = {'@scheme'}");
                testUtils.assertGeneratedContainsExpectedString(bundle, "a: str = Field(..., description='')");
        }
}
