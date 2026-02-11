package com.regnosys.rosetta.generator.python.functions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.GraphCycleProhibitedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.regnosys.rosetta.generator.python.PythonCodeGeneratorContext;
import com.regnosys.rosetta.generator.python.expressions.PythonExpressionGenerator;
import com.regnosys.rosetta.generator.python.util.PythonCodeWriter;
import com.regnosys.rosetta.generator.python.util.RuneToPythonMapper;
import com.regnosys.rosetta.rosetta.RosettaEnumeration;
import com.regnosys.rosetta.rosetta.RosettaFeature;
import com.regnosys.rosetta.rosetta.RosettaModel;
import com.regnosys.rosetta.rosetta.RosettaNamed;
import com.regnosys.rosetta.rosetta.RosettaTyped;
import com.regnosys.rosetta.rosetta.expression.RosettaConstructorExpression;
import com.regnosys.rosetta.rosetta.expression.RosettaSymbolReference;
import com.regnosys.rosetta.rosetta.simple.Annotated;
import com.regnosys.rosetta.rosetta.simple.AssignPathRoot;
import com.regnosys.rosetta.rosetta.simple.Attribute;
import com.regnosys.rosetta.rosetta.simple.Condition;
import com.regnosys.rosetta.rosetta.simple.Data;
import com.regnosys.rosetta.rosetta.simple.Function;
import com.regnosys.rosetta.rosetta.simple.Operation;
import com.regnosys.rosetta.rosetta.simple.Segment;
import com.regnosys.rosetta.rosetta.simple.ShortcutDeclaration;
import com.regnosys.rosetta.types.RObjectFactory;

import jakarta.inject.Inject;

public final class PythonFunctionGenerator {

    /**
     * The logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PythonFunctionGenerator.class);

    /**
     * The function dependency provider.
     */
    @Inject
    private PythonFunctionDependencyProvider functionDependencyProvider;

    /**
     * The expression generator.
     */
    @Inject
    private PythonExpressionGenerator expressionGenerator;

    /**
     * The object factory.
     */
    @Inject
    private RObjectFactory rObjectFactory;

    /**
     * Generates Python code for a collection of Rosetta functions.
     * 
     * @param functions the collection of Rosetta functions to generate
     * @param context   the Python code generator context
     * @return a Map of all the generated Python indexed by the file name
     */

    public Map<String, String> generate(Iterable<Function> functions,
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

        for (Function function : functions) {
            RosettaModel model = (RosettaModel) function.eContainer();
            if (model == null) {
                LOGGER.warn("Function {} has no container, skipping", function.getName());
                continue;
            }
            // String nameSpace = PythonCodeGeneratorUtil.getNamespace(model);
            try {
                String pythonFunction = generateFunction(function, enumImports);

                String functionName = RuneToPythonMapper.getFullyQualifiedObjectName(function);
                result.put(functionName, pythonFunction);
                dependencyDAG.addVertex(functionName);
                context.addFunctionName(functionName);

                addFunctionDependencies(dependencyDAG, functionName, function);
            } catch (Exception ex) {
                LOGGER.error("Exception occurred generating function {}", function.getName(), ex);
                throw new RuntimeException("Error generating Python for function " + function.getName(), ex);
            }
        }
        return result;
    }

    private void addFunctionDependencies(Graph<String, DefaultEdge> dependencyDAG, String functionName,
            Function function) {
        Set<RosettaNamed> dependencies = new HashSet<>();

        function.getInputs().forEach(input -> {
            if (input.getTypeCall() != null && input.getTypeCall().getType() != null) {
                dependencies.add(input.getTypeCall().getType());
            }
        });
        if (function.getOutput() != null && function.getOutput().getTypeCall() != null
                && function.getOutput().getTypeCall().getType() != null) {
            dependencies.add(function.getOutput().getTypeCall().getType());
        }

        Iterator<?> allContents = function.eAllContents();
        while (allContents.hasNext()) {
            Object content = allContents.next();
            if (content instanceof RosettaSymbolReference ref) {
                if (ref.getSymbol() instanceof Function || ref.getSymbol() instanceof Data
                        || ref.getSymbol() instanceof RosettaEnumeration) {
                    dependencies.add((RosettaNamed) ref.getSymbol());
                }
            } else if (content instanceof RosettaConstructorExpression cons) {
                if (cons.getTypeCall() != null && cons.getTypeCall().getType() != null) {
                    dependencies.add(cons.getTypeCall().getType());
                }
            }
        }

        for (RosettaNamed dep : dependencies) {
            String depName = "";
            if (dep instanceof Data data) {
                depName = rObjectFactory.buildRDataType(data).getQualifiedName().toString();
            } else if (dep instanceof Function function1) {
                depName = RuneToPythonMapper.getFullyQualifiedObjectName(function1);
            } else if (dep instanceof RosettaEnumeration rosettaEnumeration) {
                depName = rObjectFactory.buildREnumType(rosettaEnumeration).getQualifiedName().toString();
            }

            if (!depName.isEmpty() && !functionName.equals(depName)) {
                addDependency(dependencyDAG, functionName, depName);
            }
        }
    }

    private void addDependency(Graph<String, DefaultEdge> dependencyDAG, String className, String dependencyName) {
        dependencyDAG.addVertex(dependencyName);
        if (!className.equals(dependencyName)) {
            try {
                dependencyDAG.addEdge(dependencyName, className);
            } catch (GraphCycleProhibitedException e) {
                // Ignore
            }
        }
    }

    private boolean isCodeImplementation(Function function) {
        Annotated annotated = (Annotated) function;
        boolean hasCodeImplementationAnnotation = annotated.getAnnotations()
                .stream()
                .map(aRef -> aRef.getAnnotation())
                .anyMatch(a -> "codeImplementation".equals(a.getName()));
        return hasCodeImplementationAnnotation;
    }

    private String generateFunction(Function function, Set<String> enumImports) {
        if (function == null) {
            throw new RuntimeException("Function is null");
        }
        if (enumImports == null) {
            throw new RuntimeException("Enum imports is null");
        }
        enumImports.addAll(collectFunctionDependencies(function));

        PythonCodeWriter writer = new PythonCodeWriter();

        writer.appendLine("");
        writer.appendLine("");

        writer.appendLine("@replaceable");
        writer.appendLine("@validate_call");
        writer.appendLine("def " + RuneToPythonMapper.getBundleObjectName(function) + generateInputs(function) + ":");
        writer.indent();

        writer.appendBlock(generateDescription(function));

        if (!function.getConditions().isEmpty()) {
            writer.appendLine("_pre_registry = {}");
        }
        if (!function.getPostConditions().isEmpty()) {
            writer.appendLine("_post_registry = {}");
        }
        writer.appendLine("self = inspect.currentframe()");
        writer.appendLine("");
        if (function.getConditions().isEmpty()) {
            writer.appendLine("");
        }

        writer.appendBlock(generateConditions(function));

        boolean isCodeImplementation = isCodeImplementation(function);
        if (isCodeImplementation) {
            writer.appendLine(generateExternalFunctionCall(function));
        } else {
            int[] level = {0};
            writer.appendBlock(generateAlias(function, level));
            writer.appendBlock(generateOperations(function, level));
        }

        writer.appendBlock(generateOutput(function, isCodeImplementation));

        writer.unindent();
        writer.newLine();

        return writer.toString();
    }

    private String generateExternalFunctionCall(Function function) {
        String outputName = function.getOutput().getName();
        String qualifiedName = RuneToPythonMapper.getFullyQualifiedObjectName(function);
        String arguments = function.getInputs().stream()
                .map(RosettaNamed::getName)
                .collect(Collectors.joining(", "));

        return String.format("%s = rune_execute_native('%s'%s)",
                outputName,
                qualifiedName,
                arguments.isEmpty() ? "" : ", " + arguments);
    }

    private String generateInputs(Function function) {
        String inputs = function.getInputs().stream()
                .map(input -> {
                    String inputBundleName = RuneToPythonMapper.getBundleObjectName(input.getTypeCall().getType());
                    String inputType = RuneToPythonMapper.formatPythonType(
                            inputBundleName,
                            input.getCard().getInf(),
                            input.getCard().getSup(),
                            true);
                    return input.getName() + ": " + inputType;
                })
                .collect(Collectors.joining(", "));

        StringBuilder result = new StringBuilder("(").append(inputs).append(") -> ");
        Attribute output = function.getOutput();
        if (output != null) {
            String outputBundleName = RuneToPythonMapper.getBundleObjectName(output.getTypeCall().getType());
            String outputType = RuneToPythonMapper.formatPythonType(
                    outputBundleName,
                    1, // Force min=1 to suppress Optional/| None for return types
                    output.getCard().getSup(),
                    true);
            result.append(outputType);
        } else {
            result.append("None");
        }
        return result.toString();
    }

    private String generateOutput(Function function, boolean isCodeImplementation) {
        Attribute output = function.getOutput();
        if (output != null) {
            PythonCodeWriter writer = new PythonCodeWriter();
            if (function.getOperations().isEmpty() && function.getShortcuts().isEmpty() && !isCodeImplementation) {
                writer.appendLine(output.getName() + " = rune_resolve_attr(self, \"" + output.getName() + "\")");
            }
            String postConds = generatePostConditions(function);
            if (!postConds.isEmpty()) {
                writer.appendLine("");
                writer.appendBlock(postConds);
                writer.appendLine("");
            } else {
                writer.appendLine("");
                writer.appendLine("");
            }

            writer.appendLine("return " + output.getName());
            return writer.toString();
        }
        return "";
    }

    private String generateDescription(Function function) {
        List<Attribute> inputs = function.getInputs();
        Attribute output = function.getOutput();
        String description = function.getDefinition();

        PythonCodeWriter writer = new PythonCodeWriter();
        writer.appendLine("\"\"\"");
        if (description != null) {
            writer.appendLine(description);
        }
        writer.appendLine("");
        writer.appendLine("Parameters");
        writer.appendLine("----------");
        for (Attribute input : inputs) {
            String paramName = RuneToPythonMapper.formatPythonType(
                    RuneToPythonMapper.getFullyQualifiedObjectName(input.getTypeCall().getType()),
                    1, // Force min=1 to match legacy docstring format (no Optional)
                    input.getCard().getSup(),
                    true);
            writer.appendLine(input.getName() + " : " + paramName);
            if (input.getDefinition() != null) {
                writer.appendLine(input.getDefinition());
            }
            writer.appendLine("");
        }
        writer.appendLine("Returns");
        writer.appendLine("-------");
        if (output != null) {
            String paramName = RuneToPythonMapper.formatPythonType(
                    RuneToPythonMapper.getFullyQualifiedObjectName(output.getTypeCall().getType()),
                    1, // Force min=1 to match legacy docstring format (no Optional)
                    output.getCard().getSup(),
                    true);

            writer.appendLine(output.getName() + " : " + paramName);
        } else {
            writer.appendLine("No Return");
        }
        writer.appendLine("");
        writer.appendLine("\"\"\"");
        return writer.toString();
    }

    private Set<String> collectFunctionDependencies(Function function) {
        Set<String> enumImports = new HashSet<>();
        function.getShortcuts().forEach(
                shortcut -> functionDependencyProvider.addDependencies(shortcut.getExpression(), enumImports));
        function.getOperations().forEach(
                operation -> functionDependencyProvider.addDependencies(operation.getExpression(), enumImports));

        List<Condition> allConditions = new ArrayList<>(function.getConditions());
        allConditions.addAll(function.getPostConditions());
        allConditions.forEach(
                condition -> functionDependencyProvider.addDependencies(condition.getExpression(), enumImports));

        function.getInputs().forEach(input -> {
            if (input.getTypeCall() != null && input.getTypeCall().getType() != null) {
                functionDependencyProvider.addDependencies(input.getTypeCall().getType(), enumImports);
            }
        });

        if (function.getOutput() != null && function.getOutput().getTypeCall() != null
                && function.getOutput().getTypeCall().getType() != null) {
            functionDependencyProvider.addDependencies(function.getOutput().getTypeCall().getType(), enumImports);
        }
        return enumImports;
    }

    private String generateConditions(Function function) {
        if (!function.getConditions().isEmpty()) {
            PythonCodeWriter writer = new PythonCodeWriter();
            writer.appendLine("# conditions");
            writer.appendBlock(
                    expressionGenerator.generateFunctionConditions(function.getConditions(), "_pre_registry"));
            writer.appendLine("# Execute all registered conditions");
            writer.appendLine("rune_execute_local_conditions(_pre_registry, 'Pre-condition')");
            writer.appendLine("");
            return writer.toString();
        }
        return "";
    }

    private String generatePostConditions(Function function) {
        if (!function.getPostConditions().isEmpty()) {
            PythonCodeWriter writer = new PythonCodeWriter();
            writer.appendLine("# post-conditions");
            writer.appendBlock(
                    expressionGenerator.generateFunctionConditions(function.getPostConditions(), "_post_registry"));
            writer.appendLine("# Execute all registered post-conditions");
            writer.appendLine("rune_execute_local_conditions(_post_registry, 'Post-condition')");
            return writer.toString();
        }
        return "";
    }

    private String generateAlias(Function function, int[] level) {
        PythonCodeWriter writer = new PythonCodeWriter();
        for (ShortcutDeclaration shortcut : function.getShortcuts()) {
            expressionGenerator.setIfCondBlocks(new ArrayList<>());
            String expression = expressionGenerator.generateExpression(shortcut.getExpression(), level[0], false);

            for (String block : expressionGenerator.getIfCondBlocks()) {
                writer.appendBlock(block);
                writer.newLine();
            }
            if (!expressionGenerator.getIfCondBlocks().isEmpty()) {
                level[0] += expressionGenerator.getIfCondBlocks().size();
            }
            writer.appendLine(shortcut.getName() + " = " + expression);
        }
        return writer.toString();
    }

    private String generateOperations(Function function, int[] level) {
        if (function.getOutput() != null) {
            PythonCodeWriter writer = new PythonCodeWriter();
            List<String> setNames = new ArrayList<>();
            for (Operation operation : function.getOperations()) {
                AssignPathRoot root = operation.getAssignRoot();
                expressionGenerator.setIfCondBlocks(new ArrayList<>());
                String expression = expressionGenerator.generateExpression(operation.getExpression(), level[0], false);

                for (String block : expressionGenerator.getIfCondBlocks()) {
                    writer.appendBlock(block);
                    writer.newLine();
                }
                if (!expressionGenerator.getIfCondBlocks().isEmpty()) {
                    level[0] += expressionGenerator.getIfCondBlocks().size();
                }
                if (operation.isAdd()) {
                    writer.appendBlock(generateAddOperation(root, operation, expression, setNames));
                } else {
                    writer.appendBlock(generateSetOperation(root, operation, expression, setNames));
                }
            }
            return writer.toString();
        }
        return "";
    }

    private String generateAddOperation(AssignPathRoot root, Operation operation,
            String expression, List<String> setNames) {
        PythonCodeWriter writer = new PythonCodeWriter();
        Attribute attribute = (Attribute) root;
        String rootName = root.getName();
        if (attribute.getTypeCall().getType() instanceof RosettaEnumeration) {
            if (!setNames.contains(rootName)) {
                setNames.add(rootName);
                writer.appendLine(rootName + " = []");
            }
            writer.appendLine(rootName + ".extend(" + expression + ")");
        } else {
            if (!setNames.contains(rootName)) {
                setNames.add(rootName);
                writer.appendLine(rootName + " = " + expression);
            } else {
                if (operation.getPath() == null) {
                    writer.appendLine(rootName + ".add_rune_attr(self, " + expression + ")");
                } else {
                    String path = generateAttributesPath(operation.getPath());
                    writer.appendLine(rootName
                            + ".add_rune_attr(rune_resolve_attr(rune_resolve_attr(self, "
                            + rootName
                            + "), "
                            + path
                            + "), "
                            + expression
                            + ")");
                }
            }
        }
        return writer.toString();
    }

    private String generateSetOperation(AssignPathRoot root, Operation operation,
            String expression, List<String> setNames) {
        PythonCodeWriter writer = new PythonCodeWriter();
        Attribute attributeRoot = (Attribute) root;
        String equalsSign = " = ";
        if (attributeRoot.getTypeCall().getType() instanceof RosettaEnumeration || operation.getPath() == null) {
            writer.appendLine(attributeRoot.getName() + equalsSign + expression);
        } else {
            String bundleName = RuneToPythonMapper.getBundleObjectName(attributeRoot.getTypeCall().getType());
            if (!setNames.contains(attributeRoot.getName())) {
                setNames.add(attributeRoot.getName());
                writer.appendLine(attributeRoot.getName()
                        + equalsSign
                        + "_get_rune_object('"
                        + bundleName
                        + "', "
                        + getNextPathElementName(operation.getPath())
                        + ", "
                        + buildObject(expression, operation.getPath())
                        + ")");
            } else {
                writer.appendLine(attributeRoot.getName() + equalsSign + "set_rune_attr(rune_resolve_attr(self, '"
                        + attributeRoot.getName()
                        + "'), "
                        + generateAttributesPath(operation.getPath()) + ", " + expression + ")");
            }
        }
        return writer.toString();
    }

    private String generateAttributesPath(Segment path) {
        Segment currentPath = path;
        StringBuilder result = new StringBuilder("'");
        while (currentPath != null) {
            result.append(currentPath.getFeature().getName());
            if (currentPath.getNext() != null) {
                result.append("->");
            }
            currentPath = currentPath.getNext();
        }
        result.append("'");
        return result.toString();
    }

    private String getNextPathElementName(Segment path) {
        return (path == null) ? null : "'" + path.getFeature().getName() + "'";
    }

    private String buildObject(String expression, Segment path) {
        if (path == null || path.getNext() == null) {
            return expression;
        }

        RosettaFeature feature = path.getFeature();
        if (feature instanceof RosettaTyped typed) {
            String bundleName = RuneToPythonMapper.getBundleObjectName(typed.getTypeCall().getType());
            Segment nextPath = path.getNext();
            return "_get_rune_object('"
                    + bundleName
                    + "', "
                    + getNextPathElementName(nextPath)
                    + ", "
                    + buildObject(expression, nextPath)
                    + ")";
        }
        String featureName = (feature != null) ? feature.getName() : "Null Feature";
        Class<?> featureClass = (feature != null) ? feature.getClass() : null;
        String className = (featureClass != null) ? featureClass.getSimpleName() : "Null Feature Class";
        throw new IllegalArgumentException("Cannot build object for feature " + featureName + " of type " + className);
    }
}
