package com.regnosys.rosetta.generator.python.object

import com.google.inject.Inject
import com.regnosys.rosetta.types.RChoiceType
import com.regnosys.rosetta.types.RDataType
import com.regnosys.rosetta.utils.DeepFeatureCallUtil
import java.util.ArrayList
import java.util.HashMap
import java.util.Map

/*
 * Generate Python from Rune Choice Aliases
 */

class PythonChoiceAliasProcessor {
	
    @Inject extension DeepFeatureCallUtil

    def generateChoiceAliasesAsString (RDataType choiceType) {
        val choiceAliases = generateChoiceAliases(choiceType);
        return (choiceAliases === null) ? '' : '_CHOICE_ALIAS_MAP ={' + toString (choiceAliases) + '}';
    }

    private def Map<String, ArrayList<String>> generateChoiceAliases(RDataType choiceType) {
        // 
        // generate aliases for choice types (needed for deep path)
        //
        if (!choiceType.isEligibleForDeepFeatureCall()) {
            return null
        }
        val deepReferenceMap = new HashMap<String, ArrayList<String>>()
        val deepFeatures = choiceType.findDeepFeatures
        choiceType.getAllAttributes.toMap([it], [
            val attrType = it.getRMetaAnnotatedType.getRType
            deepFeatures.toMap([it], [
                var t = attrType
                if (t instanceof RChoiceType) {
                    t = t.asRDataType
                }
                if (t instanceof RDataType) {
                    if (t.findDeepFeatureMap.containsKey(it.name)) {
                        // look for element in hash map and create one if none found
                        var deepReference = deepReferenceMap.get(t.name)
                        if (deepReference === null) {
                            deepReference = new ArrayList<String>
                        }
                        // add the deep reference to the array and update the hash map
                        deepReference.add(it.name)
                        deepReferenceMap.put(t.name, deepReference)
                        return true
                    }
                }
                return false
            ])
        ])
        val choiceAlias = deepFeatures.toMap(
            [deepFeature|'"' + deepFeature.name + '"'], // Key extractor: use deepFeature name as the key
            [ deepFeature | // Value extractor: create and populate the list of aliases
                val aliasList = new ArrayList<String>()

                // Iterate over all non-overridden attributes
                choiceType.getAllAttributes.forEach [ attribute |
                    val attrType = attribute.getRMetaAnnotatedType.getRType
                    var t = attrType

                    // Convert RChoiceType to RDataType if applicable
                    if (t instanceof RChoiceType) {
                        t = t.asRDataType

                    }
                    // Check if t is an instance of RDataType
                    if (t instanceof RDataType) {
                        // Add the new alias to the list.  Add a deep reference if necessary
                        val deepReference = deepReferenceMap.get(t.name)
                        val resolutionMethod = (deepReference !== null && deepReference.contains(
                                deepFeature.name)) ? "rune_resolve_deep_attr" : "rune_resolve_attr"
                        aliasList.add('("' + attribute.name + '", ' + resolutionMethod + ')')
                    }
                ]
                // Return the populated list for this deepFeature
                aliasList
            ]
        )
        return (choiceAlias.isEmpty()) ? null : choiceAlias
    }
    private def toString(Map<String, ArrayList<String>> choiceAliases) {
        return (choiceAliases === null) ? '' : choiceAliases.entrySet.map[e|e.key + ":" + e.value.toString].join(",")
    }

}
