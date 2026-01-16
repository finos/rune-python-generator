package com.regnosys.rosetta.generator.python.util;

import com.regnosys.rosetta.rosetta.RosettaModel;
import com.regnosys.rosetta.types.RAttribute;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class PythonCodeGeneratorUtil {

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
        StringBuilder sb = new StringBuilder();
        sb.append("    #\n");
        sb.append("    # ").append(definition).append("\n");
        sb.append("    #\n");
        for (RAttribute attribute : attributes) {
            sb.append("     # @param ").append(attribute.getName()).append(" ").append(attribute.getDefinition())
                    .append("\n");
        }
        sb.append("    #\n");
        return sb.toString();
    }

    public static String createImports(String name) {
        return """
                # pylint: disable=line-too-long, invalid-name, missing-function-docstring
                # pylint: disable=bad-indentation, trailing-whitespace, superfluous-parens
                # pylint: disable=wrong-import-position, unused-import, unused-wildcard-import
                # pylint: disable=wildcard-import, wrong-import-order, missing-class-docstring
                # pylint: disable=missing-module-docstring, unused-variable, unnecessary-pass

                from __future__ import annotations
                from typing import Optional, Annotated
                import datetime
                import inspect
                from decimal import Decimal
                from pydantic import Field
                from rune.runtime.base_data_class import BaseDataClass
                from rune.runtime.metadata import *
                from rune.runtime.utils import *
                from rune.runtime.conditions import *
                from rune.runtime.func_proxy import *
                __all__ = ['%s']

                """.formatted(name).stripIndent();
    }

    public static String createImports() {
        return """
                # pylint: disable=line-too-long, invalid-name, missing-function-docstring
                # pylint: disable=bad-indentation, trailing-whitespace, superfluous-parens
                # pylint: disable=wrong-import-position, unused-import, unused-wildcard-import
                # pylint: disable=wildcard-import, wrong-import-order, missing-class-docstring
                # pylint: disable=missing-module-docstring
                from __future__ import annotations
                from typing import Optional, Annotated
                import datetime
                import inspect
                from decimal import Decimal
                from pydantic import Field
                from rune.runtime.base_data_class import BaseDataClass
                from rune.runtime.metadata import *
                from rune.runtime.utils import *
                from rune.runtime.conditions import *
                from rune.runtime.func_proxy import *
                """.stripIndent();
    }

    public static String createImportsFunc(String name) {
        return """
                # pylint: disable=line-too-long, invalid-name, missing-function-docstring, missing-module-docstring, superfluous-parens
                # pylint: disable=wrong-import-position, unused-import, unused-wildcard-import, wildcard-import, wrong-import-order, missing-class-docstring
                from __future__ import annotations
                import sys
                import datetime
                import inspect
                from decimal import Decimal
                from rune.runtime.base_data_class import BaseDataClass
                from rune.runtime.metadata import *
                from rune.runtime.utils import *
                from rune.runtime.conditions import *
                from rune.runtime.func_proxy import *
                """
                .stripIndent();
    }

    public static String toFileName(String namespace, String fileName) {
        return "src/" + namespace.replace(".", "/") + "/" + fileName;
    }

    public static String toPyFileName(String namespace, String fileName) {
        return toFileName(namespace, fileName) + ".py";
    }

    public static String toPyFunctionFileName(String namespace, String fileName) {
        return "src/" + namespace.replace(".", "/") + "/functions/" + fileName + ".py";
    }

    public static String createTopLevelInitFile(String version) {
        return "from .version import __version__";
    }

    public static String createVersionFile(String version) {
        String versionComma = version.replace('.', ',');
        return "version = (" + versionComma + ",0)\n" +
                "version_str = '" + version + "-0'\n" +
                "__version__ = '" + version + "'\n" +
                "__build_time__ = '" + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "'";
    }

    public static String getNamespace(RosettaModel rm) {
        return rm.getName().split("\\.")[0];
    }

    public static String createPYProjectTomlFile(String namespace, String version) {
        return """
                [build-system]
                requires = ["setuptools>=62.0"]
                build-backend = "setuptools.build_meta"

                [project]
                name = "python-%s"
                version = "%s"
                requires-python = ">= 3.11"
                dependencies = [
                   "pydantic>=2.10.3",
                   "rune.runtime>=1.0.0,<1.1.0"
                ]
                [tool.setuptools.packages.find]
                where = ["src"]""".formatted(namespace, version).stripIndent();
    }
}
