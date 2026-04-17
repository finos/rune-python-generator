/*
 * Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
 * SPDX-License-Identifier: Apache-2.0
 */
package com.regnosys.rosetta.generator.python;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.jgrapht.Graph;
import org.jgrapht.alg.connectivity.KosarajuStrongConnectivityInspector;
import org.jgrapht.graph.DefaultEdge;

import com.google.inject.Injector;
import com.regnosys.rosetta.builtin.RosettaBuiltinsService;
import com.regnosys.rosetta.generator.python.PythonCodeGeneratorCLI.PythonModelLoader;
import com.regnosys.rosetta.generator.python.PythonCodeGeneratorCLI.PythonRosettaStandaloneSetup;
import com.regnosys.rosetta.rosetta.RosettaModel;

/**
 * Utility tool to run partitioning analysis on Rosetta models and export metrics.
 */
public final class PartitioningMetricsCLI {

    private PartitioningMetricsCLI() { }

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("Usage: PartitioningMetricsCLI <rosetta-source-dir> [--type <type-name-or-fqn>]");
            System.exit(1);
        }

        String srcDir = args[0];
        String typeQuery = null;
        for (int i = 1; i < args.length - 1; i++) {
            if ("--type".equals(args[i])) {
                typeQuery = args[i + 1];
            }
        }
        Path srcDirPath = Paths.get(srcDir);

        List<Path> rosettaFiles = Files.walk(srcDirPath)
                .filter(Files::isRegularFile)
                .filter(f -> f.getFileName().toString().endsWith(".rosetta"))
                .collect(Collectors.toList());

        System.out.println("Found " + rosettaFiles.size() + " .rosetta files.");

        Injector injector = new PythonRosettaStandaloneSetup().createInjectorAndDoEMFRegistration();
        ResourceSet resourceSet = injector.getInstance(ResourceSet.class);
        List<Resource> resources = new LinkedList<>();
        RosettaBuiltinsService builtins = injector.getInstance(RosettaBuiltinsService.class);
        resources.add(resourceSet.getResource(builtins.basicTypesURI, true));
        resources.add(resourceSet.getResource(builtins.annotationsURI, true));

        for (Path path : rosettaFiles) {
            try {
                resources.add(resourceSet.getResource(URI.createFileURI(path.toString()), true));
            } catch (Exception e) {
                // Ignore
            }
        }

        PythonModelLoader modelLoader = injector.getInstance(PythonModelLoader.class);
        List<RosettaModel> models = modelLoader.getRosettaModels(resources);
        
        System.out.println("Processing " + models.size() + " Rosetta models...");

        PythonCodeGenerator pythonCodeGenerator = injector.getInstance(PythonCodeGenerator.class);
        pythonCodeGenerator.beforeAllGenerate(resourceSet, models, "0.0.0");

        for (RosettaModel model : models) {
            // This populates the internal 'contexts' map in PythonCodeGenerator
            pythonCodeGenerator.generate(model.eResource(), model, "0.0.0");
        }

        // This runs the partitioning logic (SCC analysis)
        pythonCodeGenerator.afterAllGenerate(resourceSet, models, "0.0.0");

        java.lang.reflect.Field contextsField;
        Map<String, PythonCodeGeneratorContext> contexts;
        try {
            contextsField = PythonCodeGenerator.class.getDeclaredField("contexts");
            contextsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, PythonCodeGeneratorContext> c = (Map<String, PythonCodeGeneratorContext>) contextsField.get(pythonCodeGenerator);
            contexts = c;
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        if (typeQuery != null) {
            printTypeDependencies(typeQuery, contexts);
            return;
        }

        int totalTypes = 0;
        int bundledTypes = 0;
        int standaloneTypes = 0;
        int totalSccs = 0;
        int maxSccSize = 0;
        List<String> maxSccElements = new ArrayList<>();

        try (FileWriter standaloneWriter = new FileWriter("standalone_elements.csv");
             FileWriter bundledWriter = new FileWriter("bundled_elements.csv")) {

            standaloneWriter.write("Namespace,Element Name\n");
            bundledWriter.write("Namespace,Element Name,First Dependency In Cycle\n");

            for (Map.Entry<String, PythonCodeGeneratorContext> entry : contexts.entrySet()) {
                String namespace = entry.getKey();
                PythonCodeGeneratorContext context = entry.getValue();

                Graph<String, DefaultEdge> graph = context.getDependencyDAG();
                KosarajuStrongConnectivityInspector<String, DefaultEdge> inspector = new KosarajuStrongConnectivityInspector<>(graph);
                List<Set<String>> sccs = inspector.stronglyConnectedSets();

                totalSccs += sccs.size();
                for (Set<String> scc : sccs) {
                    if (scc.size() > maxSccSize) {
                        maxSccSize = scc.size();
                        maxSccElements = new ArrayList<>(scc);
                        Collections.sort(maxSccElements);
                    }

                    boolean isCycle = scc.size() > 1;
                    if (scc.size() == 1) {
                        String node = scc.iterator().next();
                        if (graph.containsEdge(node, node)) {
                            isCycle = true;
                        }
                    }

                    for (String elementFqn : scc) {
                        String shortName = elementFqn.contains(".") ? elementFqn.substring(elementFqn.lastIndexOf('.') + 1) : elementFqn;
                        String elementNamespace = elementFqn.contains(".") ? elementFqn.substring(0, elementFqn.lastIndexOf('.')) : namespace;

                        if (isCycle) {
                            bundledTypes++;
                            String firstDep = "";
                            for (DefaultEdge edge : graph.outgoingEdgesOf(elementFqn)) {
                                String target = graph.getEdgeTarget(edge);
                                if (scc.contains(target)) {
                                    firstDep = target;
                                    break;
                                }
                            }
                            bundledWriter.write(elementNamespace + "," + shortName + "," + firstDep + "\n");
                        } else {
                            standaloneTypes++;
                            standaloneWriter.write(elementNamespace + "," + shortName + "\n");
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        totalTypes = standaloneTypes + bundledTypes;

        System.out.println("\n--- Partitioning Metrics ---");
        System.out.println("Total Rosetta Types: " + totalTypes);
        final double percentScale = 100.0;
        System.out.println("Bundled Types: " + bundledTypes + " (" + String.format("%.1f", (bundledTypes * percentScale / Math.max(1, totalTypes))) + "%)");
        System.out.println("Standalone Types: " + standaloneTypes + " (" + String.format("%.1f", (standaloneTypes * percentScale / Math.max(1, totalTypes))) + "%)");
        System.out.println("Total SCCs: " + totalSccs);
        System.out.println("Max SCC Size: " + maxSccSize);
        System.out.println("Max SCC Elements: " + maxSccElements.stream().map(f -> f.substring(f.lastIndexOf('.') + 1)).collect(Collectors.toList()));
    }

    /**
     * BFS over incoming edges to collect all types that the given type transitively depends on.
     * Edge direction in the DAG is: dependency → dependent, so incoming edges of a node
     * point to its direct dependencies.
     */
    private static void printTypeDependencies(String typeQuery, Map<String, PythonCodeGeneratorContext> contexts) {
        // Locate the node: accept either a short name or a fully-qualified name
        String rootNode = null;
        Graph<String, DefaultEdge> rootGraph = null;
        for (PythonCodeGeneratorContext ctx : contexts.values()) {
            Graph<String, DefaultEdge> graph = ctx.getDependencyDAG();
            for (String node : graph.vertexSet()) {
                String shortName = node.contains(".") ? node.substring(node.lastIndexOf('.') + 1) : node;
                if (node.equals(typeQuery) || shortName.equals(typeQuery)) {
                    rootNode = node;
                    rootGraph = graph;
                    break;
                }
            }
            if (rootNode != null) {
                break;
            }
        }

        if (rootNode == null || rootGraph == null) {
            System.err.println("Type not found in any dependency graph: " + typeQuery);
            return;
        }

        System.out.println("Dependency graph for: " + rootNode);
        System.out.println();

        // BFS following incoming edges (dependency → dependent means incoming = dependency)
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new ArrayDeque<>();
        queue.add(rootNode);
        visited.add(rootNode);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            for (DefaultEdge edge : rootGraph.incomingEdgesOf(current)) {
                String dep = rootGraph.getEdgeSource(edge);
                if (visited.add(dep)) {
                    queue.add(dep);
                }
            }
        }

        List<String> sorted = new ArrayList<>(visited);
        Collections.sort(sorted);

        System.out.println("Total types in dependency graph: " + sorted.size());
        System.out.println();
        for (String fqn : sorted) {
            System.out.println(fqn);
        }
    }
}
