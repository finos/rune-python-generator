package com.regnosys.rosetta.generator.python.expressions;

import com.regnosys.rosetta.generator.java.enums.EnumHelper;
import com.regnosys.rosetta.rosetta.*;
import com.regnosys.rosetta.rosetta.expression.*;
import com.regnosys.rosetta.rosetta.simple.Attribute;
import com.regnosys.rosetta.rosetta.simple.Condition;
import com.regnosys.rosetta.rosetta.simple.Data;
import com.regnosys.rosetta.rosetta.simple.ShortcutDeclaration;
import com.regnosys.rosetta.rosetta.simple.impl.FunctionImpl;
import com.regnosys.rosetta.generator.python.util.PythonCodeWriter;
import com.regnosys.rosetta.generator.python.util.RuneToPythonMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Generate Python for Rune Expressions
 */
public class PythonExpressionGenerator {

    private List<String> ifCondBlocks = new ArrayList<>();
    private boolean isSwitchCond = false;

    public List<String> getIfCondBlocks() {
        return ifCondBlocks;
    }

    public void setIfCondBlocks(List<String> ifCondBlocks) {
        this.ifCondBlocks = ifCondBlocks;
    }

    public boolean isSwitchCond() {
        return isSwitchCond;
    }

    public String generateExpression(RosettaExpression expr, int ifLevel, boolean isLambda) {

        if (expr == null)
            return "None";

        if (expr instanceof RosettaBooleanLiteral bool) {
            return bool.isValue() ? "True" : "False";
        } else if (expr instanceof RosettaIntLiteral i) {
            return String.valueOf(i.getValue());
        } else if (expr instanceof RosettaNumberLiteral n) {
            return n.getValue().toString();
        } else if (expr instanceof RosettaStringLiteral s) {
            return "\"" + s.getValue() + "\"";
        } else if (expr instanceof AsKeyOperation asKey) {
            return "{" + generateExpression(asKey.getArgument(), ifLevel, isLambda) + ": True}";
        } else if (expr instanceof DistinctOperation distinct) {
            return "set(" + generateExpression(distinct.getArgument(), ifLevel, isLambda) + ")";
        } else if (expr instanceof FilterOperation filter) {
            return generateFilterOperation(filter, ifLevel, isLambda);
        } else if (expr instanceof FirstOperation first) {
            return generateExpression(first.getArgument(), ifLevel, isLambda) + "[0]";
        } else if (expr instanceof FlattenOperation flatten) {
            return "rune_flatten_list(" + generateExpression(flatten.getArgument(), ifLevel, isLambda) + ")";
        } else if (expr instanceof ListLiteral listLiteral) {
            return "[" + listLiteral.getElements().stream()
                    .map(arg -> generateExpression(arg, ifLevel, isLambda))
                    .collect(Collectors.joining(", ")) + "]";
        } else if (expr instanceof LastOperation last) {
            return generateExpression(last.getArgument(), ifLevel, isLambda) + "[-1]";
        } else if (expr instanceof MapOperation mapOp) {
            return generateMapOperation(mapOp, ifLevel, isLambda);
        } else if (expr instanceof MaxOperation maxOp) {
            return "max(" + generateExpression(maxOp.getArgument(), ifLevel, isLambda) + ")";
        } else if (expr instanceof MinOperation minOp) {
            return "min(" + generateExpression(minOp.getArgument(), ifLevel, isLambda) + ")";
        } else if (expr instanceof SortOperation sort) {
            return "sorted(" + generateExpression(sort.getArgument(), ifLevel, isLambda) + ")";
        } else if (expr instanceof ThenOperation then) {
            return generateThenOperation(then, ifLevel, isLambda);
        } else if (expr instanceof SumOperation sum) {
            return "sum(" + generateExpression(sum.getArgument(), ifLevel, isLambda) + ")";
        } else if (expr instanceof SwitchOperation switchOp) {
            return generateSwitchOperation(switchOp, ifLevel, isLambda);
        } else if (expr instanceof ToEnumOperation toEnum) {
            return toEnum.getEnumeration().getName() + "("
                    + generateExpression(toEnum.getArgument(), ifLevel, isLambda) + ")";
        } else if (expr instanceof ToStringOperation toString) {
            return "rune_str(" + generateExpression(toString.getArgument(), ifLevel, isLambda) + ")";
        } else if (expr instanceof ToDateOperation toDate) {
            return "datetime.datetime.strptime(" + generateExpression(toDate.getArgument(), ifLevel, isLambda)
                    + ", \"%Y-%m-%d\").date()";
        } else if (expr instanceof ToDateTimeOperation toDateTime) {
            return "datetime.datetime.strptime(" + generateExpression(toDateTime.getArgument(), ifLevel, isLambda)
                    + ", \"%Y-%m-%d %H:%M:%S\")";
        } else if (expr instanceof ToIntOperation toInt) {
            return "int(" + generateExpression(toInt.getArgument(), ifLevel, isLambda) + ")";
        } else if (expr instanceof ToTimeOperation toTime) {
            return "datetime.datetime.strptime(" + generateExpression(toTime.getArgument(), ifLevel, isLambda)
                    + ", \"%H:%M:%S\").time()";
        } else if (expr instanceof ToZonedDateTimeOperation toZoned) {
            return "rune_zoned_date_time(" + generateExpression(toZoned.getArgument(), ifLevel, isLambda) + ")";
        } else if (expr instanceof RosettaAbsentExpression absent) {
            return "(not rune_attr_exists(" + generateExpression(absent.getArgument(), ifLevel, isLambda) + "))";
        } else if (expr instanceof RosettaBinaryOperation binary) {
            return generateBinaryExpression(binary, ifLevel, isLambda);
        } else if (expr instanceof RosettaConditionalExpression cond) {
            return generateConditionalExpression(cond, ifLevel, isLambda);
        } else if (expr instanceof RosettaConstructorExpression constructor) {
            return generateConstructorExpression(constructor, ifLevel, isLambda);
        } else if (expr instanceof RosettaCountOperation count) {
            return "rune_count(" + generateExpression(count.getArgument(), ifLevel, isLambda) + ")";
        } else if (expr instanceof RosettaDeepFeatureCall deepFeature) {
            return "rune_resolve_deep_attr(self, \"" + deepFeature.getFeature().getName() + "\")";
        } else if (expr instanceof RosettaEnumValueReference enumRef) {
            return enumRef.getEnumeration().getName() + "." + EnumHelper.convertValue(enumRef.getValue());
        } else if (expr instanceof RosettaExistsExpression exists) {
            return "rune_attr_exists(" + generateExpression(exists.getArgument(), ifLevel, isLambda) + ")";
        } else if (expr instanceof RosettaFeatureCall featureCall) {
            return generateFeatureCall(featureCall, ifLevel, isLambda);
        } else if (expr instanceof RosettaOnlyElement onlyElement) {
            return "rune_get_only_element(" + generateExpression(onlyElement.getArgument(), ifLevel, isLambda) + ")";
        } else if (expr instanceof RosettaOnlyExistsExpression onlyExists) {
            return "rune_check_one_of(self, " + generateExpression(onlyExists.getArgs().get(0), ifLevel, isLambda)
                    + ")";
        } else if (expr instanceof RosettaSymbolReference symbolRef) {
            return generateSymbolReference(symbolRef, ifLevel, isLambda);
        } else if (expr instanceof RosettaImplicitVariable implicit) {
            return implicit.getName();
        } else {
            throw new UnsupportedOperationException(
                    "Unsupported expression type of " + expr.getClass().getSimpleName());
        }
    }

    private String generateConditionalExpression(RosettaConditionalExpression expr, int ifLevel, boolean isLambda) {
        String ifExpr = generateExpression(expr.getIf(), ifLevel + 1, isLambda);
        String ifThen = generateExpression(expr.getIfthen(), ifLevel + 1, isLambda);
        String elseThen = (expr.getElsethen() != null && expr.isFull())
                ? generateExpression(expr.getElsethen(), ifLevel + 1, isLambda)
                : "True";
        String ifBlocks = """
                def _then_fn%d():
                    return %s

                def _else_fn%d():
                    return %s
                """.formatted(ifLevel, ifThen, ifLevel, elseThen).stripIndent();
        ifCondBlocks.add(ifBlocks);
        return "if_cond_fn(%s, _then_fn%d, _else_fn%d)".formatted(ifExpr, ifLevel, ifLevel);
    }

    private String generateFeatureCall(RosettaFeatureCall expr, int ifLevel, boolean isLambda) {
        if (expr.getFeature() instanceof RosettaEnumValue evalue) {
            return generateEnumString(evalue);
        }
        String right = expr.getFeature().getName();
        if ("None".equals(right)) {
            right = "NONE";
        }
        String receiver = generateExpression(expr.getReceiver(), ifLevel, isLambda);
        return (receiver == null) ? right : "rune_resolve_attr(" + receiver + ", \"" + right + "\")";
    }

    private String generateThenOperation(ThenOperation expr, int ifLevel, boolean isLambda) {
        InlineFunction funcExpr = expr.getFunction();
        String argExpr = generateExpression(expr.getArgument(), ifLevel, isLambda);
        String body = generateExpression(funcExpr.getBody(), ifLevel, true);
        String funcParams = funcExpr.getParameters().stream().map(ClosureParameter::getName)
                .collect(Collectors.joining(", "));
        String lambdaFunction = (funcParams.isEmpty()) ? "(lambda item: " + body + ")"
                : "(lambda " + funcParams + ": " + body + ")";
        return lambdaFunction + "(" + argExpr + ")";
    }

    private String generateFilterOperation(FilterOperation expr, int ifLevel, boolean isLambda) {
        String argument = generateExpression(expr.getArgument(), ifLevel, isLambda);
        String filterExpression = generateExpression(expr.getFunction().getBody(), ifLevel, true);
        return "rune_filter(" + argument + ", lambda item: " + filterExpression + ")";
    }

    private String generateMapOperation(MapOperation expr, int ifLevel, boolean isLambda) {
        InlineFunction inlineFunc = expr.getFunction();
        String funcBody = generateExpression(inlineFunc.getBody(), ifLevel, true);
        String lambdaFunction = "lambda item: " + funcBody;
        String argument = generateExpression(expr.getArgument(), ifLevel, isLambda);
        return "list(map(" + lambdaFunction + ", " + argument + "))";
    }

    private String generateConstructorExpression(RosettaConstructorExpression expr, int ifLevel, boolean isLambda) {
        String type = (expr.getTypeCall() != null && expr.getTypeCall().getType() != null)
                ? expr.getTypeCall().getType().getName()
                : null;
        String fullyQualifiedType = RuneToPythonMapper.getBundleObjectName(expr.getTypeCall().getType());
        type = fullyQualifiedType;
        if (type != null) {
            return type + "(" + expr.getValues().stream()
                    .map(pair -> pair.getKey().getName() + "="
                            + generateExpression(pair.getValue(), ifLevel, isLambda))
                    .collect(Collectors.joining(", ")) + ")";
        } else {
            return "{" + expr.getValues().stream()
                    .map(pair -> "'" + pair.getKey().getName() + "': "
                            + generateExpression(pair.getValue(), ifLevel, isLambda))
                    .collect(Collectors.joining(", ")) + "}";
        }
    }

    private String getGuardExpression(SwitchCaseGuard caseGuard, boolean isLambda) {
        if (caseGuard == null) {
            throw new UnsupportedOperationException("Null SwitchCaseGuard");
        }
        RosettaExpression literalGuard = caseGuard.getLiteralGuard();
        if (literalGuard != null) {
            return "switchAttribute == " + generateExpression(literalGuard, 0, isLambda);
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

    private String generateSwitchOperation(SwitchOperation expr, int ifLevel, boolean isLambda) {
        String attr = generateExpression(expr.getArgument(), 0, isLambda);
        PythonCodeWriter writer = new PythonCodeWriter();
        isSwitchCond = true;

        var cases = expr.getCases();
        for (int i = 0; i < cases.size(); i++) {
            var currentCase = cases.get(i);
            String funcName = currentCase.isDefault() ? "_then_default" : "_then_" + (i + 1);
            String thenExprDef = currentCase.isDefault() ? generateExpression(expr.getDefault(), 0, isLambda)
                    : generateExpression(currentCase.getExpression(), ifLevel + 1, isLambda);

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
        return writer.toString();
    }

    private String generateSymbolReference(RosettaSymbolReference expr, int ifLevel, boolean isLambda) {

        RosettaSymbol symbol = expr.getSymbol();
        if (symbol instanceof Data || symbol instanceof RosettaEnumeration) {
            return symbol.getName();
        } else if (symbol instanceof Attribute attr) {
            return generateAttributeReference(attr, isLambda);
        } else if (symbol instanceof RosettaEnumValue evalue) {
            return generateEnumString(evalue);
        } else if (symbol instanceof RosettaCallableWithArgs callable) {
            return generateCallableWithArgsCall(callable, expr, ifLevel, isLambda);
        } else if (symbol instanceof ShortcutDeclaration || symbol instanceof ClosureParameter) {
            return "rune_resolve_attr(self, \"" + symbol.getName() + "\")";
        } else {
            throw new UnsupportedOperationException(
                    "Unsupported symbol reference for: " + symbol.getClass().getSimpleName());
        }
    }

    private String generateAttributeReference(Attribute s, boolean isLambda) {
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

    private String generateCallableWithArgsCall(RosettaCallableWithArgs s, RosettaSymbolReference expr, int ifLevel,
            boolean isLambda) {
        // Dependency handled by PythonFunctionDependencyProvider
        String args = expr.getArgs().stream().map(arg -> generateExpression(arg, ifLevel, isLambda))
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

    private String generateBinaryExpression(RosettaBinaryOperation expr, int ifLevel, boolean isLambda) {

        if (expr instanceof ModifiableBinaryOperation mod) {
            if (mod.getCardMod() == null) {
                throw new UnsupportedOperationException(
                        "ModifiableBinaryOperation with expressions with no cardinality");
            }
            if ("<>".equals(mod.getOperator())) {
                return "rune_any_elements(" + generateExpression(mod.getLeft(), ifLevel, isLambda) + ", \""
                        + mod.getOperator() + "\", " + generateExpression(mod.getRight(), ifLevel, isLambda) + ")";
            } else {
                return "rune_all_elements(" + generateExpression(mod.getLeft(), ifLevel, isLambda) + ", \""
                        + mod.getOperator() + "\", " + generateExpression(mod.getRight(), ifLevel, isLambda) + ")";
            }
        } else {
            return switch (expr.getOperator()) {
                case "=" -> "(" + generateExpression(expr.getLeft(), ifLevel, isLambda) + " == "
                        + generateExpression(expr.getRight(), ifLevel, isLambda) + ")";
                case "<>" -> "(" + generateExpression(expr.getLeft(), ifLevel, isLambda) + " != "
                        + generateExpression(expr.getRight(), ifLevel, isLambda) + ")";
                case "contains" -> "rune_contains(" + generateExpression(expr.getLeft(), ifLevel, isLambda) + ", "
                        + generateExpression(expr.getRight(), ifLevel, isLambda) + ")";
                case "disjoint" -> "rune_disjoint(" + generateExpression(expr.getLeft(), ifLevel, isLambda) + ", "
                        + generateExpression(expr.getRight(), ifLevel, isLambda) + ")";
                case "join" -> generateExpression(expr.getRight(), ifLevel, isLambda) + ".join("
                        + generateExpression(expr.getLeft(), ifLevel, isLambda) + ")";
                default -> "(" + generateExpression(expr.getLeft(), ifLevel, isLambda) + " " + expr.getOperator() + " "
                        + generateExpression(expr.getRight(), ifLevel, isLambda) + ")";
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

    public String generateFunctionConditions(List<Condition> conditions, String condition_type) {

        int nConditions = 0;
        StringBuilder result = new StringBuilder();
        for (Condition cond : conditions) {
            result.append(generateFunctionConditionBoilerPlate(cond, nConditions, condition_type));
            result.append(generateIfThenElseOrSwitch(cond));

            nConditions++;
        }
        return result.toString();
    }

    public String generateThenElseForFunction(RosettaExpression expr, List<Integer> ifLevel) {
        ifCondBlocks.clear();
        generateExpression(expr, ifLevel.get(0), false);

        PythonCodeWriter writer = new PythonCodeWriter();
        if (!ifCondBlocks.isEmpty()) {
            ifLevel.set(0, ifLevel.get(0) + 1);
            for (String arg : ifCondBlocks) {
                writer.appendBlock(arg);
                writer.newLine();
            }
        }
        return writer.toString();
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

    private String generateFunctionConditionBoilerPlate(Condition cond, int nConditions, String condition_type) {
        PythonCodeWriter writer = new PythonCodeWriter();
        writer.newLine();
        writer.appendLine("@rune_local_condition(" + condition_type + ")");
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

    private String generateIfThenElseOrSwitch(Condition c) {
        ifCondBlocks.clear();
        isSwitchCond = false;
        String expr = generateExpression(c.getExpression(), 0, false);

        PythonCodeWriter writer = new PythonCodeWriter();
        writer.indent();
        if (isSwitchCond) {
            writer.appendBlock(expr);
            return writer.toString();
        }
        if (!ifCondBlocks.isEmpty()) {
            for (String arg : ifCondBlocks) {
                writer.appendBlock(arg);
                writer.appendLine("");
            }
        }
        writer.appendLine("return " + expr);
        return writer.toString();
    }
}
