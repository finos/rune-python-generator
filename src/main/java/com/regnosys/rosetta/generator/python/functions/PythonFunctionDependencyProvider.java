package com.regnosys.rosetta.generator.python.functions;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;


import com.regnosys.rosetta.rosetta.RosettaEnumValueReference;
import com.regnosys.rosetta.rosetta.RosettaEnumeration;
import com.regnosys.rosetta.rosetta.RosettaModel;
import com.regnosys.rosetta.rosetta.RosettaNamed;
import com.regnosys.rosetta.rosetta.RosettaRule;
import com.regnosys.rosetta.rosetta.RosettaSymbol;
import com.regnosys.rosetta.rosetta.expression.FilterOperation;
import com.regnosys.rosetta.rosetta.expression.InlineFunction;
import com.regnosys.rosetta.rosetta.expression.ListLiteral;
import com.regnosys.rosetta.rosetta.expression.MapOperation;
import com.regnosys.rosetta.rosetta.expression.RosettaBinaryOperation;
import com.regnosys.rosetta.rosetta.expression.RosettaConditionalExpression;
import com.regnosys.rosetta.rosetta.expression.RosettaConstructorExpression;
import com.regnosys.rosetta.rosetta.expression.RosettaExpression;
import com.regnosys.rosetta.rosetta.expression.RosettaFeatureCall;
import com.regnosys.rosetta.rosetta.expression.RosettaFunctionalOperation;
import com.regnosys.rosetta.rosetta.expression.RosettaOnlyExistsExpression;
import com.regnosys.rosetta.rosetta.expression.RosettaSymbolReference;
import com.regnosys.rosetta.rosetta.expression.RosettaUnaryOperation;
import com.regnosys.rosetta.rosetta.expression.WithMetaOperation;
import com.regnosys.rosetta.rosetta.simple.Function;
import com.regnosys.rosetta.types.REnumType;
import com.regnosys.rosetta.types.RFunction;
import com.regnosys.rosetta.types.RObjectFactory;
import com.regnosys.rosetta.types.RType;

import jakarta.inject.Inject;

/**
 * Determine the Rosetta dependencies for a Function
 */
public final class PythonFunctionDependencyProvider {
    /**
     * RObjectFactory is used to build RType objects from Rosetta objects.
     */
    @Inject
    private RObjectFactory rTypeBuilderFactory;

    /**
     * @param object
     * @param enumImports
     */
    public void addDependencies(RosettaNamed named, Set<String> enumImports) {
        if (named instanceof RosettaEnumeration enumeration) {
            String name = enumeration.getName();
            RosettaModel model = (RosettaModel) enumeration.eContainer();
            String prefix = model.getName();
            enumImports.add("import " + prefix + "." + name);
        }
    }

    public void addDependencies(InlineFunction inline, Set<String> enumImports) {
        if (inline != null) {
            addDependencies(inline.getBody(), enumImports);
        }
    }

    public void addDependencies(RosettaExpression expression, Set<String> enumImports) {
        if (expression == null) {
            return;
        }
        switch (expression) {
            case RosettaBinaryOperation binary -> {
                addDependencies(binary.getLeft(), enumImports);
                addDependencies(binary.getRight(), enumImports);
            }
            case RosettaConditionalExpression cond -> {
                addDependencies(cond.getIf(), enumImports);
                addDependencies(cond.getIfthen(), enumImports);
                addDependencies(cond.getElsethen(), enumImports);
            }
            case RosettaOnlyExistsExpression onlyExists -> {
                onlyExists.getArgs().forEach(arg -> addDependencies(arg, enumImports));
            }
            case RosettaFunctionalOperation functional -> {
                if (functional.getArgument() != null) {
                    addDependencies(functional.getArgument(), enumImports);
                }
                if (functional instanceof FilterOperation filter) {
                    addDependencies(filter.getFunction(), enumImports);
                } else if (functional instanceof MapOperation map) {
                    addDependencies(map.getFunction(), enumImports);
                }
            }
            case WithMetaOperation withMeta -> {
                addDependencies(withMeta.getArgument(), enumImports);
                withMeta.getEntries().forEach(entry -> addDependencies(entry.getValue(), enumImports));
            }
            case RosettaUnaryOperation unary -> {
                addDependencies(unary.getArgument(), enumImports);
            }
            case RosettaFeatureCall featureCall -> {
                addDependencies(featureCall.getReceiver(), enumImports);
            }
            case RosettaSymbolReference symbolRef -> {
                if (symbolRef.getSymbol() instanceof RosettaNamed named) {
                    addDependencies(named, enumImports);
                }
                symbolRef.getArgs().forEach(arg -> addDependencies(arg, enumImports));
            }
            case RosettaEnumValueReference ref -> {
                addDependencies(ref.getEnumeration(), enumImports);
            }
            case InlineFunction inline -> {
                addDependencies(inline, enumImports);
            }
            case ListLiteral listLiteral -> {
                listLiteral.getElements().forEach(el -> addDependencies(el, enumImports));
            }
            case RosettaConstructorExpression constructor -> {
                if (constructor.getTypeCall() != null && constructor.getTypeCall().getType() != null) {
                    addDependencies(constructor.getTypeCall().getType(), enumImports);
                }
                constructor.getValues()
                        .forEach(valuePair -> addDependencies(valuePair.getValue(), enumImports));
            }
            default -> {
                // For unknown expressions, we can still use eContents() but cast to RosettaExpression
                // For unknown expressions, we can still use eContents()
                expression.eContents().forEach(child -> {
                    if (child instanceof RosettaExpression expr) {
                        addDependencies(expr, enumImports);
                    } else if (child instanceof RosettaNamed named) {
                         addDependencies(named, enumImports);
                    } else if (child instanceof InlineFunction inline) {
                         addDependencies(inline, enumImports);
                    }
                });
            }
        }
    }

    public void addDependencies(RType type, Set<String> enumImports) {
        if (type instanceof REnumType enumType) {
            String name = enumType.getName();
            String prefix = enumType.getNamespace().toString();
            enumImports.add("import " + prefix + "." + name);
        }
    }

    public Set<RFunction> rFunctionDependencies(RosettaExpression expression) {
        Set<RFunction> result = new HashSet<>();
        collectSymbolReferences(expression, result);
        return result;
    }

    private void collectSymbolReferences(RosettaExpression expression, Set<RFunction> result) {
        if (expression == null) {
            return;
        }
        if (expression instanceof RosettaSymbolReference ref) {
            RosettaSymbol symbol = ref.getSymbol();
            if (symbol instanceof Function f) {
                result.add(rTypeBuilderFactory.buildRFunction(f));
            } else if (symbol instanceof RosettaRule r) {
                result.add(rTypeBuilderFactory.buildRFunction(r));
            }
            ref.getArgs().forEach(arg -> collectSymbolReferences(arg, result));
        }
        expression.eContents().forEach(child -> {
            if (child instanceof RosettaExpression expr) {
                collectSymbolReferences(expr, result);
            }
        });
    }

    public Set<RFunction> rFunctionDependencies(Iterable<? extends RosettaExpression> expressions) {
        if (expressions == null) {
            return Collections.emptySet();
        }
        return StreamSupport.stream(expressions.spliterator(), false)
                .flatMap(expr -> rFunctionDependencies(expr).stream())
                .collect(Collectors.toSet());
    }
}
