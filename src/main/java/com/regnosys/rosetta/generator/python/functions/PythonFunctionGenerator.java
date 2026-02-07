package com.regnosys.rosetta.generator.python.functions;

import com.regnosys.rosetta.rosetta.RosettaModel;
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

import com.regnosys.rosetta.generator.python.PythonCodeGeneratorContext;
import com.regnosys.rosetta.generator.python.expressions.PythonExpressionGenerator;
import com.regnosys.rosetta.generator.python.util.RuneToPythonMapper;
import com.regnosys.rosetta.generator.python.util.PythonCodeWriter;

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

                String functionName = RuneToPythonMapper.getFullyQualifiedObjectName(rf);
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
        writer.appendLine("def " + RuneToPythonMapper.getBundleObjectName(rf) + generateInputs(rf) + ":");
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

        writer.appendBlock(generateConditions(rf));

        int[] level = { 0 };
        generateAlias(writer, rf, level);
        generateOperations(writer, rf, level);
        generateOutput(writer, rf);

        writer.unindent();
        writer.newLine();

        return writer.toString();
    }

    private String generateInputs(Function function) {
        StringBuilder result = new StringBuilder("(");
        List<Attribute> inputs = function.getInputs();
        for (int i = 0; i < inputs.size(); i++) {
            Attribute input = inputs.get(i);
            String inputBundleName = RuneToPythonMapper.getBundleObjectName(input.getTypeCall().getType());
            String inputType = RuneToPythonMapper.formatPythonType(
                    inputBundleName,
                    input.getCard().getInf(),
                    input.getCard().getSup(),
                    true);
            result.append(input.getName()).append(": ").append(inputType);

            if (i < inputs.size() - 1) {
                result.append(", ");
            }
        }
        result.append(") -> ");
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

    private void generateOutput(PythonCodeWriter writer, Function function) {
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

    private void generateAlias(PythonCodeWriter writer, Function function, int[] level) {
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
    }

    private void generateOperations(PythonCodeWriter writer, Function function, int[] level) {
        if (function.getOutput() != null) {
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
    }

    private void generateSetOperation(PythonCodeWriter writer, AssignPathRoot root, Operation operation,
            Function function, String expression, List<String> setNames) {
        Attribute attributeRoot = (Attribute) root;
        String equalsSign = " = ";
        if (attributeRoot.getTypeCall().getType() instanceof RosettaEnumeration || operation.getPath() == null) {
            writer.appendLine(attributeRoot.getName() + equalsSign + expression);
        } else {
            String bundleName = RuneToPythonMapper.getBundleObjectName(attributeRoot.getTypeCall().getType());
            if (!setNames.contains(attributeRoot.getName())) {
                setNames.add(attributeRoot.getName());
                writer.appendLine(attributeRoot.getName() + equalsSign + "_get_rune_object('"
                        + bundleName + "', " +
                        getNextPathElementName(operation.getPath()) + ", "
                        + buildObject(expression, operation.getPath()) + ")");
            } else {
                writer.appendLine(attributeRoot.getName() + equalsSign + "set_rune_attr(rune_resolve_attr(self, '"
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
        throw new IllegalArgumentException("Cannot build object for feature " + feature.getName() + " of type "
                + feature.getClass().getSimpleName());
    }
}
