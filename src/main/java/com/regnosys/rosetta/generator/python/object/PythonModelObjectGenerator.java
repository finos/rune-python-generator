package com.regnosys.rosetta.generator.python.object;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.GraphCycleProhibitedException;

import com.regnosys.rosetta.generator.python.PythonCodeGeneratorContext;
import com.regnosys.rosetta.generator.python.expressions.PythonExpressionGenerator;
import com.regnosys.rosetta.generator.python.util.PythonCodeWriter;
import com.regnosys.rosetta.generator.python.util.RuneToPythonMapper;
import com.regnosys.rosetta.rosetta.RosettaModel;
import com.regnosys.rosetta.rosetta.simple.Data;
import com.regnosys.rosetta.types.RAliasType;
import com.regnosys.rosetta.types.RAttribute;
import com.regnosys.rosetta.types.RDataType;
import com.regnosys.rosetta.types.RObjectFactory;
import com.regnosys.rosetta.types.RType;
import com.regnosys.rosetta.types.TypeSystem;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

/**
 * Generate Python from Rune Types
 */
public class PythonModelObjectGenerator {

    /**
     * The R object factory.
     */
    @Inject
    private RObjectFactory rObjectFactory;
    
    /**
     * The type system.
     */
    @Inject
    private TypeSystem typeSystem;



    /**
     * The Python attribute processor.
     */
    @Inject
    private PythonAttributeProcessor pythonAttributeProcessor;

    /**
     * The Python choice alias processor.
     */
    @Inject
    private PythonChoiceAliasProcessor pythonChoiceAliasProcessor;

    /**
     * The Python expression generator provider.
     */
    @Inject
    private Provider<PythonExpressionGenerator> expressionGeneratorProvider;

    /**
     * Generate Python from the collection of Rosetta classes (of type Data).
     * 
     * @param rClasses the collection of Rosetta Classes for this model
     * @param context  the Python code generator context
     * @return a Map of all the generated Python indexed by the class name
     */
    public Map<String, String> generate(Iterable<Data> rClasses,
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

        // 1. First pass: Add all vertices and inheritance dependencies
        // This ensures inheritance edges are prioritized over attribute edges in case of cycles.
        for (Data rc : rClasses) {
            RosettaModel model = (RosettaModel) rc.eContainer();
            String className = model.getName() + "." + rc.getName();
            context.addClassName(className);
            dependencyDAG.addVertex(className);
            
            if (rc.getSuperType() != null) {
                Data superClass = rc.getSuperType();
                RosettaModel superModel = (RosettaModel) superClass.eContainer();
                String superClassName = superModel.getName() + "." + superClass.getName();
                addDependency(dependencyDAG, className, superClassName);
            }
        }

        // 2. Second pass: Generate classes and add attribute dependencies
        for (Data rc : rClasses) {
            RosettaModel model = (RosettaModel) rc.eContainer();
            String className = model.getName() + "." + rc.getName();
            
            try {
                String pythonClass = generateClass(rc, enumImports, context);
                result.put(className, pythonClass);

                // Add dependencies for attributes
                RDataType rosettaDataType = rObjectFactory.buildRDataType(rc);
                for (RAttribute attr : rosettaDataType.getOwnAttributes()) {
                    RType rt = attr.getRMetaAnnotatedType().getRType();
                    if (rt instanceof RAliasType) {
                        rt = typeSystem.stripFromTypeAliases(rt);
                    }
                    if (rt instanceof RDataType rdt) {
                        if (!RuneToPythonMapper.isRosettaBasicType(rdt)) {
                            String attrClassName = rdt.getNamespace().toString() + "." + rdt.getName();
                            addDependency(dependencyDAG, className, attrClassName);
                        }
                    }
                }

            } catch (Exception e) {
                throw new RuntimeException("Error generating Python for class " + rc.getName(), e);
            }
        }
        return result;
    }


    private void addDependency(
        Graph<String, DefaultEdge> dependencyDAG,
        String className,
        String dependencyName) {
        dependencyDAG.addVertex(dependencyName);
        if (!className.equals(dependencyName)) {
            try {
                dependencyDAG.addEdge(dependencyName, className);
            } catch (GraphCycleProhibitedException e) {
                // Ignore cycles in this context
            }
        }
    }

    private String generateClass(Data rc, Set<String> enumImports,
            PythonCodeGeneratorContext context) {
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

        return generateBody(rc, context);
    }

    /**
     * Generates the _ALLOWED_METADATA string for the type.
     * 
     * @param rc the Rosetta class
     * @return the _ALLOWED_METADATA string
     */
    private String getClassMetaDataString(Data rc) {
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
            String metaDataName = metaData.getName();
            if (metaDataName.equals("key") || metaDataName.equals("id")) {
                writer.append("'@key', '@key:external'");
            } else if (metaDataName.equals("scheme")) {
                writer.append("'@scheme'");
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

    private String generateBody(Data rc, PythonCodeGeneratorContext context) {
        PythonExpressionGenerator expressionGenerator = expressionGeneratorProvider.get();
        RDataType rosettaDataType = rObjectFactory.buildRDataType(rc);
        Map<String, List<String>> keyRefConstraints = new HashMap<>();

        PythonCodeWriter writer = new PythonCodeWriter();

        String superClassName = (rc.getSuperType() != null)
                ? RuneToPythonMapper.getBundleObjectName(rc.getSuperType())
                : "BaseDataClass";

        writer.appendLine("class " + RuneToPythonMapper.getBundleObjectName(rc) + "(" + superClassName + "):");
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

        writer.appendLine("_FQRTN = '" + RuneToPythonMapper.getFullyQualifiedName(rc) + "'");

        AttributeProcessingResult attrResult = pythonAttributeProcessor.generateAllAttributes(rc, keyRefConstraints);
        writer.appendBlock(attrResult.getAttributeCode());

        String bundleName = RuneToPythonMapper.getBundleObjectName(rc);
        List<String> updates = attrResult.getAnnotationUpdates().stream()
                .map(update -> bundleName + "." + update)
                .collect(Collectors.toList());

        context.addPostDefinitionUpdates(bundleName, updates);

        String constraints = keyRefConstraintsToString(keyRefConstraints);
        if (!constraints.isEmpty()) {
            writer.appendBlock(constraints);
        }

        writer.appendBlock(expressionGenerator.generateTypeConditions(rc));

        return writer.toString();
    }
}
