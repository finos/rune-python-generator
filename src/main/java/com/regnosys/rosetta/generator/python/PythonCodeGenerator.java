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
import org.jgrapht.alg.connectivity.KosarajuStrongConnectivityInspector;
import org.jgrapht.graph.DefaultDirectedGraph;
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
import com.regnosys.rosetta.rosetta.simple.FunctionDispatch;
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
    private PythonFunctionGenerator functionGenerator;
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
        this.contexts = new HashMap<>();
    }

    @Override
    public Map<String, ? extends CharSequence> beforeAllGenerate(
            ResourceSet set,
            Collection<? extends RosettaModel> models,
            String version) {
        LOGGER.info("Starting Python code generation for {} models", models.size());
        this.contexts.clear();
        return super.beforeAllGenerate(set, models, version);
    }

    @Override
    public Map<String, ? extends CharSequence> generate(Resource resource, RosettaModel model, String version) {
        String nameSpace = PythonCodeGeneratorUtil.getNamespace(model);
        PythonCodeGeneratorContext context = contexts.computeIfAbsent(nameSpace, k -> new PythonCodeGeneratorContext());

        context.addSubfolder(model.getName());
        model.getElements().stream()
                .filter(Data.class::isInstance)
                .map(Data.class::cast)
                .forEach(rc -> {
                    String modelName = model != null ? model.getName() : null;
                    if (modelName == null) {
                        modelName = "com.rosetta.test.model";
                    }
                    String className = modelName + "." + rc.getName();
                    context.getClassNames().add(className);
                    context.getSuperTypes().put(className,
                            rc.getSuperType() != null ? modelName + "." + rc.getSuperType().getName()
                                    : null);
                    context.getAllData().add(rc);
                });
        model.getElements().stream()
                .filter(e -> e instanceof Function && !(e instanceof FunctionDispatch))
                .map(e -> (Function) e)
                .forEach(rc -> context.getAllFunctions().add(rc));
        model.getElements().stream()
                .filter(RosettaEnumeration.class::isInstance)
                .map(RosettaEnumeration.class::cast)
                .forEach(context.getAllEnums()::add);

        return new HashMap<>();
    }

    @Override
    public Map<String, ? extends CharSequence> afterAllGenerate(
            ResourceSet set,
            Collection<? extends RosettaModel> models,
            String version) {
        LOGGER.info("Starting bundling stage for {} namespaces", contexts.size());
        Map<String, CharSequence> result = new HashMap<>();
        String cleanVersion = PythonCodeGeneratorUtil.cleanVersion(version);
        for (String nameSpace : contexts.keySet()) {
            LOGGER.info("Bundling namespace: {}", nameSpace);
            PythonCodeGeneratorContext context = contexts.get(nameSpace);
            
            // Phase 1: Scan
            pojoGenerator.scan(context.getAllData(), context);
            functionGenerator.scan(context.getAllFunctions(), context);

            // Phase 2: Partitioned Analysis
            partitionClasses(context);

            // Phase 3: Emit
            context.getClassObjects().putAll(pojoGenerator.generate(context.getAllData(), context));
            context.getFunctionObjects().putAll(functionGenerator.generate(context.getAllFunctions(), context));

            List<String> subfolders = context.getSubfolders();
            result.putAll(generateWorkspaces(getWorkspaces(subfolders), cleanVersion));
            result.putAll(generateInits(subfolders));
            result.putAll(processDAG(nameSpace, context, cleanVersion));
            result.putAll(enumGenerator.generate(context.getAllEnums()));
        }
        return result;
    }

    private void partitionClasses(PythonCodeGeneratorContext context) {
        Graph<String, DefaultEdge> dependencyDAG = context.getDependencyDAG();
        KosarajuStrongConnectivityInspector<String, DefaultEdge> inspector = 
            new KosarajuStrongConnectivityInspector<>(dependencyDAG);
        List<Set<String>> sccs = inspector.stronglyConnectedSets();
        
        Set<String> standaloneClasses = context.getStandaloneClasses();
        for (Set<String> scc : sccs) {
            if (scc.size() == 1) {
                String node = scc.iterator().next();
                // A node in size-1 SCC is standalone only if it has no self-loops
                if (!dependencyDAG.containsEdge(node, node)) {
                    standaloneClasses.add(node);
                    LOGGER.debug("Class {} is standalone", node);
                }
            }
        }
    }

    private Map<String, CharSequence> processDAG(String nameSpace,
        PythonCodeGeneratorContext context,
        String cleanVersion) {
        Map<String, CharSequence> result = new HashMap<>();
        Graph<String, DefaultEdge> dependencyDAG = context.getDependencyDAG();
        Set<String> enumImports = context.getEnumImports();
        
        result.put(PYPROJECT_TOML,
            PythonCodeGeneratorUtil.createPYProjectTomlFile(nameSpace, cleanVersion));
        
        PythonCodeWriter bundleWriter = new PythonCodeWriter();
        PythonCodeWriter dataObjectsWriter = new PythonCodeWriter();
        PythonCodeWriter functionsWriter = new PythonCodeWriter();
        PythonCodeWriter annotationUpdateWriter = new PythonCodeWriter();
        PythonCodeWriter rebuildWriter = new PythonCodeWriter();

        // 1. Prepare Header (Imports)
        bundleWriter.appendBlock(PythonCodeGeneratorUtil.createImports());
        for (String imp : context.getAdditionalImports()) {
            bundleWriter.appendLine(imp);
        }
        List<String> sortedEnumImports = new ArrayList<>(enumImports);
        Collections.sort(sortedEnumImports);
        for (String imp : sortedEnumImports) {
            String bundleImportSource = "from " + nameSpace + "._bundle";
            // Allow imports from the same namespace if they are not from the bundle itself (e.g. Enums which are separate)
            if (!imp.contains(bundleImportSource)) {
                bundleWriter.appendLine(imp);
            }
        }

        // 2. Identify SCCs and cycles
        KosarajuStrongConnectivityInspector<String, DefaultEdge> inspector = 
            new KosarajuStrongConnectivityInspector<>(dependencyDAG);
        List<Set<String>> sccs = inspector.stronglyConnectedSets();

        // 3. Build condensation graph of SCCs
        DefaultDirectedGraph<Integer, DefaultEdge> condensationGraph = new DefaultDirectedGraph<>(DefaultEdge.class);
        Map<String, Integer> typeToSccId = new HashMap<>();
        for (int i = 0; i < sccs.size(); i++) {
            condensationGraph.addVertex(i);
            for (String type : sccs.get(i)) {
                typeToSccId.put(type, i);
            }
        }
        for (DefaultEdge edge : dependencyDAG.edgeSet()) {
            int sourceId = typeToSccId.get(dependencyDAG.getEdgeSource(edge));
            int targetId = typeToSccId.get(dependencyDAG.getEdgeTarget(edge));
            if (sourceId != targetId) {
                condensationGraph.addEdge(sourceId, targetId);
            }
        }

        // 4. Get topological order of SCCs
        TopologicalOrderIterator<Integer, DefaultEdge> sccIterator = new TopologicalOrderIterator<>(condensationGraph);
        List<Integer> sccOrder = new ArrayList<>();
        while (sccIterator.hasNext()) {
            sccOrder.add(sccIterator.next());
        }
        
        // 5. Emission
        for (Integer sccId : sccOrder) {
            Set<String> scc = sccs.get(sccId);
            // Sort SCC members by inheritance for definition order
            List<String> sortedScc = sortSccByInheritance(scc, context);

            for (String name : sortedScc) {
                String bundleClassName = getBundleClassName(name, context);
                boolean isStandalone = context.getStandaloneClasses().contains(name);

                CharSequence classObject = context.getClassObjects().get(name);
                CharSequence functionObject = context.getFunctionObjects().get(name);

                if (!isStandalone) {
                    // BUNDLED CASE
                    if (classObject != null) {
                        dataObjectsWriter.newLine();
                        dataObjectsWriter.newLine();
                        dataObjectsWriter.appendBlock(classObject.toString());

                        // Phase 2: Attribute updates from context
                        List<String> updates = context.getPostDefinitionUpdates().get(bundleClassName);
                        if (updates != null && !updates.isEmpty()) {
                            for (String update : updates) {
                                annotationUpdateWriter.appendLine(update);
                            }
                        }

                        // Phase 3: Rebuild — only needed when there are delayed annotation updates
                        if (updates != null && !updates.isEmpty()) {
                            rebuildWriter.appendLine(String.format("%s.model_rebuild()", bundleClassName));
                        }
                    }
                    if (functionObject != null) {
                        functionsWriter.newLine();
                        functionsWriter.newLine();
                        functionsWriter.appendBlock(functionObject.toString());
                    }

                    // Create Proxy Stub
                    String[] parsedName = name.split("\\.");
                    String fileName = SRC + PythonCodeGeneratorUtil.toFileSystemPath(name) + ".py";
                    PythonCodeWriter stubWriter = new PythonCodeWriter();
                    stubWriter.appendLine("# pylint: disable=unused-import");
                    if (functionObject != null) {
                        stubWriter.appendLine("import sys");
                        stubWriter.appendLine("from rune.runtime.func_proxy import create_module_attr_guardian");
                    }
                    stubWriter.append("from ");
                    stubWriter.append(nameSpace);
                    stubWriter.append("._bundle import ");
                    stubWriter.append(bundleClassName);
                    stubWriter.append(" as ");
                    stubWriter.append(parsedName[parsedName.length - 1]);
                    stubWriter.newLine();
                    stubWriter.newLine();
                    stubWriter.appendLine("# EOF");
                    result.put(fileName, stubWriter.toString());

                } else {
                    // STANDALONE CASE
                    String fileName = SRC + PythonCodeGeneratorUtil.toFileSystemPath(name) + ".py";
                    PythonCodeWriter standAloneWriter = new PythonCodeWriter();

                    standAloneWriter.appendBlock(PythonCodeGeneratorUtil.createImports());
                    standAloneWriter.newLine();

                    if (classObject != null) {
                        standAloneWriter.appendBlock(classObject.toString());
                        result.put(fileName, standAloneWriter.toString());
                    }
                    if (functionObject != null && classObject == null) {
                        // Add imports for data-type and function dependencies used in this standalone function
                        Set<DefaultEdge> inEdges = dependencyDAG.incomingEdgesOf(name);
                        List<String> typeImports = new ArrayList<>();
                        for (DefaultEdge edge : inEdges) {
                            String depName = dependencyDAG.getEdgeSource(edge);
                            if (context.getStandaloneClasses().contains(depName)
                                    && (context.getClassObjects().containsKey(depName)
                                        || context.getFunctionObjects().containsKey(depName))) {
                                String shortName = depName.substring(depName.lastIndexOf('.') + 1);
                                typeImports.add("from " + depName + " import " + shortName);
                            }
                        }
                        // Add enum module imports collected during function generation
                        Set<String> enumImportsForFunc = context.getFunctionEnumImports().getOrDefault(name, Collections.emptySet());
                        List<String> funcEnumImportsSorted = new ArrayList<>(enumImportsForFunc);
                        Collections.sort(funcEnumImportsSorted);
                        Collections.sort(typeImports);
                        for (String imp : typeImports) {
                            standAloneWriter.appendLine(imp);
                        }
                        for (String imp : funcEnumImportsSorted) {
                            standAloneWriter.appendLine(imp);
                        }
                        if (!typeImports.isEmpty() || !funcEnumImportsSorted.isEmpty()) {
                            standAloneWriter.newLine();
                        }
                        standAloneWriter.appendBlock(functionObject.toString());
                        // For standalone native functions, register the native implementation
                        if (context.getNativeFunctionNames().contains(name)) {
                            standAloneWriter.newLine();
                            standAloneWriter.appendLine("rune_attempt_register_native_functions(");
                            standAloneWriter.indent();
                            standAloneWriter.appendLine("function_names=['" + name + "']");
                            standAloneWriter.unindent();
                            standAloneWriter.appendLine(")");
                        }
                        result.put(fileName, standAloneWriter.toString());
                    }
                }
            }
        }

        // 6. Final Assembly of _bundle.py
        bundleWriter.appendBlock(dataObjectsWriter.toString());

        if (!annotationUpdateWriter.toString().isEmpty()) {
            bundleWriter.newLine();
            bundleWriter.newLine();
            bundleWriter.appendLine("# Phase 2: Delayed Annotation Updates");
            bundleWriter.appendBlock(annotationUpdateWriter.toString());
        }

        if (!rebuildWriter.toString().isEmpty()) {
            bundleWriter.newLine();
            bundleWriter.newLine();
            bundleWriter.appendLine("# Phase 3: Rebuild");
            bundleWriter.appendBlock(rebuildWriter.toString());
        }

        bundleWriter.appendBlock(functionsWriter.toString());

        if (context.hasNativeFunctions()) {
            bundleWriter.newLine();
            bundleWriter.newLine();
            bundleWriter.appendLine("rune_attempt_register_native_functions(");
            bundleWriter.indent();
            bundleWriter.appendLine("function_names=[");
            bundleWriter.indent();
            for (String nativeFunctionName : context.getNativeFunctionNames()) {
                bundleWriter.appendLine("'" + nativeFunctionName + "',");
            }
            bundleWriter.unindent();
            bundleWriter.appendLine("]");
            bundleWriter.unindent();
            bundleWriter.appendLine(")");
        }

        if (context.hasFunctions()) {
            bundleWriter.newLine();
            bundleWriter.newLine();
            bundleWriter.appendLine(
                "sys.modules[__name__].__class__ = create_module_attr_guardian(sys.modules[__name__].__class__)");
        }

        boolean hasBundledContent = !dataObjectsWriter.toString().isEmpty()
                || !functionsWriter.toString().isEmpty()
                || context.hasNativeFunctions();

        if (hasBundledContent) {
            bundleWriter.newLine();
            bundleWriter.newLine();
            bundleWriter.appendLine("# EOF");
            result.put(SRC + PythonCodeGeneratorUtil.toFileSystemPath(nameSpace) + "/_bundle.py", bundleWriter.toString());
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

    private List<String> sortSccByInheritance(Set<String> scc, PythonCodeGeneratorContext context) {
        DefaultDirectedGraph<String, DefaultEdge> inheritanceGraph = new DefaultDirectedGraph<>(DefaultEdge.class);
        for (String node : scc) {
            inheritanceGraph.addVertex(node);
        }
        for (String node : scc) {
            String parent = context.getSuperTypes().get(node);
            if (parent != null && scc.contains(parent)) {
                inheritanceGraph.addEdge(parent, node); // Super -> Child
            }
        }
        TopologicalOrderIterator<String, DefaultEdge> topo = new TopologicalOrderIterator<>(inheritanceGraph);
        List<String> sorted = new ArrayList<>();
        while (topo.hasNext()) {
            sorted.add(topo.next());
        }
        return sorted;
    }

    public static String getBundleClassName(String fullName, PythonCodeGeneratorContext context) {
        if (fullName == null || !fullName.contains(".")) {
            return fullName;
        }
        return fullName.replace(".", "_");
    }
}
