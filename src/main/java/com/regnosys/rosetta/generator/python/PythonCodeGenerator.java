package com.regnosys.rosetta.generator.python;
// TODO: collect imports as a set rather than an array

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

import com.regnosys.rosetta.rosetta.RosettaEnumeration;
import com.regnosys.rosetta.rosetta.RosettaMetaType;
import com.regnosys.rosetta.rosetta.RosettaModel;
import com.regnosys.rosetta.rosetta.simple.Data;
import com.regnosys.rosetta.rosetta.simple.Function;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * <li>Handles Rosetta model namespaces and organizes output into appropriate
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

    private static final Logger LOGGER = LoggerFactory.getLogger(PythonCodeGenerator.class);

    @Inject
    private PythonModelObjectGenerator pojoGenerator;
    @Inject
    private PythonFunctionGenerator funcGenerator;
    @Inject
    private PythonEnumGenerator enumGenerator;

    private List<String> subfolders;
    private Map<String, Map<String, CharSequence>> objects = null; // Python code for types by namespace, by type name

    public PythonCodeGenerator() {
        super("python");
    }

    @Override
    public Map<String, ? extends CharSequence> beforeAllGenerate(ResourceSet set,
            Collection<? extends RosettaModel> models, String version) {
        subfolders = new ArrayList<>();
        pojoGenerator.beforeAllGenerate();
        objects = new HashMap<>();
        return Collections.emptyMap();
    }

    @Override
    public Map<String, ? extends CharSequence> generate(Resource resource, RosettaModel model, String version) {
        String cleanVersion = cleanVersion(version);

        Map<String, CharSequence> result = new HashMap<>();

        List<Data> rosettaClasses = model.getElements().stream().filter(Data.class::isInstance).map(Data.class::cast)
                .collect(Collectors.toList());

        List<RosettaMetaType> metaDataItems = model.getElements().stream().filter(RosettaMetaType.class::isInstance)
                .map(RosettaMetaType.class::cast).collect(Collectors.toList());

        List<RosettaEnumeration> rosettaEnums = model.getElements().stream()
                .filter(RosettaEnumeration.class::isInstance).map(RosettaEnumeration.class::cast)
                .collect(Collectors.toList());

        List<Function> rosettaFunctions = model.getElements().stream().filter(Function.class::isInstance)
                .map(Function.class::cast).collect(Collectors.toList());

        if (!rosettaClasses.isEmpty() ||
                !metaDataItems.isEmpty() ||
                !rosettaEnums.isEmpty() ||
                !rosettaFunctions.isEmpty()) {
            addSubfolder(model.getName());
            if (!rosettaFunctions.isEmpty()) {
                addSubfolder(model.getName() + ".functions");
            }
        }

        LOGGER.debug("Processing module: {}", model.getName());

        String namespace = PythonCodeGeneratorUtil.getNamespace(model);
        Map<String, CharSequence> currentObject = objects.get(namespace);
        if (currentObject == null) {
            currentObject = new HashMap<String, CharSequence>();
            objects.put(namespace, currentObject);
        }
        currentObject.putAll(pojoGenerator.generate(rosettaClasses, cleanVersion));
        result.putAll(enumGenerator.generate(rosettaEnums, cleanVersion));
        result.putAll(funcGenerator.generate(rosettaFunctions, cleanVersion));

        return result;
    }

    @Override
    public Map<String, ? extends CharSequence> afterAllGenerate(
            ResourceSet set,
            Collection<? extends RosettaModel> models,
            String version) {
        String cleanVersion = cleanVersion(version);
        Map<String, CharSequence> result = new HashMap<>();

        List<String> workspaces = getWorkspaces(subfolders);
        result.putAll(generateWorkspaces(workspaces, cleanVersion));
        result.putAll(generateInits(subfolders));

        for (String namespace : objects.keySet()) {
            Map<String, CharSequence> currentObject = objects.get(namespace);
            if (currentObject != null && !currentObject.isEmpty()) {
                result.put("pyproject.toml", PythonCodeGeneratorUtil.createPYProjectTomlFile(namespace, cleanVersion));
                result.putAll(pojoGenerator.afterAllGenerate(namespace, currentObject));
            }
        }
        return result;
    }

    private String cleanVersion(String version) {
        if (version == null || version.equals("${project.version}")) {
            return "0.0.0";
        }

        String[] versionParts = version.split("\\.");
        if (versionParts.length > 2) {
            String thirdPart = versionParts[2].replaceAll("[^\\d]", "");
            return versionParts[0] + "." + versionParts[1] + "." + thirdPart;
        }

        return "0.0.0";
    }

    private List<String> getWorkspaces(List<String> subfolders) {
        return subfolders.stream().map(subfolder -> subfolder.split("\\.")[0]).distinct().collect(Collectors.toList());
    }

    private Map<String, String> generateWorkspaces(List<String> workspaces, String version) {
        Map<String, String> result = new HashMap<>();

        for (String workspace : workspaces) {
            result.put(PythonCodeGeneratorUtil.toPyFileName(workspace, "__init__"),
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
                result.putIfAbsent(PythonCodeGeneratorUtil.toPyFileName(key, "__init__"), " ");
            }
        }

        return result;
    }

    private void addSubfolder(String subfolder) {
        if (!subfolders.contains(subfolder)) {
            subfolders.add(subfolder);
        }
    }
}