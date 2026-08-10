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
import java.util.HashSet;
import java.util.LinkedHashSet;
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
// todo: refactor to create a BundleAssembler See ARCHITECTURE.md §7 for the full recommendation.

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

        // Promote to bundled: any own-type class whose bundled ancestor is also an own type.
        // Pydantic's model_rebuild(force=True) does not propagate parent annotation changes to
        // a child's model_fields — each subclass must be in the bundle to receive explicit
        // Phase 2 and Phase 3 treatment for inherited deferred fields.
        Map<String, String> superTypes = context.getSuperTypes();
        boolean anyPromotion = true;
        while (anyPromotion) {
            anyPromotion = false;
            for (String cls : new ArrayList<>(standaloneClasses)) {
                if (!ownTypes.contains(cls)) {
                    continue; // external type — always standalone
                }
                String parentFqn = superTypes.get(cls);
                if (parentFqn == null || !ownTypes.contains(parentFqn)) {
                    continue; // no own-type parent
                }
                if (!standaloneClasses.contains(parentFqn)) {
                    standaloneClasses.remove(cls);
                    anyPromotion = true;
                    LOGGER.debug("Promoted {} to bundled (parent {} is bundled)", cls, parentFqn);
                }
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

        BundleHeaderResult headerResult = buildBundleHeader(context, nameSpace, bundleWriter);

        List<Set<String>> sccs = context.getSccs();
        List<Integer> sccOrder = buildCondensationGraph(context.getDependencyDAG(), sccs);

        propagateInheritedPhase2Updates(context);

        List<String> pendingRebuilds = new ArrayList<>();
        emitSortedClasses(sccOrder, sccs, context, nameSpace,
                headerResult.standaloneSupertypesOfBundled(),
                dataObjectsWriter, functionsWriter, annotationUpdateWriter, pendingRebuilds, result);

        // Add deferred standalone imports into the rebuild graph so they are ordered
        // correctly relative to bundled classes: a standalone type S must rebuild before any
        // bundled class B whose Phase 2 annotations reference S, and S itself must rebuild
        // after the bundled types it depends on.
        integrateStandaloneRebuilds(context, headerResult.deferredStandaloneImports(), pendingRebuilds);

        String rebuildContent = emitRebuildCallsInOrder(pendingRebuilds, context);
        assembleBundleFile(nameSpace, context, bundleWriter, dataObjectsWriter, functionsWriter,
                annotationUpdateWriter, rebuildContent, headerResult.deferredStandaloneImports(), result);

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
     * @param pendingRebuilds             Accumulates bundle class names that need Phase 3 model_rebuild calls.
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
        List<String> pendingRebuilds,
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
                            dataObjectsWriter, functionsWriter, annotationUpdateWriter, pendingRebuilds,
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
        List<String> pendingRebuilds,
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
                // Phase 3: Collect for rebuild — emitted in dependency order by emitRebuildCallsInOrder
                pendingRebuilds.add(bundleClassName);
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
     * Propagates Phase 2 annotation updates and rebuild deps from bundled parent classes to their
     * bundled subclasses.
     *
     * <p>Pydantic v2's {@code model_rebuild(force=True)} on a child class does not pick up changes
     * made to a parent's {@code model_fields[f].annotation} — each subclass holds its own copy of
     * the FieldInfo. Any field that was deferred ({@code None}-typed) in a bundled parent must
     * therefore also receive an explicit Phase 2 update on every bundled child that inherits it.
     *
     * <p>Rebuild deps are also propagated so that the rebuild-ordering graph correctly reflects
     * transitive field-dep constraints inherited via subclassing (e.g. if Parent.product is of
     * type NonTransferableProduct, Child inheriting that field must also rebuild NonTransferableProduct
     * before itself).
     *
     * <p>Both propagation steps run in fixpoint loops so that multi-level inheritance chains
     * (grandparent → parent → child) are handled correctly.
     */
    private void propagateInheritedPhase2Updates(PythonCodeGeneratorContext context) {
        Map<String, String> superTypes = context.getSuperTypes();
        Map<String, List<String>> updates = context.getPostDefinitionUpdates();
        Set<String> standaloneClasses = context.getStandaloneClasses();

        // Build bundled parent (bundle name) → list of bundled children (bundle names)
        Map<String, List<String>> bundledChildrenByParent = new HashMap<>();
        for (Map.Entry<String, String> entry : superTypes.entrySet()) {
            String childFqn = entry.getKey();
            String parentFqn = entry.getValue();
            if (!standaloneClasses.contains(childFqn) && !standaloneClasses.contains(parentFqn)) {
                bundledChildrenByParent
                    .computeIfAbsent(getBundleClassName(parentFqn), k -> new ArrayList<>())
                    .add(getBundleClassName(childFqn));
            }
        }

        // Fixpoint: propagate Phase 2 update strings until stable (handles multi-level chains)
        boolean anyChange = true;
        while (anyChange) {
            anyChange = false;
            for (Map.Entry<String, List<String>> entry : bundledChildrenByParent.entrySet()) {
                String parentBundle = entry.getKey();
                List<String> parentUpdates = updates.get(parentBundle);
                if (parentUpdates == null || parentUpdates.isEmpty()) {
                    continue;
                }
                for (String childBundle : entry.getValue()) {
                    Set<String> existingFields = extractFieldNamesFromUpdates(
                        updates.getOrDefault(childBundle, Collections.emptyList()));
                    Set<String> fieldsToPropagate = new HashSet<>();
                    for (String line : parentUpdates) {
                        String field = extractModelFieldsName(line);
                        if (field != null && !existingFields.contains(field)) {
                            fieldsToPropagate.add(field);
                        }
                    }
                    if (fieldsToPropagate.isEmpty()) {
                        continue;
                    }
                    List<String> toAdd = new ArrayList<>();
                    for (String line : parentUpdates) {
                        if (isUpdateLineForFields(line, fieldsToPropagate, parentBundle)) {
                            toAdd.add(childBundle + line.substring(parentBundle.length()));
                        }
                    }
                    if (!toAdd.isEmpty()) {
                        context.addPostDefinitionUpdates(childBundle, toAdd);
                        anyChange = true;
                    }
                }
            }
        }

        // Fixpoint: propagate rebuild deps until stable so transitive constraints are captured
        Map<String, Set<String>> rebuildDeps = context.getRebuildDeps();
        boolean anyDepChange = true;
        while (anyDepChange) {
            anyDepChange = false;
            for (Map.Entry<String, List<String>> entry : bundledChildrenByParent.entrySet()) {
                String parentBundle = entry.getKey();
                Set<String> parentDeps = rebuildDeps.getOrDefault(parentBundle, Collections.emptySet());
                if (parentDeps.isEmpty()) {
                    continue;
                }
                for (String childBundle : entry.getValue()) {
                    Set<String> childDeps = rebuildDeps.getOrDefault(childBundle, Collections.emptySet());
                    for (String dep : parentDeps) {
                        if (!dep.equals(childBundle) && !childDeps.contains(dep)) {
                            context.addRebuildDep(childBundle, dep);
                            anyDepChange = true;
                        }
                    }
                }
            }
        }
    }

    private String extractModelFieldsName(String updateLine) {
        int start = updateLine.indexOf(".model_fields[\"");
        if (start < 0) {
            return null;
        }
        int end = updateLine.indexOf("\"", start + 15);
        return end >= 0 ? updateLine.substring(start + 15, end) : null;
    }

    private Set<String> extractFieldNamesFromUpdates(List<String> updates) {
        Set<String> fields = new HashSet<>();
        for (String line : updates) {
            String f = extractModelFieldsName(line);
            if (f != null) {
                fields.add(f);
            }
        }
        return fields;
    }

    private boolean isUpdateLineForFields(String line, Set<String> fields, String bundleName) {
        for (String field : fields) {
            if (line.startsWith(bundleName + ".model_fields[\"" + field + "\"]")
                    || line.startsWith(bundleName + ".__annotations__[\"" + field + "\"]")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adds deferred standalone imports into the rebuild graph so they are ordered correctly
     * inside Phase 3 rather than being relegated to an unordered Phase 4 after all bundled
     * rebuilds.
     *
     * <p>Two sets of edges are added:
     * <ol>
     *   <li><b>S → bundled dep:</b> standalone class S must rebuild <em>after</em> every bundled
     *       class it references (derived from the dependency DAG).</li>
     *   <li><b>bundled B → S:</b> bundled class B must rebuild <em>after</em> standalone S
     *       whenever B's Phase 2 annotation strings mention S's class name (meaning B inlines
     *       S's schema at rebuild time).</li>
     * </ol>
     *
     * @param context         The generator context (DAG, post-definition updates, standalone set).
     * @param deferredImports List of {@code "from <module> import <ClassName>"} import strings.
     * @param pendingRebuilds The rebuild list (mutated: standalone class names are appended).
     */
    private void integrateStandaloneRebuilds(
        PythonCodeGeneratorContext context,
        List<String> deferredImports,
        List<String> pendingRebuilds
    ) {
        if (deferredImports.isEmpty()) {
            return;
        }
        Set<String> standaloneClasses = context.getStandaloneClasses();
        Map<String, List<String>> updates = context.getPostDefinitionUpdates();
        Graph<String, DefaultEdge> dag = context.getDependencyDAG();

        Set<String> pendingSet = new HashSet<>(pendingRebuilds);

        for (String imp : deferredImports) {
            int importIdx = imp.lastIndexOf(" import ");
            if (importIdx < 0) continue;
            String className = imp.substring(importIdx + " import ".length()).trim();
            String fqn = imp.substring("from ".length(), importIdx).trim();

            // Add standalone class to pendingRebuilds if not already present
            if (pendingSet.add(className)) {
                pendingRebuilds.add(className);
            }

            // Dep 1: standalone S must rebuild AFTER bundled types it directly references.
            // The DAG convention is addEdge(dependency, dependent), so INCOMING edges to S's
            // vertex are the types S depends on (S's field types), while OUTGOING edges are
            // classes that depend on S.
            if (dag.containsVertex(fqn)) {
                for (DefaultEdge edge : dag.incomingEdgesOf(fqn)) {
                    String depFqn = dag.getEdgeSource(edge);
                    if (!standaloneClasses.contains(depFqn)) {
                        // depFqn is a bundled type that S references — S must rebuild after it
                        context.addRebuildDep(className, getBundleClassName(depFqn));
                    }
                }
            }

            // Dep 2: bundled class B must rebuild AFTER standalone S if B's Phase 2
            // annotation strings reference S by its imported name.
            for (Map.Entry<String, List<String>> entry : updates.entrySet()) {
                String bundleClass = entry.getKey();
                for (String updateLine : entry.getValue()) {
                    if (updateLine.contains(className)) {
                        context.addRebuildDep(bundleClass, className);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Sorts and emits the Phase 3 {@code model_rebuild(force=True)} calls in dependency order.
     *
     * <p>Rebuild ordering is driven entirely by field-reference deps recorded in {@code context}
     * during Phase 2: if class A holds a deferred field of type B, an edge B→A is present so B
     * is rebuilt first. Inheritance edges are NOT added to the rebuild graph because
     * {@link #propagateInheritedPhase2Updates} has already copied each parent's deferred-field
     * annotations and rebuild deps onto every bundled subclass, making the child's rebuild
     * self-sufficient.
     *
     * <p>Mutual cycles (A↔B both reference each other) are handled via condensation: types
     * that form a cycle are grouped into one SCC and topo-sorted together using
     * {@link #sortRebuildScc}, which uses a field-dep-predecessor heuristic for cycle-breaking.
     */
    private String emitRebuildCallsInOrder(
        List<String> pendingRebuilds,
        PythonCodeGeneratorContext context
    ) {
        PythonCodeWriter rebuildWriter = new PythonCodeWriter();
        Set<String> rebuildSet = new LinkedHashSet<>(pendingRebuilds);
        Map<String, Set<String>> deps = context.getRebuildDeps();

        DefaultDirectedGraph<String, DefaultEdge> rebuildGraph = new DefaultDirectedGraph<>(DefaultEdge.class);
        for (String cls : rebuildSet) {
            rebuildGraph.addVertex(cls);
        }
        // Field-reference deps: B must rebuild before A when A has a deferred field of type B.
        for (String cls : rebuildSet) {
            for (String dep : deps.getOrDefault(cls, Collections.emptySet())) {
                if (rebuildSet.contains(dep)) {
                    rebuildGraph.addEdge(dep, cls); // dep must be rebuilt before cls
                }
            }
        }

        // Condense cycles: group mutually-dependent types into SCCs, topo-sort the condensation.
        KosarajuStrongConnectivityInspector<String, DefaultEdge> sccInspector =
            new KosarajuStrongConnectivityInspector<>(rebuildGraph);
        List<Set<String>> rebuildSccs = sccInspector.stronglyConnectedSets();

        Map<String, Integer> clsToScc = new HashMap<>();
        DefaultDirectedGraph<Integer, DefaultEdge> condensation = new DefaultDirectedGraph<>(DefaultEdge.class);
        for (int i = 0; i < rebuildSccs.size(); i++) {
            condensation.addVertex(i);
            for (String cls : rebuildSccs.get(i)) {
                clsToScc.put(cls, i);
            }
        }
        for (DefaultEdge edge : rebuildGraph.edgeSet()) {
            int src = clsToScc.get(rebuildGraph.getEdgeSource(edge));
            int tgt = clsToScc.get(rebuildGraph.getEdgeTarget(edge));
            if (src != tgt) {
                condensation.addEdge(src, tgt);
            }
        }

        TopologicalOrderIterator<Integer, DefaultEdge> topo = new TopologicalOrderIterator<>(condensation);
        while (topo.hasNext()) {
            Set<String> sccMembers = rebuildSccs.get(topo.next());
            if (sccMembers.size() == 1) {
                rebuildWriter.appendLine(String.format("%s.model_rebuild(force=True)", sccMembers.iterator().next()));
            } else {
                // Multi-element SCC (genuine field-dep cycle): sort by field-dep-predecessor heuristic
                List<String> sortedMembers = sortRebuildScc(sccMembers, pendingRebuilds, deps);
                for (String cls : sortedMembers) {
                    rebuildWriter.appendLine(String.format("%s.model_rebuild(force=True)", cls));
                }
            }
        }
        return rebuildWriter.toString();
    }

    /**
     * Sorts rebuild-SCC members using Kahn's algorithm with field-dep-predecessor cycle-breaking.
     *
     * <p>Directed edges within the SCC subgraph come from field-reference deps: if class B
     * holds a deferred field of type A, an edge A→B means A must rebuild before B.
     *
     * <p>When all remaining nodes have in-degree &gt; 0 (genuine field-dep cycle), the cycle is
     * broken by preferring nodes that have the fewest field-dep predecessors still in the
     * remaining set — i.e., nodes that are least constrained by unresolved deps. Ties are broken
     * by position in {@code pendingRebuilds} (class-definition order).
     *
     * @param sccMembers      Bundle class names in this rebuild SCC.
     * @param pendingRebuilds All rebuild bundle class names in insertion order (for tie-breaking).
     * @param rebuildDeps     Bundle-name-to-set map: class → set of deps that must rebuild first.
     * @return SCC members in a rebuild order that respects as many dep edges as possible.
     */
    private List<String> sortRebuildScc(
        Set<String> sccMembers,
        List<String> pendingRebuilds,
        Map<String, Set<String>> rebuildDeps
    ) {
        // Build successors and in-degree maps within this SCC.
        // Edge A → B means A must rebuild before B.
        Map<String, Set<String>> successors = new HashMap<>();
        Map<String, Integer> inDegree = new HashMap<>();
        for (String m : sccMembers) {
            successors.put(m, new HashSet<>());
            inDegree.put(m, 0);
        }

        // Field-ref edges: dep → cls (dep must rebuild before cls)
        for (String cls : sccMembers) {
            for (String dep : rebuildDeps.getOrDefault(cls, Collections.emptySet())) {
                if (sccMembers.contains(dep) && !dep.equals(cls)) {
                    if (successors.get(dep).add(cls)) {
                        inDegree.merge(cls, 1, Integer::sum);
                    }
                }
            }
        }

        // Build pendingRebuilds position map for stable tie-breaking
        Map<String, Integer> pendingPos = new HashMap<>();
        for (int i = 0; i < pendingRebuilds.size(); i++) {
            pendingPos.putIfAbsent(pendingRebuilds.get(i), i);
        }

        // Kahn's algorithm with field-dep-predecessor cycle-breaking
        List<String> result = new ArrayList<>();
        Set<String> remaining = new HashSet<>(sccMembers);

        while (!remaining.isEmpty()) {
            // Prefer nodes with in-degree 0, ordered by pendingRebuilds position
            String chosen = null;
            int chosenPos = Integer.MAX_VALUE;
            for (String m : remaining) {
                if (inDegree.get(m) == 0) {
                    int pos = pendingPos.getOrDefault(m, Integer.MAX_VALUE);
                    if (chosen == null || pos < chosenPos) {
                        chosen = m;
                        chosenPos = pos;
                    }
                }
            }

            if (chosen == null) {
                // Cycle-breaking: prefer nodes with fewest field-dep predecessors still in remaining.
                // A node with fewer unresolved predecessors can be rebuilt with less schema
                // incompleteness — the field-dep edge target (the class being depended upon)
                // should be emitted first because others depend on it.
                int minPreds = Integer.MAX_VALUE;
                for (String m : remaining) {
                    int preds = 0;
                    for (String dep : rebuildDeps.getOrDefault(m, Collections.emptySet())) {
                        if (remaining.contains(dep) && !dep.equals(m)) {
                            preds++;
                        }
                    }
                    minPreds = Math.min(minPreds, preds);
                }
                int bestPos = Integer.MAX_VALUE;
                for (String m : remaining) {
                    int preds = 0;
                    for (String dep : rebuildDeps.getOrDefault(m, Collections.emptySet())) {
                        if (remaining.contains(dep) && !dep.equals(m)) {
                            preds++;
                        }
                    }
                    if (preds == minPreds) {
                        int pos = pendingPos.getOrDefault(m, Integer.MAX_VALUE);
                        if (chosen == null || pos < bestPos) {
                            chosen = m;
                            bestPos = pos;
                        }
                    }
                }
                if (chosen == null) {
                    chosen = remaining.iterator().next(); // fallback, should never reach here
                }
            }

            result.add(chosen);
            remaining.remove(chosen);
            for (String succ : successors.getOrDefault(chosen, Collections.emptySet())) {
                if (remaining.contains(succ)) {
                    inDegree.merge(succ, -1, Integer::sum);
                }
            }
        }

        return result;
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
     * @param rebuildContent         Sorted Phase 3 model_rebuild calls as a string.
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
        String rebuildContent,
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

        if (rebuildContent != null && !rebuildContent.isEmpty()) {
            bundleWriter.newLine();
            bundleWriter.newLine();
            bundleWriter.appendLine("# Phase 3: Rebuild");
            bundleWriter.appendBlock(rebuildContent);
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

    /**
     * Carries the result of {@link #buildBundleHeader}.
     *
     * @param deferredStandaloneImports    imports that must be written after the bundle
     * @param standaloneSupertypesOfBundled super-types of bundled classes that are standalone
     */
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
