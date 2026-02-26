package com.regnosys.rosetta.generator.python;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedAcyclicGraph;

public final class PythonCodeGeneratorContext {
    /**
     * The list of subfolders.
     */
    private List<String> subfolders = null;
    /**
     * The map of Python code for types by nameSpace, by type name.
     */
    private Map<String, CharSequence> objects = null;
    /**
     * The dependency DAG.
     */
    private Graph<String, DefaultEdge> dependencyDAG = null;
    /**
     * The set of enum imports.
     */
    private Set<String> enumImports = null;
    /**
     * The set of function names.
     */
    private HashSet<String> functionNames = null;
    /**
     * The map of post definition updates.
     */
    private Map<String, List<String>> postDefinitionUpdates = null;
    /**
     * Additional imports
     */
    private List<String> additionalImports = null;
    /**
     * The set of native function names.
     */
    private LinkedHashSet<String> nativeFunctionNames = null;

    public PythonCodeGeneratorContext() {
        this.subfolders = new ArrayList<>();
        this.objects = new HashMap<>();
        this.dependencyDAG = new DirectedAcyclicGraph<>(DefaultEdge.class);
        this.enumImports = new HashSet<>();
        this.functionNames = new HashSet<>();
        this.postDefinitionUpdates = new HashMap<>();
        this.additionalImports = new ArrayList<>();
        this.nativeFunctionNames = new LinkedHashSet<>();
    }

    public List<String> getSubfolders() {
        return subfolders;
    }

    public Map<String, CharSequence> getObjects() {
        return objects;
    }

    public Graph<String, DefaultEdge> getDependencyDAG() {
        return dependencyDAG;
    }

    public Set<String> getEnumImports() {
        return enumImports;
    }

    public Map<String, List<String>> getPostDefinitionUpdates() {
        return postDefinitionUpdates;
    }

    public void addPostDefinitionUpdates(String className, List<String> updates) {
        postDefinitionUpdates.computeIfAbsent(className, k -> new ArrayList<>()).addAll(updates);
    }

    public void addSubfolder(String subfolder) {
        if (!subfolders.contains(subfolder)) {
            subfolders.add(subfolder);
        }
    }

    public void addFunctionName(String functionName) {
        if (!functionNames.contains(functionName)) {
            functionNames.add(functionName);
        }
    }

    public boolean hasFunctionName(String functionName) {
        return functionNames.contains(functionName);
    }

    public boolean hasFunctions() {
        return !functionNames.isEmpty();
    }

    public List<String> getAdditionalImports() {
        return additionalImports;
    }

    public void addAdditionalImport(String importStatement) {
        if (!additionalImports.contains(importStatement)) {
            additionalImports.add(importStatement);
        }
    }

    public boolean hasNativeFunctions() {
        return !nativeFunctionNames.isEmpty();
    }
    public void addNativeFunctionName(String nativeFunctionName) {
        this.nativeFunctionNames.add(nativeFunctionName);
    }

    public Set<String> getNativeFunctionNames() {
        return nativeFunctionNames;
    }
}
