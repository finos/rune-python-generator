package com.regnosys.rosetta.generator.python;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedAcyclicGraph;

public class PythonCodeGeneratorContext {
    private List<String> subfolders = null;
    private Map<String, CharSequence> objects = null; // Python code for types by nameSpace, by type name
    private Graph<String, DefaultEdge> dependencyDAG = null;
    private Set<String> enumImports = null;
    private HashSet<String> functionNames = null;

    public PythonCodeGeneratorContext() {
        this.subfolders = new ArrayList<>();
        this.objects = new HashMap<>();
        this.dependencyDAG = new DirectedAcyclicGraph<>(DefaultEdge.class);
        this.enumImports = new HashSet<>();
        this.functionNames = new HashSet<>();
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
}
