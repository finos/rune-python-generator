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

/**
 * Determine the Rosetta dependencies for a Rosetta object
 */
public class FunctionDependencyProvider {
    @Inject
    private RObjectFactory rTypeBuilderFactory;

    private final Set<EObject> visited = new HashSet<>();

    public Set<EObject> findDependencies(EObject object) {
        if (visited.contains(object)) {
            return Collections.emptySet();
        }
        return generateDependencies(object);
    }

    public Set<EObject> generateDependencies(EObject object) {
        if (object == null) {
            return Collections.emptySet();
        }

        Set<EObject> dependencies;
        if (object instanceof RosettaBinaryOperation binary) {
            dependencies = new HashSet<>();
            dependencies.addAll(generateDependencies(binary.getLeft()));
            dependencies.addAll(generateDependencies(binary.getRight()));
        } else if (object instanceof RosettaConditionalExpression cond) {
            dependencies = new HashSet<>();
            dependencies.addAll(generateDependencies(cond.getIf()));
            dependencies.addAll(generateDependencies(cond.getIfthen()));
            dependencies.addAll(generateDependencies(cond.getElsethen()));
        } else if (object instanceof RosettaOnlyExistsExpression onlyExists) {
            dependencies = findDependenciesFromIterable(onlyExists.getArgs());
        } else if (object instanceof RosettaFunctionalOperation functional) {
            dependencies = new HashSet<>();
            dependencies.addAll(generateDependencies(functional.getArgument()));
            dependencies.addAll(generateDependencies(functional.getFunction()));
        } else if (object instanceof RosettaUnaryOperation unary) {
            dependencies = generateDependencies(unary.getArgument());
        } else if (object instanceof RosettaFeatureCall featureCall) {
            dependencies = generateDependencies(featureCall.getReceiver());
        } else if (object instanceof RosettaSymbolReference symbolRef) {
            dependencies = new HashSet<>();
            dependencies.addAll(generateDependencies(symbolRef.getSymbol()));
            dependencies.addAll(findDependenciesFromIterable(symbolRef.getArgs()));
        } else if (object instanceof Function || object instanceof Data || object instanceof RosettaEnumeration) {
            dependencies = new HashSet<>(Collections.singleton(object));
        } else if (object instanceof InlineFunction inline) {
            dependencies = generateDependencies(inline.getBody());
        } else if (object instanceof ListLiteral listLiteral) {
            dependencies = listLiteral.getElements().stream()
                    .flatMap(el -> generateDependencies(el).stream())
                    .collect(Collectors.toSet());
        } else if (object instanceof RosettaConstructorExpression constructor) {
            dependencies = new HashSet<>();
            if (constructor.getTypeCall() != null && constructor.getTypeCall().getType() != null) {
                dependencies.add(constructor.getTypeCall().getType());
            }
            constructor.getValues()
                    .forEach(valuePair -> dependencies.addAll(generateDependencies(valuePair.getValue())));
        } else if (object instanceof RosettaExternalFunction ||
                object instanceof RosettaEnumValueReference ||
                object instanceof RosettaLiteral ||
                object instanceof RosettaImplicitVariable ||
                object instanceof RosettaSymbol ||
                object instanceof RosettaDeepFeatureCall) {
            dependencies = Collections.emptySet();
        } else {
            throw new IllegalArgumentException(object.eClass().getName()
                    + ": generating dependency in a function for this type is not yet implemented.");
        }

        if (!dependencies.isEmpty()) {
            visited.add(object);
        }
        return dependencies;
    }

    public Set<EObject> findDependenciesFromIterable(Iterable<? extends EObject> objects) {
        if (objects == null) {
            return Collections.emptySet();
        }
        return StreamSupport.stream(objects.spliterator(), false)
                .flatMap(obj -> generateDependencies(obj).stream())
                .collect(Collectors.toSet());
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

    public void reset() {
        visited.clear();
    }
}
