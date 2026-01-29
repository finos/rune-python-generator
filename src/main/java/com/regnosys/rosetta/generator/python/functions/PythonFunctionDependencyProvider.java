package com.regnosys.rosetta.generator.python.functions;

import com.regnosys.rosetta.rosetta.*;
import com.regnosys.rosetta.rosetta.expression.*;
import com.regnosys.rosetta.rosetta.simple.Data;
import com.regnosys.rosetta.rosetta.simple.Function;

import com.regnosys.rosetta.types.RFunction;
import com.regnosys.rosetta.types.RObjectFactory;
import jakarta.inject.Inject;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

// TODO: does this need to be here? RosettaFunctionalOperation functional

/**
 * Determine the Rosetta dependencies for a Rosetta object
 */
public class PythonFunctionDependencyProvider {
    @Inject
    private RObjectFactory rTypeBuilderFactory;

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
            // NOP
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
        } else if (object instanceof Function ||
                object instanceof Data ||
                object instanceof RosettaExternalFunction ||
                object instanceof RosettaLiteral ||
                object instanceof RosettaImplicitVariable ||
                object instanceof RosettaSymbol ||
                object instanceof RosettaDeepFeatureCall ||
                object instanceof RosettaBasicType ||
                object instanceof RosettaRecordType) {
            return;
        } else {
            throw new IllegalArgumentException(object.eClass().getName()
                    + ": generating dependency in a function for this type is not yet implemented.");
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
