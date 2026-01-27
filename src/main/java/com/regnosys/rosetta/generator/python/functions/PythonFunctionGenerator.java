package com.regnosys.rosetta.generator.python.functions;

import com.regnosys.rosetta.generator.python.expressions.PythonExpressionGenerator;
import com.regnosys.rosetta.generator.python.util.PythonCodeGeneratorUtil;
import com.regnosys.rosetta.generator.python.util.PythonCodeWriter;
import com.regnosys.rosetta.generator.python.util.RuneToPythonMapper;
import com.regnosys.rosetta.rosetta.RosettaEnumeration;
import com.regnosys.rosetta.rosetta.RosettaFeature;
import com.regnosys.rosetta.rosetta.RosettaModel;
import com.regnosys.rosetta.rosetta.RosettaTyped;
import com.regnosys.rosetta.rosetta.simple.*;
import jakarta.inject.Inject;
import org.eclipse.emf.ecore.EObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class PythonFunctionGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(PythonFunctionGenerator.class);

    @Inject
    private FunctionDependencyProvider functionDependencyProvider;

    @Inject
    private PythonExpressionGenerator expressionGenerator;

    /**
     * Generate Python from the collection of Rosetta functions.
     * 
     * @param rosettaFunctions the collection of Rosetta functions to generate
     * @param version          the version for this collection of functions
     * @return a Map of all the generated Python indexed by the file name
     */

    public Map<String, String> generate(Iterable<Function> rosettaFunctions, String version) {
        Map<String, String> result = new HashMap<>();

        for (Function func : rosettaFunctions) {
            RosettaModel tr = (RosettaModel) func.eContainer();
            String namespace = tr.getName();
            try {
                String functionAsString = generateFunctions(func, version);
                result.put(PythonCodeGeneratorUtil.toPyFunctionFileName(namespace, func.getName()),
                        PythonCodeGeneratorUtil.createImportsFunc(func.getName()) + functionAsString);
            } catch (Exception ex) {
                LOGGER.error("Exception occurred generating func {}", func.getName(), ex);
            }
        }

        return result;
    }

    private String generateFunctions(Function function, String version) {
        Set<EObject> dependencies = collectFunctionDependencies(function);
        PythonCodeWriter writer = new PythonCodeWriter();

        writer.appendBlock(generateImports(dependencies, function));
        writer.appendLine("");
        writer.appendLine("");

        writer.appendLine("@replaceable");
        writer.appendLine("def " + function.getName() + generatesInputs(function) + ":");
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

        writer.appendBlock(generateTypeOrFunctionConditions(function));

        generateIfBlocks(writer, function);
        generateAlias(writer, function);
        generateOperations(writer, function);
        generatesOutput(writer, function);

        writer.unindent();
        writer.newLine();
        writer.appendLine(
                "sys.modules[__name__].__class__ = create_module_attr_guardian(sys.modules[__name__].__class__)");

        return writer.toString();
    }

    private String generateImports(Iterable<EObject> dependencies, Function function) {
        PythonCodeWriter writer = new PythonCodeWriter();

        for (EObject dependency : dependencies) {
            RosettaModel tr = (RosettaModel) dependency.eContainer();
            String importPath = tr.getName();
            if (dependency instanceof Function func) {
                writer.appendLine("from " + importPath + ".functions." + func.getName() + " import " + func.getName());
            } else if (dependency instanceof RosettaEnumeration enumeration) {
                writer.appendLine(
                        "from " + importPath + "." + enumeration.getName() + " import " + enumeration.getName());
            } else if (dependency instanceof Data data) {
                writer.appendLine("from " + importPath + "." + data.getName() + " import " + data.getName());
            }
        }
        writer.newLine();
        writer.appendLine("__all__ = ['" + function.getName() + "']");

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

    private String generatesInputs(Function function) {
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

    private Set<EObject> collectFunctionDependencies(Function func) {
        Set<EObject> dependencies = new HashSet<>();

        func.getShortcuts().forEach(
                shortcut -> dependencies.addAll(functionDependencyProvider.findDependencies(shortcut.getExpression())));
        func.getOperations().forEach(operation -> dependencies
                .addAll(functionDependencyProvider.findDependencies(operation.getExpression())));

        List<Condition> allConditions = new ArrayList<>(func.getConditions());
        allConditions.addAll(func.getPostConditions());
        allConditions.forEach(condition -> dependencies
                .addAll(functionDependencyProvider.findDependencies(condition.getExpression())));

        func.getInputs().forEach(input -> {
            if (input.getTypeCall() != null && input.getTypeCall().getType() != null) {
                dependencies.add(input.getTypeCall().getType());
            }
        });

        if (func.getOutput() != null && func.getOutput().getTypeCall() != null
                && func.getOutput().getTypeCall().getType() != null) {
            dependencies.add(func.getOutput().getTypeCall().getType());
        }

        dependencies.removeIf(it -> it instanceof Function f && f.getName().equals(func.getName()));

        return dependencies;
    }

    private void generateIfBlocks(PythonCodeWriter writer, Function function) {
        List<Integer> levelList = new ArrayList<>(Collections.singletonList(0));
        for (ShortcutDeclaration shortcut : function.getShortcuts()) {
            writer.appendBlock(expressionGenerator.generateThenElseForFunction(shortcut.getExpression(), levelList,
                    new HashSet<>()));
        }
        for (Operation operation : function.getOperations()) {
            writer.appendBlock(expressionGenerator.generateThenElseForFunction(operation.getExpression(), levelList,
                    new HashSet<>()));
        }
    }

    private String generateTypeOrFunctionConditions(Function function) {
        if (!function.getConditions().isEmpty()) {
            PythonCodeWriter writer = new PythonCodeWriter();
            writer.appendLine("# conditions");
            writer.appendBlock(
                    expressionGenerator.generateFunctionConditions(function.getConditions(), "_pre_registry",
                            new HashSet<>()));
            writer.appendLine("# Execute all registered conditions");
            writer.appendLine("execute_local_conditions(_pre_registry, 'Pre-condition')");
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
                    expressionGenerator.generateFunctionConditions(function.getPostConditions(), "_post_registry",
                            new HashSet<>()));
            writer.appendLine("# Execute all registered post-conditions");
            writer.appendLine("execute_local_conditions(_post_registry, 'Post-condition')");
            return writer.toString();
        }
        return "";
    }

    private void generateAlias(PythonCodeWriter writer, Function function) {
        int level = 0;

        for (ShortcutDeclaration shortcut : function.getShortcuts()) {
            expressionGenerator.setIfCondBlocks(new ArrayList<>());
            String expression = expressionGenerator.generateExpression(shortcut.getExpression(), level, false,
                    new HashSet<>());

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
                String expression = expressionGenerator.generateExpression(operation.getExpression(), level, false,
                        new HashSet<>());

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
                    writer.appendLine(root.getName() + ".add_rune_attr(rune_resolve_attr(rune_resolve_attr(self, "
                            + root.getName() + "), " + path + "), " + expression + ")");
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
