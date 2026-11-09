/*
 * Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
 * SPDX-License-Identifier: Apache-2.0
 */
package com.regnosys.rosetta.generator.python.object;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.regnosys.rosetta.generator.python.PythonCodeGeneratorContext;
import com.regnosys.rosetta.generator.python.expressions.PythonExpressionGenerator;
import com.regnosys.rosetta.generator.python.util.PythonCodeWriter;
import com.regnosys.rosetta.generator.python.util.RuneToPythonMapper;
import com.regnosys.rosetta.rosetta.RosettaModel;
import com.regnosys.rosetta.rosetta.simple.Choice;
import com.regnosys.rosetta.rosetta.simple.Data;
import com.regnosys.rosetta.types.RAliasType;
import com.regnosys.rosetta.types.RAttribute;
import com.regnosys.rosetta.types.RChoiceType;
import com.regnosys.rosetta.types.RDataType;
import com.regnosys.rosetta.types.RObjectFactory;
import com.regnosys.rosetta.types.RType;
import com.regnosys.rosetta.types.TypeSystem;
import com.regnosys.rosetta.rosetta.expression.RosettaExpression;

import com.regnosys.rosetta.generator.python.functions.PythonFunctionDependencyProvider;

import com.regnosys.rosetta.generator.python.util.PythonDependencyCollector;
import com.regnosys.rosetta.rosetta.RosettaEnumeration;
import com.regnosys.rosetta.rosetta.simple.Function;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

/**
 * Generate Python from Rune Types
 */
public class PythonModelObjectGenerator {

    /**
     * The logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PythonModelObjectGenerator.class);

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
     * Collects the functions, types and enums an expression refers to.
     */
    @Inject
    private PythonDependencyCollector dependencyCollector;

    /**
     * Collects the imports that condition expressions need.
     */
    @Inject
    private PythonFunctionDependencyProvider functionDependencyProvider;

    /**
     * Scan Rosetta classes to build dependency graph and record super types.
     *
     * @param rClasses the collection of Rosetta Classes for this model
     * @param context  the Python code generator context
     */
    public void scan(Iterable<Data> rClasses, PythonCodeGeneratorContext context) {
        Graph<String, DefaultEdge> dependencyDAG = context.getDependencyDAG();
        for (Data rc : rClasses) {
            String className = context.getFullyQualifiedName(rc);
            context.addClassName(className);
            dependencyDAG.addVertex(className);

            if (rc.getSuperType() != null) {
                String superClassName = context.getFullyQualifiedName(rc.getSuperType());
                addDependency(dependencyDAG, className, superClassName);
                context.getSuperTypes().put(className, superClassName);
            }

            // Add dependencies for attributes
            RDataType rosettaDataType = rObjectFactory.buildRDataType(rc);
            for (RAttribute attr : rosettaDataType.getOwnAttributes()) {
                RType rt = attr.getRMetaAnnotatedType().getRType();
                if (rt instanceof RAliasType) {
                    rt = typeSystem.stripFromTypeAliases(rt);
                }
                // Normalize choice types to their underlying data type, mirroring what
                // getImportsFromAttributes does. Without this, a dependency on a choice-type
                // attribute is invisible to the DAG, the cycle goes undetected, and both
                // types are classified as standalone — causing a Python circular import.
                if (rt instanceof RChoiceType rChoiceType) {
                    rt = rChoiceType.asRDataType();
                }
                if (rt instanceof RDataType rdt) {
                    if (!RuneToPythonMapper.isRosettaBasicType(rdt)) {
                        String attrClassName = context.applyPrefix(rdt.getNamespace().toString()) + "." + rdt.getName();
                        addDependency(dependencyDAG, className, attrClassName);
                    }
                }
            }

            // Add dependencies for conditions (Data Rules)
            for (com.regnosys.rosetta.rosetta.simple.Condition condition : rc.getConditions()) {
                addExpressionDependencies(dependencyDAG, className, condition.getExpression(), context);
            }
        }
    }

    private void addExpressionDependencies(
        Graph<String, DefaultEdge> dependencyDAG,
        String className,
        RosettaExpression expression,
        PythonCodeGeneratorContext context) {
        PythonDependencyCollector.Dependencies found = dependencyCollector.collect(expression);
        found.symbols().stream()
                .filter(symbol -> symbol instanceof Data || symbol instanceof Function
                        || symbol instanceof RosettaEnumeration)
                .forEach(symbol -> addDependency(dependencyDAG, className, context.getFullyQualifiedName(symbol)));
        found.constructedTypes()
                .forEach(type -> addDependency(dependencyDAG, className, context.getFullyQualifiedName(type)));
    }

    /**
     * Generate Python from the collection of Rosetta classes (of type Data).
     *
     * @param rClasses the collection of Rosetta Classes for this model
     * @param context  the Python code generator context
     * @return a Map of all the generated Python indexed by the class name
     */
    public final Map<String, String> generate(Iterable<Data> rClasses, PythonCodeGeneratorContext context) {
        Set<String> enumImports = context.getEnumImports();
        Map<String, String> result = new HashMap<>();

        for (Data rc : rClasses) {
            String className = context.getFullyQualifiedName(rc);
            try {
                String pythonClass = generateClass(rc, enumImports, context);
                result.put(className, pythonClass);
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
            dependencyDAG.addEdge(dependencyName, className);
        }
    }

    private String generateClass(Data rc, Set<String> enumImports, PythonCodeGeneratorContext context) {
        RosettaModel model = rc.getModel();
        String className = context.applyPrefix(model.getName()) + "." + rc.getName();
        boolean isStandalone = context.getStandaloneClasses().contains(className);
        if (rc.getSuperType() != null && rc.getSuperType().getName() == null) {
            throw new RuntimeException(
                    "The class superType for " + rc.getName() + " exists but its name is null");
        }
        if (enumImports == null) {
            throw new RuntimeException("Enum imports not initialized");
        }

        if (!isStandalone) {
            pythonAttributeProcessor.getImportsFromAttributes(rc, enumImports, context, false);
            addConditionImports(rc, enumImports, context);
        }

        return generateBody(rc, context, isStandalone);
    }

    /**
     * Adds the imports used by the type's conditions, such as an enum that only they refer to.
     */
    private void addConditionImports(Data rc, Set<String> imports, PythonCodeGeneratorContext context) {
        String sourceNamespace = context.applyPrefix(rc.getModel().getName());
        for (com.regnosys.rosetta.rosetta.simple.Condition condition : rc.getConditions()) {
            functionDependencyProvider.addDependencies(condition.getExpression(), imports, sourceNamespace, context);
        }
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

    private String generateBody(Data rc, PythonCodeGeneratorContext context, boolean isStandalone) {
        PythonExpressionGenerator expressionGenerator = expressionGeneratorProvider.get();
        expressionGenerator.setContext(context);
        expressionGenerator.setWritingStandaloneFile(isStandalone);
        RDataType rosettaDataType = rObjectFactory.buildRDataType(rc);
        Map<String, List<String>> keyRefConstraints = new HashMap<>();

        PythonCodeWriter writer = new PythonCodeWriter();

        String superClassName;
        if (rc.getSuperType() != null) {
            String superFQN = context.getFullyQualifiedName(rc.getSuperType());
            if (isStandalone || context.getStandaloneClasses().contains(superFQN)) {
                // Modules use the class name directly (with an import)
                superClassName = rc.getSuperType().getName();
            } else {
                // Internal bundle uses flattened name
                superClassName = context.getBundleObjectName(rc.getSuperType());
            }
        } else {
            superClassName = "BaseDataClass";
        }

        if (isStandalone) {
            // Standard imports come from createImports() in processDAG — only add type-specific imports here
            if (rc.getSuperType() != null) {
                String superFQN = context.getFullyQualifiedName(rc.getSuperType());
                String superClassNameInImport = rc.getSuperType().getName();
                writer.appendLine(String.format("from %s import %s", superFQN, superClassNameInImport));
            }
            // Add attribute-level imports (all Data types safe for standalone files)
            Set<String> imports = new java.util.HashSet<>();
            pythonAttributeProcessor.getImportsFromAttributes(rc, imports, context, true);
            addConditionImports(rc, imports, context);
            for (String imp : imports) {
                writer.appendLine(imp);
            }
            writer.newLine();
        }

        String classNameDefinition = isStandalone ? rc.getName() : context.getBundleObjectName(rc);
        writer.appendLine("class " + classNameDefinition + "(" + superClassName + "):");
        writer.indent();

        // Every class defers schema compilation. Standalone classes build on first use, which
        // avoids compiling schemas that are never used. Bundled classes wait until all class
        // bodies are defined: by the time Phase 3 model_rebuild(force=True) runs, all cyclic
        // types are fully available, so Pydantic can build schemas ~4× faster (~1.8 GB / ~5s for
        // CDM vs ~7.9 GB / ~17s without defer_build). Phase 3 is still required for bundled
        // classes: Pydantic builds None-typed placeholder schemas eagerly even with
        // defer_build=True, so an explicit model_rebuild is needed to pick up Phase 2 annotation
        // updates.
        writer.appendLine("model_config = ConfigDict(defer_build=True)");

        // A generated choice looks the same as a type with optional attributes and a one-of
        // condition, but its JSON format differs, so the runtime has to be told which it is.
        if (rc instanceof Choice) {
            writer.appendLine("_RUNE_CHOICE: ClassVar[bool] = True");
        }

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

        if (!isStandalone) {
            writer.appendLine("_FQRTN: ClassVar[str] = '" + RuneToPythonMapper.getFullyQualifiedName(rc) + "'");
        }

        AttributeProcessingResult attrResult = pythonAttributeProcessor.generateAllAttributes(rc, keyRefConstraints, context);
        writer.appendBlock(attrResult.getAttributeCode());

        if (!isStandalone) {
            String bundleName = context.getBundleObjectName(rc);
            List<String> updates = attrResult.getAnnotationUpdates().stream()
                    .map(update -> bundleName + "." + update)
                    .collect(Collectors.toList());

            context.addPostDefinitionUpdates(bundleName, updates);
        }

        String constraints = keyRefConstraintsToString(keyRefConstraints);
        if (!constraints.isEmpty()) {
            writer.appendBlock(constraints);
        }

        writer.appendBlock(expressionGenerator.generateTypeConditions(rc));

        return writer.toString();
    }
}
