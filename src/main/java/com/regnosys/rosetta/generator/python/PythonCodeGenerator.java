package com.regnosys.rosetta.generator.python;

import static com.regnosys.rosetta.generator.python.util.PythonCodeGeneratorConstants.INIT;
import static com.regnosys.rosetta.generator.python.util.PythonCodeGeneratorConstants.PYPROJECT_TOML;
import static com.regnosys.rosetta.generator.python.util.PythonCodeGeneratorConstants.PYTHON;
import static com.regnosys.rosetta.generator.python.util.PythonCodeGeneratorConstants.SRC;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.TopologicalOrderIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.regnosys.rosetta.generator.external.AbstractExternalGenerator;
import com.regnosys.rosetta.generator.python.enums.PythonEnumGenerator;
import com.regnosys.rosetta.generator.python.functions.PythonFunctionGenerator;
import com.regnosys.rosetta.generator.python.object.PythonModelObjectGenerator;
import com.regnosys.rosetta.generator.python.util.PythonCodeGeneratorUtil;
import com.regnosys.rosetta.generator.python.util.PythonCodeWriter;
import com.regnosys.rosetta.rosetta.RosettaEnumeration;
import com.regnosys.rosetta.rosetta.RosettaModel;
import com.regnosys.rosetta.rosetta.simple.Data;
import com.regnosys.rosetta.rosetta.simple.Function;

// todo: function support
// todo: review and consolidate unit tests
// todo: review migrating choice alias processor to PythonModelObjectGenerator

import jakarta.inject.Inject;

/**
 * PythonCodeGenerator is an external generator for the Rosetta DSL that
 * produces Python code
 * from Rosetta model definitions. It supports the generation of Python classes,
 * enums, and functions
 * based on the structure and semantics of the input Rosetta models.
 * <p>
 * This generator is designed to be used as part of the Rosetta code generation
 * pipeline and is
 * typically invoked by the Rosetta build tools or CLI. It processes Rosetta
 * models and outputs
 * Python source files, including project metadata such as
 * <code>pyproject.toml</code>.
 * </p>
 *
 * <h2>Features</h2>
 * <ul>
 * <li>Generates Python classes from Rosetta Data types</li>
 * <li>Generates Python enums from Rosetta enumerations</li>
 * <li>Generates Python functions from Rosetta function definitions</li>
 * <li>Handles Rosetta model name spaces and organizes output into appropriate
 * Python packages</li>
 * <li>Produces project files for Python packaging (e.g.,
 * <code>pyproject.toml</code>)</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <p>
 * Typically, this class is not used directly, but is invoked by the Rosetta
 * code generation
 * infrastructure. It can be integrated into build pipelines or called from a
 * CLI tool.
 * </p>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * This class is not thread-safe and should be used in a single-threaded
 * context.
 * </p>
 *
 * <h2>Extensibility</h2>
 * <p>
 * The generator is designed to be extensible. Additional features or
 * customizations can be
 * implemented by extending this class or its collaborators.
 * </p>
 *
 * @author Plamen Neykov
 * @author Daniel Schwartz
 * @see com.regnosys.rosetta.generator.external.AbstractExternalGenerator
 * @see com.regnosys.rosetta.generator.python.PythonCodeGeneratorCLI
 */

public final class PythonCodeGenerator extends AbstractExternalGenerator {

    /**
     * The Python model object generator.
     */
    @Inject
    private PythonModelObjectGenerator pojoGenerator;
    /**
     * The Python function generator.
     */
    @Inject
    private PythonFunctionGenerator funcGenerator;
    /**
     * The Python enum generator.
     */
    @Inject
    private PythonEnumGenerator enumGenerator;

    /**
     * The logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PythonCodeGenerator.class);

    /**
     * The contexts.
     */
    private Map<String, PythonCodeGeneratorContext> contexts = null;

    /**
     * The PythonCodeGenerator constructor.
     */
    public PythonCodeGenerator() {
        super(PYTHON);
        contexts = new HashMap<>();
    }

    @Override
    public Map<String, ? extends CharSequence> beforeAllGenerate(ResourceSet set,
            Collection<? extends RosettaModel> models, String version) {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, ? extends CharSequence> generate(Resource resource, 
        RosettaModel model,
        String version) {
        if (model == null) {
            throw new IllegalArgumentException("Model is null");
        }
        LOGGER.debug("Processing module: {}", model.getName());

        String nameSpace = PythonCodeGeneratorUtil.getNamespace(model);
        PythonCodeGeneratorContext context = contexts.get(nameSpace);
        if (context == null) {
            context = new PythonCodeGeneratorContext();
            contexts.put(nameSpace, context);
        }

        Map<String, CharSequence> result = new HashMap<>();

        List<Data> rosettaClasses = model.getElements()
            .stream()
            .filter(Data.class::isInstance)
            .map(Data.class::cast)
            .collect(Collectors.toList());

        List<RosettaEnumeration> rosettaEnums = model.getElements()
            .stream()
            .filter(RosettaEnumeration.class::isInstance)
            .map(RosettaEnumeration.class::cast)
            .collect(Collectors.toList());

        List<Function> rosettaFunctions = model.getElements()
            .stream()
            .filter(Function.class::isInstance)
            .map(Function.class::cast)
            .collect(Collectors.toList());

        if (!rosettaClasses.isEmpty() || !rosettaEnums.isEmpty() || !rosettaFunctions.isEmpty()) {
            context.addSubfolder(model.getName());
            if (!rosettaFunctions.isEmpty()) {
                context.addSubfolder(model.getName() + ".functions");
            }
        }

        Map<String, CharSequence> currentObjects = context.getObjects();
        currentObjects.putAll(pojoGenerator.generate(rosettaClasses, context));
        result.putAll(enumGenerator.generate(rosettaEnums));
        Map<String, String> currentFunctions = funcGenerator.generate(rosettaFunctions, context);
        currentObjects.putAll(currentFunctions);

        return result;
    }

    @Override
    public Map<String, ? extends CharSequence> afterAllGenerate(
            ResourceSet set,
            Collection<? extends RosettaModel> models,
            String version) {
        Map<String, CharSequence> result = new HashMap<>();
        String cleanVersion = PythonCodeGeneratorUtil.cleanVersion(version);
        for (String nameSpace : contexts.keySet()) {
            PythonCodeGeneratorContext context = contexts.get(nameSpace);
            List<String> subfolders = context.getSubfolders();
            result.putAll(generateWorkspaces(getWorkspaces(subfolders), cleanVersion));
            result.putAll(generateInits(subfolders));
            result.putAll(processDAG(nameSpace, context, cleanVersion));
        }
        return result;
    }

    private Map<String, CharSequence> processDAG(String nameSpace,
        PythonCodeGeneratorContext context,
        String cleanVersion) {
        if (nameSpace == null || context == null || cleanVersion == null) {
            throw new IllegalArgumentException("Invalid arguments");
        }
        Map<String, CharSequence> result = new HashMap<>();
        Map<String, CharSequence> nameSpaceObjects = context.getObjects();
        Graph<String, DefaultEdge> dependencyDAG = context.getDependencyDAG();
        Set<String> enumImports = context.getEnumImports();

        if (nameSpaceObjects != null
            && !nameSpaceObjects.isEmpty()
            && dependencyDAG != null
            && enumImports != null) {
            result.put(PYPROJECT_TOML,
                PythonCodeGeneratorUtil.createPYProjectTomlFile(nameSpace, cleanVersion));
            PythonCodeWriter bundleWriter = new PythonCodeWriter();
            PythonCodeWriter annotationUpdateWriter = new PythonCodeWriter();
            PythonCodeWriter rebuildWriter = new PythonCodeWriter();
            TopologicalOrderIterator<String, DefaultEdge> topologicalOrderIterator =
                new TopologicalOrderIterator<>(dependencyDAG);

            // for each element in the ordered collection add the generated class to the
            // bundle and add a stub class to the results
            boolean isFirst = true;
            while (topologicalOrderIterator.hasNext()) {
                if (isFirst) {
                    bundleWriter.appendBlock(PythonCodeGeneratorUtil.createImports());
                    for (String imp : context.getAdditionalImports()) {
                        bundleWriter.appendLine(imp);
                    }

                    List<String> sortedEnumImports = new ArrayList<>(enumImports);
                    Collections.sort(sortedEnumImports);
                    for (String imp : sortedEnumImports) {
                        bundleWriter.appendLine(imp);
                    }
                    isFirst = false;
                }
                String name = topologicalOrderIterator.next();
                String bundleClassName = name.replace('.', '_');
                CharSequence object = nameSpaceObjects.get(name);
                if (object != null) {
                    // append the class to the bundle
                    bundleWriter.newLine();
                    bundleWriter.newLine();
                    bundleWriter.appendBlock(object.toString());

                    // Collect Phase 2 & 3 updates
                    List<String> updates = context.getPostDefinitionUpdates().get(bundleClassName);
                    if (updates != null && !updates.isEmpty()) {
                        for (String update : updates) {
                            annotationUpdateWriter.appendLine(update);
                        }
                        rebuildWriter.appendLine(String.format("%s.model_rebuild()", bundleClassName));
                    }

                    // create the stub
                    String[] parsedName = name.split("\\.");
                    String stubFileName = SRC + String.join("/", parsedName) + ".py";

                    boolean isFunction = context.hasFunctionName(name);
                    PythonCodeWriter stubWriter = new PythonCodeWriter();
                    stubWriter.appendLine("# pylint: disable=unused-import");
                    if (isFunction) {
                        stubWriter.appendLine("import sys");
                        stubWriter.appendLine("from rune.runtime.func_proxy import create_module_attr_guardian");
                    }
                    stubWriter.append("from ");
                    stubWriter.append(parsedName[0]);
                    stubWriter.append("._bundle import ");
                    stubWriter.append(bundleClassName);
                    stubWriter.append(" as ");
                    stubWriter.append(parsedName[parsedName.length - 1]);
                    if (isFunction) {
                        stubWriter.newLine();
                        stubWriter.newLine();
                        stubWriter.appendLine(
                            "sys.modules[__name__].__class__ = create_module_attr_guardian(sys.modules[__name__].__class__)");
                    }
                    stubWriter.newLine();
                    stubWriter.newLine();
                    stubWriter.appendLine("# EOF");

                    result.put(stubFileName, stubWriter.toString());
                }
            }

            // Append Phase 2 & 3 updates to the bundle
            if (!annotationUpdateWriter.toString().isEmpty()) {
                bundleWriter.newLine();
                bundleWriter.appendLine("# Phase 2: Delayed Annotation Updates");
                bundleWriter.appendBlock(annotationUpdateWriter.toString());
            }

            if (!rebuildWriter.toString().isEmpty()) {
                bundleWriter.newLine();
                bundleWriter.appendLine("# Phase 3: Rebuild");
                bundleWriter.appendBlock(rebuildWriter.toString());
            }

            if (context.hasFunctions()) {
                bundleWriter.newLine();
                bundleWriter.appendLine(
                    "sys.modules[__name__].__class__ = create_module_attr_guardian(sys.modules[__name__].__class__)");
            }

            Set<String> nativeFunctionNames = context.getNativeFunctionNames();

            if (!nativeFunctionNames.isEmpty()) {
                bundleWriter.newLine();
                bundleWriter.appendLine("rune_attempt_register_native_functions(");
                bundleWriter.indent();
                bundleWriter.appendLine("function_names=[");
                bundleWriter.indent();
                for (String nativeFunctionName : nativeFunctionNames) {
                    bundleWriter.appendLine("'" + nativeFunctionName + "',");
                }
                bundleWriter.unindent();
                bundleWriter.appendLine("]");
                bundleWriter.unindent();
                bundleWriter.appendLine(")");
            }
            bundleWriter.newLine();
            bundleWriter.newLine();
            bundleWriter.appendLine("# EOF");
            result.put(SRC + nameSpace + "/_bundle.py", bundleWriter.toString());
        }
        return result;
    }

    private List<String> getWorkspaces(List<String> subfolders) {
        return subfolders
            .stream()
            .map(subfolder -> subfolder.split("\\.")[0])
            .distinct()
            .collect(Collectors.toList());
    }

    private Map<String, String> generateWorkspaces(List<String> workspaces, String version) {
        Map<String, String> result = new HashMap<>();

        for (String workspace : workspaces) {
            result.put(PythonCodeGeneratorUtil.toPyFileName(workspace, INIT),
                    PythonCodeGeneratorUtil.createTopLevelInitFile(version));
            result.put(PythonCodeGeneratorUtil.toPyFileName(workspace, "version"),
                    PythonCodeGeneratorUtil.createVersionFile(version));
            result.put(PythonCodeGeneratorUtil.toFileName(workspace, "py.typed"), "");
        }

        return result;
    }

    private Map<String, String> generateInits(List<String> subfolders) {
        Map<String, String> result = new HashMap<>();

        for (String subfolder : subfolders) {
            String[] parts = subfolder.split("\\.");
            for (int i = 1; i < parts.length; i++) {
                String key = String.join(".", Arrays.copyOfRange(parts, 0, i + 1));
                result.putIfAbsent(PythonCodeGeneratorUtil.toPyFileName(key, INIT), " ");
            }
        }

        return result;
    }

}
