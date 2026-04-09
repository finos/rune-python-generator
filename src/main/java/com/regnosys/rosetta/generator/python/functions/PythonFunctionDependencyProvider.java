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
import com.regnosys.rosetta.types.RAliasType;
import com.regnosys.rosetta.types.RDataType;
import com.regnosys.rosetta.types.REnumType;
import com.regnosys.rosetta.types.RFunction;
import com.regnosys.rosetta.types.RObjectFactory;
import com.regnosys.rosetta.types.RType;
import com.regnosys.rosetta.types.TypeSystem;

import com.regnosys.rosetta.generator.python.PythonCodeGeneratorContext;

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

    @Inject
    private TypeSystem typeSystem;

    public void addDependencies(RosettaNamed named, Set<String> enumImports, String sourceNamespace,
            PythonCodeGeneratorContext context) {
        if (named instanceof RosettaEnumeration enumeration) {
            String name = enumeration.getName();
            RosettaModel model = (RosettaModel) enumeration.eContainer();
            String prefix = context.applyPrefix(model.getName());
            enumImports.add("import " + prefix + "." + name);
        } else if (named instanceof com.regnosys.rosetta.rosetta.simple.Data data) {
            String name = data.getName();
            RosettaModel model = (RosettaModel) data.eContainer();
            String fullNamespace = context.applyPrefix(model.getName());
            String rootNamespace = fullNamespace.split("\\.")[0];
            String sourceRoot = sourceNamespace.split("\\.")[0];

            if (!rootNamespace.equals(sourceRoot)) {
                enumImports.add(String.format("from %s.%s import %s", fullNamespace, name, name));
            }
        } else if (named instanceof com.regnosys.rosetta.rosetta.RosettaTypeAlias alias) {
             // Recursive call with the referred type
             if (alias.getTypeCall() != null && alias.getTypeCall().getType() != null) {
                 addDependencies(alias.getTypeCall().getType(), enumImports, sourceNamespace, context);
             }
        }
    }

    public void addDependencies(InlineFunction inline, Set<String> enumImports, String sourceNamespace,
            PythonCodeGeneratorContext context) {
        if (inline != null) {
            addDependencies(inline.getBody(), enumImports, sourceNamespace, context);
        }
    }

    public void addDependencies(RosettaExpression expression, Set<String> enumImports, String sourceNamespace,
            PythonCodeGeneratorContext context) {
        if (expression == null) {
            return;
        }
        switch (expression) {
            case RosettaBinaryOperation binary -> {
                addDependencies(binary.getLeft(), enumImports, sourceNamespace, context);
                addDependencies(binary.getRight(), enumImports, sourceNamespace, context);
            }
            case RosettaConditionalExpression cond -> {
                addDependencies(cond.getIf(), enumImports, sourceNamespace, context);
                addDependencies(cond.getIfthen(), enumImports, sourceNamespace, context);
                addDependencies(cond.getElsethen(), enumImports, sourceNamespace, context);
            }
            case RosettaOnlyExistsExpression onlyExists -> {
                onlyExists.getArgs().forEach(arg -> addDependencies(arg, enumImports, sourceNamespace, context));
            }
            case RosettaFunctionalOperation functional -> {
                if (functional.getArgument() != null) {
                    addDependencies(functional.getArgument(), enumImports, sourceNamespace, context);
                }
                if (functional instanceof FilterOperation filter) {
                    addDependencies(filter.getFunction(), enumImports, sourceNamespace, context);
                } else if (functional instanceof MapOperation map) {
                    addDependencies(map.getFunction(), enumImports, sourceNamespace, context);
                }
            }
            case WithMetaOperation withMeta -> {
                addDependencies(withMeta.getArgument(), enumImports, sourceNamespace, context);
                withMeta.getEntries().forEach(entry -> addDependencies(entry.getValue(), enumImports, sourceNamespace, context));
            }
            case RosettaUnaryOperation unary -> {
                addDependencies(unary.getArgument(), enumImports, sourceNamespace, context);
            }
            case RosettaFeatureCall featureCall -> {
                addDependencies(featureCall.getReceiver(), enumImports, sourceNamespace, context);
            }
            case RosettaSymbolReference symbolRef -> {
                if (symbolRef.getSymbol() instanceof RosettaNamed named) {
                    addDependencies(named, enumImports, sourceNamespace, context);
                }
                symbolRef.getArgs().forEach(arg -> addDependencies(arg, enumImports, sourceNamespace, context));
            }
            case RosettaEnumValueReference ref -> {
                addDependencies(ref.getEnumeration(), enumImports, sourceNamespace, context);
            }
            case InlineFunction inline -> {
                addDependencies(inline, enumImports, sourceNamespace, context);
            }
            case ListLiteral listLiteral -> {
                listLiteral.getElements().forEach(el -> addDependencies(el, enumImports, sourceNamespace, context));
            }
            case RosettaConstructorExpression constructor -> {
                if (constructor.getTypeCall() != null && constructor.getTypeCall().getType() != null) {
                    addDependencies(constructor.getTypeCall().getType(), enumImports, sourceNamespace, context);
                }
                constructor.getValues()
                        .forEach(valuePair -> addDependencies(valuePair.getValue(), enumImports, sourceNamespace, context));
            }
            default -> {
                // For unknown expressions, we can still use eContents()
                expression.eContents().forEach(child -> {
                    if (child instanceof RosettaExpression expr) {
                        addDependencies(expr, enumImports, sourceNamespace, context);
                    } else if (child instanceof RosettaNamed named) {
                         addDependencies(named, enumImports, sourceNamespace, context);
                    } else if (child instanceof InlineFunction inline) {
                         addDependencies(inline, enumImports, sourceNamespace, context);
                    }
                });
            }
        }
    }

    public void addDependencies(RType type, Set<String> enumImports, String sourceNamespace,
            PythonCodeGeneratorContext context) {
        if (type instanceof RAliasType) {
            type = typeSystem.stripFromTypeAliases(type);
        }
        if (type instanceof REnumType enumType) {
            String name = enumType.getName();
            String prefix = context.applyPrefix(enumType.getNamespace().toString());
            enumImports.add("import " + prefix + "." + name);
        } else if (type instanceof RDataType dataType) {
            String name = dataType.getName();
            String fullNamespace = context.applyPrefix(dataType.getNamespace().toString());
            String rootNamespace = fullNamespace.split("\\.")[0];
            String sourceRoot = sourceNamespace.split("\\.")[0];

            if (!rootNamespace.equals(sourceRoot)) {
                enumImports.add(String.format("from %s.%s import %s", fullNamespace, name, name));
            }
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
