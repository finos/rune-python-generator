/*
 * Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
 * SPDX-License-Identifier: Apache-2.0
 */
package com.regnosys.rosetta.generator.python.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

import com.regnosys.rosetta.rosetta.RosettaModel;
import com.regnosys.rosetta.types.RAttribute;

public final class PythonCodeGeneratorUtil {

    private PythonCodeGeneratorUtil() {
    }

public static String fileComment(String version) {
        return """
                # This file is auto-generated from the Rune Python Generator, do not edit.
                # Version: %s

                """.formatted(version).stripIndent();
    }

    public static String comment(String definition) {
        if (definition == null || definition.isEmpty()) {
            return "";
        }
        return """
                    #
                    # %s
                    #
                """.formatted(definition).stripIndent();
    }

    public static String classComment(String definition, Iterable<RAttribute> attributes) {
        if (definition == null || definition.isEmpty()) {
            return "";
        }
        PythonCodeWriter writer = new PythonCodeWriter();
        writer.appendLine("#");
        writer.appendLine("# " + definition);
        writer.appendLine("#");
        for (RAttribute attribute : attributes) {
            writer.appendLine("# @param " + attribute.getName() + " " + attribute.getDefinition());
        }
        writer.appendLine("#");
        return writer.toString();
    }

    public static String createImports() {
        return """
                # pylint: disable=line-too-long, invalid-name, missing-function-docstring
                # pylint: disable=bad-indentation, trailing-whitespace, superfluous-parens
                # pylint: disable=wrong-import-position, unused-import, unused-wildcard-import
                # pylint: disable=wildcard-import, wrong-import-order, missing-class-docstring
                # pylint: disable=missing-module-docstring
                from __future__ import annotations
                import datetime
                import functools
                import inspect
                import sys
                from decimal import Decimal
                from typing import Annotated, Optional

                from pydantic import Field, validate_call

                from rune.runtime.base_data_class import BaseDataClass
                from rune.runtime.cow import rune_cow
                from rune.runtime.conditions import *
                from rune.runtime.func_proxy import *
                from rune.runtime.metadata import *
                from rune.runtime.native_registry import rune_attempt_register_native_functions, rune_execute_native
                from rune.runtime.object_builder import ObjectBuilder
                from rune.runtime.utils import *

                """;
    }

    public static String toFileSystemPath(String namespace) {
        return namespace.replace(".", "/");
    }

    public static String toFlattenedName(String name) {
        return name.replace(".", "_");
    }

    public static String toFileName(String namespace, String fileName) {
        return "src/" + toFileSystemPath(namespace) + "/" + fileName;
    }

    public static String toPyFileName(String namespace, String fileName) {
        return toFileName(namespace, fileName) + ".py";
    }

    public static String createTopLevelInitFile(String version, String namespacePrefix, Set<String> nativeFunctionNames) {
        StringBuilder sb = new StringBuilder();
        sb.append("from .version import __version__\n");
        if (nativeFunctionNames != null && !nativeFunctionNames.isEmpty()) {
            sb.append("from rune.runtime.native_registry import rune_register_native as _rune_register_native\n");
        }
        String namespacePrefixOrNone = (namespacePrefix == null) ? "None" : "'" + namespacePrefix + "'";
        sb.append("rune_namespace_prefix=" + namespacePrefixOrNone + "\n");
        sb.append(
            """
            from rune.runtime.base_data_class import BaseDataClass
            def rune_deserialize(
                rune_data,
                validate_model=True,
                check_rune_constraints=True,
                strict=False,
                raise_validation_errors=True,
            ):
                return BaseDataClass.rune_deserialize(
                    rune_data,
                    validate_model=validate_model,
                    check_rune_constraints=check_rune_constraints,
                    strict=strict,
                    raise_validation_errors=raise_validation_errors,
                    namespace_prefix=rune_namespace_prefix,
                )
            """);
        if (nativeFunctionNames != null && !nativeFunctionNames.isEmpty()) {
            sb.append("# TODO: replace with rune_attempt_register_native_functions once runtime supports rune/native path convention:\n");
            sb.append("# rune_attempt_register_native_functions(\n");
            sb.append("#     function_names=[\n");
            for (String fqn : nativeFunctionNames) {
                sb.append("#         '").append(fqn).append("',\n");
            }
            sb.append("#     ]\n");
            sb.append("# )\n");
            int i = 0;
            for (String fqn : nativeFunctionNames) {
                String[] parts = fqn.split("\\.");
                String functionName = parts[parts.length - 1];
                // Strip .functions.<FunctionName> (last 2 parts) to derive the rune namespace,
                // then append .rune.native to get the native implementation module path.
                // The native file is at <namespace>/rune/native/<FunctionName>.py,
                // so import the function from that specific module file.
                String nativeModule = String.join(".", java.util.Arrays.copyOfRange(parts, 0, parts.length - 2)) + ".rune.native." + functionName;
                sb.append("try:\n");
                sb.append("    from ").append(nativeModule).append(" import ").append(functionName)
                  .append(" as _native_impl_").append(i).append("\n");
                sb.append("    _rune_register_native('").append(fqn).append("', _native_impl_").append(i).append(")\n");
                sb.append("except ImportError:\n");
                sb.append("    pass\n");
                i++;
            }
        }
        return sb.toString();
    }

    public static String createVersionFile(String version) {
        String versionComma = version.replace('.', ',');
        return "version = ("
                + versionComma + ",0)\n"
                + "version_str = '" + version + "-0'\n"
                + "__version__ = '" + version + "'\n"
                + "__build_time__ = '"
                + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "'";
    }

    public static String getNamespace(RosettaModel rm) {
        if (rm.getName() == null) {
            return "com.rosetta.test.model".split("\\.")[0];
        }
        return rm.getName().split("\\.")[0];
    }

    public static String createPYProjectTomlFile(String namespace, String version) {
        return createPYProjectTomlFile(namespace, version, null);
    }

    public static String createPYProjectTomlFile(String namespace, String version, String projectName) {
        String name = (projectName != null && !projectName.isBlank())
                ? projectName
                : "python-" + namespace;
        return """
                [build-system]
                requires = ["setuptools>=62.0"]
                build-backend = "setuptools.build_meta"

                [project]
                name = "%s"
                version = "%s"
                requires-python = ">= 3.11"
                dependencies = [
                   "pydantic>=2.10.3",
                   "rune.runtime>=1.0.0,<2.0.0"
                ]
                [tool.setuptools.packages.find]
                where = ["src"]""".formatted(name, version).stripIndent();
    }

    public static String cleanVersion(String version) {
        if (version == null || version.equals("${project.version}")) {
            return "0.0.0";
        }

        // Preserve PEP 440 dev versions already converted by the CLI (e.g. "1.2.3.dev4")
        if (version.matches("\\d+\\.\\d+\\.\\d+\\.dev\\d+")) {
            return version;
        }

        String[] versionParts = version.split("\\.");
        if (versionParts.length > 2) {
            String thirdPart = versionParts[2].replaceAll("[^\\d]", "");
            return versionParts[0] + "." + versionParts[1] + "." + thirdPart;
        }

        return "0.0.0";
    }
}
