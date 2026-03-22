package com.regnosys.rosetta.generator.python;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import com.regnosys.rosetta.rosetta.RosettaEnumeration;
import com.regnosys.rosetta.rosetta.simple.Data;
import com.regnosys.rosetta.rosetta.simple.Function;

public final class PythonCodeGeneratorContext {
    /**
     * The list of subfolders.
     */
    private List<String> subfolders = null;
    /**
     * The map of Python code for classes by name.
     */
    private Map<String, CharSequence> classObjects = null;
    /**
     * The map of Python code for functions by name.
     */
    private Map<String, CharSequence> functionObjects = null;
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
     * The set of class names.
     */
    private HashSet<String> classNames = null;
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
    /**
     * The set of classes that are acyclic (standalone).
     */
    private Set<String> standaloneClasses = null;
    /**
     * All Data elements for this context.
     */
    private List<Data> allData = null;
    /**
     * All Function elements for this context.
     */
    private List<Function> allFunctions = null;
    /**
     * All Enum elements for this context.
     */
    private List<RosettaEnumeration> allEnums = null;
    /**
     * The map of child -> parent (FQN)
     */
    private Map<String, String> superTypes = null;
    /**
     * Per-function enum imports (function FQN -> set of "import X.Y.Z" strings).
     * Used to emit enum imports into standalone function files.
     */
    private Map<String, Set<String>> functionEnumImports = null;

    public PythonCodeGeneratorContext() {
        this.subfolders = new ArrayList<>();
        this.classObjects = new HashMap<>();
        this.functionObjects = new HashMap<>();
        this.dependencyDAG = new DefaultDirectedGraph<>(DefaultEdge.class);
        this.enumImports = new HashSet<>();
        this.functionNames = new HashSet<>();
        this.classNames = new HashSet<>();
        this.postDefinitionUpdates = new HashMap<>();
        this.additionalImports = new ArrayList<>();
        this.nativeFunctionNames = new LinkedHashSet<>();
        this.standaloneClasses = new HashSet<>();
        this.allData = new ArrayList<>();
        this.allFunctions = new ArrayList<>();
        this.allEnums = new ArrayList<>();
        this.superTypes = new HashMap<>();
        this.functionEnumImports = new HashMap<>();
    }

    public List<String> getSubfolders() {
        return subfolders;
    }

    public Map<String, CharSequence> getClassObjects() {
        return classObjects;
    }

    public Map<String, CharSequence> getFunctionObjects() {
        return functionObjects;
    }

    public Graph<String, DefaultEdge> getDependencyDAG() {
        return dependencyDAG;
    }

    public Set<String> getEnumImports() {
        return enumImports;
    }

    public Map<String, Set<String>> getFunctionEnumImports() {
        return functionEnumImports;
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

    public Set<String> getFunctionNames() {
        return functionNames;
    }

    public void addFunctionName(String functionName) {
        if (!functionNames.contains(functionName)) {
            functionNames.add(functionName);
        }
    }

    public boolean hasFunctionName(String functionName) {
        return functionNames.contains(functionName);
    }

    public void addClassName(String className) {
        if (!classNames.contains(className)) {
            classNames.add(className);
        }
    }

    public boolean hasClassName(String className) {
        return classNames.contains(className);
    }

    public Set<String> getClassNames() {
        return classNames;
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

    public Set<String> getStandaloneClasses() {
        return standaloneClasses;
    }

    public List<Data> getAllData() {
        return allData;
    }

    public List<Function> getAllFunctions() {
        return allFunctions;
    }

    public List<RosettaEnumeration> getAllEnums() {
        return allEnums;
    }

    public Map<String, String> getSuperTypes() {
        return superTypes;
    }
}
