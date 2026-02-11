package com.regnosys.rosetta.generator.python.object;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.regnosys.rosetta.generator.python.util.PythonCodeWriter;
import com.regnosys.rosetta.types.RAttribute;
import com.regnosys.rosetta.types.RChoiceType;
import com.regnosys.rosetta.types.RDataType;
import com.regnosys.rosetta.types.RType;
import com.regnosys.rosetta.utils.DeepFeatureCallUtil;

import jakarta.inject.Inject;

/**
 * Generate Python from Rune Choice Aliases
 */
public class PythonChoiceAliasProcessor {

    /**
     * The deep feature call utility.
     */
    @Inject
    private DeepFeatureCallUtil deepFeatureCallUtil;

    /**
     * Generates the _CHOICE_ALIAS_MAP as a string for inclusion in class
     * definitions.
     * 
     * @param choiceType The data type to process.
     * @return The formatted string or an empty string if no aliases.
     */
    public String generateChoiceAliasesAsString(RDataType choiceType) {
        PythonCodeWriter writer = new PythonCodeWriter();
        generateChoiceAliases(writer, choiceType);
        return writer.toString().trim();
    }

    /**
     * Generates and writes the _CHOICE_ALIAS_MAP directly to the writer.
     * 
     * @param writer     The writer to write to.
     * @param choiceType The data type to process.
     */
    public void generateChoiceAliases(PythonCodeWriter writer, RDataType choiceType) {
        Map<String, List<String>> choiceAliases = calculateChoiceAliases(choiceType);
        if (choiceAliases == null || choiceAliases.isEmpty()) {
            return;
        }

        writer.appendLine("_CHOICE_ALIAS_MAP ={" + toString(choiceAliases) + "}");
    }

    private Map<String, List<String>> calculateChoiceAliases(RDataType choiceType) {
        // generate aliases for choice types (needed for deep path)
        if (!deepFeatureCallUtil.isEligibleForDeepFeatureCall(choiceType)) {
            return null;
        }

        Map<String, List<String>> deepReferenceMap = new HashMap<>();
        Collection<RAttribute> deepFeatures = deepFeatureCallUtil.findDeepFeatures(choiceType);

        for (RAttribute ra : choiceType.getAllAttributes()) {
            RType rt = ra.getRMetaAnnotatedType().getRType();
            RDataType t = getRDataType(rt);

            if (t != null) {
                Map<String, RAttribute> tDeepFeatures = deepFeatureCallUtil.findDeepFeatureMap(t);
                for (RAttribute df : deepFeatures) {
                    if (tDeepFeatures.containsKey(df.getName())) {
                        deepReferenceMap.computeIfAbsent(t.getName(), k -> new ArrayList<>()).add(df.getName());
                    }
                }
            }
        }

        Map<String, List<String>> choiceAliasMap = new HashMap<>();
        for (RAttribute deepFeature : deepFeatures) {
            String dfName = deepFeature.getName();
            List<String> aliasList = new ArrayList<>();

            for (RAttribute attribute : choiceType.getAllAttributes()) {
                RType rt = attribute.getRMetaAnnotatedType().getRType();
                RDataType t = getRDataType(rt);

                if (t != null) {
                    List<String> deepRefs = deepReferenceMap.get(t.getName());
                    String resolutionMethod = (deepRefs != null && deepRefs.contains(dfName))
                            ? "rune_resolve_deep_attr"
                            : "rune_resolve_attr";
                    aliasList.add("(\"" + attribute.getName() + "\", " + resolutionMethod + ")");
                }
            }

            choiceAliasMap.put("\"" + dfName + "\"", aliasList);
        }

        return choiceAliasMap.isEmpty() ? null : choiceAliasMap;
    }

    @SuppressWarnings("deprecation")
    private RDataType getRDataType(RType rt) {
        if (rt instanceof RChoiceType choice) {
            return choice.asRDataType();
        }
        if (rt instanceof RDataType dt) {
            return dt;
        }
        return null;
    }

    private String toString(Map<String, List<String>> choiceAliases) {
        if (choiceAliases == null) {
            return "";
        }
        return choiceAliases.entrySet().stream()
                .map(e -> e.getKey() + ":[" + String.join(", ", e.getValue()) + "]")
                .collect(Collectors.joining(","));
    }
}
