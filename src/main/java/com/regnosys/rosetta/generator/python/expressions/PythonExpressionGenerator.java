package com.regnosys.rosetta.generator.python.expressions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.regnosys.rosetta.generator.java.enums.EnumHelper;
import com.regnosys.rosetta.generator.python.util.PythonCodeWriter;
import com.regnosys.rosetta.generator.python.util.RuneToPythonMapper;
import com.regnosys.rosetta.rosetta.RosettaCallableWithArgs;
import com.regnosys.rosetta.rosetta.RosettaEnumValue;
import com.regnosys.rosetta.rosetta.RosettaEnumValueReference;
import com.regnosys.rosetta.rosetta.RosettaEnumeration;
import com.regnosys.rosetta.rosetta.RosettaFeature;
import com.regnosys.rosetta.rosetta.RosettaSymbol;
import com.regnosys.rosetta.rosetta.expression.AsKeyOperation;
import com.regnosys.rosetta.rosetta.expression.ChoiceOperation;
import com.regnosys.rosetta.rosetta.expression.ClosureParameter;
import com.regnosys.rosetta.rosetta.expression.DistinctOperation;
import com.regnosys.rosetta.rosetta.expression.ExistsModifier;
import com.regnosys.rosetta.rosetta.expression.FilterOperation;
import com.regnosys.rosetta.rosetta.expression.FirstOperation;
import com.regnosys.rosetta.rosetta.expression.FlattenOperation;
import com.regnosys.rosetta.rosetta.expression.InlineFunction;
import com.regnosys.rosetta.rosetta.expression.LastOperation;
import com.regnosys.rosetta.rosetta.expression.ListLiteral;
import com.regnosys.rosetta.rosetta.expression.MapOperation;
import com.regnosys.rosetta.rosetta.expression.MaxOperation;
import com.regnosys.rosetta.rosetta.expression.MinOperation;
import com.regnosys.rosetta.rosetta.expression.ModifiableBinaryOperation;
import com.regnosys.rosetta.rosetta.expression.Necessity;
import com.regnosys.rosetta.rosetta.expression.OneOfOperation;
import com.regnosys.rosetta.rosetta.expression.ReverseOperation;
import com.regnosys.rosetta.rosetta.expression.RosettaAbsentExpression;
import com.regnosys.rosetta.rosetta.expression.RosettaBinaryOperation;
import com.regnosys.rosetta.rosetta.expression.RosettaBooleanLiteral;
import com.regnosys.rosetta.rosetta.expression.RosettaConditionalExpression;
import com.regnosys.rosetta.rosetta.expression.RosettaConstructorExpression;
import com.regnosys.rosetta.rosetta.expression.RosettaCountOperation;
import com.regnosys.rosetta.rosetta.expression.RosettaDeepFeatureCall;
import com.regnosys.rosetta.rosetta.expression.RosettaExistsExpression;
import com.regnosys.rosetta.rosetta.expression.RosettaExpression;
import com.regnosys.rosetta.rosetta.expression.RosettaFeatureCall;
import com.regnosys.rosetta.rosetta.expression.RosettaImplicitVariable;
import com.regnosys.rosetta.rosetta.expression.RosettaIntLiteral;
import com.regnosys.rosetta.rosetta.expression.RosettaNumberLiteral;
import com.regnosys.rosetta.rosetta.expression.RosettaOnlyElement;
import com.regnosys.rosetta.rosetta.expression.RosettaOnlyExistsExpression;
import com.regnosys.rosetta.rosetta.expression.RosettaStringLiteral;
import com.regnosys.rosetta.rosetta.expression.RosettaSymbolReference;
import com.regnosys.rosetta.rosetta.expression.SortOperation;
import com.regnosys.rosetta.rosetta.expression.SumOperation;
import com.regnosys.rosetta.rosetta.expression.SwitchCaseGuard;
import com.regnosys.rosetta.rosetta.expression.SwitchOperation;
import com.regnosys.rosetta.rosetta.expression.ThenOperation;
import com.regnosys.rosetta.rosetta.expression.ToDateOperation;
import com.regnosys.rosetta.rosetta.expression.ToDateTimeOperation;
import com.regnosys.rosetta.rosetta.expression.ToEnumOperation;
import com.regnosys.rosetta.rosetta.expression.ToIntOperation;
import com.regnosys.rosetta.rosetta.expression.ToStringOperation;
import com.regnosys.rosetta.rosetta.expression.ToTimeOperation;
import com.regnosys.rosetta.rosetta.expression.ToZonedDateTimeOperation;
import com.regnosys.rosetta.rosetta.expression.WithMetaOperation;
import com.regnosys.rosetta.rosetta.simple.Attribute;
import com.regnosys.rosetta.rosetta.simple.Condition;
import com.regnosys.rosetta.rosetta.simple.Data;
import com.regnosys.rosetta.rosetta.simple.ShortcutDeclaration;
import com.regnosys.rosetta.rosetta.simple.impl.FunctionImpl;

/**
 * Generate Python for Rune Expressions
 */
public final class PythonExpressionGenerator {

    /**
     * The list of if condition blocks.
     */
    private List<String> ifCondBlocks = new ArrayList<>();
    /**
     * Whether the current expression is a switch condition.
     */
    private boolean isSwitchCond = false;
    /**
     * Tracks Rosetta symbols that currently map to a lambda parameter (e.g.,
     * "item")
     */
    private final Map<RosettaSymbol, String> shadowedSymbols = new HashMap<>();

    /**
     * Gets the list of if condition blocks.
     * 
     * @return The list of if condition blocks.
     */
    public List<String> getIfCondBlocks() {
        return ifCondBlocks;
    }

    /**
     * The counter for generated helper functions.
     */
    private int generatedFunctionCounter = 0;

    /**
     * Clears the block list.
     */
    public void clearBlocks() {
        ifCondBlocks.clear();
    }

    /**
     * Resets the counters.
     */
    public void resetCounters() {
        generatedFunctionCounter = 0;
    }

    /**
     * Checks if the current expression is a switch condition.
     * 
     * @return True if the current expression is a switch condition, false
     *         otherwise.
     */
    public boolean isSwitchCond() {
        return isSwitchCond;
    }

    /**
     * Generates Python code for a Rune expression.
     * 
     * @param expr     The Rune expression to generate Python code for.
     * @param isLambda Whether the expression is a lambda.
     * @return The generated Python code.
     */
    public String generateExpression(RosettaExpression expr, boolean isLambda) {

        switch (expr) {
            case null -> {
                return "None";
            }
            case RosettaBooleanLiteral bool -> {
                return bool.isValue() ? "True" : "False";
            }
            case RosettaIntLiteral i -> {
                return String.valueOf(i.getValue());
            }
            case RosettaNumberLiteral n -> {
                return "Decimal('" + n.getValue().toString() + "')";
            }
            case RosettaStringLiteral s -> {
                return "\"" + s.getValue() + "\"";
            }
            case AsKeyOperation asKey -> {
                return "{" + generateExpression(asKey.getArgument(), isLambda) + ": True}";
            }
            case DistinctOperation distinct -> {
                return "set(" + generateExpression(distinct.getArgument(), isLambda) + ")";
            }
            case FilterOperation filter -> {
                return generateFilterOperation(filter, isLambda);
            }
            case FirstOperation first -> {
                return generateExpression(first.getArgument(), isLambda) + "[0]";
            }
            case FlattenOperation flatten -> {
                return "rune_flatten_list(" + generateExpression(flatten.getArgument(), isLambda) + ")";
            }
            case ListLiteral listLiteral -> {
                return "[" + listLiteral.getElements().stream()
                        .map(arg -> generateExpression(arg, isLambda))
                        .collect(Collectors.joining(", ")) + "]";
            }
            case LastOperation last -> {
                return generateExpression(last.getArgument(), isLambda) + "[-1]";
            }
            case MapOperation mapOp -> {
                return generateMapOperation(mapOp, isLambda);
            }
            case MaxOperation maxOp -> {
                return "max(" + generateExpression(maxOp.getArgument(), isLambda) + ")";
            }
            case MinOperation minOp -> {
                return "min(" + generateExpression(minOp.getArgument(), isLambda) + ")";
            }
            case SortOperation sort -> {
                return "sorted(" + generateExpression(sort.getArgument(), isLambda) + ")";
            }
            case ThenOperation then -> {
                return generateThenOperation(then, isLambda);
            }
            case SumOperation sum -> {
                return "sum(" + generateExpression(sum.getArgument(), isLambda) + ")";
            }
            case ReverseOperation reverse -> {
                return "list(reversed(" + generateExpression(reverse.getArgument(), isLambda) + "))";
            }
            case SwitchOperation switchOp -> {
                return generateSwitchOperation(switchOp, isLambda);
            }
            case ToEnumOperation toEnum -> {
                return toEnum.getEnumeration().getName() + "("
                        + generateExpression(toEnum.getArgument(), isLambda) + ")";
            }
            case ToStringOperation toString -> {
                return "rune_str(" + generateExpression(toString.getArgument(), isLambda) + ")";
            }
            case ToDateOperation toDate -> {
                return "datetime.datetime.strptime(" + generateExpression(toDate.getArgument(), isLambda)
                        + ", \"%Y-%m-%d\").date()";
            }
            case ToDateTimeOperation toDateTime -> {
                return "datetime.datetime.strptime(" + generateExpression(toDateTime.getArgument(), isLambda)
                        + ", \"%Y-%m-%d %H:%M:%S\")";
            }
            case ToIntOperation toInt -> {
                return "int(" + generateExpression(toInt.getArgument(), isLambda) + ")";
            }
            case ToTimeOperation toTime -> {
                return "datetime.datetime.strptime(" + generateExpression(toTime.getArgument(), isLambda)
                        + ", \"%H:%M:%S\").time()";
            }
            case ToZonedDateTimeOperation toZoned -> {
                return "rune_zoned_date_time(" + generateExpression(toZoned.getArgument(), isLambda) + ")";
            }
            case RosettaAbsentExpression absent -> {
                return "(not rune_attr_exists(" + generateExpression(absent.getArgument(), isLambda) + "))";
            }
            case RosettaBinaryOperation binary -> {
                return generateBinaryExpression(binary, isLambda);
            }
            case RosettaConditionalExpression cond -> {
                return generateConditionalExpression(cond, isLambda);
            }
            case RosettaConstructorExpression constructor -> {
                return generateConstructorExpression(constructor, isLambda);
            }
            case RosettaCountOperation count -> {
                return "rune_count(" + generateExpression(count.getArgument(), isLambda) + ")";
            }
            case RosettaDeepFeatureCall deepFeature -> {
                return "rune_resolve_deep_attr(self, \"" + deepFeature.getFeature().getName() + "\")";
            }
            case RosettaEnumValueReference enumRef -> {
                return enumRef.getEnumeration().getName() + "." + EnumHelper.convertValue(enumRef.getValue());
            }
            case RosettaExistsExpression exists -> {
                String arg = generateExpression(exists.getArgument(), isLambda);
                if (exists.getModifier() == ExistsModifier.SINGLE) {
                    return "rune_attr_exists(" + arg + ", \"single\")";
                } else if (exists.getModifier() == ExistsModifier.MULTIPLE) {
                    return "rune_attr_exists(" + arg + ", \"multiple\")";
                }
                return "rune_attr_exists(" + arg + ")";
            }
            case RosettaFeatureCall featureCall -> {
                return generateFeatureCall(featureCall, isLambda);
            }
            case RosettaOnlyElement onlyElement -> {
                return "rune_get_only_element(" + generateExpression(onlyElement.getArgument(), isLambda)
                        + ")";
            }
            case RosettaOnlyExistsExpression onlyExists -> {
                String args = onlyExists.getArgs().stream()
                        .map(arg -> generateExpression(arg, isLambda))
                        .collect(Collectors.joining(", "));
                return "rune_check_one_of(self, " + args + ")";
            }
            case RosettaSymbolReference symbolRef -> {
                return generateSymbolReference(symbolRef, isLambda);
            }
            case RosettaImplicitVariable implicit -> {
                return implicit.getName();
            }
            case WithMetaOperation withMeta -> {
                return generateWithMetaOperation(withMeta, isLambda);
            }
            default -> throw new UnsupportedOperationException(
                    "Unsupported expression type of " + expr.getClass().getSimpleName());
        }
    }

    private String generateConditionalExpression(RosettaConditionalExpression expr, boolean isLambda) {
        int currentId = generatedFunctionCounter++;
        String ifExpr = generateExpression(expr.getIf(), isLambda);
        String ifThen = generateExpression(expr.getIfthen(), isLambda);
        String elseThen = (expr.getElsethen() != null && expr.isFull())
                ? generateExpression(expr.getElsethen(), isLambda)
                : "True";
        String ifBlocks = """
                def _then_fn%d():
                    return %s

                def _else_fn%d():
                    return %s
                """.formatted(currentId, ifThen, currentId, elseThen).stripIndent();
        ifCondBlocks.add(ifBlocks);
        return "if_cond_fn(%s, _then_fn%d, _else_fn%d)".formatted(ifExpr, currentId, currentId);
    }

    private String generateFeatureCall(RosettaFeatureCall expr, boolean isLambda) {
        if (expr.getFeature() instanceof RosettaEnumValue evalue) {
            return generateEnumString(evalue);
        }
        String right = expr.getFeature().getName();
        if ("None".equals(right)) {
            right = "NONE";
        }
        String receiver = generateExpression(expr.getReceiver(), isLambda);
        return (receiver == null) ? right : "rune_resolve_attr(" + receiver + ", \"" + right + "\")";
    }

    private String generateThenOperation(ThenOperation expr, boolean isLambda) {
        InlineFunction funcExpr = expr.getFunction();
        String argExpr = generateExpression(expr.getArgument(), isLambda);

        String funcParams = funcExpr.getParameters().stream().map(ClosureParameter::getName)
                .collect(Collectors.joining(", "));
        String param = funcParams.isEmpty() ? "item" : funcParams;

        RosettaSymbol symbolToShadow = null;
        if (expr.getArgument() instanceof RosettaSymbolReference ref) {
            symbolToShadow = ref.getSymbol();
            shadowedSymbols.put(symbolToShadow, param);
        }

        try {
            String body = generateExpression(funcExpr.getBody(), true);
            String lambdaFunction = (funcParams.isEmpty()) ? "(lambda item: " + body + ")"
                    : "(lambda " + funcParams + ": " + body + ")";
            return lambdaFunction + "(" + argExpr + ")";
        } finally {
            if (symbolToShadow != null) {
                shadowedSymbols.remove(symbolToShadow);
            }
        }
    }

    private String generateFilterOperation(FilterOperation expr, boolean isLambda) {
        String argument = generateExpression(expr.getArgument(), isLambda);
        String param = expr.getFunction().getParameters().isEmpty() ? "item"
                : expr.getFunction().getParameters().get(0).getName();

        RosettaSymbol symbolToShadow = null;
        if (expr.getArgument() instanceof RosettaSymbolReference ref) {
            symbolToShadow = ref.getSymbol();
            shadowedSymbols.put(symbolToShadow, param);
        }

        try {
            String filterExpression = generateExpression(expr.getFunction().getBody(), true);
            return "rune_filter(" + argument + ", lambda " + param + ": " + filterExpression + ")";
        } finally {
            if (symbolToShadow != null) {
                shadowedSymbols.remove(symbolToShadow);
            }
        }
    }

    private String generateMapOperation(MapOperation expr, boolean isLambda) {
        InlineFunction inlineFunc = expr.getFunction();
        String param = inlineFunc.getParameters().isEmpty() ? "item" : inlineFunc.getParameters().get(0).getName();
        String argument = generateExpression(expr.getArgument(), isLambda);

        RosettaSymbol symbolToShadow = null;
        if (expr.getArgument() instanceof RosettaSymbolReference ref) {
            symbolToShadow = ref.getSymbol();
            shadowedSymbols.put(symbolToShadow, param);
        }

        try {
            String funcBody = generateExpression(inlineFunc.getBody(), true);
            String lambdaFunction = "lambda " + param + ": " + funcBody;
            return "list(map(" + lambdaFunction + ", " + argument + "))";
        } finally {
            if (symbolToShadow != null) {
                shadowedSymbols.remove(symbolToShadow);
            }
        }
    }

    private String generateConstructorExpression(RosettaConstructorExpression expr, boolean isLambda) {
        String type = (expr.getTypeCall() != null && expr.getTypeCall().getType() != null)
                ? expr.getTypeCall().getType().getName()
                : null;
        String fullyQualifiedType = RuneToPythonMapper.getBundleObjectName(expr.getTypeCall().getType());
        type = fullyQualifiedType;
        if (type != null) {
            return type + "(" + expr.getValues().stream()
                    .map(pair -> pair.getKey().getName() + "="
                            + generateExpression(pair.getValue(), isLambda))
                    .collect(Collectors.joining(", ")) + ")";
        } else {
            return "{" + expr.getValues().stream()
                    .map(pair -> "'" + pair.getKey().getName() + "': "
                            + generateExpression(pair.getValue(), isLambda))
                    .collect(Collectors.joining(", ")) + "}";
        }
    }

    private String getGuardExpression(SwitchCaseGuard caseGuard, boolean isLambda) {
        if (caseGuard == null) {
            throw new UnsupportedOperationException("Null SwitchCaseGuard");
        }
        RosettaExpression literalGuard = caseGuard.getLiteralGuard();
        if (literalGuard != null) {
            return "switchAttribute == " + generateExpression(literalGuard, isLambda);
        }
        RosettaEnumValue enumGuard = caseGuard.getEnumGuard();
        if (enumGuard != null) {
            return "switchAttribute == rune_resolve_attr(" + generateEnumString(enumGuard) + ",\""
                    + enumGuard.getName() + "\")";
        }
        RosettaFeature optionGuard = caseGuard.getChoiceOptionGuard();
        if (optionGuard != null) {
            return "rune_resolve_attr(switchAttribute,\"" + optionGuard.getName() + "\")";
        }
        Data dataGuard = caseGuard.getDataGuard();
        if (dataGuard != null) {
            return "rune_resolve_attr(switchAttribute,\"" + dataGuard.getName() + "\")";
        }
        throw new UnsupportedOperationException("Unsupported SwitchCaseGuard type");
    }

    private String generateSymbolReference(RosettaSymbolReference expr, boolean isLambda) {

        RosettaSymbol symbol = expr.getSymbol();
        if (symbol instanceof Data || symbol instanceof RosettaEnumeration) {
            return symbol.getName();
        } else if (symbol instanceof Attribute attr) {
            return generateAttributeReference(attr, isLambda);
        } else if (symbol instanceof RosettaEnumValue evalue) {
            return generateEnumString(evalue);
        } else if (symbol instanceof RosettaCallableWithArgs callable) {
            return generateCallableWithArgsCall(callable, expr, isLambda);
        } else if (symbol instanceof ClosureParameter) {
            return symbol.getName();
        } else if (symbol instanceof ShortcutDeclaration) {
            return "rune_resolve_attr(self, \"" + symbol.getName() + "\")";
        } else {
            throw new UnsupportedOperationException(
                    "Unsupported symbol reference for: " + symbol.getClass().getSimpleName());
        }
    }

    private String generateAttributeReference(Attribute s, boolean isLambda) {
        if (shadowedSymbols.containsKey(s)) {
            return shadowedSymbols.get(s);
        }
        if (isLambda) {
            boolean notInput = true;
            if (s.eContainer() instanceof FunctionImpl c) {
                for (Attribute inputAttr : c.getInputs()) {
                    if (inputAttr.getName().equals(s.getName())) {
                        notInput = false;
                        break;
                    }
                }
            }
            return notInput ? "rune_resolve_attr(item, \"" + s.getName() + "\")"
                    : "rune_resolve_attr(self, \"" + s.getName() + "\")";
        } else {
            return "rune_resolve_attr(self, \"" + s.getName() + "\")";
        }
    }

    private String generateEnumString(RosettaEnumValue rev) {
        String value = EnumHelper.convertValue(rev);
        RosettaEnumeration parent = rev.getEnumeration();
        String parentName = parent.getName();
        String modelName = parent.getModel().getName();
        return modelName + "." + parentName + "." + parentName + "." + value;
    }

    private String generateCallableWithArgsCall(RosettaCallableWithArgs s, RosettaSymbolReference expr,
            boolean isLambda) {
        // Dependency handled by PythonFunctionDependencyProvider
        String args = expr.getArgs().stream().map(arg -> generateExpression(arg, isLambda))
                .collect(Collectors.joining(", "));
        String funcName = s.getName();
        if ("Max".equals(funcName)) {
            funcName = "max";
        } else if ("Min".equals(funcName)) {
            funcName = "min";
        } else {
            funcName = RuneToPythonMapper.getBundleObjectName(s);
        }
        return funcName + "(" + args + ")";
    }

    private String generateBinaryExpression(RosettaBinaryOperation expr, boolean isLambda) {

        if (expr instanceof ModifiableBinaryOperation mod) {
            if (mod.getCardMod() == null) {
                throw new UnsupportedOperationException(
                        "ModifiableBinaryOperation with expressions with no cardinality");
            }
            if ("<>".equals(mod.getOperator())) {
                return "rune_any_elements(" + generateExpression(mod.getLeft(), isLambda) + ", \""
                        + mod.getOperator() + "\", " + generateExpression(mod.getRight(), isLambda) + ")";
            } else {
                return "rune_all_elements(" + generateExpression(mod.getLeft(), isLambda) + ", \""
                        + mod.getOperator() + "\", " + generateExpression(mod.getRight(), isLambda) + ")";
            }
        } else {
            return switch (expr.getOperator()) {
                case "=" -> "(" + generateExpression(expr.getLeft(), isLambda) + " == "
                        + generateExpression(expr.getRight(), isLambda) + ")";
                case "<>" -> "(" + generateExpression(expr.getLeft(), isLambda) + " != "
                        + generateExpression(expr.getRight(), isLambda) + ")";
                case "contains" -> "rune_contains(" + generateExpression(expr.getLeft(), isLambda) + ", "
                        + generateExpression(expr.getRight(), isLambda) + ")";
                case "disjoint" -> "rune_disjoint(" + generateExpression(expr.getLeft(), isLambda) + ", "
                        + generateExpression(expr.getRight(), isLambda) + ")";
                case "join" -> generateExpression(expr.getRight(), isLambda) + ".join("
                        + generateExpression(expr.getLeft(), isLambda) + ")";
                case "default" -> "(" + generateExpression(expr.getLeft(), isLambda) + " if "
                        + generateExpression(expr.getLeft(), isLambda) + " is not None else "
                        + generateExpression(expr.getRight(), isLambda) + ")";
                default -> "(" + generateExpression(expr.getLeft(), isLambda) + " " + expr.getOperator() + " "
                        + generateExpression(expr.getRight(), isLambda) + ")";
            };
        }
    }

    public String generateTypeOrFunctionConditions(Data cls) {
        int nConditions = 0;
        StringBuilder result = new StringBuilder();
        for (Condition cond : cls.getConditions()) {
            result.append(generateConditionBoilerPlate(cond, nConditions));
            if (isConstraintCondition(cond)) {
                result.append(generateConstraintCondition(cls, cond));
            } else {
                result.append(generateIfThenElseOrSwitch(cond));
            }
            nConditions++;
        }
        return result.toString();
    }

    public String generateFunctionConditions(List<Condition> conditions, String conditionType) {

        int nConditions = 0;
        StringBuilder result = new StringBuilder();
        for (Condition cond : conditions) {
            result.append(generateFunctionConditionBoilerPlate(cond, nConditions, conditionType));
            result.append(generateIfThenElseOrSwitch(cond));

            nConditions++;
        }
        return result.toString();
    }

    private boolean isConstraintCondition(Condition cond) {
        return isOneOf(cond) || isChoice(cond);
    }

    private boolean isOneOf(Condition cond) {
        return cond.getExpression() instanceof OneOfOperation;
    }

    private boolean isChoice(Condition cond) {
        return cond.getExpression() instanceof ChoiceOperation;
    }

    private String generateConditionBoilerPlate(Condition cond, int nConditions) {
        PythonCodeWriter writer = new PythonCodeWriter();
        writer.newLine();
        writer.appendLine("@rune_condition");
        String name = cond.getName() != null ? cond.getName() : "";
        writer.appendLine("def condition_" + nConditions + "_" + name + "(self):");
        writer.indent();
        if (cond.getDefinition() != null) {
            writer.appendLine("\"\"\"");
            writer.appendLine(cond.getDefinition());
            writer.appendLine("\"\"\"");
        }
        writer.appendLine("item = self");
        return writer.toString();
    }

    private String generateFunctionConditionBoilerPlate(Condition cond, int nConditions, String conditionType) {
        PythonCodeWriter writer = new PythonCodeWriter();
        writer.newLine();
        writer.appendLine("@rune_local_condition(" + conditionType + ")");
        String name = cond.getName() != null ? cond.getName() : "";
        writer.appendLine("def condition_" + nConditions + "_" + name + "():");
        writer.indent();
        if (cond.getDefinition() != null) {
            writer.appendLine("\"\"\"");
            writer.appendLine(cond.getDefinition());
            writer.appendLine("\"\"\"");
        }
        writer.appendLine("item = self");
        return writer.toString();
    }

    private String generateConstraintCondition(Data cls, Condition cond) {
        RosettaExpression expression = cond.getExpression();
        List<Attribute> attributes = cls.getAttributes();
        String necessity = "necessity=True";
        if (expression instanceof ChoiceOperation choice) {
            attributes = choice.getAttributes();
            if (choice.getNecessity() == Necessity.OPTIONAL) {
                necessity = "necessity=False";
            }
        }
        String attrs = attributes.stream().map(a -> "'" + a.getName() + "'").collect(Collectors.joining(", "));
        PythonCodeWriter writer = new PythonCodeWriter();
        writer.indent();
        writer.appendLine("return rune_check_one_of(self, " + attrs + ", " + necessity + ")");
        return writer.toString();
    }


    /**
     * Generates an if-then-else or switch statement for a condition.
     * 
     * @param c The condition to generate an if-then-else or switch statement for.
     * @return The generated if-then-else or switch statement.
     */
    private String generateIfThenElseOrSwitch(Condition c) {
        clearBlocks();
        resetCounters();
        String expr = generateExpression(c.getExpression(), false);

        PythonCodeWriter writer = new PythonCodeWriter();
        writer.indent();

        if (!ifCondBlocks.isEmpty()) {
            for (String arg : ifCondBlocks) {
                writer.appendBlock(arg);
                writer.appendLine("");
            }
        }
        writer.appendLine("return " + expr);
        return writer.toString();
    }

    // ... (helper methods like isConstraintCondition, etc. remain unchanged) ...

    private String generateSwitchOperation(SwitchOperation expr, boolean isLambda) {
        String attr = generateExpression(expr.getArgument(), isLambda);
        PythonCodeWriter writer = new PythonCodeWriter();

        String switchFuncName = "_switch_fn_" + generatedFunctionCounter++;

        writer.appendLine("def " + switchFuncName + "():");
        writer.indent();

        var cases = expr.getCases();
        for (int i = 0; i < cases.size(); i++) {
            var currentCase = cases.get(i);
            String funcName = currentCase.isDefault() ? "_then_default" : "_then_" + (i + 1);
            String thenExprDef = currentCase.isDefault() ? generateExpression(expr.getDefault(), isLambda)
                    : generateExpression(currentCase.getExpression(), isLambda);

            writer.appendLine("def " + funcName + "():");
            writer.indent();
            writer.appendLine("return " + thenExprDef);
            writer.unindent();
        }

        writer.appendLine("switchAttribute = " + attr);

        for (int i = 0; i < cases.size(); i++) {
            var currentCase = cases.get(i);
            String funcName = currentCase.isDefault() ? "_then_default" : "_then_" + (i + 1);
            if (currentCase.isDefault()) {
                writer.appendLine("else:");
            } else {
                SwitchCaseGuard guard = currentCase.getGuard();
                String prefix = (i == 0) ? "if " : "elif ";
                writer.appendLine(prefix + getGuardExpression(guard, isLambda) + ":");
            }
            writer.indent();
            writer.appendLine("return " + funcName + "()");
            writer.unindent();
        }

        writer.unindent();

        ifCondBlocks.add(writer.toString());
        return switchFuncName + "()";
    }

    private String generateWithMetaOperation(WithMetaOperation expr, boolean isLambda) {
        String arg = generateExpression(expr.getArgument(), isLambda);
        String entries = expr.getEntries().stream()
                .map(entry -> {
                    String key = entry.getKey().getName();
                    String mappedKey = switch (key) {
                        case "scheme" -> "@scheme";
                        case "id", "key" -> "@key";
                        case "reference" -> "@ref";
                        case "location" -> "@key:scoped";
                        case "address" -> "@ref:scoped";
                        default -> key;
                    };
                    return "'" + mappedKey + "': " + generateExpression(entry.getValue(), isLambda);
                })
                .collect(Collectors.joining(", "));
        return "rune_with_meta(" + arg + ", {" + entries + "})";
    }
}
