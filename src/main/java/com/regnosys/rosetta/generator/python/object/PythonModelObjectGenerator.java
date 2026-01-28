package com.regnosys.rosetta.generator.python.object;

import com.regnosys.rosetta.generator.python.PythonCodeGeneratorContext;

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
import org.jgrapht.graph.GraphCycleProhibitedException;

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

    /**
     * Generate Python from the collection of Rosetta classes (of type Data).
     * 
     * @param rClasses the collection of Rosetta Classes for this model
     * @param version  the version for this collection of classes
     * @return a Map of all the generated Python indexed by the class name
     */
    public Map<String, String> generate(Iterable<Data> rClasses, String version,
            PythonCodeGeneratorContext context) {
        Graph<String, DefaultEdge> dependencyDAG = context.getDependencyDAG();
        if (dependencyDAG == null) {
            throw new RuntimeException("Dependency DAG not initialized");
        }
        Set<String> enumImports = context.getEnumImports();
        if (enumImports == null) {
            throw new RuntimeException("Enum imports not initialized");
        }

        Map<String, String> result = new HashMap<>();

        for (Data rc : rClasses) {
            RosettaModel model = (RosettaModel) rc.eContainer();
            String nameSpace = PythonCodeGeneratorUtil.getNamespace(model);

            // Generate Python for the class
            try {
                String pythonClass = generateClass(rc, nameSpace, version, enumImports);

                // construct the class name using "." as a delimiter
                String className = model.getName() + "." + rc.getName();
                result.put(className, pythonClass);

                dependencyDAG.addVertex(className);
                if (rc.getSuperType() != null) {
                    Data superClass = rc.getSuperType();
                    RosettaModel superModel = (RosettaModel) superClass.eContainer();
                    String superClassName = superModel.getName() + "." + superClass.getName();

                    addDependency(dependencyDAG, className, superClassName);
                }
            } catch (Exception e) {
                throw new RuntimeException("Error generating Python for class " + rc.getName(), e);
            }
        }
        return result;
    }

    private void addDependency(Graph<String, DefaultEdge> dependencyDAG, String className, String dependencyName) {
        dependencyDAG.addVertex(dependencyName);
        if (!className.equals(dependencyName)) {
            try {
                dependencyDAG.addEdge(dependencyName, className);
            } catch (GraphCycleProhibitedException e) {
                // Ignore cycles in this context
            }
        }
    }

    private String generateClass(Data rc, String nameSpace, String version, Set<String> enumImports) {
        if (rc == null) {
            throw new RuntimeException("Rosetta class not initialized");
        }
        if (rc.getSuperType() != null && rc.getSuperType().getName() == null) {
            throw new RuntimeException(
                    "The class superType for " + rc.getName() + " exists but its name is null");
        }
        if (enumImports == null) {
            throw new RuntimeException("Enum imports not initialized");
        }

        pythonAttributeProcessor.getImportsFromAttributes(rc, enumImports);

        return generateBody(rc, enumImports);
    }

    private String getClassMetaDataString(Data rc) {
        // generate _ALLOWED_METADATA string for the type
        RDataType rcRData = rObjectFactory.buildRDataType(rc);
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

    private String generateBody(Data rc, Set<String> enumImports) {
        RDataType rosettaDataType = rObjectFactory.buildRDataType(rc);
        Map<String, List<String>> keyRefConstraints = new HashMap<>();

        PythonCodeWriter writer = new PythonCodeWriter();

        String superClassName = (rc.getSuperType() != null)
                ? PythonCodeGeneratorUtil.createBundleObjectName(rc.getSuperType())
                : "BaseDataClass";

        writer.appendLine("class " + PythonCodeGeneratorUtil.createBundleObjectName(rc) + "(" + superClassName + "):");
        writer.indent();

        String metaData = getClassMetaDataString(rc);
        if (!metaData.isEmpty()) {
            writer.appendBlock(metaData);
        }

        pythonChoiceAliasProcessor.generateChoiceAliases(writer, rosettaDataType);

        if (rc.getDefinition() != null) {
            writer.appendLine("\"\"\"");
            writer.appendLine(rc.getDefinition());
            writer.appendLine("\"\"\"");
        }

        writer.appendLine("_FQRTN = '" + PythonCodeGeneratorUtil.createFullyQualifiedObjectName(rc) + "'");

        writer.appendBlock(pythonAttributeProcessor.generateAllAttributes(rc, keyRefConstraints));

        String constraints = keyRefConstraintsToString(keyRefConstraints);
        if (!constraints.isEmpty()) {
            writer.appendBlock(constraints);
        }

        writer.appendBlock(expressionGenerator.generateTypeOrFunctionConditions(rc, enumImports));

        return writer.toString();
    }
}
