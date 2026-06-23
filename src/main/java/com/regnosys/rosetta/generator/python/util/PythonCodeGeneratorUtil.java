/*
 * Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
 * SPDX-License-Identifier: Apache-2.0
 */
package com.regnosys.rosetta.generator.python.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.regnosys.rosetta.rosetta.RosettaModel;
import com.regnosys.rosetta.types.RAttribute;

public final class PythonCodeGeneratorUtil {

    public static final Pattern VALID_VERSION_PATTERN = Pattern.compile("\\d+\\.\\d+\\.\\d+");
    public static final Pattern DEV_VERSION_PATTERN = Pattern.compile("(\\d+\\.\\d+\\.\\d+)-dev\\.(\\d+)");
    public static final Pattern SNAPSHOT_VERSION_PATTERN = Pattern.compile("(\\d+\\.\\d+\\.\\d+)\\.[A-Za-z][A-Za-z0-9_-]+-SNAPSHOT");
    private static final Pattern PEP440_DEV_VERSION_PATTERN = Pattern.compile("\\d+\\.\\d+\\.\\d+\\.dev\\d+");

    private PythonCodeGeneratorUtil() {
    }

    public static boolean isValidVersion(String version) {
        if (version == null) {
            return false;
        }
        return VALID_VERSION_PATTERN.matcher(version).matches()
            || DEV_VERSION_PATTERN.matcher(version).matches()
            || SNAPSHOT_VERSION_PATTERN.matcher(version).matches();
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

    public static String createTopLevelInitFile(String version, String namespacePrefix) {
        StringBuilder sb = new StringBuilder();
        sb.append("from .version import __version__\n");
        sb.append("from rune.runtime.base_data_class import BaseDataClass\n");
        String namespacePrefixOrNone = (namespacePrefix == null) ? "None" : "'" + namespacePrefix + "'";
        sb.append("rune_namespace_prefix=" + namespacePrefixOrNone + "\n\n");
        sb.append(
            """
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
        return sb.toString();
    }

    public static String createVersionFile(String version) {
        String numericBase = version.contains(".dev")
                ? version.substring(0, version.indexOf(".dev"))
                : version;
        String versionTuple = numericBase.replace('.', ',');
        return "version = ("
                + versionTuple + ")\n"
                + "version_str = '" + version + "'\n"
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
                requires = ["setuptools>=77.0.3"]
                build-backend = "setuptools.build_meta"

                [project]
                name = "%s"
                version = "%s"
                license = "Apache-2.0"
                requires-python = ">= 3.11"
                dependencies = [
                   "pydantic>=2.10.3",
                   "rune.runtime>=2.0.0,<3.0.0"
                ]
                readme = "README.md"
                classifiers = [
                    "Programming Language :: Python :: 3 :: Only",
                    "Programming Language :: Python :: 3.11",
                    "Programming Language :: Python :: 3.12",
                    "Programming Language :: Python :: 3.13"
                ]                
                authors = [
                    { name = "Daniel Schwartz" },
                    { name = "Plamen Neykov" },
                    { name = "Others (See AUTHORS)" }
                ]

                [tool.setuptools.packages.find]
                where = ["src"]""".formatted(name, version).stripIndent();
    }

    public static String cleanVersion(String version) {
        if (version == null || version.equals("${project.version}")) {
            return "0.0.0";
        }
        if (PEP440_DEV_VERSION_PATTERN.matcher(version).matches()) {
            return version;
        }
        Matcher snapshotMatcher = SNAPSHOT_VERSION_PATTERN.matcher(version);
        if (snapshotMatcher.matches()) {
            return snapshotMatcher.group(1) + ".dev0";
        }
        Matcher devMatcher = DEV_VERSION_PATTERN.matcher(version);
        if (devMatcher.matches()) {
            return devMatcher.group(1) + ".dev" + devMatcher.group(2);
        }
        if (VALID_VERSION_PATTERN.matcher(version).matches()) {
            return version;
        }
        return "0.0.0";
    }
    public static String mapMetaTypeToMetaDataName(String metaTypeName) {
        return switch (metaTypeName) {
            case "reference" -> "ref";
            case "key" -> "key";
            case "scheme" -> "scheme";
            case "id" -> "key_external";
            case "location" -> "key_scoped";
            case "address" -> "ref_scoped";
            default -> throw new UnsupportedOperationException("Unsupported meta type: " + metaTypeName);
        };
    }
}
