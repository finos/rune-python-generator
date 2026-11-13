/*
 * Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
 * SPDX-License-Identifier: Apache-2.0
 */
package com.regnosys.rosetta.generator.python.util;

import java.util.LinkedHashSet;
import java.util.Set;

import com.regnosys.rosetta.rosetta.RosettaEnumValue;
import com.regnosys.rosetta.rosetta.RosettaEnumeration;
import com.regnosys.rosetta.rosetta.RosettaNamed;
import com.regnosys.rosetta.rosetta.expression.ArithmeticOperation;
import com.regnosys.rosetta.rosetta.expression.AsKeyOperation;
import com.regnosys.rosetta.rosetta.expression.AsOperation;
import com.regnosys.rosetta.rosetta.expression.ChoiceOperation;
import com.regnosys.rosetta.rosetta.expression.ComparisonOperation;
import com.regnosys.rosetta.rosetta.expression.DefaultOperation;
import com.regnosys.rosetta.rosetta.expression.DistinctOperation;
import com.regnosys.rosetta.rosetta.expression.EqualityOperation;
import com.regnosys.rosetta.rosetta.expression.FilterOperation;
import com.regnosys.rosetta.rosetta.expression.FirstOperation;
import com.regnosys.rosetta.rosetta.expression.FlattenOperation;
import com.regnosys.rosetta.rosetta.expression.InlineFunction;
import com.regnosys.rosetta.rosetta.expression.JoinOperation;
import com.regnosys.rosetta.rosetta.expression.LastOperation;
import com.regnosys.rosetta.rosetta.expression.ListLiteral;
import com.regnosys.rosetta.rosetta.expression.LogicalOperation;
import com.regnosys.rosetta.rosetta.expression.MapOperation;
import com.regnosys.rosetta.rosetta.expression.MaxOperation;
import com.regnosys.rosetta.rosetta.expression.MinOperation;
import com.regnosys.rosetta.rosetta.expression.OneOfOperation;
import com.regnosys.rosetta.rosetta.expression.ReduceOperation;
import com.regnosys.rosetta.rosetta.expression.ReverseOperation;
import com.regnosys.rosetta.rosetta.expression.RosettaAbsentExpression;
import com.regnosys.rosetta.rosetta.expression.RosettaBinaryOperation;
import com.regnosys.rosetta.rosetta.expression.RosettaBooleanLiteral;
import com.regnosys.rosetta.rosetta.expression.RosettaConditionalExpression;
import com.regnosys.rosetta.rosetta.expression.RosettaConstructorExpression;
import com.regnosys.rosetta.rosetta.expression.RosettaContainsExpression;
import com.regnosys.rosetta.rosetta.expression.RosettaCountOperation;
import com.regnosys.rosetta.rosetta.expression.RosettaDeepFeatureCall;
import com.regnosys.rosetta.rosetta.expression.RosettaDisjointExpression;
import com.regnosys.rosetta.rosetta.expression.RosettaExistsExpression;
import com.regnosys.rosetta.rosetta.expression.RosettaExpression;
import com.regnosys.rosetta.rosetta.expression.RosettaFeatureCall;
import com.regnosys.rosetta.rosetta.expression.RosettaFunctionalOperation;
import com.regnosys.rosetta.rosetta.expression.RosettaImplicitVariable;
import com.regnosys.rosetta.rosetta.expression.RosettaIntLiteral;
import com.regnosys.rosetta.rosetta.expression.RosettaNumberLiteral;
import com.regnosys.rosetta.rosetta.expression.RosettaOnlyElement;
import com.regnosys.rosetta.rosetta.expression.RosettaOnlyExistsExpression;
import com.regnosys.rosetta.rosetta.expression.RosettaStringLiteral;
import com.regnosys.rosetta.rosetta.expression.RosettaSuperCall;
import com.regnosys.rosetta.rosetta.expression.RosettaSymbolReference;
import com.regnosys.rosetta.rosetta.expression.RosettaUnaryOperation;
import com.regnosys.rosetta.rosetta.expression.SortOperation;
import com.regnosys.rosetta.rosetta.expression.SumOperation;
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
import com.regnosys.rosetta.rosetta.simple.Data;
import com.regnosys.rosetta.types.RType;
import com.regnosys.rosetta.types.RosettaTypeProvider;
import com.regnosys.rosetta.utils.RosettaExpressionSwitch;

import jakarta.inject.Inject;

/**
 * Collects what a Rune expression refers to: the functions, types and enums that generated code
 * needs to import, or that the dependency graph needs as edges.
 *
 * <p>There is one case for every Rune expression type, so a new expression type stops the build
 * until it is handled here. Each case visits every sub-expression, including those held by
 * elements that aren't expressions themselves, such as switch cases and with-meta entries.
 *
 * <p>References are kept by kind, because each caller uses a different selection of them.
 */
public final class PythonDependencyCollector
        extends RosettaExpressionSwitch<Void, PythonDependencyCollector.Dependencies> {

    /**
     * The references found in one or more expressions.
     */
    public static final class Dependencies {
        /** The named elements referred to directly; see {@link #symbols()}. */
        private final Set<RosettaNamed> symbols = new LinkedHashSet<>();
        /** The types built with a constructor; see {@link #constructedTypes()}. */
        private final Set<RosettaNamed> constructedTypes = new LinkedHashSet<>();
        /** The data types named as the target of {@code as}. */
        private final Set<Data> asTargets = new LinkedHashSet<>();
        /** The enums named as the target of {@code to-enum}. */
        private final Set<RosettaEnumeration> toEnumTargets = new LinkedHashSet<>();
        /** The enum types that enum values are written through; see {@link #enumValueTypes()}. */
        private final Set<RType> enumValueTypes = new LinkedHashSet<>();
        /** The data types that switch cases test the value against. */
        private final Set<Data> switchCaseTypes = new LinkedHashSet<>();

        /**
         * @return the named elements referred to directly, such as functions, types, enums and
         *         attributes; enum values are in {@link #enumValueTypes()} instead
         */
        public Set<RosettaNamed> symbols() {
            return symbols;
        }

        /**
         * @return the types built with a constructor, {@code Type { ... }}
         */
        public Set<RosettaNamed> constructedTypes() {
            return constructedTypes;
        }

        /**
         * @return the data types named as the target of {@code as}
         */
        public Set<Data> asTargets() {
            return asTargets;
        }

        /**
         * @return the enums named as the target of {@code to-enum}
         */
        public Set<RosettaEnumeration> toEnumTargets() {
            return toEnumTargets;
        }

        /**
         * @return the data types that switch cases test the value against
         */
        public Set<Data> switchCaseTypes() {
            return switchCaseTypes;
        }

        /**
         * @return the enum types that enum values are written through: the expression's own enum
         *         type, which for an extended enum isn't the enum declaring the value
         */
        public Set<RType> enumValueTypes() {
            return enumValueTypes;
        }
    }

    /** Gives the enum type an enum value is written through. */
    @Inject
    private RosettaTypeProvider typeProvider;

    /**
     * Collects the references in an expression.
     *
     * @param expression the expression, or null
     * @return the references found
     */
    public Dependencies collect(RosettaExpression expression) {
        Dependencies dependencies = new Dependencies();
        visit(expression, dependencies);
        return dependencies;
    }

    /**
     * Collects the references in each expression.
     *
     * @param expressions the expressions; null elements are ignored
     * @return the references found in all of them
     */
    public Dependencies collect(Iterable<? extends RosettaExpression> expressions) {
        Dependencies dependencies = new Dependencies();
        expressions.forEach(expression -> visit(expression, dependencies));
        return dependencies;
    }

    private void visit(RosettaExpression expression, Dependencies dependencies) {
        if (expression != null) {
            doSwitch(expression, dependencies);
        }
    }

    private void visit(InlineFunction function, Dependencies dependencies) {
        if (function != null) {
            visit(function.getBody(), dependencies);
        }
    }

    private Void unary(RosettaUnaryOperation expr, Dependencies dependencies) {
        visit(expr.getArgument(), dependencies);
        return null;
    }

    private Void binary(RosettaBinaryOperation expr, Dependencies dependencies) {
        visit(expr.getLeft(), dependencies);
        visit(expr.getRight(), dependencies);
        return null;
    }

    private Void functional(RosettaFunctionalOperation expr, Dependencies dependencies) {
        visit(expr.getArgument(), dependencies);
        visit(expr.getFunction(), dependencies);
        return null;
    }

    private Void none() {
        return null;
    }

    // -------------------------------------------------------------------------
    // Expressions that refer to something
    // -------------------------------------------------------------------------

    @Override
    protected Void caseSymbolReference(RosettaSymbolReference expr, Dependencies dependencies) {
        if (expr.getSymbol() instanceof RosettaEnumValue) {
            dependencies.enumValueTypes.add(typeProvider.getRMetaAnnotatedType(expr).getRType());
        } else if (expr.getSymbol() instanceof RosettaNamed named) {
            dependencies.symbols.add(named);
        }
        expr.getArgs().forEach(arg -> visit(arg, dependencies));
        return null;
    }

    @Override
    protected Void caseConstructorExpression(RosettaConstructorExpression expr, Dependencies dependencies) {
        if (expr.getTypeCall() != null && expr.getTypeCall().getType() != null) {
            dependencies.constructedTypes.add(expr.getTypeCall().getType());
        }
        expr.getValues().forEach(pair -> visit(pair.getValue(), dependencies));
        return null;
    }

    @Override
    protected Void caseAsOperation(AsOperation expr, Dependencies dependencies) {
        // A choice option target is just an attribute on the choice's own class
        if (expr.getType() instanceof Data target) {
            dependencies.asTargets.add(target);
        }
        return unary(expr, dependencies);
    }

    @Override
    protected Void caseToEnumOperation(ToEnumOperation expr, Dependencies dependencies) {
        dependencies.toEnumTargets.add(expr.getEnumeration());
        return unary(expr, dependencies);
    }

    @Override
    protected Void caseSwitchOperation(SwitchOperation expr, Dependencies dependencies) {
        visit(expr.getArgument(), dependencies);
        expr.getCases().forEach(switchCase -> {
            visit(switchCase.getExpression(), dependencies);
            if (switchCase.getGuard() != null) {
                visit(switchCase.getGuard().getLiteralGuard(), dependencies);
                if (switchCase.getGuard().getDataGuard() != null) {
                    dependencies.switchCaseTypes.add(switchCase.getGuard().getDataGuard());
                }
                if (switchCase.getGuard().getEnumGuard() != null) {
                    // The guard is written through the switch argument's enum type
                    dependencies.enumValueTypes.add(
                            typeProvider.getRMetaAnnotatedType(expr.getArgument()).getRType());
                }
            }
        });
        visit(expr.getDefault(), dependencies);
        return null;
    }

    @Override
    protected Void caseWithMetaOperation(WithMetaOperation expr, Dependencies dependencies) {
        visit(expr.getArgument(), dependencies);
        expr.getEntries().forEach(entry -> visit(entry.getValue(), dependencies));
        return null;
    }

    // -------------------------------------------------------------------------
    // Expressions that only contain other expressions
    // -------------------------------------------------------------------------

    @Override
    protected Void caseListLiteral(ListLiteral expr, Dependencies dependencies) {
        expr.getElements().forEach(element -> visit(element, dependencies));
        return null;
    }

    @Override
    protected Void caseConditionalExpression(RosettaConditionalExpression expr, Dependencies dependencies) {
        visit(expr.getIf(), dependencies);
        visit(expr.getIfthen(), dependencies);
        visit(expr.getElsethen(), dependencies);
        return null;
    }

    @Override
    protected Void caseFeatureCall(RosettaFeatureCall expr, Dependencies dependencies) {
        visit(expr.getReceiver(), dependencies);
        return null;
    }

    @Override
    protected Void caseDeepFeatureCall(RosettaDeepFeatureCall expr, Dependencies dependencies) {
        visit(expr.getReceiver(), dependencies);
        return null;
    }

    @Override
    protected Void caseOnlyExists(RosettaOnlyExistsExpression expr, Dependencies dependencies) {
        expr.getArgs().forEach(arg -> visit(arg, dependencies));
        return null;
    }

    @Override
    protected Void caseSuperCall(RosettaSuperCall expr, Dependencies dependencies) {
        expr.getArgs().forEach(arg -> visit(arg, dependencies));
        return null;
    }

    @Override
    protected Void caseImplicitVariable(RosettaImplicitVariable expr, Dependencies dependencies) {
        return none();
    }

    @Override
    protected Void caseBooleanLiteral(RosettaBooleanLiteral expr, Dependencies dependencies) {
        return none();
    }

    @Override
    protected Void caseIntLiteral(RosettaIntLiteral expr, Dependencies dependencies) {
        return none();
    }

    @Override
    protected Void caseNumberLiteral(RosettaNumberLiteral expr, Dependencies dependencies) {
        return none();
    }

    @Override
    protected Void caseStringLiteral(RosettaStringLiteral expr, Dependencies dependencies) {
        return none();
    }

    // Binary operations

    @Override
    protected Void caseAddOperation(ArithmeticOperation expr, Dependencies dependencies) {
        return binary(expr, dependencies);
    }

    @Override
    protected Void caseSubtractOperation(ArithmeticOperation expr, Dependencies dependencies) {
        return binary(expr, dependencies);
    }

    @Override
    protected Void caseMultiplyOperation(ArithmeticOperation expr, Dependencies dependencies) {
        return binary(expr, dependencies);
    }

    @Override
    protected Void caseDivideOperation(ArithmeticOperation expr, Dependencies dependencies) {
        return binary(expr, dependencies);
    }

    @Override
    protected Void caseJoinOperation(JoinOperation expr, Dependencies dependencies) {
        return binary(expr, dependencies);
    }

    @Override
    protected Void caseAndOperation(LogicalOperation expr, Dependencies dependencies) {
        return binary(expr, dependencies);
    }

    @Override
    protected Void caseOrOperation(LogicalOperation expr, Dependencies dependencies) {
        return binary(expr, dependencies);
    }

    @Override
    protected Void caseLessThanOperation(ComparisonOperation expr, Dependencies dependencies) {
        return binary(expr, dependencies);
    }

    @Override
    protected Void caseLessThanOrEqualOperation(ComparisonOperation expr, Dependencies dependencies) {
        return binary(expr, dependencies);
    }

    @Override
    protected Void caseGreaterThanOperation(ComparisonOperation expr, Dependencies dependencies) {
        return binary(expr, dependencies);
    }

    @Override
    protected Void caseGreaterThanOrEqualOperation(ComparisonOperation expr, Dependencies dependencies) {
        return binary(expr, dependencies);
    }

    @Override
    protected Void caseEqualsOperation(EqualityOperation expr, Dependencies dependencies) {
        return binary(expr, dependencies);
    }

    @Override
    protected Void caseNotEqualsOperation(EqualityOperation expr, Dependencies dependencies) {
        return binary(expr, dependencies);
    }

    @Override
    protected Void caseContainsOperation(RosettaContainsExpression expr, Dependencies dependencies) {
        return binary(expr, dependencies);
    }

    @Override
    protected Void caseDisjointOperation(RosettaDisjointExpression expr, Dependencies dependencies) {
        return binary(expr, dependencies);
    }

    @Override
    protected Void caseDefaultOperation(DefaultOperation expr, Dependencies dependencies) {
        return binary(expr, dependencies);
    }

    // Unary operations

    @Override
    protected Void caseAsKeyOperation(AsKeyOperation expr, Dependencies dependencies) {
        return unary(expr, dependencies);
    }

    @Override
    protected Void caseChoiceOperation(ChoiceOperation expr, Dependencies dependencies) {
        return unary(expr, dependencies);
    }

    @Override
    protected Void caseOneOfOperation(OneOfOperation expr, Dependencies dependencies) {
        return unary(expr, dependencies);
    }

    @Override
    protected Void caseAbsentOperation(RosettaAbsentExpression expr, Dependencies dependencies) {
        return unary(expr, dependencies);
    }

    @Override
    protected Void caseCountOperation(RosettaCountOperation expr, Dependencies dependencies) {
        return unary(expr, dependencies);
    }

    @Override
    protected Void caseExistsOperation(RosettaExistsExpression expr, Dependencies dependencies) {
        return unary(expr, dependencies);
    }

    @Override
    protected Void caseDistinctOperation(DistinctOperation expr, Dependencies dependencies) {
        return unary(expr, dependencies);
    }

    @Override
    protected Void caseFirstOperation(FirstOperation expr, Dependencies dependencies) {
        return unary(expr, dependencies);
    }

    @Override
    protected Void caseFlattenOperation(FlattenOperation expr, Dependencies dependencies) {
        return unary(expr, dependencies);
    }

    @Override
    protected Void caseLastOperation(LastOperation expr, Dependencies dependencies) {
        return unary(expr, dependencies);
    }

    @Override
    protected Void caseReverseOperation(ReverseOperation expr, Dependencies dependencies) {
        return unary(expr, dependencies);
    }

    @Override
    protected Void caseOnlyElementOperation(RosettaOnlyElement expr, Dependencies dependencies) {
        return unary(expr, dependencies);
    }

    @Override
    protected Void caseSumOperation(SumOperation expr, Dependencies dependencies) {
        return unary(expr, dependencies);
    }

    @Override
    protected Void caseToStringOperation(ToStringOperation expr, Dependencies dependencies) {
        return unary(expr, dependencies);
    }

    @Override
    protected Void caseToNumberOperation(ToNumberOperation expr, Dependencies dependencies) {
        return unary(expr, dependencies);
    }

    @Override
    protected Void caseToIntOperation(ToIntOperation expr, Dependencies dependencies) {
        return unary(expr, dependencies);
    }

    @Override
    protected Void caseToTimeOperation(ToTimeOperation expr, Dependencies dependencies) {
        return unary(expr, dependencies);
    }

    @Override
    protected Void caseToDateOperation(ToDateOperation expr, Dependencies dependencies) {
        return unary(expr, dependencies);
    }

    @Override
    protected Void caseToDateTimeOperation(ToDateTimeOperation expr, Dependencies dependencies) {
        return unary(expr, dependencies);
    }

    @Override
    protected Void caseToZonedDateTimeOperation(ToZonedDateTimeOperation expr, Dependencies dependencies) {
        return unary(expr, dependencies);
    }

    // Operations with an inline function

    @Override
    protected Void caseFilterOperation(FilterOperation expr, Dependencies dependencies) {
        return functional(expr, dependencies);
    }

    @Override
    protected Void caseMapOperation(MapOperation expr, Dependencies dependencies) {
        return functional(expr, dependencies);
    }

    @Override
    protected Void caseMaxOperation(MaxOperation expr, Dependencies dependencies) {
        return functional(expr, dependencies);
    }

    @Override
    protected Void caseMinOperation(MinOperation expr, Dependencies dependencies) {
        return functional(expr, dependencies);
    }

    @Override
    protected Void caseReduceOperation(ReduceOperation expr, Dependencies dependencies) {
        return functional(expr, dependencies);
    }

    @Override
    protected Void caseSortOperation(SortOperation expr, Dependencies dependencies) {
        return functional(expr, dependencies);
    }

    @Override
    protected Void caseThenOperation(ThenOperation expr, Dependencies dependencies) {
        return functional(expr, dependencies);
    }
}
