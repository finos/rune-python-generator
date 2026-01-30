package com.regnosys.rosetta.generator.python.object;

import com.regnosys.rosetta.generator.python.util.PythonCodeWriter;
import com.regnosys.rosetta.generator.python.util.RuneToPythonMapper;
import com.regnosys.rosetta.rosetta.simple.Data;
import com.regnosys.rosetta.types.*;
import com.regnosys.rosetta.types.builtin.RNumberType;
import com.regnosys.rosetta.types.builtin.RStringType;
import com.regnosys.rosetta.types.REnumType;
import jakarta.inject.Inject;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Generate Python for Rune Attributes
 */
public class PythonAttributeProcessor {

    @Inject
    private RObjectFactory rObjectFactory;

    @Inject
    private TypeSystem typeSystem;

    public String generateAllAttributes(Data rc, Map<String, List<String>> keyRefConstraints) {
        RDataType buildRDataType = rObjectFactory.buildRDataType(rc);
        Collection<RAttribute> allAttributes = buildRDataType.getOwnAttributes();

        if (allAttributes.isEmpty() && rc.getConditions().isEmpty()) {
            return "pass";
        }

        PythonCodeWriter writer = new PythonCodeWriter();
        for (RAttribute ra : allAttributes) {
            generateAttribute(writer, rc, ra, keyRefConstraints);
        }
        return writer.toString();
    }

    private void generateAttribute(PythonCodeWriter writer, Data rc, RAttribute ra,
            Map<String, List<String>> keyRefConstraints) {
        RType rt = ra.getRMetaAnnotatedType().getRType();

        if (rt instanceof RAliasType) {
            rt = typeSystem.stripFromTypeAliases(rt);
        }

        String attrTypeName = RuneToPythonMapper.toPythonType(rt);
        if (rt instanceof RNumberType numberType && numberType.isInteger() && !"int".equals(attrTypeName)) {
            attrTypeName = "int";
        }

        if (attrTypeName == null) {
            throw new RuntimeException(
                    "Attribute type is null for " + ra.getName() + " in class " + rc.getName());
        }

        Map<String, String> attrProp = processProperties(rt);
        Map<String, String> cardinalityMap = processCardinality(ra);
        ArrayList<String> validators = processMetaDataAttributes(ra, attrTypeName, keyRefConstraints);

        createAttributeString(writer, ra, attrTypeName, rt, validators, attrProp, cardinalityMap);
    }

    private void createAttributeString(
            PythonCodeWriter writer,
            RAttribute ra,
            String attrTypeNameIn,
            RType rt,
            ArrayList<String> validators,
            Map<String, String> attrProp,
            Map<String, String> cardinalityMap) {

        String propString = createPropString(attrProp);
        String attrName = RuneToPythonMapper.mangleName(ra.getName());

        String attrTypeName = (!validators.isEmpty())
                ? RuneToPythonMapper.getAttributeTypeWithMeta(attrTypeNameIn)
                : attrTypeNameIn;

        String attrTypeNameOut = RuneToPythonMapper.getFlattenedTypeName(rt, attrTypeName);

        String metaPrefix = "";
        String metaSuffix = "";

        if (!validators.isEmpty()) {
            metaPrefix = getMetaDataPrefix(validators);
            metaSuffix = getMetaDataSuffix(validators, attrTypeNameOut);
        } else if (!RuneToPythonMapper.isRosettaBasicType(rt) && !(rt instanceof REnumType)) {
            metaPrefix = "Annotated[";
            metaSuffix = ", " + attrTypeNameOut + ".serializer(), " + attrTypeNameOut + ".validator()]";
        }

        String baseType = metaPrefix + attrTypeNameOut + metaSuffix;
        RCardinality cardinality = ra.getCardinality();
        int min = cardinality.getMin();
        int max = cardinality.getMax().orElse(-1);
        boolean isList = (cardinality.isMulti() || max > 1);

        // If it is a list and we have properties (e.g. max_digits), these properties
        // belong to the element, not the list.
        // So we wrap the element type in Annotated[Type, Field(...properties...)].
        boolean propertiesAppliedToInnerType = false;
        if (isList && !attrProp.isEmpty()) {
            baseType = "Annotated[" + baseType + ", Field(" + propString + ")]";
            propertiesAppliedToInnerType = true;
        }

        String pythonType = RuneToPythonMapper.formatPythonType(baseType, min, max, false);

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
        writer.appendLine(lineBuilder.toString());

        if (ra.getDefinition() != null) {
            writer.appendLine("\"\"\"");
            writer.appendLine(ra.getDefinition());
            writer.appendLine("\"\"\"");
        }
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

    private ArrayList<String> processMetaDataAttributes(
            RAttribute ra,
            String attrTypeName,
            Map<String, List<String>> keyRefConstraints) {
        RMetaAnnotatedType attrRMAT = ra.getRMetaAnnotatedType();
        ArrayList<String> validators = new ArrayList<>();
        ArrayList<String> otherMeta = new ArrayList<>();

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
                    }
                    case "scheme" -> otherMeta.add("@scheme");
                    case "location" -> validators.add("@key:scoped");
                    case "address" -> validators.add("@ref:scoped");
                    default -> throw new IllegalStateException("Unsupported metadata attribute: " + ma.getName());
                }
            });
        }
        if (!validators.isEmpty()) {
            keyRefConstraints.put(ra.getName(), new ArrayList<>(validators));
        }
        validators.addAll(otherMeta);
        return validators;
    }

    private Map<String, String> processProperties(RType rt) {
        Map<String, String> attrProp = new HashMap<>();
        if (rt instanceof RStringType stringType) {
            stringType.getPattern().ifPresent(value -> attrProp.put("pattern", "r'^" + value + "*$'"));
            stringType.getInterval().getMin().ifPresent(value -> {
                if (value > 0)
                    attrProp.put("min_length", value.toString());
            });
            stringType.getInterval().getMax().ifPresent(value -> attrProp.put("max_length", value.toString()));
        } else if (rt instanceof RNumberType numberType) {
            if (!numberType.isInteger()) {
                numberType.getDigits().ifPresent(value -> attrProp.put("max_digits", value.toString()));
                numberType.getFractionalDigits().ifPresent(value -> attrProp.put("decimal_places", value.toString()));
                numberType.getInterval().getMin().ifPresent(value -> attrProp.put("ge", value.toPlainString()));
                numberType.getInterval().getMax().ifPresent(value -> attrProp.put("le", value.toPlainString()));
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
            if (!attr.getName().equals("reference") &&
                    !attr.getName().equals("meta") &&
                    !attr.getName().equals("scheme") &&
                    !RuneToPythonMapper.isRosettaBasicType(attr)) {
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
}
