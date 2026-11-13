/*
 * Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
 * SPDX-License-Identifier: Apache-2.0
 */
package com.regnosys.rosetta.generator.python.object;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.regnosys.rosetta.generator.python.util.PythonCodeWriter;
import com.regnosys.rosetta.generator.python.util.RuneToPythonMapper;
import com.regnosys.rosetta.rosetta.simple.Choice;
import com.regnosys.rosetta.types.RChoiceOption;
import com.regnosys.rosetta.types.RChoiceType;
import com.regnosys.rosetta.types.RDataType;
import com.regnosys.rosetta.types.REnumType;
import com.regnosys.rosetta.types.RObjectFactory;
import com.regnosys.rosetta.types.RType;
import com.regnosys.rosetta.types.TypeSystem;
import com.regnosys.rosetta.types.builtin.RBuiltinTypeService;

import jakarta.inject.Inject;

/**
 * Generates the {@code _CHOICE_DISPATCH} table of a choice class (#333).
 *
 * <p>The runtime reads a choice in the Rune JSON format, where {@code @type} names the selected
 * leaf, not the option of the choice that holds it. The table maps each {@code @type} the choice
 * can be read with to the Python attribute name of that option:
 * <ul>
 * <li>a data type or enum is keyed by its Rune fully qualified name;</li>
 * <li>a basic type is keyed by the option name;</li>
 * <li>type aliases use the key of their underlying type, retaining the alias option name for basic types;</li>
 * <li>a nested choice contributes its leaves; its own FQN is included only when the option has
 * metadata, because Java writes that option's metadata/reference wrapper with its own type.</li>
 * </ul>
 */
public class PythonChoiceDispatchProcessor {

    /** Builds the choice type, with its own and nested options. */
    @Inject
    private RObjectFactory rObjectFactory;

    /** Strips type aliases from option types. */
    @Inject
    private TypeSystem typeSystem;

    /** Provides the {@code nothing} type, which options may have and the table skips. */
    @Inject
    private RBuiltinTypeService builtins;

    /**
     * Writes the {@code _CHOICE_DISPATCH} class attribute of a choice.
     *
     * @param writer the writer for the class body
     * @param choice the choice the class is generated from
     * @throws IllegalStateException if two options of the choice lead to the same {@code @type}
     */
    public void generateChoiceDispatch(PythonCodeWriter writer, Choice choice) {
        Map<String, String> dispatch = buildDispatch(choice);
        writer.appendLine("_CHOICE_DISPATCH: ClassVar[dict[str, str]] = {");
        writer.indent();
        dispatch.forEach((key, attribute) -> writer.appendLine(String.format("\"%s\": \"%s\",", key, attribute)));
        writer.unindent();
        writer.appendLine("}");
    }

    private Map<String, String> buildDispatch(Choice choice) {
        RChoiceType choiceType = rObjectFactory.buildRChoiceType(choice);
        // Sorted, so the generated table is stable
        Map<String, String> attributeByKey = new TreeMap<>();
        Map<String, String> optionByKey = new TreeMap<>();
        for (RChoiceOption option : choiceType.getOwnOptions()) {
            String optionName = option.getEObject().getName();
            String attribute = RuneToPythonMapper.mangleName(optionName);
            Set<String> keys = new TreeSet<>();
            collectOptionKeys(option, keys);
            for (String key : keys) {
                String owner = optionByKey.putIfAbsent(key, optionName);
                if (owner != null && !owner.equals(optionName)) {
                    throw new IllegalStateException(String.format(
                            "Choice %s: '%s' is reached through both '%s' and '%s', so its option can't be "
                                    + "told from @type",
                            fullyQualifiedName(choiceType), key, owner, optionName));
                }
                attributeByKey.put(key, attribute);
            }
        }
        return attributeByKey;
    }

    private void collectOptionKeys(RChoiceOption option, Set<String> keys) {
        RType optionType = typeSystem.stripFromTypeAliases(option.getType().getRType());
        if (builtins.NOTHING.equals(optionType)) {
            return;
        }
        if (optionType instanceof RChoiceType nested) {
            // A choice cannot own type-level metadata. Attribute metadata belongs to
            // the option's wrapper, whose @type still identifies the nested choice.
            if (option.getType().hasAttributeMeta()) {
                keys.add(fullyQualifiedName(nested));
            }
            for (RChoiceOption nestedOption : nested.getOwnOptions()) {
                collectOptionKeys(nestedOption, keys);
            }
        } else {
            keys.add(key(optionType, option.getEObject().getName()));
        }
    }

    private static String key(RType optionType, String optionName) {
        return optionType instanceof RDataType || optionType instanceof REnumType
                ? fullyQualifiedName(optionType)
                : optionName;
    }

    private static String fullyQualifiedName(RType type) {
        return type.getNamespace().toString() + "." + type.getName();
    }
}
