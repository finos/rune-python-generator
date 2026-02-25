package com.regnosys.rosetta.generator.python.util;

import java.util.HashSet;
import java.util.Set;

import com.regnosys.rosetta.rosetta.RosettaEnumeration;
import com.regnosys.rosetta.rosetta.RosettaModel;
import com.regnosys.rosetta.rosetta.RosettaNamed;
import com.regnosys.rosetta.rosetta.simple.Function;
import com.regnosys.rosetta.types.RAttribute;
import com.regnosys.rosetta.types.REnumType;
import com.regnosys.rosetta.types.RType;
import com.regnosys.rosetta.types.builtin.RNumberType;

//todo: compare getFlattenedTypeName and getFullyQualifiedObjectName
/**
 * A utility class for mapping Rune (Rosetta) types and attributes to their
 * corresponding Python types.
 * This class also handles name mangling for Python keywords and reserved words.
 */
public final class RuneToPythonMapper {
    /**
     * The set of Python keywords and soft keywords.
     */
    private static final Set<String> PYTHON_KEYWORDS = new HashSet<>();

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private RuneToPythonMapper() {
    }

    static {
        // Initialize the set with Python keywords and soft keywords
        PYTHON_KEYWORDS.add("False");
        PYTHON_KEYWORDS.add("await");
        PYTHON_KEYWORDS.add("else");
        PYTHON_KEYWORDS.add("import");
        PYTHON_KEYWORDS.add("pass");
        PYTHON_KEYWORDS.add("None");
        PYTHON_KEYWORDS.add("break");
        PYTHON_KEYWORDS.add("except");
        PYTHON_KEYWORDS.add("in");
        PYTHON_KEYWORDS.add("raise");
        PYTHON_KEYWORDS.add("True");
        PYTHON_KEYWORDS.add("class");
        PYTHON_KEYWORDS.add("finally");
        PYTHON_KEYWORDS.add("is");
        PYTHON_KEYWORDS.add("return");
        PYTHON_KEYWORDS.add("and");
        PYTHON_KEYWORDS.add("continue");
        PYTHON_KEYWORDS.add("for");
        PYTHON_KEYWORDS.add("lambda");
        PYTHON_KEYWORDS.add("try");
        PYTHON_KEYWORDS.add("as");
        PYTHON_KEYWORDS.add("def");
        PYTHON_KEYWORDS.add("from");
        PYTHON_KEYWORDS.add("nonlocal");
        PYTHON_KEYWORDS.add("while");
        PYTHON_KEYWORDS.add("assert");
        PYTHON_KEYWORDS.add("del");
        PYTHON_KEYWORDS.add("global");
        PYTHON_KEYWORDS.add("not");
        PYTHON_KEYWORDS.add("with");
        PYTHON_KEYWORDS.add("async");
        PYTHON_KEYWORDS.add("elif");
        PYTHON_KEYWORDS.add("if");
        PYTHON_KEYWORDS.add("or");
        PYTHON_KEYWORDS.add("yield");
        PYTHON_KEYWORDS.add("match");
        PYTHON_KEYWORDS.add("case");
        PYTHON_KEYWORDS.add("type");
        PYTHON_KEYWORDS.add("_");
    }

    /**
     * Define the set of Python types as a static final field.
     */
    private static final Set<String> PYTHON_TYPES = Set.of(
            "int", "str", "Decimal", "date", "datetime", "datetime.datetime",
            "datetime.date", "datetime.time", "time", "bool");

    /**
     * Check if the attribute is a Python keyword or starts with an underscore.
     *
     * @param attrib the attribute name
     * @return the mangled name
     */
    public static String mangleName(String attrib) {
        if (PYTHON_KEYWORDS.contains(attrib) || attrib.charAt(0) == '_') {
            return "rune_attr_" + attrib;
        }
        return attrib;
    }

    /**
     * Inner private function to convert from Rosetta type to Python type.
     * Returns null if no matching type.
     */
    private static String toPythonBasicTypeInnerFunction(String typeName) {
        switch (typeName) {
            case "string":
            case "eventType":
            case "calculation":
            case "productType":
                return "str";
            case "time":
                return "datetime.time";
            case "date":
                return "datetime.date";
            case "dateTime":
            case "zonedDateTime":
                return "datetime.datetime";
            case "number":
                return "Decimal";
            case "boolean":
                return "bool";
            case "int":
                return "int";
            default:
                return null;
        }
    }

    /**
     * Inner private function to convert from Rosetta type to Python type.
     * Returns null if no matching type.
     *
     * @param attributeType the attribute type
     * @return the Python type with meta, or the original attribute type if no match
     */
    public static String getAttributeTypeWithMeta(String attributeType) {
        switch (attributeType) {
            case "str":
                return "StrWithMeta";
            case "datetime.time":
                return "TimeWithMeta";
            case "datetime.date":
                return "DateWithMeta";
            case "datetime.datetime":
                return "DateTimeWithMeta";
            case "Decimal":
                return "NumberWithMeta";
            case "bool":
                return "BoolWithMeta";
            case "int":
                return "IntWithMeta";
            default:
                return attributeType;
        }
    }

    public static String getFullyQualifiedObjectName(RosettaNamed rn) {
        RosettaModel model = (RosettaModel) rn.eContainer();
        if (model == null) {
            throw new RuntimeException("Rosetta model not found for data " + rn.getName());
        }

        if (rn instanceof REnumType) {
            return ((REnumType) rn).getQualifiedName().toString() + "." + rn.getName();
        }
        String typeName = toPythonBasicTypeInnerFunction(rn.getName());
        if (typeName == null) {
            String function = (rn instanceof Function) ? ".functions" : "";
            typeName = model.getName() + function + "." + rn.getName();
            if (rn instanceof RosettaEnumeration) {
                typeName += "." + rn.getName();
            }
        }
        return typeName;
    }

    public static String getBundleObjectName(RosettaNamed rn, boolean useQuotes) {
        String fullyQualifiedObjectName = getFullyQualifiedObjectName(rn);
        if (rn instanceof RosettaEnumeration || isRosettaBasicType(rn.getName())) {
            return fullyQualifiedObjectName;
        }
        String bundleName = fullyQualifiedObjectName.replace(".", "_");
        if (useQuotes) {
            return "\"" + bundleName + "\"";
        }
        return bundleName;
    }

    public static String getBundleObjectName(RosettaNamed rn) {
        return getBundleObjectName(rn, false);
    }

    /**
     * Convert from Rune type as string to Python type.
     *
     * @param rosettaType the Rune type name
     * @return the Python type name
     */
    public static String toPythonBasicType(String rosettaType) {
        String pythonType = toPythonBasicTypeInnerFunction(rosettaType);
        return (pythonType == null) ? rosettaType : pythonType;
    }

    /**
     * Convert from Rune RType to Python type with optional quoting for forward
     * references.
     *
     * @param rt        the Rune RType object
     * @param useQuotes whether to wrap the type name in quotes for forward
     *                  references
     * @return the Python type name string, or null if rt is null
     */
    public static String toPythonType(RType rt, boolean useQuotes) {
        if (rt == null) {
            return null;
        }
        String typeName = rt.getName();
        // if it is a number type and it is an integer, then it is an int
        if (rt instanceof RNumberType numberType && numberType.isInteger()) {
            typeName = "int";
        }
        var pythonType = toPythonBasicTypeInnerFunction(typeName);
        if (pythonType == null) {
            String rtName = rt.getName();
            pythonType = rt.getNamespace().toString() + "." + rtName;
            pythonType = (rt instanceof REnumType) ? pythonType + "." + rtName : pythonType;

            if (!isRosettaBasicType(rt) && !(rt instanceof REnumType)) {
                pythonType = getFlattenedTypeName(rt, pythonType);
                if (useQuotes) {
                    pythonType = "\"" + pythonType + "\"";
                }
            }
        }
        return pythonType;
    }

    /**
     * Convert from Rune RType to Python type.
     *
     * @param rt the Rune RType object
     * @return the Python type name string, or null if rt is null
     */
    public static String toPythonType(RType rt) {
        return toPythonType(rt, false);
    }

    /**
     * Check if the given type is a basic type.
     *
     * @param rtName the type name
     * @return true if it is a basic type, false otherwise
     */
    public static boolean isRosettaBasicType(String rtName) {
        return (toPythonBasicTypeInnerFunction(rtName) != null);
    }

    /**
     * Check if the given type is a basic type.
     *
     * @param rt the RType object
     * @return true if it is a basic type, false otherwise
     */
    public static boolean isRosettaBasicType(RType rt) {
        return (toPythonBasicTypeInnerFunction(rt.getName()) != null);
    }

    /**
     * Check if the given attribute is a basic type.
     *
     * @param ra the RAttribute object
     * @return true if the attribute's type is a basic type, false otherwise
     */
    public static boolean isRosettaBasicType(RAttribute ra) {
        if (ra == null) {
            return false;
        }
        RType rt = ra.getRMetaAnnotatedType().getRType();
        return (rt != null) ? isRosettaBasicType(rt.getName()) : false;
    }

    /**
     * Formats a Python type string based on cardinality and context.
     *
     * @param baseType        the base Python type (e.g., "str", "Decimal")
     * @param min             the minimum cardinality
     * @param max             the maximum cardinality
     * @param isInputArgument true if this is for a function input argument (uses "
     *                        | None"), false for a class field (uses
     *                        "Optional[...]").
     * @return the formatted Python type string
     */
    public static String formatCardinality(String baseType,
        int min,
        int max,
        boolean isInputArgument) {
        String type = baseType;

        if (max > 1 || max == -1 || max == 0) {
            type = "list[" + type + "]";
        }

        if (min == 0) {
            type = (isInputArgument) ? type + " | None" : "Optional[" + type + "]";
        }
        return type;
    }

    public static String getFlattenedTypeName(RType type, String typeName) {
        if (isRosettaBasicType(type) || type instanceof REnumType) {
            return typeName;
        }
        return typeName.replace('.', '_');
    }

    /**
     * Check if the given type is in the set of Python types.
     *
     * @param pythonType the Python type name
     * @return true if it is a known Python type, false otherwise
     */
    public static boolean isPythonBasicType(final String pythonType) {
        return PYTHON_TYPES.contains(pythonType);
    }
}
