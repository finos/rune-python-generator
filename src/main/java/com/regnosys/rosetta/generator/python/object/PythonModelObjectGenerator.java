package com.regnosys.rosetta.generator.python.object;

import com.regnosys.rosetta.generator.python.expressions.PythonExpressionGenerator;
import com.regnosys.rosetta.generator.python.util.PythonCodeGeneratorUtil;
import com.regnosys.rosetta.generator.python.util.PythonCodeWriter;
import com.regnosys.rosetta.rosetta.RosettaModel;
import com.regnosys.rosetta.rosetta.simple.Data;
import com.regnosys.rosetta.types.RDataType;
import com.regnosys.rosetta.types.RObjectFactory;
import jakarta.inject.Inject;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.jgrapht.graph.GraphCycleProhibitedException;
import org.jgrapht.traverse.TopologicalOrderIterator;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Generate Python from Rune Types
 */
public class PythonModelObjectGenerator {

    @Inject
    private RObjectFactory rObjectFactory;

    @Inject
    private PythonExpressionGenerator expressionGenerator;

    @Inject
    private PythonAttributeProcessor pythonAttributeProcessor;

    @Inject
    private PythonChoiceAliasProcessor pythonChoiceAliasProcessor;

    private Graph<String, DefaultEdge> dependencyDAG = null;
    private Set<String> enumImports = null;

    public void beforeAllGenerate(Graph<String, DefaultEdge> dependencyDAGIn, Set<String> enumImportsIn) {
        dependencyDAG = dependencyDAGIn;
        enumImports = enumImportsIn;
    }

    /**
     * Generate Python from the collection of Rosetta classes (of type Data).
     * Note: this function updates the dependency graph used by afterAllGenerate to
     * create the bundle
     * 
     * @param rosettaClasses the collection of Rosetta Classes for this model
     * @param version        the version for this collection of classes
     * @return a Map of all the generated Python indexed by the class name
     */
    public Map<String, String> generate(Iterable<Data> rosettaClasses, String version) {
        if (dependencyDAG == null) {
            throw new RuntimeException("Dependency DAG not initialized");
        }
        if (enumImports == null) {
            throw new RuntimeException("Enum imports not initialized");
        }
        Map<String, String> result = new HashMap<>();

        for (Data rosettaClass : rosettaClasses) {
            RosettaModel model = (RosettaModel) rosettaClass.eContainer();
            String nameSpace = PythonCodeGeneratorUtil.getNamespace(model);

            // Generate Python for the class
            String pythonClass = generateClass(rosettaClass, nameSpace, version);

            // construct the class name using "." as a delimiter
            String className = model.getName() + "." + rosettaClass.getName();
            result.put(className, pythonClass);

            dependencyDAG.addVertex(className);
            if (rosettaClass.getSuperType() != null) {
                Data superClass = rosettaClass.getSuperType();
                RosettaModel superModel = (RosettaModel) superClass.eContainer();
                String superClassName = superModel.getName() + "." + superClass.getName();
                addDependency(className, superClassName);
            }
        }
        return result;
    }

    private void addDependency(String className, String dependencyName) {
        dependencyDAG.addVertex(dependencyName);
        if (!className.equals(dependencyName)) {
            try {
                dependencyDAG.addEdge(dependencyName, className);
            } catch (GraphCycleProhibitedException e) {
                // Ignore cycles in this context
            }
        }
    }

    private String generateClass(Data rosettaClass, String nameSpace, String version) {
        if (rosettaClass.getSuperType() != null && rosettaClass.getSuperType().getName() == null) {
            throw new RuntimeException(
                    "The class superType for " + rosettaClass.getName() + " exists but its name is null");
        }

        Set<String> enumImportsFound = pythonAttributeProcessor.getImportsFromAttributes(rosettaClass);
        enumImports.addAll(enumImportsFound);
        expressionGenerator.setImportsFound(new ArrayList<>(enumImportsFound));

        return generateBody(rosettaClass);
    }

    private String getClassMetaDataString(Data rosettaClass) {
        // generate _ALLOWED_METADATA string for the type
        RDataType rcRData = rObjectFactory.buildRDataType(rosettaClass);
        PythonCodeWriter writer = new PythonCodeWriter();
        boolean first = true;

        for (var metaData : rcRData.getMetaAttributes()) {
            if (first) {
                first = false;
                writer.append("_ALLOWED_METADATA = {");
            } else {
                writer.append(", ");
            }
            switch (metaData.getName()) {
                case "key" -> writer.append("'@key', '@key:external'");
                case "scheme" -> writer.append("'@scheme'");
            }
        }
        if (!first) {
            writer.append("}");
            writer.newLine();
        }
        return writer.toString();
    }

    private String keyRefConstraintsToString(Map<String, List<String>> keyRefConstraints) {
        if (keyRefConstraints.isEmpty()) {
            return "";
        }
        PythonCodeWriter writer = new PythonCodeWriter();
        writer.newLine();
        writer.appendLine("_KEY_REF_CONSTRAINTS = {");
        writer.indent();

        List<Map.Entry<String, List<String>>> entries = new ArrayList<>(keyRefConstraints.entrySet());
        for (int i = 0; i < entries.size(); i++) {
            Map.Entry<String, List<String>> entry = entries.get(i);
            String items = entry.getValue().stream()
                    .map(item -> "'" + item + "'")
                    .collect(Collectors.joining(", "));

            String line = "'" + entry.getKey() + "': {" + items + "}";
            if (i < entries.size() - 1) {
                line += ",";
            }
            writer.appendLine(line);
        }

        writer.unindent();
        writer.appendLine("}");
        return writer.toString();
    }

    private String getFullyQualifiedName(Data rosettaClass) {
        RosettaModel model = (RosettaModel) rosettaClass.eContainer();
        return model.getName() + "." + rosettaClass.getName();
    }

    private String getBundleClassName(Data rosettaClass) {
        return getFullyQualifiedName(rosettaClass).replace(".", "_");
    }

    private String generateBody(Data rosettaClass) {
        RDataType rosettaDataType = rObjectFactory.buildRDataType(rosettaClass);
        Map<String, List<String>> keyRefConstraints = new HashMap<>();

        PythonCodeWriter writer = new PythonCodeWriter();

        String superClassName = (rosettaClass.getSuperType() != null)
                ? getBundleClassName(rosettaClass.getSuperType())
                : "BaseDataClass";

        writer.appendLine("class " + getBundleClassName(rosettaClass) + "(" + superClassName + "):");
        writer.indent();

        String metaData = getClassMetaDataString(rosettaClass);
        if (!metaData.isEmpty()) {
            writer.appendBlock(metaData);
        }

        pythonChoiceAliasProcessor.generateChoiceAliases(writer, rosettaDataType);

        if (rosettaClass.getDefinition() != null) {
            writer.appendLine("\"\"\"");
            writer.appendLine(rosettaClass.getDefinition());
            writer.appendLine("\"\"\"");
        }

        writer.appendLine("_FQRTN = '" + getFullyQualifiedName(rosettaClass) + "'");

        writer.appendBlock(pythonAttributeProcessor.generateAllAttributes(rosettaClass, keyRefConstraints));

        String constraints = keyRefConstraintsToString(keyRefConstraints);
        if (!constraints.isEmpty()) {
            writer.appendBlock(constraints);
        }

        writer.appendBlock(expressionGenerator.generateTypeOrFunctionConditions(rosettaClass));

        return writer.toString();
    }
}
