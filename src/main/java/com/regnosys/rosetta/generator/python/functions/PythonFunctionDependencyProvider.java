/*
 * Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
 * SPDX-License-Identifier: Apache-2.0
 */
package com.regnosys.rosetta.generator.python.functions;

import java.util.Set;

import com.regnosys.rosetta.rosetta.RosettaEnumValue;
import com.regnosys.rosetta.rosetta.RosettaEnumeration;
import com.regnosys.rosetta.rosetta.RosettaModel;
import com.regnosys.rosetta.rosetta.RosettaNamed;
import com.regnosys.rosetta.rosetta.expression.InlineFunction;
import com.regnosys.rosetta.rosetta.expression.RosettaExpression;
import com.regnosys.rosetta.types.RAliasType;
import com.regnosys.rosetta.types.RDataType;
import com.regnosys.rosetta.types.REnumType;
import com.regnosys.rosetta.types.RType;
import com.regnosys.rosetta.types.RosettaTypeProvider;
import com.regnosys.rosetta.types.TypeSystem;

import com.regnosys.rosetta.generator.python.PythonCodeGeneratorContext;

import com.regnosys.rosetta.generator.python.util.PythonDependencyCollector;
import jakarta.inject.Inject;

/**
 * Determine the Rosetta dependencies for a Function
 */
public final class PythonFunctionDependencyProvider {
    /**
     * Collects the functions, types and enums an expression refers to.
     */
    @Inject
    private PythonDependencyCollector dependencyCollector;

    /** The Rosetta type system. */
    @Inject
    private TypeSystem typeSystem;

    /** Gives the enum type an expression refers to, which may extend the enum declaring a value. */
    @Inject
    private RosettaTypeProvider typeProvider;

    public void addDependencies(RosettaNamed named, Set<String> enumImports, String sourceNamespace,
            PythonCodeGeneratorContext context) {
        if (named instanceof RosettaEnumeration enumeration) {
            String name = enumeration.getName();
            RosettaModel model = enumeration.getModel();
            String prefix = context.applyPrefix(model.getName());
            enumImports.add("import " + prefix + "." + name);
        } else if (named instanceof RosettaEnumValue enumValue) {
            // A value written without its type, such as `AssetPrice`, still needs its enum
            addDependencies(enumValue.getEnumeration(), enumImports, sourceNamespace, context);
        } else if (named instanceof com.regnosys.rosetta.rosetta.simple.Data data) {
            String name = data.getName();
            RosettaModel model = data.getModel();
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
        PythonDependencyCollector.Dependencies found = dependencyCollector.collect(expression);
        found.symbols().forEach(named -> addDependencies(named, enumImports, sourceNamespace, context));
        found.constructedTypes().forEach(type -> addDependencies(type, enumImports, sourceNamespace, context));
        found.toEnumTargets().forEach(target -> addDependencies(target, enumImports, sourceNamespace, context));
        // Written through the expression's enum type, as in PythonExpressionGenerator
        found.enumValueTypes().forEach(type -> addDependencies(type, enumImports, sourceNamespace, context));
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
}
