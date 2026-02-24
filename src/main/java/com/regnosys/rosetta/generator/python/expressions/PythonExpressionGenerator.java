package com.regnosys.rosetta.generator.python.expressions;

import java.util.ArrayList;
import java.util.List;
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
     * Result of an expression generation.
     * 
     * @param expression      The generated Python code.
     * @param companionBlocks The list of required companion code blocks.
     */
    public record ExpressionResult(String expression, List<String> companionBlocks) {
    }

    /**
     * The list of if condition blocks.
     */
    private final List<String> ifCondBlocks = new ArrayList<>();


    /**
     * The counter for generated helper functions.
     */
    private int generatedFunctionCounter = 0;

    /**
     * Clears the block list.
     */
    private void clearBlocks() {
        ifCondBlocks.clear();
    }

    /**
     * Resets the generator state for a new lifecycle (e.g., a new function or condition).
     */
    public void initialize() {
        generatedFunctionCounter = 0;
    }


    /**
     * Generates Python code for an expression and returns it along with any required 
     * companion blocks. This method automatically manages internal state (clearing blocks).
     * 
     * @param expr  The Rune expression to generate Python code for.
     * @param scope The current expression scope.
     * @return The generated Python code and its companion blocks.
     */
    public ExpressionResult generate(RosettaExpression expr, PythonExpressionScope scope) {
        clearBlocks();
        String code = generateExpression(expr, scope);
        List<String> blocks = new ArrayList<>(ifCondBlocks);
        clearBlocks();
        return new ExpressionResult(code, blocks);
    }

    /**
     * Generates Python code for a Rune expression.
     * 
     * @param expr  The Rune expression to generate Python code for.
     * @param scope The current expression scope.
     * @return The generated Python code.
     */
    private String generateExpression(RosettaExpression expr, PythonExpressionScope scope) {

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
                return "{" + generateExpression(asKey.getArgument(), scope) + ": True}";
            }
            case DistinctOperation distinct -> {
                return "set(" + generateExpression(distinct.getArgument(), scope) + ")";
            }
            case FilterOperation filter -> {
                return generateFilterOperation(filter, scope);
            }
            case FirstOperation first -> {
                return generateExpression(first.getArgument(), scope) + "[0]";
            }
            case FlattenOperation flatten -> {
                return "rune_flatten_list(" + generateExpression(flatten.getArgument(), scope) + ")";
            }
            case ListLiteral listLiteral -> {
                return "[" + listLiteral.getElements().stream()
                        .map(arg -> generateExpression(arg, scope))
                        .collect(Collectors.joining(", ")) + "]";
            }
            case LastOperation last -> {
                return generateExpression(last.getArgument(), scope) + "[-1]";
            }
            case MapOperation mapOp -> {
                return generateMapOperation(mapOp, scope);
            }
            case MaxOperation maxOp -> {
                return "max(" + generateExpression(maxOp.getArgument(), scope) + ")";
            }
            case MinOperation minOp -> {
                return "min(" + generateExpression(minOp.getArgument(), scope) + ")";
            }
            case SortOperation sort -> {
                return "sorted(" + generateExpression(sort.getArgument(), scope) + ")";
            }
            case ThenOperation then -> {
                return generateThenOperation(then, scope);
            }
            case SumOperation sum -> {
                return "sum(" + generateExpression(sum.getArgument(), scope) + ")";
            }
            case ReverseOperation reverse -> {
                return "list(reversed(" + generateExpression(reverse.getArgument(), scope) + "))";
            }
            case SwitchOperation switchOp -> {
                return generateSwitchOperation(switchOp, scope);
            }
            case ToEnumOperation toEnum -> {
                return toEnum.getEnumeration().getName() + "("
                        + generateExpression(toEnum.getArgument(), scope) + ")";
            }
            case ToStringOperation toString -> {
                return "rune_str(" + generateExpression(toString.getArgument(), scope) + ")";
            }
            case ToDateOperation toDate -> {
                return "datetime.datetime.strptime(" + generateExpression(toDate.getArgument(), scope)
                        + ", \"%Y-%m-%d\").date()";
            }
            case ToDateTimeOperation toDateTime -> {
                return "datetime.datetime.strptime(" + generateExpression(toDateTime.getArgument(), scope)
                        + ", \"%Y-%m-%d %H:%M:%S\")";
            }
            case ToIntOperation toInt -> {
                return "int(" + generateExpression(toInt.getArgument(), scope) + ")";
            }
            case ToTimeOperation toTime -> {
                return "datetime.datetime.strptime(" + generateExpression(toTime.getArgument(), scope)
                        + ", \"%H:%M:%S\").time()";
            }
            case ToZonedDateTimeOperation toZoned -> {
                return "rune_zoned_date_time(" + generateExpression(toZoned.getArgument(), scope) + ")";
            }
            case RosettaAbsentExpression absent -> {
                return "(not rune_attr_exists(" + generateExpression(absent.getArgument(), scope) + "))";
            }
            case RosettaBinaryOperation binary -> {
                return generateBinaryExpression(binary, scope);
            }
            case RosettaConditionalExpression cond -> {
                return generateConditionalExpression(cond, scope);
            }
            case RosettaConstructorExpression constructor -> {
                return generateConstructorExpression(constructor, scope);
            }
            case RosettaCountOperation count -> {
                return "rune_count(" + generateExpression(count.getArgument(), scope) + ")";
            }
            case RosettaDeepFeatureCall deepFeature -> {
                return "rune_resolve_deep_attr(self, \"" + deepFeature.getFeature().getName() + "\")";
            }
            case RosettaEnumValueReference enumRef -> {
                return enumRef.getEnumeration().getName() + "." + EnumHelper.convertValue(enumRef.getValue());
            }
            case RosettaExistsExpression exists -> {
                String arg = generateExpression(exists.getArgument(), scope);
                if (exists.getModifier() == ExistsModifier.SINGLE) {
                    return "rune_attr_exists(" + arg + ", \"single\")";
                } else if (exists.getModifier() == ExistsModifier.MULTIPLE) {
                    return "rune_attr_exists(" + arg + ", \"multiple\")";
                }
                return "rune_attr_exists(" + arg + ")";
            }
            case RosettaFeatureCall featureCall -> {
                return generateFeatureCall(featureCall, scope);
            }
            case RosettaOnlyElement onlyElement -> {
                return "rune_get_only_element(" + generateExpression(onlyElement.getArgument(), scope)
                        + ")";
            }
            case RosettaOnlyExistsExpression onlyExists -> {
                String args = onlyExists.getArgs().stream()
                        .map(arg -> generateExpression(arg, scope))
                        .collect(Collectors.joining(", "));
                return "rune_check_one_of(self, " + args + ")";
            }
            case RosettaSymbolReference symbolRef -> {
                return generateSymbolReference(symbolRef, scope);
            }
            case RosettaImplicitVariable implicit -> {
                return implicit.getName();
            }
            case WithMetaOperation withMeta -> {
                return generateWithMetaOperation(withMeta, scope);
            }
            default -> throw new UnsupportedOperationException(
                    "Unsupported expression type of " + expr.getClass().getSimpleName());
        }
    }

    private String generateConditionalExpression(RosettaConditionalExpression expr, PythonExpressionScope scope) {
        int currentId = generatedFunctionCounter++;
        String ifExpr = generateExpression(expr.getIf(), scope);
        String ifThen = generateExpression(expr.getIfthen(), scope);
        String elseThen = (expr.getElsethen() != null && expr.isFull())
                ? generateExpression(expr.getElsethen(), scope)
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

    private String generateFeatureCall(RosettaFeatureCall expr, PythonExpressionScope scope) {
        if (expr.getFeature() instanceof RosettaEnumValue evalue) {
            return generateEnumString(evalue);
        }
        String right = expr.getFeature().getName();
        if ("None".equals(right)) {
            right = "NONE";
        }
        String receiver = generateExpression(expr.getReceiver(), scope);
        return (receiver == null) ? right : "rune_resolve_attr(" + receiver + ", \"" + right + "\")";
    }

    private String generateThenOperation(ThenOperation expr, PythonExpressionScope scope) {
        InlineFunction funcExpr = expr.getFunction();
        String argExpr = generateExpression(expr.getArgument(), scope);

        String funcParams = funcExpr.getParameters().stream().map(ClosureParameter::getName)
                .collect(Collectors.joining(", "));
        String param = funcParams.isEmpty() ? "item" : funcParams;

        PythonExpressionScope subScope = scope.withReceiver(param);
        if (expr.getArgument() instanceof RosettaSymbolReference ref) {
            subScope = subScope.withShadow(ref.getSymbol(), param);
        }

        String body = generateExpression(funcExpr.getBody(), subScope);
        String lambdaFunction = (funcParams.isEmpty()) ? "(lambda item: " + body + ")"
                : "(lambda " + funcParams + ": " + body + ")";
        return lambdaFunction + "(" + argExpr + ")";
    }

    private String generateFilterOperation(FilterOperation expr, PythonExpressionScope scope) {
        String argument = generateExpression(expr.getArgument(), scope);
        String param = expr.getFunction().getParameters().isEmpty() ? "item"
                : expr.getFunction().getParameters().get(0).getName();

        PythonExpressionScope subScope = scope.withReceiver(param);
        if (expr.getArgument() instanceof RosettaSymbolReference ref) {
            subScope = subScope.withShadow(ref.getSymbol(), param);
        }

        String filterExpression = generateExpression(expr.getFunction().getBody(), subScope);
        return "rune_filter(" + argument + ", lambda " + param + ": " + filterExpression + ")";
    }

    private String generateMapOperation(MapOperation expr, PythonExpressionScope scope) {
        InlineFunction inlineFunc = expr.getFunction();
        String param = inlineFunc.getParameters().isEmpty() ? "item" : inlineFunc.getParameters().get(0).getName();
        String argument = generateExpression(expr.getArgument(), scope);

        PythonExpressionScope subScope = scope.withReceiver(param);
        if (expr.getArgument() instanceof RosettaSymbolReference ref) {
            subScope = subScope.withShadow(ref.getSymbol(), param);
        }

        String funcBody = generateExpression(inlineFunc.getBody(), subScope);
        String lambdaFunction = "lambda " + param + ": " + funcBody;
        return "list(map(" + lambdaFunction + ", " + argument + "))";
    }

    private String generateConstructorExpression(RosettaConstructorExpression expr, PythonExpressionScope scope) {
        String type = (expr.getTypeCall() != null && expr.getTypeCall().getType() != null)
                ? expr.getTypeCall().getType().getName()
                : null;
        String fullyQualifiedType = RuneToPythonMapper.getBundleObjectName(expr.getTypeCall().getType());
        type = fullyQualifiedType;
        if (type != null) {
            return type + "(" + expr.getValues().stream()
                    .map(pair -> pair.getKey().getName() + "="
                            + generateExpression(pair.getValue(), scope))
                    .collect(Collectors.joining(", ")) + ")";
        } else {
            return "{" + expr.getValues().stream()
                    .map(pair -> "'" + pair.getKey().getName() + "': "
                            + generateExpression(pair.getValue(), scope))
                    .collect(Collectors.joining(", ")) + "}";
        }
    }

    private String getGuardExpression(SwitchCaseGuard caseGuard, PythonExpressionScope scope) {
        if (caseGuard == null) {
            throw new UnsupportedOperationException("Null SwitchCaseGuard");
        }
        RosettaExpression literalGuard = caseGuard.getLiteralGuard();
        if (literalGuard != null) {
            return "switchAttribute == " + generateExpression(literalGuard, scope);
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

    private String generateSymbolReference(RosettaSymbolReference expr, PythonExpressionScope scope) {

        RosettaSymbol symbol = expr.getSymbol();
        if (symbol instanceof Data || symbol instanceof RosettaEnumeration) {
            return symbol.getName();
        } else if (symbol instanceof Attribute attr) {
            return generateAttributeReference(attr, scope);
        } else if (symbol instanceof RosettaEnumValue evalue) {
            return generateEnumString(evalue);
        } else if (symbol instanceof RosettaCallableWithArgs callable) {
            return generateCallableWithArgsCall(callable, expr, scope);
        } else if (symbol instanceof ClosureParameter) {
            return symbol.getName();
        } else if (symbol instanceof ShortcutDeclaration) {
            return "rune_resolve_attr(self, \"" + symbol.getName() + "\")";
        } else {
            throw new UnsupportedOperationException(
                    "Unsupported symbol reference for: " + symbol.getClass().getSimpleName());
        }
    }

    private String generateAttributeReference(Attribute s, PythonExpressionScope scope) {
        if (scope.shadowedSymbols().containsKey(s)) {
            return scope.shadowedSymbols().get(s);
        }
        boolean isInput = false;
        if (s.eContainer() instanceof FunctionImpl c) {
            for (Attribute inputAttr : c.getInputs()) {
                if (inputAttr.getName().equals(s.getName())) {
                    isInput = true;
                    break;
                }
            }
        }
        String receiver = isInput ? "self" : scope.receiver();
        return "rune_resolve_attr(" + receiver + ", \"" + s.getName() + "\")";
    }

    private String generateEnumString(RosettaEnumValue rev) {
        String value = EnumHelper.convertValue(rev);
        RosettaEnumeration parent = rev.getEnumeration();
        String parentName = parent.getName();
        String modelName = parent.getModel().getName();
        return modelName + "." + parentName + "." + parentName + "." + value;
    }

    private String generateCallableWithArgsCall(RosettaCallableWithArgs s, RosettaSymbolReference expr,
            PythonExpressionScope scope) {
        // Dependency handled by PythonFunctionDependencyProvider
        String args = expr.getArgs().stream().map(arg -> generateExpression(arg, scope))
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

    private String generateBinaryExpression(RosettaBinaryOperation expr, PythonExpressionScope scope) {

        if (expr instanceof ModifiableBinaryOperation mod) {
            if (mod.getCardMod() == null) {
                throw new UnsupportedOperationException(
                        "ModifiableBinaryOperation with expressions with no cardinality");
            }
            if ("<>".equals(mod.getOperator())) {
                return "rune_any_elements(" + generateExpression(mod.getLeft(), scope) + ", \""
                        + mod.getOperator() + "\", " + generateExpression(mod.getRight(), scope) + ")";
            } else {
                return "rune_all_elements(" + generateExpression(mod.getLeft(), scope) + ", \""
                        + mod.getOperator() + "\", " + generateExpression(mod.getRight(), scope) + ")";
            }
        } else {
            return switch (expr.getOperator()) {
                case "=" -> "(" + generateExpression(expr.getLeft(), scope) + " == "
                        + generateExpression(expr.getRight(), scope) + ")";
                case "<>" -> "(" + generateExpression(expr.getLeft(), scope) + " != "
                        + generateExpression(expr.getRight(), scope) + ")";
                case "contains" -> "rune_contains(" + generateExpression(expr.getLeft(), scope) + ", "
                        + generateExpression(expr.getRight(), scope) + ")";
                case "disjoint" -> "rune_disjoint(" + generateExpression(expr.getLeft(), scope) + ", "
                        + generateExpression(expr.getRight(), scope) + ")";
                case "join" -> generateExpression(expr.getRight(), scope) + ".join("
                        + generateExpression(expr.getLeft(), scope) + ")";
                case "default" -> "(" + generateExpression(expr.getLeft(), scope) + " if "
                        + generateExpression(expr.getLeft(), scope) + " is not None else "
                        + generateExpression(expr.getRight(), scope) + ")";
                default -> "(" + generateExpression(expr.getLeft(), scope) + " " + expr.getOperator() + " "
                        + generateExpression(expr.getRight(), scope) + ")";
            };
        }
    }

    public String generateTypeConditions(Data cls) {
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
        initialize();
        ExpressionResult result = generate(c.getExpression(), PythonExpressionScope.of("self"));

        PythonCodeWriter writer = new PythonCodeWriter();
        writer.indent();

        if (!result.companionBlocks().isEmpty()) {
            for (String block : result.companionBlocks()) {
                writer.appendBlock(block);
                writer.appendLine("");
            }
        }
        writer.appendLine("return " + result.expression());
        return writer.toString();
    }

    // ... (helper methods like isConstraintCondition, etc. remain unchanged) ...

    private String generateSwitchOperation(SwitchOperation expr, PythonExpressionScope scope) {
        String attr = generateExpression(expr.getArgument(), scope);
        PythonCodeWriter writer = new PythonCodeWriter();

        String switchFuncName = "_switch_fn_" + generatedFunctionCounter++;

        writer.appendLine("def " + switchFuncName + "():");
        writer.indent();

        var cases = expr.getCases();
        for (int i = 0; i < cases.size(); i++) {
            var currentCase = cases.get(i);
            String funcName = currentCase.isDefault() ? "_then_default" : "_then_" + (i + 1);
            String thenExprDef = currentCase.isDefault() ? generateExpression(expr.getDefault(), scope)
                    : generateExpression(currentCase.getExpression(), scope);

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
                writer.appendLine(prefix + getGuardExpression(guard, scope) + ":");
            }
            writer.indent();
            writer.appendLine("return " + funcName + "()");
            writer.unindent();
        }

        writer.unindent();

        ifCondBlocks.add(writer.toString());
        return switchFuncName + "()";
    }

    private String generateWithMetaOperation(WithMetaOperation expr, PythonExpressionScope scope) {
        String arg = generateExpression(expr.getArgument(), scope);
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
                    return "'" + mappedKey + "': " + generateExpression(entry.getValue(), scope);
                })
                .collect(Collectors.joining(", "));
        return "rune_with_meta(" + arg + ", {" + entries + "})";
    }
}
