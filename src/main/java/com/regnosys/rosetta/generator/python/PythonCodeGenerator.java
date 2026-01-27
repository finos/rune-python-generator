package com.regnosys.rosetta.generator.python;
// TODO: re-engineer type generation to use an object that has the features carried throughout the generation (imports, etc.)

// TODO: function support
// TODO: review and consolidate unit tests
// TODO: review migrating choice alias processor to PythonModelObjectGenerator

import jakarta.inject.Inject;
import com.regnosys.rosetta.generator.external.AbstractExternalGenerator;
import com.regnosys.rosetta.generator.python.enums.PythonEnumGenerator;
import com.regnosys.rosetta.generator.python.functions.PythonFunctionGenerator;
import com.regnosys.rosetta.generator.python.object.PythonModelObjectGenerator;
import com.regnosys.rosetta.generator.python.util.PythonCodeGeneratorUtil;
import com.regnosys.rosetta.generator.python.util.PythonCodeWriter;
import static com.regnosys.rosetta.generator.python.util.PythonCodeGeneratorConstants.*;

import com.regnosys.rosetta.rosetta.RosettaEnumeration;
import com.regnosys.rosetta.rosetta.RosettaModel;
import com.regnosys.rosetta.rosetta.simple.Data;
import com.regnosys.rosetta.rosetta.simple.Function;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;

import org.jgrapht.traverse.TopologicalOrderIterator;

import java.util.*;
import java.util.stream.Collectors;

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

public class PythonCodeGenerator extends AbstractExternalGenerator {

    @Inject
    private PythonModelObjectGenerator pojoGenerator;
    @Inject
    private PythonFunctionGenerator funcGenerator;
    @Inject
    private PythonEnumGenerator enumGenerator;

    private static final Logger LOGGER = LoggerFactory.getLogger(PythonCodeGenerator.class);

    private Map<String, PythonCodeGeneratorContext> contexts = null;

    public PythonCodeGenerator() {
        super(PYTHON);
    }

    @Override
    public Map<String, ? extends CharSequence> beforeAllGenerate(ResourceSet set,
            Collection<? extends RosettaModel> models, String version) {

        contexts = new HashMap<>();
        return Collections.emptyMap();
    }

    @Override
    public Map<String, ? extends CharSequence> generate(Resource resource, RosettaModel model, String version) {
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

        String cleanVersion = PythonCodeGeneratorUtil.cleanVersion(version);

        Map<String, CharSequence> result = new HashMap<>();

        List<Data> rosettaClasses = model.getElements().stream().filter(Data.class::isInstance).map(Data.class::cast)
                .collect(Collectors.toList());

        List<RosettaEnumeration> rosettaEnums = model.getElements().stream()
                .filter(RosettaEnumeration.class::isInstance).map(RosettaEnumeration.class::cast)
                .collect(Collectors.toList());

        List<Function> rosettaFunctions = model.getElements().stream().filter(Function.class::isInstance)
                .map(Function.class::cast).collect(Collectors.toList());

        if (!rosettaClasses.isEmpty() || !rosettaEnums.isEmpty() || !rosettaFunctions.isEmpty()) {
            context.addSubfolder(model.getName());
            if (!rosettaFunctions.isEmpty()) {
                context.addSubfolder(model.getName() + ".functions");
            }
        }

        Map<String, CharSequence> currentObject = context.getObjects();
        currentObject.putAll(pojoGenerator.generate(rosettaClasses, cleanVersion, context.getDependencyDAG(),
                context.getEnumImports()));
        result.putAll(enumGenerator.generate(rosettaEnums, cleanVersion));
        result.putAll(funcGenerator.generate(rosettaFunctions, cleanVersion));

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

    private Map<String, CharSequence> processDAG(String nameSpace, PythonCodeGeneratorContext context,
            String cleanVersion) {
        if (nameSpace == null || context == null || cleanVersion == null) {
            throw new IllegalArgumentException("Invalid arguments");
        }
        Map<String, CharSequence> result = new HashMap<>();
        Map<String, CharSequence> nameSpaceObjects = context.getObjects();
        Graph<String, DefaultEdge> dependencyDAG = context.getDependencyDAG();
        Set<String> enumImports = context.getEnumImports();

        if (nameSpaceObjects != null && !nameSpaceObjects.isEmpty() && dependencyDAG != null && enumImports != null) {
            result.put(PYPROJECT_TOML, PythonCodeGeneratorUtil.createPYProjectTomlFile(nameSpace, cleanVersion));
            PythonCodeWriter bundleWriter = new PythonCodeWriter();
            TopologicalOrderIterator<String, DefaultEdge> topologicalOrderIterator = new TopologicalOrderIterator<>(
                    dependencyDAG);

            // for each element in the ordered collection add the generated class to the
            // bundle and add a stub class to the results
            boolean isFirst = true;
            while (topologicalOrderIterator.hasNext()) {
                if (isFirst) {
                    bundleWriter.appendBlock(PythonCodeGeneratorUtil.createImports());
                    List<String> sortedEnumImports = new ArrayList<>(enumImports);
                    Collections.sort(sortedEnumImports);
                    for (String imp : sortedEnumImports) {
                        bundleWriter.appendLine(imp);
                    }
                    isFirst = false;
                }
                String name = topologicalOrderIterator.next();
                CharSequence object = nameSpaceObjects.get(name);
                if (object != null) {
                    // append the class to the bundle
                    bundleWriter.newLine();
                    bundleWriter.newLine();
                    bundleWriter.appendBlock(object.toString());

                    // create the stub
                    String[] parsedName = name.split("\\.");
                    String stubFileName = SRC + String.join("/", parsedName) + ".py";

                    PythonCodeWriter stubWriter = new PythonCodeWriter();
                    stubWriter.appendLine("# pylint: disable=unused-import");
                    stubWriter.append("from ");
                    stubWriter.append(parsedName[0]);
                    stubWriter.append("._bundle import ");
                    stubWriter.append(name.replace('.', '_'));
                    stubWriter.append(" as ");
                    stubWriter.append(parsedName[parsedName.length - 1]);
                    stubWriter.newLine();
                    stubWriter.newLine();
                    stubWriter.appendLine("# EOF");

                    result.put(stubFileName, stubWriter.toString());
                }
            }
            bundleWriter.newLine();
            bundleWriter.appendLine("# EOF");
            result.put(SRC + nameSpace + "/_bundle.py", bundleWriter.toString());
        }
        return result;
    }

    private List<String> getWorkspaces(List<String> subfolders) {
        return subfolders.stream().map(subfolder -> subfolder.split("\\.")[0]).distinct().collect(Collectors.toList());
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