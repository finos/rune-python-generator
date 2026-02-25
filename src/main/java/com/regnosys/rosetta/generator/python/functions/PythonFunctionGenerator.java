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
import com.regnosys.rosetta.generator.python.expressions.PythonExpressionScope;
import com.regnosys.rosetta.generator.python.util.PythonCodeWriter;
import com.regnosys.rosetta.generator.python.util.RuneToPythonMapper;
import com.regnosys.rosetta.rosetta.RosettaEnumeration;
import com.regnosys.rosetta.rosetta.RosettaModel;
import com.regnosys.rosetta.rosetta.RosettaNamed;
import com.regnosys.rosetta.rosetta.RosettaType;
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

        if (context.getEnumImports() == null) {
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
                String pythonFunction = generateFunction(function, context);

                String functionName = RuneToPythonMapper.getFullyQualifiedObjectName(function);
                result.put(functionName, pythonFunction);
                dependencyDAG.addVertex(functionName);
                context.addFunctionName(functionName);

                addFunctionDependencies(dependencyDAG, functionName, function);
            } catch (Exception ex) {
                LOGGER.error("Exception occurred generating function {}", function.getName(), ex);
                throw new RuntimeException(
                    "Error generating Python for function " + function.getName(), ex
                );
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
            switch (content) {
                case RosettaSymbolReference ref -> {
                    if (ref.getSymbol() instanceof Function || ref.getSymbol() instanceof Data
                            || ref.getSymbol() instanceof RosettaEnumeration) {
                        dependencies.add((RosettaNamed) ref.getSymbol());
                    }
                }
                case RosettaConstructorExpression cons -> {
                    if (cons.getTypeCall() != null && cons.getTypeCall().getType() != null) {
                        dependencies.add(cons.getTypeCall().getType());
                    }
                }
                default -> {
                }
            }
        }

        for (RosettaNamed dep : dependencies) {
            String depName = "";
            switch (dep) {
                case Data data -> depName = rObjectFactory
                    .buildRDataType(data)
                    .getQualifiedName()
                    .toString();
                case Function function1 -> depName = RuneToPythonMapper.getFullyQualifiedObjectName(function1);
                case RosettaEnumeration rosettaEnumeration -> depName = rObjectFactory
                    .buildREnumType(rosettaEnumeration)
                    .getQualifiedName()
                    .toString();
                default -> {
                }
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

    private String generateFunction(Function function, PythonCodeGeneratorContext context) {
        if (function == null) {
            throw new RuntimeException("Function is null");
        }
        if (context.getEnumImports() == null) {
            throw new RuntimeException("Enum imports is null");
        }
        context.getEnumImports().addAll(collectFunctionDependencies(function));

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
        expressionGenerator.initialize();

        boolean isCodeImplementation = isCodeImplementation(function);
        if (isCodeImplementation) {
            writer.appendLine(generateExternalFunctionCall(function));
        } else {
            writer.appendBlock(generateAlias(function));
            writer.appendBlock(generateOperations(function, context));
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
                    String inputType = RuneToPythonMapper.formatCardinality(
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
            String outputType = RuneToPythonMapper.formatCardinality(
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
            String paramName = RuneToPythonMapper.formatCardinality(
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
            String paramName = RuneToPythonMapper.formatCardinality(
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

    private String generateAlias(Function function) {
        PythonCodeWriter writer = new PythonCodeWriter();
        for (ShortcutDeclaration shortcut : function.getShortcuts()) {
            PythonExpressionGenerator.ExpressionResult result = expressionGenerator.generate(
                    shortcut.getExpression(),
                    PythonExpressionScope.of("self"));

            for (String block : result.companionBlocks()) {
                writer.appendBlock(block);
                writer.newLine();
            }
            writer.appendLine(shortcut.getName() + " = " + result.expression());
        }
        return writer.toString();
    }

    private String generateOperations(Function function, PythonCodeGeneratorContext context) {
        if (function.getOutput() != null) {
            PythonCodeWriter writer = new PythonCodeWriter();
            PythonFunctionGenerationScope scope = new PythonFunctionGenerationScope();

            // 1. Preamble: Proactive initialization
            Set<String> preambleCandidates = new HashSet<>();
            Set<String> seenRoots = new HashSet<>();
            for (Operation operation : function.getOperations()) {
                String rootName = operation.getAssignRoot().getName();
                if (!seenRoots.contains(rootName)) {
                    seenRoots.add(rootName);
                    // A root is a candidate if it's first touched by an 'add' or a nested path
                    if (operation.isAdd() || operation.getPath() != null) {
                        preambleCandidates.add(rootName);
                    }
                }
            }

            for (Operation operation : function.getOperations()) {
                AssignPathRoot root = operation.getAssignRoot();
                String rootName = root.getName();

                // 1a. Root initialization
                if (preambleCandidates.contains(rootName) && !scope.isInitialized(rootName)) {
                    Attribute attribute = (Attribute) root;
                    RosettaType type = attribute.getTypeCall().getType();
                    if (attribute.getCard().isIsMany() || type instanceof RosettaEnumeration) {
                        writer.appendLine(rootName + " = []");
                        scope.markInitialized(rootName);
                    } else if (operation.getPath() != null) {
                        String bundleName = RuneToPythonMapper.getBundleObjectName(type);
                        scope.markAsObjectBuilder(rootName);
                        writer.appendLine(rootName + " = ObjectBuilder(" + bundleName + ")");
                    }
                }

                // 1b. Nested path initialization
                if (operation.getPath() != null && scope.isObjectBuilder(rootName)) {
                    initializePathInPreamble(rootName, operation.getPath(), function, writer, scope);
                }
            }

            // 2. Execution: Actual operations
            for (Operation operation : function.getOperations()) {
                AssignPathRoot root = operation.getAssignRoot();

                PythonExpressionGenerator.ExpressionResult result = expressionGenerator.generate(
                        operation.getExpression(),
                        PythonExpressionScope.of("self"));

                for (String block : result.companionBlocks()) {
                    writer.appendBlock(block);
                    writer.newLine();
                }

                String expression = result.expression();
                if (operation.isAdd()) {
                    writer.appendBlock(generateAddOperation(root, operation, expression, scope));
                } else {
                    writer.appendBlock(generateSetOperation(root, operation, expression, scope));
                }
            }
            if (scope.hasObjectBuilders()) {
                context.addAdditionalImport("from rune.runtime.object_builder import ObjectBuilder");
                for (String setName : scope.getObjectBuilderNames()) {
                    writer.appendLine(setName + " = " + setName + ".to_model()");
                }
            }
            return writer.toString();
        }
        return "";
    }

    private String generateDottedPath(Segment path) {
        List<String> segments = new ArrayList<>();
        Segment current = path;
        while (current != null) {
            segments.add(current.getFeature().getName());
            current = current.getNext();
        }
        return String.join(".", segments);
    }

    private String generateSetOperation(AssignPathRoot root, Operation operation,
            String expression, PythonFunctionGenerationScope scope) {
        PythonCodeWriter writer = new PythonCodeWriter();
        Attribute attributeRoot = (Attribute) root;
        String rootName = attributeRoot.getName();

        if (attributeRoot.getTypeCall().getType() instanceof RosettaEnumeration || operation.getPath() == null) {
            writer.appendLine(rootName + " = " + expression);
            scope.markInitialized(rootName);
        } else {
            if (!scope.isInitialized(rootName)) {
                String bundleName = RuneToPythonMapper.getBundleObjectName(attributeRoot.getTypeCall().getType());
                scope.markAsObjectBuilder(rootName);
                writer.appendLine(rootName + " = ObjectBuilder(" + bundleName + ")");
            }

            if (scope.isObjectBuilder(rootName)) {
                writer.appendLine(rootName + "." + generateDottedPath(operation.getPath()) + " = " + expression);
            } else {
                writer.appendLine(
                        "set_rune_attr(" 
                        + rootName
                        + ", "
                        + generateAttributesPath(operation.getPath())
                        + ", "
                        + expression
                        + ")");
            }
        }
        return writer.toString();
    }

    private String generateAddOperation(AssignPathRoot root, Operation operation,
            String expression, PythonFunctionGenerationScope scope) {
        PythonCodeWriter writer = new PythonCodeWriter();
        Attribute attribute = (Attribute) root;
        String rootName = root.getName();
        RosettaType attributeType = attribute.getTypeCall().getType();
        if (attributeType == null) {
            throw new RuntimeException("Attribute type is null");
        }

        if (attributeType instanceof RosettaEnumeration) {
            writer.appendLine(rootName + ".extend(" + expression + ")");
        } else if (isMany(operation)) {
            if (scope.isObjectBuilder(rootName)) {
                String path = generateDottedPath(operation.getPath());
                writer.appendLine("rune_add_to_list(" + rootName + "." + path + ", " + expression + ")");
            } else {
                writer.newLine();
                writer.appendLine("rune_add_to_list(" + rootName + ", " + expression + ")");
            }
        } else {
            if (!scope.isInitialized(rootName)) {
                if (operation.getPath() != null) {
                    String bundleName = RuneToPythonMapper.getBundleObjectName(attributeType);
                    scope.markAsObjectBuilder(rootName);
                    writer.appendLine(rootName + " = ObjectBuilder(" + bundleName + ")");
                    writer.appendLine(rootName + "." + generateDottedPath(operation.getPath()) + " = " + expression);
                } else {
                    writer.appendLine(rootName + " = " + expression);
                    scope.markInitialized(rootName);
                }
            } else {
                if (operation.getPath() == null) {
                    writer.appendLine(rootName + ".add_rune_attr(self, " + expression + ")");
                } else {
                    if (scope.isObjectBuilder(rootName)) {
                        writer.appendLine(rootName + "." + generateDottedPath(operation.getPath()) + " = " + expression);
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
        return writer.toString();
    }

    private boolean isMany(Operation operation) {
        if (operation.getPath() == null) {
            return ((Attribute) operation.getAssignRoot()).getCard().isIsMany();
        }
        Segment current = operation.getPath();
        while (current.getNext() != null) {
            current = current.getNext();
        }
        return ((Attribute) current.getFeature()).getCard().isIsMany();
    }

    private void initializePathInPreamble(String rootName, Segment path, Function function,
            PythonCodeWriter writer, PythonFunctionGenerationScope scope) {
        Segment current = path;
        String currentPath = rootName;

        while (current != null) {
            Attribute attribute = (Attribute) current.getFeature();
            String attrName = attribute.getName();
            currentPath += "." + attrName;

            if (!scope.isInitialized(currentPath)) {
                // Determine if this path element is ever first-touched by an 'add'
                boolean needsPreamble = false;
                for (Operation op : function.getOperations()) {
                    if (op.getAssignRoot().getName().equals(rootName) && op.getPath() != null) {
                        // Compare the path segments up to this point
                        if (isSamePath(path, op.getPath(), current)) {
                            if (op.isAdd() || op.getPath().getNext() != null) {
                                needsPreamble = true;
                            }
                            break; // Logic determined by first touch
                        }
                    }
                }

                if (needsPreamble) {
                    if (attribute.getCard().isIsMany()) {
                        writer.appendLine(currentPath + " = []");
                        scope.markInitialized(currentPath);
                    } else if (current.getNext() != null) {
                        String bundleName = RuneToPythonMapper.getBundleObjectName(attribute.getTypeCall().getType());
                        writer.appendLine(currentPath + " = ObjectBuilder(" + bundleName + ")");
                        scope.markInitialized(currentPath);
                    }
                }
            }
            current = current.getNext();
        }
    }

    private boolean isSamePath(Segment targetPath, Segment opPath, Segment upToSegment) {
        Segment t = targetPath;
        Segment o = opPath;
        while (t != null && o != null) {
            if (!t.getFeature().getName().equals(o.getFeature().getName())) {
                return false;
            }
            if (t == upToSegment) {
                return true;
            }
            t = t.getNext();
            o = o.getNext();
        }
        return false;
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

    /**
     * Tracks the initialization state and metadata of local Python variables within
     * a function.
     */
    private static class PythonFunctionGenerationScope {
        /** The set of variable names that have been initialized in this scope. */
        private final HashSet<String> initializedNames = new HashSet<>();
        /** The set of variable names that are marked as ObjectBuilders. */
        private final HashSet<String> objectBuilderNames = new HashSet<>();

        /**
         * Checks if a variable name has been initialized in this scope.
         * 
         * @param name the variable name
         * @return true if initialized, false otherwise
         */
        public boolean isInitialized(String name) {
            return initializedNames.contains(name);
        }

        /**
         * Marks a variable name as initialized with a simple value or list.
         * 
         * @param name the variable name
         */
        public void markInitialized(String name) {
            initializedNames.add(name);
        }

        /**
         * Marks a variable name as an ObjectBuilder. This will trigger .to_model()
         * finalization.
         * 
         * @param name the variable name
         */
        public void markAsObjectBuilder(String name) {
            objectBuilderNames.add(name);
            initializedNames.add(name);
        }

        /**
         * Checks if a variable name is marked as an ObjectBuilder.
         *
         * @param name the variable name
         * @return true if it's an ObjectBuilder, false otherwise
         */
        public boolean isObjectBuilder(String name) {
            return objectBuilderNames.contains(name);
        }

        /**
         * @return true if any ObjectBuilders were created in this scope
         */
        public boolean hasObjectBuilders() {
            return !objectBuilderNames.isEmpty();
        }


        /**
         * @return the set of names that are ObjectBuilders
         */
        public Set<String> getObjectBuilderNames() {
            return objectBuilderNames;
        }
    }

}
