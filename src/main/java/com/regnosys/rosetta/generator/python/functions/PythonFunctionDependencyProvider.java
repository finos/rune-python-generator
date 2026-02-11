package com.regnosys.rosetta.generator.python.functions;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;

import com.regnosys.rosetta.rosetta.RosettaBasicType;
import com.regnosys.rosetta.rosetta.RosettaEnumValueReference;
import com.regnosys.rosetta.rosetta.RosettaEnumeration;
import com.regnosys.rosetta.rosetta.RosettaExternalFunction;
import com.regnosys.rosetta.rosetta.RosettaModel;
import com.regnosys.rosetta.rosetta.RosettaRecordType;
import com.regnosys.rosetta.rosetta.RosettaRule;
import com.regnosys.rosetta.rosetta.RosettaSymbol;
import com.regnosys.rosetta.rosetta.RosettaTypeAlias;
import com.regnosys.rosetta.rosetta.expression.FilterOperation;
import com.regnosys.rosetta.rosetta.expression.InlineFunction;
import com.regnosys.rosetta.rosetta.expression.ListLiteral;
import com.regnosys.rosetta.rosetta.expression.MapOperation;
import com.regnosys.rosetta.rosetta.expression.RosettaBinaryOperation;
import com.regnosys.rosetta.rosetta.expression.RosettaConditionalExpression;
import com.regnosys.rosetta.rosetta.expression.RosettaConstructorExpression;
import com.regnosys.rosetta.rosetta.expression.RosettaDeepFeatureCall;
import com.regnosys.rosetta.rosetta.expression.RosettaExpression;
import com.regnosys.rosetta.rosetta.expression.RosettaFeatureCall;
import com.regnosys.rosetta.rosetta.expression.RosettaFunctionalOperation;
import com.regnosys.rosetta.rosetta.expression.RosettaImplicitVariable;
import com.regnosys.rosetta.rosetta.expression.RosettaLiteral;
import com.regnosys.rosetta.rosetta.expression.RosettaOnlyExistsExpression;
import com.regnosys.rosetta.rosetta.expression.RosettaSymbolReference;
import com.regnosys.rosetta.rosetta.expression.RosettaUnaryOperation;
import com.regnosys.rosetta.rosetta.expression.WithMetaOperation;
import com.regnosys.rosetta.rosetta.simple.Data;
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
    public void addDependencies(EObject object, Set<String> enumImports) {
        if (object instanceof RosettaEnumeration enumeration) {
            String name = enumeration.getName();
            RosettaModel model = (RosettaModel) enumeration.eContainer();
            String prefix = model.getName();
            enumImports.add("import " + prefix + "." + name);
        } else if (object instanceof RosettaEnumValueReference ref) {
            addDependencies(ref.getEnumeration(), enumImports);
        } else if (object instanceof RosettaBinaryOperation binary) {
            addDependencies(binary.getLeft(), enumImports);
            addDependencies(binary.getRight(), enumImports);
        } else if (object instanceof RosettaConditionalExpression cond) {
            addDependencies(cond.getIf(), enumImports);
            addDependencies(cond.getIfthen(), enumImports);
            addDependencies(cond.getElsethen(), enumImports);
        } else if (object instanceof RosettaOnlyExistsExpression onlyExists) {
            onlyExists.getArgs().forEach(arg -> addDependencies(arg, enumImports));
        } else if (object instanceof RosettaFunctionalOperation functional) {
            if (functional.getArgument() != null) {
                addDependencies(functional.getArgument(), enumImports);
            }
            if (functional instanceof FilterOperation filter) {
                addDependencies(filter.getFunction(), enumImports);
            } else if (functional instanceof MapOperation map) {
                addDependencies(map.getFunction(), enumImports);
            }
        } else if (object instanceof WithMetaOperation withMeta) {
            addDependencies(withMeta.getArgument(), enumImports);
            withMeta.getEntries().forEach(entry -> addDependencies(entry.getValue(), enumImports));
        } else if (object instanceof RosettaUnaryOperation unary) {
            addDependencies(unary.getArgument(), enumImports);
        } else if (object instanceof RosettaFeatureCall featureCall) {
            addDependencies(featureCall.getReceiver(), enumImports);
        } else if (object instanceof RosettaSymbolReference symbolRef) {
            addDependencies(symbolRef.getSymbol(), enumImports);
            symbolRef.getArgs().forEach(arg -> addDependencies(arg, enumImports));
        } else if (object instanceof InlineFunction inline) {
            addDependencies(inline.getBody(), enumImports);
        } else if (object instanceof ListLiteral listLiteral) {
            listLiteral.getElements().forEach(el -> addDependencies(el, enumImports));
        } else if (object instanceof RosettaConstructorExpression constructor) {
            if (constructor.getTypeCall() != null && constructor.getTypeCall().getType() != null) {
                addDependencies(constructor.getTypeCall().getType(), enumImports);
            }
            constructor.getValues()
                    .forEach(valuePair -> addDependencies(valuePair.getValue(), enumImports));
        } else if (object instanceof Function
                || object instanceof Data
                || object instanceof RosettaExternalFunction
                || object instanceof RosettaLiteral
                || object instanceof RosettaImplicitVariable
                || object instanceof RosettaSymbol
                || object instanceof RosettaDeepFeatureCall
                || object instanceof RosettaBasicType
                || object instanceof RosettaRecordType
                || object instanceof RosettaTypeAlias) {
            return;
        } else if (object != null) {
            // Recurse into all children for unknown EObjects to ensure thorough dependency
            // collection
            object.eContents().forEach(child -> addDependencies(child, enumImports));
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
        List<RosettaSymbolReference> symbolRefs = EcoreUtil2.eAllOfType(expression, RosettaSymbolReference.class);
        Set<RFunction> result = new HashSet<>();
        for (RosettaSymbolReference ref : symbolRefs) {
            RosettaSymbol symbol = ref.getSymbol();
            if (symbol instanceof Function f) {
                result.add(rTypeBuilderFactory.buildRFunction(f));
            } else if (symbol instanceof RosettaRule r) {
                result.add(rTypeBuilderFactory.buildRFunction(r));
            }
        }
        return result;
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
