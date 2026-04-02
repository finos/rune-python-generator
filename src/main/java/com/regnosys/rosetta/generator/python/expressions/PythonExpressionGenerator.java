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
import com.regnosys.rosetta.rosetta.expression.CardinalityModifier;
import com.regnosys.rosetta.rosetta.expression.ModifiableBinaryOperation;
import com.regnosys.rosetta.rosetta.expression.Necessity;
import com.regnosys.rosetta.rosetta.expression.OneOfOperation;
import com.regnosys.rosetta.rosetta.expression.ReverseOperation;
import com.regnosys.rosetta.rosetta.expression.ReduceOperation;
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
import com.regnosys.rosetta.rosetta.expression.ToNumberOperation;
import com.regnosys.rosetta.rosetta.expression.ToStringOperation;
import com.regnosys.rosetta.rosetta.expression.ToTimeOperation;
import com.regnosys.rosetta.rosetta.expression.ToZonedDateTimeOperation;
import com.regnosys.rosetta.rosetta.expression.WithMetaOperation;
import com.regnosys.rosetta.rosetta.simple.Attribute;
import com.regnosys.rosetta.rosetta.simple.Condition;
import com.regnosys.rosetta.rosetta.simple.Data;
import com.regnosys.rosetta.rosetta.simple.ShortcutDeclaration;
import com.regnosys.rosetta.rosetta.simple.impl.FunctionImpl;
import com.regnosys.rosetta.types.CardinalityProvider;
import com.regnosys.rosetta.types.RChoiceType;
import com.regnosys.rosetta.types.RDataType;
import com.regnosys.rosetta.types.REnumType;
import com.regnosys.rosetta.types.RMetaAnnotatedType;
import com.regnosys.rosetta.types.RType;
import com.regnosys.rosetta.types.RosettaTypeProvider;

import jakarta.inject.Inject;

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
     * The Rosetta type provider.
     */
    @Inject
    private RosettaTypeProvider typeProvider;

    /**
     * The cardinality provider.
     */
    @Inject
    private CardinalityProvider cardinalityProvider;

    /**
     * The counter for generated helper functions.
     */
    private int generatedFunctionCounter = 0;

    /**
     * The set of standalone class/function FQNs for context-aware name resolution.
     */
    private java.util.Set<String> standaloneClasses = java.util.Collections.emptySet();

    public void setStandaloneClasses(java.util.Set<String> standaloneClasses) {
        this.standaloneClasses = standaloneClasses;
    }

    /**
     * Clears the block list.
     */
    private void clearBlocks() {
        ifCondBlocks.clear();
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
    private String generateExpression(RosettaExpression expr,
        PythonExpressionScope scope) {

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
                String arg = generateExpression(asKey.getArgument(), scope);
                if (cardinalityProvider.isMulti(asKey.getArgument())) {
                    return "[Reference(x) for x in (" + arg + " or []) if x is not None]";
                }
                return "Reference(" + arg + ")";
            }
            case DistinctOperation distinct -> {
                return generateDistinctOperation(distinct, scope);
            }
            case FilterOperation filter -> {
                return generateFilterOperation(filter, scope);
            }
            case FirstOperation first -> {
                return "next((x for x in (" + generateExpression(first.getArgument(), scope) + " or []) if x is not None), None)";
            }
            case FlattenOperation flatten -> {
                return generateFlattenOperation(flatten, scope);
            }
            case ListLiteral listLiteral -> {
                return "[" + listLiteral.getElements().stream()
                        .map(arg -> generateExpression(arg, scope))
                        .collect(Collectors.joining(", ")) + "]";
            }
            case LastOperation last -> {
                return generateLastOperation(last, scope);
            }
            case MapOperation mapOp -> {
                return generateMapOperation(mapOp, scope);
            }
            case MaxOperation maxOp -> {
                return generateMaxOperation(maxOp, scope);
            }
            case MinOperation minOp -> {
                return generateMinOperation(minOp, scope);
            }
            case SortOperation sort -> {
                return generateSortOperation(sort, scope);
            }
            case ThenOperation then -> {
                return generateThenOperation(then, scope);
            }
            case SumOperation sum -> {
                return generateSumOperation(sum, scope);
            }
            case ReverseOperation reverse -> {
                return generateReverseOperation(reverse, scope);
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
            case ToNumberOperation toNumber -> {
                return "Decimal(" + generateExpression(toNumber.getArgument(), scope) + ")";
            }
            case ReduceOperation reduceOp -> {
                return generateReduceOperation(reduceOp, scope);
            }
            case OneOfOperation oneOf -> {
                return generateOneOfOperation(oneOf, scope);
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
                return generateCountOperation(count, scope);
            }
            case RosettaDeepFeatureCall deepFeature -> {
                return "rune_resolve_deep_attr(self, \"" + deepFeature.getFeature().getName() + "\")";
            }
            case RosettaEnumValueReference enumRef -> {
                return enumRef.getEnumeration().getName() + "." + EnumHelper.convertValue(enumRef.getValue());
            }
            case RosettaExistsExpression exists -> {
                return generateExistsOperation(exists, scope);
            }
            case RosettaFeatureCall featureCall -> {
                return generateFeatureCall(featureCall, scope);
            }
            case RosettaOnlyElement onlyElement -> {
                return "rune_get_only_element([x for x in (" + generateExpression(onlyElement.getArgument(), scope) + " or []) if x is not None])";
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

    private String generateReduceOperation(ReduceOperation expr, PythonExpressionScope scope) {
        String argument = generateExpression(expr.getArgument(), scope);
        InlineFunction func = expr.getFunction();
        String accParam = func.getParameters().isEmpty() ? "a" : func.getParameters().get(0).getName();
        String itemParam = func.getParameters().size() < 2 ? "b" : func.getParameters().get(1).getName();

        PythonExpressionScope subScope = scope.withReceiver(itemParam);
        if (!func.getParameters().isEmpty()) {
            subScope = subScope.withShadow(func.getParameters().get(0), accParam);
        }
        String body = generateExpression(func.getBody(), subScope);

        return "functools.reduce(lambda " + accParam + ", " + itemParam + ": " + body + ", " + argument + ")";
    }

    private String generateOneOfOperation(OneOfOperation expr, PythonExpressionScope scope) {
        String argument = generateExpression(expr.getArgument(), scope);
        RMetaAnnotatedType metaType = typeProvider.getRMetaAnnotatedType(expr.getArgument());
        if (metaType != null) {
            RType type = metaType.getRType();
            RDataType dt = null;
            if (type instanceof RDataType) {
                dt = (RDataType) type;
            } else if (type instanceof RChoiceType ct) {
                dt = ct.asRDataType();
            }
            if (dt != null) {
                String attrs = dt.getAllAttributes().stream()
                        .map(a -> "'" + a.getName() + "'")
                        .collect(Collectors.joining(", "));
                return "rune_check_one_of(" + argument + ", " + attrs + ")";
            }
        }
        return "rune_check_one_of(" + argument + ")";
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
        return "[x for x in map(" + lambdaFunction + ", " + argument + " or []) if x is not None]";
    }

    private String generateConstructorExpression(RosettaConstructorExpression expr, PythonExpressionScope scope) {
        String type = null;
        if (expr.getTypeCall() != null && expr.getTypeCall().getType() != null) {
            com.regnosys.rosetta.rosetta.RosettaNamed typeNamed = expr.getTypeCall().getType();
            String fqn = RuneToPythonMapper.getFullyQualifiedName(typeNamed);
            type = standaloneClasses.contains(fqn) ? typeNamed.getName() : RuneToPythonMapper.getBundleObjectName(typeNamed);
        }
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
        funcName = switch (funcName) {
            case "Max" -> "max";
            case "Min" -> "min";
            case null, default -> {
                String fqn = RuneToPythonMapper.getFullyQualifiedName(s);
                yield standaloneClasses.contains(fqn) ? s.getName() : RuneToPythonMapper.getBundleObjectName(s);
            }
        };
        return "rune_call_unchecked(" + funcName + ", " + args + ")";
    }

    private String generateBinaryExpression(RosettaBinaryOperation expr, PythonExpressionScope scope) {

        if (expr instanceof ModifiableBinaryOperation mod) {
            String left = generateExpression(mod.getLeft(), scope);
            String right = generateExpression(mod.getRight(), scope);
            String op = mod.getOperator();
            if (mod.getCardMod() == CardinalityModifier.ANY) {
                // any <op> y  — cross-product, handles scalar or list rhs
                return "rune_any_elements(" + left + ", \"" + op + "\", " + right + ")";
            } else if (mod.getCardMod() == CardinalityModifier.ALL) {
                if ("<>".equals(op) && !cardinalityProvider.isMulti(mod.getRight())) {
                    // all <> scalar  ≡  not any(el == y for el in x)
                    // De Morgan avoids rune_all_elements pairwise zip which fails when len(x) != len([y])
                    return "(not rune_any_elements(" + left + ", \"=\", " + right + "))";
                }
                // all <op> list  — rune_all_elements pairwise is correct when both sides are lists
                return "rune_all_elements(" + left + ", \"" + op + "\", " + right + ")";
            } else {
                // no cardinality modifier — plain list equality/inequality
                if ("<>".equals(op)) {
                    // x <> y  ≡  not (x and y are pairwise equal)
                    return "(not rune_all_elements(" + left + ", \"=\", " + right + "))";
                }
                // x = y  — pairwise equality
                return "rune_all_elements(" + left + ", \"" + op + "\", " + right + ")";
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
                case "join" -> "(lambda items, sep: (sep or \"\").join(x for x in (items or []) if x is not None) if items is not None else None)("
                        + generateExpression(expr.getLeft(), scope) + ", " + generateExpression(expr.getRight(), scope) + ")";
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
        RType argType = typeProvider.getRMetaAnnotatedType(expr.getArgument()).getRType();
        String arg = generateExpression(expr.getArgument(), scope);

        // Build metadata as plain Python kwargs (key=value).
        // set_meta / *WithMeta constructors convert plain names to @-prefixed keys internally.
        String kwargs = expr.getEntries().stream()
                .map(entry -> entry.getKey().getName() + "=" + generateExpression(entry.getValue(), scope))
                .collect(Collectors.joining(", "));

        // Basic type: Python immutability requires constructing a new *WithMeta wrapper.
        if (RuneToPythonMapper.isRosettaBasicType(argType)) {
            String withMetaType = RuneToPythonMapper.getAttributeTypeWithMeta(
                    RuneToPythonMapper.toPythonType(argType, false));
            return withMetaType + "(" + arg + ", " + kwargs + ")";
        }

        // Enum type: use EnumWithMetaMixin.deserialize as the public factory.
        // Keys must be @-prefixed in the dict since deserialize receives serialised form.
        // The class reference uses the dotted module path consistent with how enum values
        // are referenced elsewhere in function bodies (namespace.EnumName.EnumName).
        if (argType instanceof REnumType) {
            String enumName = argType.getName();
            String enumType = argType.getNamespace().toString() + "." + enumName + "." + enumName;
            List<String[]> mappedEntries = expr.getEntries().stream()
                    .map(entry -> {
                        String mappedKey = switch (entry.getKey().getName()) {
                            case "scheme" -> "@scheme";
                            case "id", "key" -> "@key";
                            case "reference" -> "@ref";
                            default -> "@" + entry.getKey().getName();
                        };
                        return new String[]{mappedKey, generateExpression(entry.getValue(), scope)};
                    })
                    .toList();
            String dictEntries = mappedEntries.stream()
                    .map(e -> "'" + e[0] + "': " + e[1])
                    .collect(Collectors.joining(", "));
            String allowedMeta = mappedEntries.stream()
                    .map(e -> "'" + e[0] + "'")
                    .collect(Collectors.joining(", "));
            return enumType + ".deserialize({'@data': " + arg + ", " + dictEntries + "}, allowed_meta={" + allowedMeta + "})";
        }

        // Complex type (BaseDataClass subclass): mutate in-place via set_meta.
        // Lambda assigns arg to _wm once to avoid double-evaluation, then returns it.
        return "(lambda _wm: (_wm.set_meta(check_allowed=False, " + kwargs + "), _wm)[-1])(" + arg + ")";
    }

    private String generateMaxOperation(MaxOperation expr, PythonExpressionScope scope) {
        String argument = generateExpression(expr.getArgument(), scope);
        InlineFunction inlineFunc = expr.getFunction();
        if (inlineFunc == null) {
            return "(lambda items: max((x for x in (items or []) if x is not None), default=None) if items is not None else None)(" + argument + ")";
        }
        String param = inlineFunc.getParameters().isEmpty() ? "item" : inlineFunc.getParameters().get(0).getName();
        PythonExpressionScope subScope = scope.withReceiver(param);
        if (expr.getArgument() instanceof RosettaSymbolReference ref) {
            subScope = subScope.withShadow(ref.getSymbol(), param);
        }
        String funcBody = generateExpression(inlineFunc.getBody(), subScope);
        return "(lambda items: max((x for x in (items or []) if x is not None), key=lambda " + param + ": " + funcBody + ", default=None) if items is not None else None)(" + argument + ")";
    }

    private String generateMinOperation(MinOperation expr, PythonExpressionScope scope) {
        String argument = generateExpression(expr.getArgument(), scope);
        InlineFunction inlineFunc = expr.getFunction();
        if (inlineFunc == null) {
            return "(lambda items: min((x for x in (items or []) if x is not None), default=None) if items is not None else None)(" + argument + ")";
        }
        String param = inlineFunc.getParameters().isEmpty() ? "item" : inlineFunc.getParameters().get(0).getName();
        PythonExpressionScope subScope = scope.withReceiver(param);
        if (expr.getArgument() instanceof RosettaSymbolReference ref) {
            subScope = subScope.withShadow(ref.getSymbol(), param);
        }
        String funcBody = generateExpression(inlineFunc.getBody(), subScope);
        return "(lambda items: min((x for x in (items or []) if x is not None), key=lambda " + param + ": " + funcBody + ", default=None) if items is not None else None)(" + argument + ")";
    }

    private String generateSortOperation(SortOperation expr, PythonExpressionScope scope) {
        String argument = generateExpression(expr.getArgument(), scope);
        InlineFunction inlineFunc = expr.getFunction();
        if (inlineFunc == null) {
            return "(lambda items: sorted(x for x in (items or []) if x is not None) if items is not None else None)(" + argument + ")";
        }
        String param = inlineFunc.getParameters().isEmpty() ? "item" : inlineFunc.getParameters().get(0).getName();
        PythonExpressionScope subScope = scope.withReceiver(param);
        if (expr.getArgument() instanceof RosettaSymbolReference ref) {
            subScope = subScope.withShadow(ref.getSymbol(), param);
        }
        String funcBody = generateExpression(inlineFunc.getBody(), subScope);
        return "(lambda items: sorted((x for x in (items or []) if x is not None), key=lambda " + param + ": " + funcBody + ") if items is not None else None)(" + argument + ")";
    }
    private String generateDistinctOperation(DistinctOperation distinct, PythonExpressionScope scope) {
        String arg = generateExpression(distinct.getArgument(), scope);
        return "(lambda items: set(x for x in (items or []) if x is not None) if items is not None else None)(" + arg + ")";
    }

    private String generateFlattenOperation(FlattenOperation flatten, PythonExpressionScope scope) {
        String arg = generateExpression(flatten.getArgument(), scope);
        return "(lambda nested: [x for sub in (nested or []) if sub is not None for x in (sub if (hasattr(sub, '__iter__') and not isinstance(sub, (str, dict, bytes, bytearray))) else [sub]) if x is not None] if nested is not None else None)(" + arg + ")";
    }

    private String generateLastOperation(LastOperation last, PythonExpressionScope scope) {
        return "next((x for x in reversed(" + generateExpression(last.getArgument(), scope) + " or []) if x is not None), None)";
    }

    private String generateSumOperation(SumOperation sum, PythonExpressionScope scope) {
        String arg = generateExpression(sum.getArgument(), scope);
        return "(lambda items: sum(x for x in (items or []) if x is not None) if items is not None else None)(" + arg + ")";
    }

    private String generateReverseOperation(ReverseOperation reverse, PythonExpressionScope scope) {
        String arg = generateExpression(reverse.getArgument(), scope);
        return "(lambda items: list(reversed([x for x in (items or []) if x is not None])) if items is not None else None)(" + arg + ")";
    }

    private String generateCountOperation(RosettaCountOperation count, PythonExpressionScope scope) {
        String arg = generateExpression(count.getArgument(), scope);
        return "(lambda items: sum(1 for x in (items if (hasattr(items, '__iter__') and not isinstance(items, (str, dict, bytes, bytearray))) else ([items] if items is not None else [])) if x is not None))(" + arg + ")";
    }

    private String generateExistsOperation(RosettaExistsExpression exists, PythonExpressionScope scope) {
        String arg = generateExpression(exists.getArgument(), scope);
        if (exists.getModifier() == ExistsModifier.SINGLE) {
            return "rune_attr_exists(" + arg + ", \"single\")";
        } else if (exists.getModifier() == ExistsModifier.MULTIPLE) {
            return "rune_attr_exists(" + arg + ", \"multiple\")";
        }
        return "rune_attr_exists(" + arg + ")";
    }
}
