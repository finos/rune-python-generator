/*
 * Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
 * SPDX-License-Identifier: Apache-2.0
 */
package com.regnosys.rosetta.generator.python;

import static com.regnosys.rosetta.generator.python.util.PythonCodeGeneratorConstants.INIT;
import static com.regnosys.rosetta.generator.python.util.PythonCodeGeneratorConstants.PYPROJECT_TOML;
import static com.regnosys.rosetta.generator.python.util.PythonCodeGeneratorConstants.PYTHON;
import static com.regnosys.rosetta.generator.python.util.PythonCodeGeneratorConstants.SRC;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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
     * Optional override for the pyproject.toml project name.
     * When null, the name is derived from the namespace as "python-&lt;first-segment&gt;".
     */
    private String projectName = null;

    /**
     * Overrides the pyproject.toml project name. When not set (or set to null),
     * the name is derived from the namespace as "python-&lt;first-segment&gt;".
     *
     * @param projectName the project name, or null for default behaviour
     */
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    /**
     * Optional namespace prefix prepended to every generated namespace.
     * When set to "finos", "cdm.event.common" becomes "finos.cdm.event.common".
     */
    private String namespacePrefix = null;

    /**
     * Sets the namespace prefix applied to all generated namespaces.
     *
     * @param namespacePrefix the prefix (e.g. {@code "finos"}), or {@code null} for none
     */
    public void setNamespacePrefix(String namespacePrefix) {
        this.namespacePrefix = namespacePrefix;
    }

    /**
     * Returns the effective (prefix-aware) model name used as the context key
     * and subfolder path.
     */
    private String effectiveModelName(RosettaModel model) {
        return com.regnosys.rosetta.generator.python.util.RuneToPythonMapper.applyPrefix(
                model.getName(), namespacePrefix);
    }

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
        this.contexts.clear();

        // Phase 1: Accumulate all elements from all models into per-namespace contexts.
        for (RosettaModel model : models) {
            String effectiveName = effectiveModelName(model);
            String nameSpace = effectiveName.split("\\.")[0];
            PythonCodeGeneratorContext context = contexts.computeIfAbsent(nameSpace, k -> {
                PythonCodeGeneratorContext c = new PythonCodeGeneratorContext();
                c.setNamespacePrefix(namespacePrefix);
                return c;
            });

            boolean hasContent = model.getElements().stream()
                    .anyMatch(e -> e instanceof Data
                            || (e instanceof Function && !(e instanceof FunctionDispatch))
                            || e instanceof RosettaEnumeration);
            if (hasContent) {
                context.addSubfolder(effectiveName);
            }
            boolean hasFunctions = model.getElements().stream()
                    .anyMatch(e -> e instanceof Function && !(e instanceof FunctionDispatch));
            if (hasFunctions) {
                context.addSubfolder(effectiveName + ".functions");
            }
            model.getElements().stream()
                    .filter(Data.class::isInstance)
                    .map(Data.class::cast)
                    .forEach(context.getAllData()::add);
            model.getElements().stream()
                    .filter(e -> e instanceof Function && !(e instanceof FunctionDispatch))
                    .map(e -> (Function) e)
                    .forEach(context.getAllFunctions()::add);
            model.getElements().stream()
                    .filter(RosettaEnumeration.class::isInstance)
                    .map(RosettaEnumeration.class::cast)
                    .forEach(context.getAllEnums()::add);
        }

        // Phase 2: Scan and analyse — requires the full element set for each namespace.
        for (PythonCodeGeneratorContext context : contexts.values()) {
            pojoGenerator.scan(context.getAllData(), context);
            functionGenerator.scan(context.getAllFunctions(), context);
            partitionClasses(context);
        }

        return Collections.emptyMap();
    }

    @Override
    public Map<String, ? extends CharSequence> generate(Resource resource, RosettaModel model, String version) {
        Map<String, CharSequence> result = new HashMap<>();
        String nameSpace = effectiveModelName(model).split("\\.")[0];
        PythonCodeGeneratorContext context = contexts.get(nameSpace);
        if (context == null) {
            return result;
        }

        List<Data> modelData = model.getElements().stream()
                .filter(Data.class::isInstance)
                .map(Data.class::cast)
                .collect(Collectors.toList());
        List<Function> modelFunctions = model.getElements().stream()
                .filter(e -> e instanceof Function && !(e instanceof FunctionDispatch))
                .map(e -> (Function) e)
                .collect(Collectors.toList());
        List<RosettaEnumeration> modelEnums = model.getElements().stream()
                .filter(RosettaEnumeration.class::isInstance)
                .map(RosettaEnumeration.class::cast)
                .collect(Collectors.toList());

        context.getClassObjects().putAll(pojoGenerator.generate(modelData, context));
        context.getFunctionObjects().putAll(functionGenerator.generate(modelFunctions, context));
        result.putAll(enumGenerator.generate(modelEnums, context));

        return result;
    }

    @Override
    public Map<String, ? extends CharSequence> afterAllGenerate(
        ResourceSet set,
        Collection<? extends RosettaModel> models,
        String version
    ) {
        Map<String, CharSequence> result = new HashMap<>();
        String cleanVersion = PythonCodeGeneratorUtil.cleanVersion(version);

        for (Map.Entry<String, PythonCodeGeneratorContext> entry : contexts.entrySet()) {
            String nameSpace = entry.getKey();
            PythonCodeGeneratorContext context = entry.getValue();

            List<String> subfolders = context.getSubfolders();
            result.putAll(generateWorkspaces(context, cleanVersion));
            result.putAll(generateInits(subfolders));
            result.putAll(processDAG(nameSpace, context, cleanVersion));
        }

        String resolvedProjectName;
        if (projectName != null && !projectName.isBlank()) {
            resolvedProjectName = projectName;
        } else {
            String derivedNamespace = contexts.entrySet().stream()
                    .max(Comparator.comparingInt(e -> e.getValue().getSubfolders().size()))
                    .map(Map.Entry::getKey)
                    .orElse("unknown");
            resolvedProjectName = "python-" + derivedNamespace;
            if (contexts.size() > 1) {
                LOGGER.warn(
                        "Multiple top-level namespaces found: {}. Defaulting pyproject.toml project name to '{}' "
                                + "(largest namespace by file count). Set an explicit project name via setProjectName() to suppress this warning.",
                        contexts.keySet(), resolvedProjectName);
            }
        }
        result.put(PYPROJECT_TOML, PythonCodeGeneratorUtil.createPYProjectTomlFile(null, cleanVersion, resolvedProjectName));
        return result;
    }

    private void partitionClasses(PythonCodeGeneratorContext context) {
        Graph<String, DefaultEdge> dependencyDAG = context.getDependencyDAG();
        KosarajuStrongConnectivityInspector<String, DefaultEdge> inspector =
            new KosarajuStrongConnectivityInspector<>(dependencyDAG);
        List<Set<String>> sccs = inspector.stronglyConnectedSets();
        context.setSccs(sccs);

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

        // Types from other namespaces are always external — accessed via their fully-qualified
        // proxy-stub path, never via another namespace's _bundle.  Mark them standalone
        // unconditionally so that import generation uses "from fpml.x.y.Z import Z" rather
        // than "from fpml._bundle import fpml_x_y_Z".
        Set<String> ownTypes = context.getClassNames();
        for (String vertex : dependencyDAG.vertexSet()) {
            if (!ownTypes.contains(vertex)) {
                standaloneClasses.add(vertex);
            }
        }
    }

    private Map<String, CharSequence> processDAG(
        String nameSpace,
        PythonCodeGeneratorContext context,
        String cleanVersion
    ) {
        Map<String, CharSequence> result = new HashMap<>();

        PythonCodeWriter bundleWriter = new PythonCodeWriter();
        PythonCodeWriter dataObjectsWriter = new PythonCodeWriter();
        PythonCodeWriter functionsWriter = new PythonCodeWriter();
        PythonCodeWriter annotationUpdateWriter = new PythonCodeWriter();
        PythonCodeWriter rebuildWriter = new PythonCodeWriter();

        BundleHeaderResult headerResult = buildBundleHeader(context, nameSpace, bundleWriter);

        List<Set<String>> sccs = context.getSccs();
        List<Integer> sccOrder = buildCondensationGraph(context.getDependencyDAG(), sccs);

        emitSortedClasses(sccOrder, sccs, context, nameSpace,
                headerResult.standaloneSupertypesOfBundled(),
                dataObjectsWriter, functionsWriter, annotationUpdateWriter, rebuildWriter, result);

        assembleBundleFile(nameSpace, context, bundleWriter, dataObjectsWriter, functionsWriter,
                annotationUpdateWriter, rebuildWriter, headerResult.deferredStandaloneImports(), result);

        return result;
    }

    /**
     * Writes standard imports and additional imports to the bundle header, classifies enum imports
     * as either header-safe (enum modules) or deferred (standalone class references), and identifies
     * standalone supertypes of bundled classes that must be imported inline rather than deferred.
     *
     * @param context       The code generator context.
     * @param nameSpace     The namespace of the bundle being generated.
     * @param bundleWriter  The writer to append header content to (mutated).
     * @return A {@link BundleHeaderResult} containing the deferred standalone imports and the set
     *         of standalone supertypes of bundled classes.
     */
    private BundleHeaderResult buildBundleHeader(PythonCodeGeneratorContext context,
            String nameSpace,
            PythonCodeWriter bundleWriter) {
        bundleWriter.appendBlock(PythonCodeGeneratorUtil.createImports());
        for (String imp : context.getAdditionalImports()) {
            bundleWriter.appendLine(imp);
        }

        // Split imports: enum module imports are safe in the header (enums never import from
        // the bundle). Standalone-class imports ("from X import Y") must be deferred until
        // after all bundled class definitions, because a standalone class may itself transitively
        // import a bundled class — and at header-evaluation time that bundled class is not yet
        // defined in the partially-initialised bundle module.
        //
        // Exception: a standalone type used as a DIRECT BASE CLASS of a bundled type cannot be
        // deferred. Python evaluates base-class expressions immediately at class-definition time
        // (unlike attribute annotations which are lazy strings under PEP 563). Such imports must
        // stay in the header.
        Set<String> standaloneClasses = context.getStandaloneClasses();
        Set<String> standaloneSupertypesOfBundled = new java.util.HashSet<>();
        for (Map.Entry<String, String> entry : context.getSuperTypes().entrySet()) {
            String childFqn = entry.getKey();
            String parentFqn = entry.getValue();
            if (parentFqn != null
                    && !standaloneClasses.contains(childFqn)   // child is bundled
                    && standaloneClasses.contains(parentFqn)) { // parent is standalone
                standaloneSupertypesOfBundled.add(parentFqn);
            }
        }

        List<String> deferredStandaloneImports = new ArrayList<>();
        List<String> sortedEnumImports = new ArrayList<>(context.getEnumImports());
        Collections.sort(sortedEnumImports);
        String bundleImportSource = "from " + nameSpace + "._bundle";
        for (String imp : sortedEnumImports) {
            // Allow imports from the same namespace if they are not from the bundle itself (e.g. Enums which are separate)
            if (!imp.contains(bundleImportSource)) {
                if (imp.startsWith("from ")) {
                    // Extract the FQN from "from <fqn> import <Name>"
                    String fqn = imp.substring("from ".length(), imp.indexOf(" import "));
                    if (!standaloneSupertypesOfBundled.contains(fqn)) {
                        // Attribute-type-only import: safe to defer until after class definitions.
                        // (Standalone supertypes of bundled classes are handled inline in emitSortedClasses.)
                        deferredStandaloneImports.add(imp);
                    }
                } else {
                    // Enum module import — safe to put in the header
                    bundleWriter.appendLine(imp);
                }
            }
        }

        return new BundleHeaderResult(deferredStandaloneImports, standaloneSupertypesOfBundled);
    }

    /**
     * Builds the condensation graph of the dependency DAG (one node per SCC) and returns the
     * topological ordering of SCC ids.
     *
     * @param dependencyDAG The type dependency graph.
     * @param sccs          The strongly-connected components, indexed by id (list position).
     * @return The SCC ids in topological order.
     */
    private List<Integer> buildCondensationGraph(
        Graph<String, DefaultEdge> dependencyDAG,
        List<Set<String>> sccs
    ) {
        DefaultDirectedGraph<Integer, DefaultEdge> condensationGraph =
            new DefaultDirectedGraph<>(DefaultEdge.class);
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

        TopologicalOrderIterator<Integer, DefaultEdge> sccIterator =
            new TopologicalOrderIterator<>(condensationGraph);
        List<Integer> sccOrder = new ArrayList<>();
        while (sccIterator.hasNext()) {
            sccOrder.add(sccIterator.next());
        }
        return sccOrder;
    }

    /**
     * Walks SCCs in topological order and emits bundled class/function bodies, proxy stubs, and
     * standalone class/function files. Bundled-class annotation updates and rebuild calls are
     * accumulated in the provided writers; proxy stub and standalone files are placed directly
     * into {@code result}.
     *
     * @param sccOrder                    SCC ids in topological order.
     * @param sccs                        Strongly-connected components (indexed by id).
     * @param context                     The code generator context.
     * @param nameSpace                   The bundle namespace.
     * @param standaloneSupertypesOfBundled  Standalone types used as direct base classes of
     *                                    bundled types, requiring inline import before the subclass.
     * @param dataObjectsWriter           Accumulates bundled class bodies.
     * @param functionsWriter             Accumulates bundled function bodies.
     * @param annotationUpdateWriter      Accumulates Phase 2 annotation updates.
     * @param rebuildWriter               Accumulates Phase 3 model_rebuild calls.
     * @param result                      Map to receive proxy stub and standalone file entries.
     */
    private void emitSortedClasses(
        List<Integer> sccOrder,
        List<Set<String>> sccs,
        PythonCodeGeneratorContext context,
        String nameSpace,
        Set<String> standaloneSupertypesOfBundled,
        PythonCodeWriter dataObjectsWriter,
        PythonCodeWriter functionsWriter,
        PythonCodeWriter annotationUpdateWriter,
        PythonCodeWriter rebuildWriter,
        Map<String, CharSequence> result
    ) {
        Graph<String, DefaultEdge> dependencyDAG = context.getDependencyDAG();
        Set<String> standaloneClasses = context.getStandaloneClasses();
        // Track standalone supertype imports already emitted inline to avoid duplicates.
        Set<String> emittedInlineSupertypeImports = new java.util.HashSet<>();

        for (Integer sccId : sccOrder) {
            Set<String> scc = sccs.get(sccId);
            // Sort SCC members by inheritance for definition order
            List<String> sortedScc = sortSccByInheritance(scc, context);

            for (String name : sortedScc) {
                String bundleClassName = getBundleClassName(name);
                boolean isStandalone = standaloneClasses.contains(name);

                CharSequence classObject = context.getClassObjects().get(name);
                CharSequence functionObject = context.getFunctionObjects().get(name);

                if (!isStandalone) {
                    emitBundledClass(name, bundleClassName, classObject, functionObject,
                            nameSpace, context, standaloneClasses, standaloneSupertypesOfBundled,
                            emittedInlineSupertypeImports,
                            dataObjectsWriter, functionsWriter, annotationUpdateWriter, rebuildWriter,
                            result);
                } else {
                    emitStandaloneFile(name, classObject, functionObject,
                            nameSpace, context, dependencyDAG, result);
                }
            }
        }
    }

    /**
     * Emits a single bundled class or function body into the appropriate writers and creates
     * a lazy proxy stub file for it.
     */
    private void emitBundledClass(
        String name,
        String bundleClassName,
        CharSequence classObject,
        CharSequence functionObject,
        String nameSpace,
        PythonCodeGeneratorContext context,
        Set<String> standaloneClasses,
        Set<String> standaloneSupertypesOfBundled,
        Set<String> emittedInlineSupertypeImports,
        PythonCodeWriter dataObjectsWriter,
        PythonCodeWriter functionsWriter,
        PythonCodeWriter annotationUpdateWriter,
        PythonCodeWriter rebuildWriter,
        Map<String, CharSequence> result
    ) {
        if (classObject != null) {
            // If this bundled class extends a standalone type, emit that import
            // inline here — after all bundled types the standalone depends on have
            // been defined, and before the class statement that uses it as a base.
            String superFqn = context.getSuperTypes().get(name);
            if (superFqn != null && standaloneClasses.contains(superFqn)
                    && emittedInlineSupertypeImports.add(superFqn)) {
                String superName = superFqn.substring(superFqn.lastIndexOf('.') + 1);
                dataObjectsWriter.newLine();
                dataObjectsWriter.newLine();
                dataObjectsWriter.appendLine(String.format("from %s import %s", superFqn, superName));
            }
            dataObjectsWriter.newLine();
            dataObjectsWriter.newLine();
            dataObjectsWriter.appendBlock(classObject.toString());

            // Phase 2: Attribute updates from context
            List<String> updates = context.getPostDefinitionUpdates().get(bundleClassName);
            if (updates != null && !updates.isEmpty()) {
                for (String update : updates) {
                    annotationUpdateWriter.appendLine(update);
                }
                // Phase 3: Rebuild — only needed when there are delayed annotation updates
                rebuildWriter.appendLine(String.format("%s.model_rebuild(force=True)", bundleClassName));
            }
        }
        if (functionObject != null) {
            functionsWriter.newLine();
            functionsWriter.newLine();
            functionsWriter.appendBlock(functionObject.toString());
        }

        // Create Proxy Stub (lazy — defers bundle import until first attribute access).
        // A direct "from bundle import X" in the stub would trigger bundle loading
        // immediately, which can cause a circular ImportError when the stub itself
        // was the entry point that started the bundle loading.  Instead we use a
        // module-level __getattr__ so the stub module loads instantly and the bundle
        // import only happens when the exported name is first accessed (by which time
        // the bundle is fully initialised).
        result.put(
            SRC + PythonCodeGeneratorUtil.toFileSystemPath(name) + ".py",
            generateProxyStub(name, nameSpace, bundleClassName, functionObject != null));
    }

    /**
     * Emits a standalone class or function file (not part of the bundle).
     */
    private void emitStandaloneFile(
        String name,
        CharSequence classObject,
        CharSequence functionObject,
        String nameSpace,
        PythonCodeGeneratorContext context,
        Graph<String, DefaultEdge> dependencyDAG,
        Map<String, CharSequence> result
    ) {
        String fileName = SRC + PythonCodeGeneratorUtil.toFileSystemPath(name) + ".py";
        PythonCodeWriter standAloneWriter = new PythonCodeWriter();
        standAloneWriter.appendBlock(PythonCodeGeneratorUtil.createImports());
        standAloneWriter.newLine();

        if (classObject != null) {
            standAloneWriter.appendBlock(classObject.toString());
            result.put(fileName, standAloneWriter.toString());
        } else if (functionObject != null) {
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
            Set<String> enumImportsForFunc =
                context.getFunctionEnumImports().getOrDefault(name, Collections.emptySet());
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
                standAloneWriter.append("function_names=['" + context.stripNamespacePrefix(name) + "'], ");
                standAloneWriter.appendLine("rune_namespace_prefix=" + context.getnamespacePrefixOrNone());
                standAloneWriter.unindent();
                standAloneWriter.appendLine(")");
            }
            result.put(fileName, standAloneWriter.toString());
        }
    }

    /**
     * Generates the lazy proxy stub file content for a bundled type.
     *
     * @param name            Fully-qualified type name (e.g., {@code com.example.Foo}).
     * @param nameSpace       The bundle namespace (e.g., {@code com.example}).
     * @param bundleClassName The flattened bundle class name (e.g., {@code com_example_Foo}).
     * @param hasFunction     Whether the type has an associated function object in the bundle.
     * @return The stub file content as a string.
     */
    private String generateProxyStub(
        String name, 
        String nameSpace,
        String bundleClassName, 
        boolean hasFunction
    ) {
        String[] parsedName = name.split("\\.");
        String shortName = parsedName[parsedName.length - 1];
        PythonCodeWriter stubWriter = new PythonCodeWriter();
        stubWriter.appendLine("# pylint: disable=unused-import");
        if (hasFunction) {
            stubWriter.appendLine("import sys");
            stubWriter.appendLine("from rune.runtime.func_proxy import create_module_attr_guardian");
            stubWriter.newLine();
        }
        stubWriter.appendLine("def __getattr__(name: str):");
        stubWriter.indent();
        stubWriter.appendLine("if name == '" + shortName + "':");
        stubWriter.indent();
        stubWriter.appendLine("import " + nameSpace + "._bundle as _b");
        stubWriter.appendLine("_v = _b." + bundleClassName);
        stubWriter.appendLine("globals()['" + shortName + "'] = _v");
        stubWriter.appendLine("return _v");
        stubWriter.unindent();
        stubWriter.appendLine("raise AttributeError(name)");
        stubWriter.unindent();
        stubWriter.newLine();
        stubWriter.appendLine("# EOF");
        return stubWriter.toString();
    }

    /**
     * Assembles the final {@code _bundle.py} content from the accumulated writers and adds it to
     * the result map if there is any bundled content to emit.
     *
     * @param nameSpace              The bundle namespace.
     * @param context                The code generator context.
     * @param bundleWriter           Writer containing the bundle header (already populated).
     * @param dataObjectsWriter      Accumulated bundled class bodies.
     * @param functionsWriter        Accumulated bundled function bodies.
     * @param annotationUpdateWriter Accumulated Phase 2 annotation updates.
     * @param rebuildWriter          Accumulated Phase 3 model_rebuild calls.
     * @param deferredImports        Standalone-class imports to emit after class definitions.
     * @param result                 Map to receive the assembled bundle file entry.
     */
    private void assembleBundleFile(
        String nameSpace,
        PythonCodeGeneratorContext context,
        PythonCodeWriter bundleWriter,
        PythonCodeWriter dataObjectsWriter,
        PythonCodeWriter functionsWriter,
        PythonCodeWriter annotationUpdateWriter,
        PythonCodeWriter rebuildWriter,
        List<String> deferredImports,
        Map<String, CharSequence> result
    ) {
        bundleWriter.appendBlock(dataObjectsWriter.toString());

        // Deferred standalone-class imports: emitted after all bundled class definitions so
        // that when those standalone modules are loaded they can safely import bundled types
        // from this bundle (which are now already defined).
        if (!deferredImports.isEmpty()) {
            bundleWriter.newLine();
            bundleWriter.newLine();
            bundleWriter.appendLine("# Standalone type imports (deferred to avoid circular import at bundle load time)");
            for (String imp : deferredImports) {
                bundleWriter.appendLine(imp);
            }
        }

        if (!annotationUpdateWriter.isEmpty()) {
            bundleWriter.newLine();
            bundleWriter.newLine();
            bundleWriter.appendLine("# Phase 2: Delayed Annotation Updates");
            bundleWriter.appendBlock(annotationUpdateWriter.toString());
        }

        if (!rebuildWriter.isEmpty()) {
            bundleWriter.newLine();
            bundleWriter.newLine();
            bundleWriter.appendLine("# Phase 3: Rebuild");
            bundleWriter.appendBlock(rebuildWriter.toString());
        }

        bundleWriter.appendBlock(functionsWriter.toString());

        if (context.hasFunctions()) {
            bundleWriter.newLine();
            bundleWriter.newLine();
            bundleWriter.appendLine(
                "sys.modules[__name__].__class__ = create_module_attr_guardian(sys.modules[__name__].__class__)");
        }

        boolean hasBundledContent = !dataObjectsWriter.isEmpty()
                || !functionsWriter.isEmpty();

        if (hasBundledContent) {
            bundleWriter.newLine();
            bundleWriter.newLine();
            bundleWriter.appendLine("# EOF");
            result.put(SRC + PythonCodeGeneratorUtil.toFileSystemPath(nameSpace) + "/_bundle.py",
                    bundleWriter.toString());
        }
    }

    /** Carries the result of {@link #buildBundleHeader}. */
    private record BundleHeaderResult(
            List<String> deferredStandaloneImports,
            Set<String> standaloneSupertypesOfBundled) {
    }

    private List<String> getWorkspaces(List<String> subfolders) {
        return subfolders
            .stream()
            .map(subfolder -> subfolder.split("\\.")[0])
            .distinct()
            .collect(Collectors.toList());
    }

    private Map<String, String> generateWorkspaces(PythonCodeGeneratorContext context, String version) {
        Map<String, String> result = new HashMap<>();
        List<String> workspaces = getWorkspaces(context.getSubfolders());
        for (String workspace : workspaces) {
            result.put(
                PythonCodeGeneratorUtil.toPyFileName(workspace, INIT),
                PythonCodeGeneratorUtil.createTopLevelInitFile(version, namespacePrefix)
            );
            result.put(
                PythonCodeGeneratorUtil.toPyFileName(workspace, "version"),
                PythonCodeGeneratorUtil.createVersionFile(version)
            );
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

    public static String getBundleClassName(String fullName) {
        if (fullName == null || !fullName.contains(".")) {
            return fullName;
        }
        return fullName.replace(".", "_");
    }
}
