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
    private Set<String> imports = null;

    public void beforeAllGenerate() {
        dependencyDAG = new DirectedAcyclicGraph<>(DefaultEdge.class);
        imports = new HashSet<>();
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
        Map<String, String> result = new HashMap<>();

        for (Data rosettaClass : rosettaClasses) {
            RosettaModel model = (RosettaModel) rosettaClass.eContainer();
            String nameSpace = PythonCodeGeneratorUtil.getNamespace(model);

            // Generate Python for the class and ensure 4-space indentation
            String pythonClass = generateClass(rosettaClass, nameSpace, version).replace("\t", "    ");

            // use "." as a delimiter to preserve the use of "_" in the name
            String className = model.getName() + "." + rosettaClass.getName();
            result.put(className, pythonClass);

            if (dependencyDAG != null) {
                dependencyDAG.addVertex(className);
                if (rosettaClass.getSuperType() != null) {
                    Data superClass = rosettaClass.getSuperType();
                    RosettaModel superModel = (RosettaModel) superClass.eContainer();
                    String superClassName = superModel.getName() + "." + superClass.getName();
                    addDependency(className, superClassName);
                }
            }
        }
        return result;
    }

    public Map<String, String> afterAllGenerate(String namespace, Map<String, ? extends CharSequence> objects) {
        // create bundle and stub classes
        Map<String, String> result = new HashMap<>();
        if (dependencyDAG != null) {
            PythonCodeWriter bundleWriter = new PythonCodeWriter();
            TopologicalOrderIterator<String, DefaultEdge> topologicalOrderIterator = new TopologicalOrderIterator<>(
                    dependencyDAG);

            // for each element in the ordered collection add the generated class to the
            // bundle and add a stub class to the results
            boolean isFirst = true;
            while (topologicalOrderIterator.hasNext()) {
                if (isFirst) {
                    bundleWriter.appendBlock(PythonCodeGeneratorUtil.createImports());
                    List<String> sortedImports = new ArrayList<>(imports);
                    Collections.sort(sortedImports);
                    for (String imp : sortedImports) {
                        bundleWriter.appendLine(imp);
                    }
                    isFirst = false;
                }
                String name = topologicalOrderIterator.next();
                CharSequence object = objects.get(name);
                if (object != null) {
                    // append the class to the bundle
                    bundleWriter.newLine();
                    bundleWriter.newLine();
                    bundleWriter.appendBlock(object.toString());

                    // create the stub
                    String[] parsedName = name.split("\\.");
                    String stubFileName = "src/" + String.join("/", parsedName) + ".py";

                    PythonCodeWriter stubWriter = new PythonCodeWriter();
                    stubWriter.appendLine("# pylint: disable=unused-import");
                    stubWriter.append("from ");
                    stubWriter.append(parsedName[0]);
                    stubWriter.append("._bundle import ");
                    stubWriter.append(name.replace('.', '_'));
                    stubWriter.append(" as ");
                    stubWriter.append(parsedName[parsedName.length - 1]);
                    stubWriter.newLine();
                    stubWriter.newLine();
                    stubWriter.appendLine("# EOF");

                    result.put(stubFileName, stubWriter.toString());
                }
            }
            bundleWriter.newLine();
            bundleWriter.appendLine("# EOF");
            result.put("src/" + namespace + "/_bundle.py", bundleWriter.toString());
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

        Set<String> importsFound = pythonAttributeProcessor.getImportsFromAttributes(rosettaClass);
        imports.addAll(importsFound);
        expressionGenerator.setImportsFound(new ArrayList<>(importsFound));

        return generateBody(rosettaClass);
    }

    private String getClassMetaDataString(Data rosettaClass) {
        // generate _ALLOWED_METADATA string for the type
        RDataType rcRData = rObjectFactory.buildRDataType(rosettaClass);
        StringBuilder builder = new StringBuilder();
        boolean first = true;

        for (var metaData : rcRData.getMetaAttributes()) {
            if (first) {
                first = false;
                builder.append("_ALLOWED_METADATA = {");
            } else {
                builder.append(", ");
            }
            switch (metaData.getName()) {
                case "key" -> builder.append("'@key', '@key:external'");
                case "scheme" -> builder.append("'@scheme'");
            }
        }
        if (!first) {
            builder.append("}\n");
        }
        return builder.toString();
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
        String choiceAliases = pythonChoiceAliasProcessor.generateChoiceAliasesAsString(rosettaDataType);
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

        if (!choiceAliases.isEmpty()) {
            writer.appendLine(choiceAliases);
        }

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
