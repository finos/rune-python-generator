package com.regnosys.rosetta.generator.python.object;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.regnosys.rosetta.generator.python.util.PythonCodeWriter;
import com.regnosys.rosetta.generator.python.util.RuneToPythonMapper;
import com.regnosys.rosetta.rosetta.simple.Data;
import com.regnosys.rosetta.types.RAliasType;
import com.regnosys.rosetta.types.RAttribute;
import com.regnosys.rosetta.types.RCardinality;
import com.regnosys.rosetta.types.RDataType;
import com.regnosys.rosetta.types.REnumType;
import com.regnosys.rosetta.types.RMetaAnnotatedType;
import com.regnosys.rosetta.types.RObjectFactory;
import com.regnosys.rosetta.types.RType;
import com.regnosys.rosetta.types.TypeSystem;
import com.regnosys.rosetta.types.builtin.RNumberType;
import com.regnosys.rosetta.types.builtin.RStringType;

import jakarta.inject.Inject;

/**
 * Generate Python for Rune Attributes
 */
public final class PythonAttributeProcessor {

    /**
     * The R object factory.
     */
    @Inject
    private RObjectFactory rObjectFactory;

    /**
     * The type system.
     */
    @Inject
    private TypeSystem typeSystem;

    /**
     * Generate Python code for all attributes of a given Data type.
     * 
     * @param rc                The Data type to generate attributes for.
     * @param keyRefConstraints A map of key reference constraints.
     * @return An AttributeProcessingResult containing the generated Python code and
     *         a list of annotation updates.
     */
    public AttributeProcessingResult generateAllAttributes(Data rc, Map<String, List<String>> keyRefConstraints) {
        RDataType buildRDataType = rObjectFactory.buildRDataType(rc);
        Collection<RAttribute> allAttributes = buildRDataType.getOwnAttributes();

        if (allAttributes.isEmpty() && rc.getConditions().isEmpty()) {
            return new AttributeProcessingResult("pass", Collections.emptyList());
        }

        PythonCodeWriter writer = new PythonCodeWriter();
        List<String> annotationUpdates = new ArrayList<>();
        for (RAttribute ra : allAttributes) {
            writer.appendBlock(generateAttribute(rc, ra, keyRefConstraints, annotationUpdates));
        }
        return new AttributeProcessingResult(writer.toString(), annotationUpdates);
    }

    private String generateAttribute(Data rc, RAttribute ra,
            Map<String, List<String>> keyRefConstraints, List<String> annotationUpdates) {
        RType rt = ra.getRMetaAnnotatedType().getRType();

        if (rt instanceof RAliasType) {
            rt = typeSystem.stripFromTypeAliases(rt);
        }

        String attrTypeName = RuneToPythonMapper.toPythonType(rt);
        if (attrTypeName == null) {
            throw new RuntimeException(
                    "Attribute type is null for " + ra.getName() + " in class " + rc.getName());
        }

        Map<String, String> attrProp = processProperties(rt);
        Map<String, String> cardinalityMap = processCardinality(ra);
        MetaDataResult metaDataResult = processMetaDataAttributes(ra, attrTypeName, keyRefConstraints);

        return createAttributeString(ra, attrTypeName, rt, metaDataResult, attrProp, cardinalityMap, annotationUpdates);
    }

    private String createAttributeString(
            RAttribute ra,
            String attrTypeNameIn,
            RType rt,
            MetaDataResult metaDataResult,
            Map<String, String> attrProp,
            Map<String, String> cardinalityMap,
            List<String> annotationUpdates) {

        ArrayList<String> validators = metaDataResult.getValidators();
        String propString = createPropString(attrProp);
        String attrName = RuneToPythonMapper.mangleName(ra.getName());
        RCardinality cardinality = ra.getCardinality();
        int min = cardinality.getMin();
        int max = cardinality.getMax().orElse(-1);
        boolean isList = (cardinality.isMulti() || max > 1);

        String attrTypeName = (!validators.isEmpty())
                ? RuneToPythonMapper.getAttributeTypeWithMeta(attrTypeNameIn)
                : attrTypeNameIn;

        String attrTypeNameOut = RuneToPythonMapper.getFlattenedTypeName(rt, attrTypeName);
        String typeHint = attrTypeNameOut;
        if (metaDataResult.hasReference()) {
            typeHint += " | BaseReference";
        }

        boolean isDelayed = !RuneToPythonMapper.isRosettaBasicType(rt) && !(rt instanceof REnumType);

        String metaPrefix = "";
        String metaSuffix = "";

        if (!validators.isEmpty()) {
            metaPrefix = getMetaDataPrefix(validators);
            metaSuffix = getMetaDataSuffix(validators, attrTypeNameOut);
        } else if (isDelayed) {
            metaPrefix = "Annotated[";
            metaSuffix = ", " + attrTypeNameOut + ".serializer(), " + attrTypeNameOut + ".validator()]";
        }

        String fullBaseType = metaPrefix + typeHint + metaSuffix;
        boolean propertiesAppliedToInnerType = false;
        if (isList && !attrProp.isEmpty()) {
            fullBaseType = "Annotated[" + fullBaseType + ", Field(" + propString + ")]";
            propertiesAppliedToInnerType = true;
        }

        String pythonType;
        if (isDelayed) {
            // Phase 2 update statement
            String fullType = RuneToPythonMapper.formatPythonType(fullBaseType, min, max, false);
            annotationUpdates.add("__annotations__[\"" + attrName + "\"] = " + fullType);

            // Phase 1 (Clean) type
            pythonType = RuneToPythonMapper.formatPythonType(typeHint, min, max, false);
        } else {
            pythonType = RuneToPythonMapper.formatPythonType(fullBaseType, min, max, false);
        }

        String attrDesc = (ra.getDefinition() == null)
                ? ""
                : ra.getDefinition().replaceAll("\\s+", " ").replace("'", "\\'");

        StringBuilder lineBuilder = new StringBuilder();
        lineBuilder.append(attrName).append(": ");
        lineBuilder.append(pythonType);

        lineBuilder.append(" = Field(");
        lineBuilder.append(cardinalityMap.get("fieldDefault"));
        lineBuilder.append(", description='");
        lineBuilder.append(attrDesc);
        lineBuilder.append("'");
        lineBuilder.append(cardinalityMap.get("cardinalityString"));

        // Only append propString to the outer Field if it hasn't been applied to the
        // inner type
        if (!propString.isEmpty() && !propertiesAppliedToInnerType) {
            lineBuilder.append(", ");
            lineBuilder.append(propString);
        }

        lineBuilder.append(")");

        if (ra.getDefinition() != null) {
            lineBuilder.append("\n\"\"\"\n");
            lineBuilder.append(ra.getDefinition());
            lineBuilder.append("\n\"\"\"");
        }
        return lineBuilder.toString();
    }

    private String getMetaDataSuffix(ArrayList<String> validators, String attrTypeName) {
        if (validators.isEmpty()) {
            return "";
        }
        String joinedValidators = validators.stream()
                .map(v -> "'" + v + "'")
                .collect(Collectors.joining(", "));

        String trailingComma = (validators.size() == 1) ? ", " : "";

        return ", " + attrTypeName + ".serializer(), " + attrTypeName + ".validator((" + joinedValidators
                + trailingComma + "))]";
    }

    private String getMetaDataPrefix(ArrayList<String> validators) {
        return (validators.isEmpty()) ? "" : "Annotated[";
    }

    /**
     * Processes the meta-data attributes of a given attribute.
     * 
     * @param ra                The attribute to process.
     * @param attrTypeName      The name of the attribute type.
     * @param keyRefConstraints A map of key reference constraints.
     * @return A MetaDataResult containing the validators and other meta-data.
     */
    private MetaDataResult processMetaDataAttributes(
            RAttribute ra,
            String attrTypeName,
            Map<String, List<String>> keyRefConstraints) {
        ArrayList<String> validators = new ArrayList<>();
        ArrayList<String> otherMeta = new ArrayList<>();
        final boolean[] hasReference = new boolean[] { false };

        RMetaAnnotatedType attrRMAT = ra.getRMetaAnnotatedType();
        if (attrRMAT.hasAttributeMeta()) {
            attrRMAT.getMetaAttributes().forEach(ma -> {
                switch (ma.getName()) {
                    case "key", "id" -> {
                        validators.add("@key");
                        validators.add("@key:external");
                    }
                    case "reference" -> {
                        validators.add("@ref");
                        validators.add("@ref:external");
                        hasReference[0] = true;
                    }
                    case "scheme" -> otherMeta.add("@scheme");
                    case "location" -> validators.add("@key:scoped");
                    case "address" -> {
                        validators.add("@ref:scoped");
                        hasReference[0] = true;
                    }
                    default -> throw new IllegalStateException("Unsupported metadata attribute: " + ma.getName());
                }
            });
        }
        if (!validators.isEmpty()) {
            keyRefConstraints.put(ra.getName(), new ArrayList<>(validators));
        }
        validators.addAll(otherMeta);
        return new MetaDataResult(validators, hasReference[0]);
    }

    private Map<String, String> processProperties(RType rt) {
        Map<String, String> attrProp = new HashMap<>();
        if (rt instanceof RStringType stringType) {
            stringType.getPattern().ifPresent(value -> attrProp.put("pattern", "r'^" + value + "*$'"));
            stringType.getInterval().getMin().ifPresent(value -> {
                if (value > 0) {
                    attrProp.put("min_length", value.toString());
                }
            });
            stringType.getInterval().getMax().ifPresent(value -> attrProp.put("max_length", value.toString()));
        } else if (rt instanceof RNumberType numberType) {
            if (!numberType.isInteger()) {
                numberType.getDigits().ifPresent(value -> attrProp.put("max_digits", value.toString()));
                numberType.getFractionalDigits().ifPresent(value -> attrProp.put("decimal_places", value.toString()));
                numberType.getInterval().getMin()
                        .ifPresent(value -> attrProp.put("ge", "Decimal('" + value.toPlainString() + "')"));
                numberType.getInterval().getMax()
                        .ifPresent(value -> attrProp.put("le", "Decimal('" + value.toPlainString() + "')"));
            }
        }
        return attrProp;
    }

    private String createPropString(Map<String, String> attrProp) {
        return attrProp.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(", "));
    }

    private Map<String, String> processCardinality(RAttribute ra) {
        Map<String, String> cardinalityMap = new HashMap<>();
        RCardinality cardinality = ra.getCardinality();
        int lowerBound = cardinality.getMin();
        Optional<Integer> upperCardinality = cardinality.getMax();
        boolean upperBoundIsGTOne = (upperCardinality.isPresent() && upperCardinality.get() > 1);

        String fieldDefault = "";
        String cardinalityString = "";

        // Default constraints
        if (lowerBound == 0) {
            fieldDefault = "None";
            if (cardinality.isMulti() || upperBoundIsGTOne) {
                if (upperBoundIsGTOne) {
                    cardinalityString = ", max_length=" + upperCardinality.get().toString();
                }
            }
        } else if (lowerBound == 1) {
            fieldDefault = "...";
            if (cardinality.isMulti() || upperBoundIsGTOne) {
                cardinalityString = ", min_length=1";
                if (upperBoundIsGTOne) {
                    cardinalityString += ", max_length=" + upperCardinality.get().toString();
                }
            }
        } else {
            fieldDefault = "...";
            cardinalityString = ", min_length=" + lowerBound;
            if (upperCardinality.isPresent()) {
                int upperBound = upperCardinality.get();
                if (upperBound > 1) {
                    cardinalityString += ", max_length=" + upperBound;
                }
            }
        }

        cardinalityMap.put("cardinalityString", cardinalityString);
        cardinalityMap.put("fieldDefault", fieldDefault);
        return cardinalityMap;
    }

    public void getImportsFromAttributes(Data rc, Set<String> enumImports) {
        /**
         * Get ENUM imports from attributes (all other dependencies are handled by the
         * bundle)
         * 
         * @param rc
         * @param enumImports
         */
        RDataType buildRDataType = rObjectFactory.buildRDataType(rc);
        Collection<RAttribute> allAttributes = buildRDataType.getOwnAttributes();

        for (RAttribute attr : allAttributes) {
            if (!attr.getName().equals("reference")
                    && !attr.getName().equals("meta")
                    && !attr.getName().equals("scheme")
                    && !RuneToPythonMapper.isRosettaBasicType(attr)) {
                RType rt = attr.getRMetaAnnotatedType().getRType();
                if (rt instanceof RAliasType) {
                    rt = typeSystem.stripFromTypeAliases(rt);
                }
                if (rt == null) {
                    throw new RuntimeException(
                            "Attribute type is null for " + attr.getName() + " for class " + rc.getName());
                }

                if (rt instanceof REnumType) {
                    enumImports.add("import " + ((REnumType) rt).getQualifiedName());
                }
            }
        }
    }

    private static class MetaDataResult {
        /**
         * The validators for the attribute.
         */
        private final ArrayList<String> validators;
        /**
         * Whether the attribute has a reference.
         */
        private final boolean hasReference;

        /**
         * Creates a new MetaDataResult.
         * 
         * @param validators   The validators for the attribute.
         * @param hasReference Whether the attribute has a reference.
         */
        MetaDataResult(ArrayList<String> validators, boolean hasReference) {
            this.validators = validators;
            this.hasReference = hasReference;
        }

        public ArrayList<String> getValidators() {
            return validators;
        }

        public boolean hasReference() {
            return hasReference;
        }
    }
}
