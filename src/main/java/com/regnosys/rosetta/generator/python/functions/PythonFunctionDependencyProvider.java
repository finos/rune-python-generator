package com.regnosys.rosetta.generator.python.functions;

import com.regnosys.rosetta.rosetta.*;
import com.regnosys.rosetta.rosetta.expression.*;
import com.regnosys.rosetta.rosetta.simple.Data;
import com.regnosys.rosetta.rosetta.simple.Function;

import com.regnosys.rosetta.types.REnumType;
import com.regnosys.rosetta.types.RFunction;
import com.regnosys.rosetta.types.RObjectFactory;
import com.regnosys.rosetta.types.RType;
import jakarta.inject.Inject;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Determine the Rosetta dependencies for a Rosetta object
 */
public class PythonFunctionDependencyProvider {
    @Inject
    private RObjectFactory rTypeBuilderFactory;

    public void addDependencies(EObject object, Set<String> enumImports) {
        addDependencies(object, enumImports, null);
    }

    public void addDependencies(EObject object, Set<String> enumImports, String namespacePrefix) {
        if (object instanceof RosettaEnumeration enumeration) {
            String name = enumeration.getName();
            RosettaModel model = (RosettaModel) enumeration.eContainer();
            String prefix = applyPrefix(model.getName(), namespacePrefix);
            enumImports.add("import " + prefix + "." + name);
        } else if (object instanceof RosettaEnumValueReference ref) {
            addDependencies(ref.getEnumeration(), enumImports, namespacePrefix);
        } else if (object instanceof RosettaBinaryOperation binary) {
            addDependencies(binary.getLeft(), enumImports, namespacePrefix);
            addDependencies(binary.getRight(), enumImports, namespacePrefix);
        } else if (object instanceof RosettaConditionalExpression cond) {
            addDependencies(cond.getIf(), enumImports, namespacePrefix);
            addDependencies(cond.getIfthen(), enumImports, namespacePrefix);
            addDependencies(cond.getElsethen(), enumImports, namespacePrefix);
        } else if (object instanceof RosettaOnlyExistsExpression onlyExists) {
            onlyExists.getArgs().forEach(arg -> addDependencies(arg, enumImports, namespacePrefix));
        } else if (object instanceof RosettaFunctionalOperation functional) {
            if (functional.getArgument() != null) {
                addDependencies(functional.getArgument(), enumImports, namespacePrefix);
            }
            if (functional instanceof FilterOperation filter) {
                addDependencies(filter.getFunction(), enumImports, namespacePrefix);
            } else if (functional instanceof MapOperation map) {
                addDependencies(map.getFunction(), enumImports, namespacePrefix);
            }
        } else if (object instanceof RosettaUnaryOperation unary) {
            addDependencies(unary.getArgument(), enumImports, namespacePrefix);
        } else if (object instanceof RosettaFeatureCall featureCall) {
            addDependencies(featureCall.getReceiver(), enumImports, namespacePrefix);
        } else if (object instanceof RosettaSymbolReference symbolRef) {
            addDependencies(symbolRef.getSymbol(), enumImports, namespacePrefix);
            symbolRef.getArgs().forEach(arg -> addDependencies(arg, enumImports, namespacePrefix));
        } else if (object instanceof InlineFunction inline) {
            addDependencies(inline.getBody(), enumImports, namespacePrefix);
        } else if (object instanceof ListLiteral listLiteral) {
            listLiteral.getElements().forEach(el -> addDependencies(el, enumImports, namespacePrefix));
        } else if (object instanceof RosettaConstructorExpression constructor) {
            if (constructor.getTypeCall() != null && constructor.getTypeCall().getType() != null) {
                addDependencies(constructor.getTypeCall().getType(), enumImports, namespacePrefix);
            }
            constructor.getValues()
                    .forEach(valuePair -> addDependencies(valuePair.getValue(), enumImports, namespacePrefix));
        } else if (object instanceof Function ||
                object instanceof Data ||
                object instanceof RosettaExternalFunction ||
                object instanceof RosettaLiteral ||
                object instanceof RosettaImplicitVariable ||
                object instanceof RosettaSymbol ||
                object instanceof RosettaDeepFeatureCall ||
                object instanceof RosettaBasicType ||
                object instanceof RosettaRecordType ||
                object instanceof RosettaTypeAlias) {
            return;
        } else if (object != null) {
            // Recurse into all children for unknown EObjects to ensure thorough dependency
            // collection
            object.eContents().forEach(child -> addDependencies(child, enumImports, namespacePrefix));
        }
    }

    public void addDependencies(RType type, Set<String> enumImports) {
        addDependencies(type, enumImports, null);
    }

    public void addDependencies(RType type, Set<String> enumImports, String namespacePrefix) {
        if (type instanceof REnumType enumType) {
            String name = enumType.getName();
            String prefix = applyPrefix(enumType.getNamespace().toString(), namespacePrefix);
            enumImports.add("import " + prefix + "." + name);
        }
    }

    private static String applyPrefix(String name, String namespacePrefix) {
        return (namespacePrefix != null && !namespacePrefix.isBlank())
                ? namespacePrefix + "." + name
                : name;
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
