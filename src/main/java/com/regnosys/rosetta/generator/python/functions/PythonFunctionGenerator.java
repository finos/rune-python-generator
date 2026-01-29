package com.regnosys.rosetta.generator.python.functions;

import com.regnosys.rosetta.generator.python.PythonCodeGeneratorContext;

import com.regnosys.rosetta.generator.python.expressions.PythonExpressionGenerator;
import com.regnosys.rosetta.generator.python.util.PythonCodeGeneratorUtil;
import com.regnosys.rosetta.generator.python.util.PythonCodeWriter;
import com.regnosys.rosetta.rosetta.RosettaModel;
import com.regnosys.rosetta.generator.python.util.RuneToPythonMapper;
import com.regnosys.rosetta.rosetta.RosettaEnumeration;
import com.regnosys.rosetta.rosetta.RosettaFeature;
import com.regnosys.rosetta.rosetta.RosettaTyped;
import com.regnosys.rosetta.rosetta.simple.*;
import jakarta.inject.Inject;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class PythonFunctionGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(PythonFunctionGenerator.class);

    @Inject
    private PythonFunctionDependencyProvider functionDependencyProvider;

    @Inject
    private PythonExpressionGenerator expressionGenerator;

    /**
     * Generate Python from the collection of Rosetta functions.
     * 
     * @param rFunctions the collection of Rosetta functions to generate
     * @param version    the version for this collection of functions
     * @return a Map of all the generated Python indexed by the file name
     */

    public Map<String, String> generate(Iterable<Function> rFunctions, String version,
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

        for (Function rf : rFunctions) {
            RosettaModel model = (RosettaModel) rf.eContainer();
            if (model == null) {
                LOGGER.warn("Function {} has no container, skipping", rf.getName());
                continue;
            }
            // String nameSpace = PythonCodeGeneratorUtil.getNamespace(model);
            try {
                String pythonFunction = generateFunction(rf, version, enumImports);

                String functionName = PythonCodeGeneratorUtil.createFullyQualifiedObjectName(rf);
                result.put(functionName, pythonFunction);
                dependencyDAG.addVertex(functionName);
                context.addFunctionName(functionName);
            } catch (Exception ex) {
                LOGGER.error("Exception occurred generating rf {}", rf.getName(), ex);
                throw new RuntimeException("Error generating Python for function " + rf.getName(), ex);
            }
        }
        return result;
    }

    private String generateFunction(Function rf, String version, Set<String> enumImports) {
        if (rf == null) {
            throw new RuntimeException("Function is null");
        }
        if (enumImports == null) {
            throw new RuntimeException("Enum imports is null");
        }
        enumImports.addAll(collectFunctionDependencies(rf));
        PythonCodeWriter writer = new PythonCodeWriter();

        writer.appendLine("");
        writer.appendLine("");

        writer.appendLine("@replaceable");
        writer.appendLine("@validate_call");
        writer.appendLine("def " + PythonCodeGeneratorUtil.createBundleObjectName(rf) + generateInputs(rf) + ":");
        writer.indent();

        writer.appendBlock(generateDescription(rf));

        if (!rf.getConditions().isEmpty()) {
            writer.appendLine("_pre_registry = {}");
        }
        if (!rf.getPostConditions().isEmpty()) {
            writer.appendLine("_post_registry = {}");
        }
        writer.appendLine("self = inspect.currentframe()");
        writer.appendLine("");
        if (rf.getConditions().isEmpty()) {
            writer.appendLine("");
        }

        writer.appendBlock(generateTypeOrFunctionConditions(rf));

        generateIfBlocks(writer, rf);
        generateAlias(writer, rf);
        generateOperations(writer, rf);
        generatesOutput(writer, rf);

        writer.unindent();
        writer.newLine();

        return writer.toString();
    }

    private void generatesOutput(PythonCodeWriter writer, Function function) {
        Attribute output = function.getOutput();
        if (output != null) {
            if (function.getOperations().isEmpty() && function.getShortcuts().isEmpty()) {
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
        }
    }

    private String generateInputs(Function function) {
        List<Attribute> inputs = function.getInputs();
        Attribute output = function.getOutput();

        StringBuilder result = new StringBuilder("(");
        for (int i = 0; i < inputs.size(); i++) {
            Attribute input = inputs.get(i);
            String typeName = input.getTypeCall().getType().getName();
            String type = input.getCard().getSup() == 0
                    ? "list[" + RuneToPythonMapper.toPythonBasicType(typeName) + "]"
                    : RuneToPythonMapper.toPythonBasicType(typeName);
            result.append(input.getName()).append(": ").append(type);
            if (input.getCard().getInf() == 0) {
                result.append(" | None");
            }
            if (i < inputs.size() - 1) {
                result.append(", ");
            }
        }
        result.append(") -> ");
        if (output != null) {
            result.append(RuneToPythonMapper.toPythonBasicType(output.getTypeCall().getType().getName()));
        } else {
            result.append("None");
        }
        return result.toString();
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
        writer.appendLine("Parameters ");
        writer.appendLine("----------");
        for (Attribute input : inputs) {
            writer.appendLine(input.getName() + " : " + input.getTypeCall().getType().getName());
            if (input.getDefinition() != null) {
                writer.appendLine(input.getDefinition());
            }
            writer.appendLine("");
        }
        writer.appendLine("Returns");
        writer.appendLine("-------");
        if (output != null) {
            writer.appendLine(output.getName() + " : " + output.getTypeCall().getType().getName());
        } else {
            writer.appendLine("No Return");
        }
        writer.appendLine("");
        writer.appendLine("\"\"\"");
        return writer.toString();
    }

    private Set<String> collectFunctionDependencies(Function rf) {
        Set<String> enumImports = new HashSet<>();
        rf.getShortcuts().forEach(
                shortcut -> functionDependencyProvider.addDependencies(shortcut.getExpression(), enumImports));
        rf.getOperations().forEach(
                operation -> functionDependencyProvider.addDependencies(operation.getExpression(), enumImports));

        List<Condition> allConditions = new ArrayList<>(rf.getConditions());
        allConditions.addAll(rf.getPostConditions());
        allConditions.forEach(
                condition -> functionDependencyProvider.addDependencies(condition.getExpression(), enumImports));

        rf.getInputs().forEach(input -> {
            if (input.getTypeCall() != null && input.getTypeCall().getType() != null) {
                functionDependencyProvider.addDependencies(input.getTypeCall().getType(), enumImports);
            }
        });

        if (rf.getOutput() != null && rf.getOutput().getTypeCall() != null
                && rf.getOutput().getTypeCall().getType() != null) {
            functionDependencyProvider.addDependencies(rf.getOutput().getTypeCall().getType(), enumImports);
        }
        return enumImports;
    }

    private void generateIfBlocks(PythonCodeWriter writer, Function function) {
        List<Integer> levelList = new ArrayList<>(Collections.singletonList(0));
        for (ShortcutDeclaration shortcut : function.getShortcuts()) {
            writer.appendBlock(expressionGenerator.generateThenElseForFunction(shortcut.getExpression(), levelList));
        }
        for (Operation operation : function.getOperations()) {
            writer.appendBlock(expressionGenerator.generateThenElseForFunction(operation.getExpression(), levelList));
        }
    }

    private String generateTypeOrFunctionConditions(Function function) {
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

    private void generateAlias(PythonCodeWriter writer, Function function) {
        int level = 0;

        for (ShortcutDeclaration shortcut : function.getShortcuts()) {
            expressionGenerator.setIfCondBlocks(new ArrayList<>());
            String expression = expressionGenerator.generateExpression(shortcut.getExpression(), level, false);

            if (!expressionGenerator.getIfCondBlocks().isEmpty()) {
                level += 1;
            }
            writer.appendLine(shortcut.getName() + " = " + expression);
        }
    }

    private void generateOperations(PythonCodeWriter writer, Function function) {
        int level = 0;
        if (function.getOutput() != null) {
            List<String> setNames = new ArrayList<>();
            for (Operation operation : function.getOperations()) {
                AssignPathRoot root = operation.getAssignRoot();
                String expression = expressionGenerator.generateExpression(operation.getExpression(), level, false);

                if (!expressionGenerator.getIfCondBlocks().isEmpty()) {
                    level += 1;
                }
                if (operation.isAdd()) {
                    generateAddOperation(writer, root, operation, function, expression, setNames);
                } else {
                    generateSetOperation(writer, root, operation, function, expression, setNames);
                }
            }
        }
    }

    private void generateAddOperation(PythonCodeWriter writer, AssignPathRoot root, Operation operation,
            Function function, String expression, List<String> setNames) {
        Attribute attribute = (Attribute) root;

        if (attribute.getTypeCall().getType() instanceof RosettaEnumeration) {
            if (!setNames.contains(root.getName())) {
                setNames.add(root.getName());
                writer.appendLine(root.getName() + " = []");
            }
            writer.appendLine(root.getName() + ".extend(" + expression + ")");
        } else {
            if (!setNames.contains(root.getName())) {
                setNames.add(root.getName());
                String spacer = (expression.startsWith("if_cond_fn") || root.getName().equals("result")) ? " =  "
                        : " = ";
                writer.appendLine(root.getName() + spacer + expression);
            } else {
                if (operation.getPath() == null) {
                    writer.appendLine(root.getName() + ".add_rune_attr(self, " + expression + ")");
                } else {
                    String path = generateAttributesPath(operation.getPath());
                    writer.appendLine(root.getName()
                            + ".add_rune_attr(rune_resolve_attr(rune_resolve_attr(self, "
                            + root.getName()
                            + "), "
                            + path
                            + "), "
                            + expression
                            + ")");
                }
            }
        }
    }

    private void generateSetOperation(PythonCodeWriter writer, AssignPathRoot root, Operation operation,
            Function function, String expression, List<String> setNames) {
        Attribute attributeRoot = (Attribute) root;
        String name = attributeRoot.getName();
        String spacer = (expression.startsWith("if_cond_fn") || name.equals("result")) ? " =  " : " = ";
        if (attributeRoot.getTypeCall().getType() instanceof RosettaEnumeration || operation.getPath() == null) {
            writer.appendLine(attributeRoot.getName() + spacer + expression);
        } else {
            if (!setNames.contains(attributeRoot.getName())) {
                setNames.add(attributeRoot.getName());
                writer.appendLine(attributeRoot.getName() + spacer + "_get_rune_object('"
                        + attributeRoot.getTypeCall().getType().getName() + "', " +
                        getNextPathElementName(operation.getPath()) + ", "
                        + buildObject(expression, operation.getPath()) + ")");
            } else {
                writer.appendLine(attributeRoot.getName() + spacer + "set_rune_attr(rune_resolve_attr(self, '"
                        + attributeRoot.getName()
                        + "'), " +
                        generateAttributesPath(operation.getPath()) + ", " + expression + ")");
            }
        }
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
        if (path != null) {
            RosettaFeature feature = path.getFeature();
            return "'" + feature.getName() + "'";
        }
        return "null";
    }

    private String buildObject(String expression, Segment path) {
        if (path == null || path.getNext() == null) {
            return expression;
        }

        RosettaFeature feature = path.getFeature();
        if (feature instanceof RosettaTyped typed) {
            return _get_rune_object(typed.getTypeCall().getType().getName(), path.getNext(), expression);
        }
        throw new IllegalArgumentException("Cannot build object for feature " + feature.getName() + " of type "
                + feature.getClass().getSimpleName());
    }

    private String _get_rune_object(String typeName, Segment nextPath, String expression) {
        return "_get_rune_object('" + typeName + "', " + getNextPathElementName(nextPath) + ", "
                + buildObject(expression, nextPath) + ")";
    }
}
