package com.regnosys.rosetta.generator.python.util;

import java.util.Map;
import java.util.Set;

import com.regnosys.rosetta.types.RAttribute;
import com.regnosys.rosetta.types.REnumType;
import com.regnosys.rosetta.types.RType;

/**
 * Utility for mapping Rune/Rosetta types and identifiers to Python-friendly names and types.
 *
 * Note: Keep the Python keyword/soft-keyword list aligned with the Python version you target.
 * You can regenerate a canonical list via:
 *   ```python 
 *   import keyword; print(sorted(keyword.kwlist))
 *   ```
 * and add any soft keywords you wish to avoid (e.g., "match", "case") for your generator context.
 */

public final class RuneToPythonMapper {

    private RuneToPythonMapper() {
        // Prevent instantiation (utility class)
    }

    // Prefix used to avoid collisions with Python reserved words or leading underscores.
    private static final String GENERATED_ATTR_PREFIX = "rune_attr_";

    // Python keywords and soft keywords to avoid as identifiers (case-sensitive, as in Python).
    // Sorted alphabetically for maintainability.
    private static final Set<String> PYTHON_KEYWORDS = Set.of(
        "False", "None", "True", "_",
        "and", "as", "assert", "async", "await",
        "break",
        "case", "class", "continue",
        "def", "del",
        "elif", "else", "except",
        "finally", "for", "from",
        "global",
        "if", "import", "in", "is",
        "lambda",
        "match",
        "nonlocal", "not",
        "or",
        "pass",
        "raise", "return",
        "try", "type",
        "while", "with",
        "yield"
    );

    // Python basic types recognized by the generator. Includes both qualified and unqualified forms
    // as they may appear in generated output or user-provided annotations.
    private static final Set<String> PYTHON_TYPES = Set.of(
        "Decimal", "bool", "date", "datetime", "datetime.date",
        "datetime.datetime", "datetime.time", "int", "str", "time"
    );

    // Mapping from Rosetta scalar types to Python basic types used by the generator.
    // Keys are the Rosetta/Rune type names.
    private static final Map<String, String> ROSETTA_TO_PY_BASIC = Map.ofEntries(
        Map.entry("boolean", "bool"),
        Map.entry("calculation", "str"),
        Map.entry("date", "datetime.date"),
        Map.entry("dateTime", "datetime.datetime"),
        Map.entry("eventType", "str"),
        Map.entry("int", "int"),
        Map.entry("number", "Decimal"),
        Map.entry("productType", "str"),
        Map.entry("string", "str"),
        Map.entry("time", "datetime.time"),
        Map.entry("zonedDateTime", "datetime.datetime")
    );

    // Mapping from Python basic types to their "WithMeta" counterparts used by the generator.
    private static final Map<String, String> PY_BASIC_TO_META = Map.ofEntries(
        Map.entry("Decimal", "NumberWithMeta"),
        Map.entry("bool", "BoolWithMeta"),
        Map.entry("datetime.date", "DateWithMeta"),
        Map.entry("datetime.datetime", "DateTimeWithMeta"),
        Map.entry("datetime.time", "TimeWithMeta"),
        Map.entry("int", "IntWithMeta"),
        Map.entry("str", "StrWithMeta")
    );

    /**
     * Mangles an identifier to avoid clashes with Python reserved words and names starting with "_".
     *
     * Null handling:
     * - Returns null if the input is null (caller-friendly for streaming/pipelines).
     *
     * @param name original identifier (may be null)
     * @return either the original name or the prefixed name if mangling is required
     */
    public static String mangleName(String name) {
        if (name == null) return null;
        if (PYTHON_KEYWORDS.contains(name) || name.startsWith("_")) {
            return GENERATED_ATTR_PREFIX + name;
        }
        return name;
    }

    /**
     * Maps a Python basic type to its WithMeta wrapper type used by the generator.
     *
     * @param pythonType Python basic type name (may be null)
     * @return the WithMeta type if known; otherwise returns the input unchanged; returns null if input is null
     */
    public static String getAttributeTypeWithMeta(String pythonType) {
        if (pythonType == null) return null;
        return PY_BASIC_TO_META.getOrDefault(pythonType, pythonType);
    }

    /**
     * Maps a Rosetta/Rune scalar type name to a Python basic type.
     *
     * @param rosettaType Rosetta type name (may be null)
     * @return Python basic type if mapped; otherwise the original input; null if input is null
     */
    public static String toPythonBasicType(String rosettaType) {
        if (rosettaType == null) return null;
        return ROSETTA_TO_PY_BASIC.getOrDefault(rosettaType, rosettaType);
    }

    /**
     * Maps an RType to its Python type representation.
     * - If it's a Rosetta basic type, returns the mapped Python basic type.
     * - Otherwise returns a qualified name "namespace.TypeName".
     * - For enums, appends ".TypeName" to reference the enum class within its module/package.
     *
     * @param rt Rosetta type (may be null)
     * @return Python type name (possibly qualified), or null if input is null
     */
    public static String toPythonType(RType rt) {
        if (rt == null) return null;

        // Prefer basic mapping when available
        String basic = ROSETTA_TO_PY_BASIC.get(rt.getName());
        if (basic != null) return basic;

        String rtName = rt.getName();
        String namespace = rt.getNamespace() != null ? rt.getNamespace().toString() : "";
        String qualified = namespace.isEmpty() ? rtName : namespace + "." + rtName;

        // For enums, retain original behavior: append ".<TypeName>"
        return (rt instanceof REnumType) ? qualified + "." + rtName : qualified;
    }

    /**
     * Checks whether a Rosetta type name corresponds to a Python basic type.
     *
     * @param rtName Rosetta type name (may be null)
     * @return true if the name maps to a Python basic type; false otherwise
     */
    public static boolean isRosettaBasicType(String rtName) {
        return rtName != null && ROSETTA_TO_PY_BASIC.containsKey(rtName);
    }

    /**
     * Checks whether a Rosetta RType corresponds to a Python basic type.
     *
     * @param rt Rosetta type (may be null)
     * @return true if the type maps to a Python basic type; false otherwise
     */
    public static boolean isRosettaBasicType(RType rt) {
        return rt != null && isRosettaBasicType(rt.getName());
    }

    /**
     * Safely checks if an attribute's type maps to a Python basic type.
     * Defensive null checks avoid NullPointerExceptions on partially-populated models.
     *
     * @param ra Rosetta attribute (may be null)
     * @return true if the attribute's type maps to a Python basic type; false otherwise
     */
    public static boolean isRosettaBasicType(RAttribute ra) {
        if (ra == null || ra.getRMetaAnnotatedType() == null) return false;
        RType rt = ra.getRMetaAnnotatedType().getRType();
        return rt != null && isRosettaBasicType(rt.getName());
    }

    /**
     * Checks whether a Python type name is one of the generator's recognized basic types.
     *
     * @param pythonType Python type name (may be null)
     * @return true if the type is recognized as a basic Python type by the generator; false otherwise
     */
    public static boolean isPythonBasicType(String pythonType) {
        return pythonType != null && PYTHON_TYPES.contains(pythonType);
    }
}