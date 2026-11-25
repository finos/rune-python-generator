/*
 * Copyright (c) 2023 CLOUDRISK Limited and FT Advisory LLC
 * SPDX-License-Identifier: Apache-2.0
 */
package com.regnosys.rosetta.generator.python.util;

public final class PythonCodeGeneratorConstants {
    private PythonCodeGeneratorConstants() {
        // Restricted constructor
    }

    /**
     * The Python language identifier.
     */
    public static final String PYTHON = "python";
    /**
     * The source directory path.
     */
    public static final String SRC = "src/";
    /**
     * The pyproject.toml file name.
     */
    public static final String PYPROJECT_TOML = "pyproject.toml";
    /**
     * The README file name, which pyproject.toml names as the package description.
     */
    public static final String README_MD = "README.md";
    /**
     * The __init__ file name.
     */
    public static final String INIT = "__init__";
}
