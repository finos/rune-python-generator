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

    public String generateAllAttributes(Data rosettaClass, Map<String, List<String>> keyRefConstraints) {
        RDataType buildRDataType = rObjectFactory.buildRDataType(rosettaClass);
        Collection<RAttribute> allAttributes = buildRDataType.getOwnAttributes();

        if (allAttributes.isEmpty() && rosettaClass.getConditions().isEmpty()) {
            return "pass";
        }

        PythonCodeWriter writer = new PythonCodeWriter();
        for (RAttribute ra : allAttributes) {
            generateAttribute(writer, rosettaClass, ra, keyRefConstraints);
        }
        return writer.toString();
    }

    private void generateAttribute(PythonCodeWriter writer, Data rosettaClass, RAttribute ra,
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
                    "Attribute type is null for " + ra.getName() + " in class " + rosettaClass.getName());
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
        boolean isRosettaBasicType = RuneToPythonMapper.isRosettaBasicType(rt);
        String attrName = RuneToPythonMapper.mangleName(ra.getName());

        String metaPrefix = "";
        String metaSuffix = "";

        String attrTypeName = (!validators.isEmpty())
                ? RuneToPythonMapper.getAttributeTypeWithMeta(attrTypeNameIn)
                : attrTypeNameIn;

        String attrTypeNameOut = (isRosettaBasicType || rt instanceof REnumType)
                ? attrTypeName
                : attrTypeName.replace('.', '_');

        if (!validators.isEmpty()) {
            metaPrefix = getMetaDataPrefix(validators);
            metaSuffix = getMetaDataSuffix(validators, attrTypeNameOut);
        } else if (!isRosettaBasicType && !(rt instanceof REnumType)) {
            metaPrefix = "Annotated[";
            metaSuffix = ", " + attrTypeNameOut + ".serializer(), " + attrTypeNameOut + ".validator()]";
        }

        String attrDesc = (ra.getDefinition() == null)
                ? ""
                : ra.getDefinition().replaceAll("\\s+", " ").replace("'", "\\'");

        // Write the attribute line
        writer.appendIndent();
        writer.append(attrName);
        writer.append(": ");

        if (!attrProp.isEmpty() && !cardinalityMap.isEmpty() && cardinalityMap.get("cardinalityString").length() > 0) {
            writer.append(cardinalityMap.get("cardinalityPrefix"));
            writer.append("Annotated[");
            writer.append(attrTypeNameOut);
            writer.append(", Field(");
            writer.append(propString);
            if (!metaSuffix.isEmpty()) {
                writer.append(metaSuffix);
            } else {
                writer.append(")]");
            }
            writer.append(cardinalityMap.get("cardinalitySuffix"));
            writer.append(" = Field(");
            writer.append(cardinalityMap.get("fieldDefault"));
            writer.append(", description='");
            writer.append(attrDesc);
            writer.append("'");
            writer.append(cardinalityMap.get("cardinalityString"));
            writer.append(")\n");
        } else {
            writer.append(cardinalityMap.get("cardinalityPrefix"));
            writer.append(metaPrefix);
            writer.append(attrTypeNameOut);
            writer.append(metaSuffix);
            writer.append(cardinalityMap.get("cardinalitySuffix"));
            writer.append(" = Field(");
            writer.append(cardinalityMap.get("fieldDefault"));
            writer.append(", description='");
            writer.append(attrDesc);
            writer.append("'");
            writer.append(cardinalityMap.get("cardinalityString"));
            if (!propString.isEmpty()) {
                writer.append(", ");
                writer.append(propString);
            }
            writer.append(")\n");
        }

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
        String cardinalityPrefix = "";
        String cardinalitySuffix = "";
        String cardinalityString = "";

        switch (lowerBound) {
            case 0 -> {
                cardinalityPrefix = "Optional[";
                cardinalitySuffix = "]";
                fieldDefault = "None";
                if (cardinality.isMulti() || upperBoundIsGTOne) {
                    cardinalityPrefix += "list[";
                    cardinalitySuffix += "]";
                    if (upperBoundIsGTOne) {
                        cardinalityString = ", max_length=" + upperCardinality.get().toString();
                    }
                }
            }
            case 1 -> {
                fieldDefault = "...";
                if (cardinality.isMulti() || upperBoundIsGTOne) {
                    cardinalityPrefix = "list[";
                    cardinalitySuffix = "]";
                    cardinalityString = ", min_length=1";
                    if (upperBoundIsGTOne) {
                        cardinalityString += ", max_length=" + upperCardinality.get().toString();
                    }
                }
            }
            default -> {
                cardinalityPrefix = "list[";
                cardinalitySuffix = "]";
                cardinalityString = ", min_length=" + lowerBound;
                fieldDefault = "...";
                if (upperCardinality.isPresent()) {
                    int upperBound = upperCardinality.get();
                    if (upperBound > 1) {
                        cardinalityString += ", max_length=" + upperBound;
                    }
                }
            }
        }
        cardinalityMap.put("cardinalityPrefix", cardinalityPrefix);
        cardinalityMap.put("cardinalitySuffix", cardinalitySuffix);
        cardinalityMap.put("cardinalityString", cardinalityString);
        cardinalityMap.put("fieldDefault", fieldDefault);
        return cardinalityMap;
    }

    public Set<String> getImportsFromAttributes(Data rosettaClass) {
        RDataType buildRDataType = rObjectFactory.buildRDataType(rosettaClass);
        Collection<RAttribute> allAttributes = buildRDataType.getOwnAttributes();

        return allAttributes.stream()
                .filter(it -> !it.getName().equals("reference") && !it.getName().equals("meta")
                        && !it.getName().equals("scheme"))
                .filter(it -> !RuneToPythonMapper.isRosettaBasicType(it))
                .map(it -> {
                    RType rt = it.getRMetaAnnotatedType().getRType();
                    if (rt instanceof RAliasType) {
                        rt = typeSystem.stripFromTypeAliases(rt);
                    }
                    if (rt == null) {
                        throw new RuntimeException(
                                "Attribute type is null for " + it.getName() + " for class " + rosettaClass.getName());
                    }
                    return rt;
                })
                .filter(rt -> rt instanceof REnumType)
                .map(rt -> "import " + ((REnumType) rt).getQualifiedName())
                .collect(Collectors.toSet());
    }
}
