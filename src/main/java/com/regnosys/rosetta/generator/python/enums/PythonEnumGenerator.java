package com.regnosys.rosetta.generator.python.enums;

import com.regnosys.rosetta.generator.java.enums.EnumHelper;
import com.regnosys.rosetta.generator.python.util.PythonCodeGeneratorUtil;
import com.regnosys.rosetta.generator.python.util.PythonCodeWriter;
import com.regnosys.rosetta.rosetta.RosettaEnumeration;
import com.regnosys.rosetta.rosetta.RosettaEnumValue;
import com.regnosys.rosetta.rosetta.RosettaModel;

import java.util.*;

public class PythonEnumGenerator {

    /**
     * Generate Python from the collection of Rosetta enumerations.
     * 
     * @param rosettaEnums the collection of Rosetta enumerations to generate
     * @param version      the version for this collection of enumerations
     * @return a Map of all the generated Python indexed by the file name
     */
    public Map<String, String> generate(Iterable<RosettaEnumeration> rosettaEnums, String version) {
        Map<String, String> result = new HashMap<>();
        for (RosettaEnumeration enumeration : rosettaEnums) {
            RosettaModel tr = (RosettaModel) enumeration.eContainer();
            String namespace = tr.getName();

            PythonCodeWriter writer = new PythonCodeWriter();
            writer.appendLine("# pylint: disable=missing-module-docstring, invalid-name, line-too-long");
            writer.appendLine("from enum import Enum");
            writer.appendLine("import rune.runtime.metadata");
            writer.newLine();
            writer.appendLine("__all__ = ['" + enumeration.getName() + "']");
            writer.newLine();

            generateEnumClass(writer, enumeration);
            result.put(PythonCodeGeneratorUtil.toPyFileName(namespace, enumeration.getName()), writer.toString());
        }
        return result;
    }

    private List<RosettaEnumValue> getAllEnumValues(RosettaEnumeration enumeration) {
        List<RosettaEnumValue> enumValues = new ArrayList<>();
        RosettaEnumeration current = enumeration;

        while (current != null) {
            enumValues.addAll(current.getEnumValues());
            current = current.getParent();
        }
        enumValues.sort(Comparator.comparing(RosettaEnumValue::getName));
        return enumValues;
    }

    private void generateEnumClass(PythonCodeWriter writer, RosettaEnumeration enume) {
        List<RosettaEnumValue> allValues = getAllEnumValues(enume);
        writer.appendLine("class " + enume.getName() + "(rune.runtime.metadata.EnumWithMetaMixin, Enum):");
        writer.indent();

        if (enume.getDefinition() != null) {
            writer.appendLine("\"\"\"");
            writer.appendLine(enume.getDefinition());
            writer.appendLine("\"\"\"");
        }

        if (allValues.isEmpty()) {
            writer.appendLine("pass");
        } else {
            for (RosettaEnumValue value : allValues) {
                String valName = EnumHelper.convertValue(value);
                String display = (value.getDisplay() != null) ? value.getDisplay() : value.getName();
                writer.appendLine(valName + " = \"" + display + "\"");
                if (value.getDefinition() != null) {
                    writer.appendLine("\"\"\"");
                    writer.appendLine(value.getDefinition());
                    writer.appendLine("\"\"\"");
                }
            }
        }
        writer.unindent();
    }
}
