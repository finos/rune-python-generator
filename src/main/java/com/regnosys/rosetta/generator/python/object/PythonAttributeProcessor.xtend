package com.regnosys.rosetta.generator.python.object

import com.google.inject.Inject
import com.regnosys.rosetta.generator.python.util.RuneToPythonMapper
import com.regnosys.rosetta.rosetta.simple.Data
import com.regnosys.rosetta.types.RType
import com.regnosys.rosetta.types.RAliasType
import com.regnosys.rosetta.types.RAttribute
import com.regnosys.rosetta.types.RObjectFactory
import com.regnosys.rosetta.types.TypeSystem
import com.regnosys.rosetta.types.builtin.RNumberType
import com.regnosys.rosetta.types.builtin.RStringType
import java.util.ArrayList
import java.util.HashMap
import java.util.HashSet
import org.eclipse.xtend2.lib.StringConcatenation
import java.util.Map
import com.regnosys.rosetta.types.REnumType
import java.util.List

/*
 * Generate Python for Rune Attributes
 */

class PythonAttributeProcessor {

    @Inject extension RObjectFactory
    @Inject TypeSystem typeSystem;

    def CharSequence generateAllAttributes(Data rosettaClass, Map<String, String> metaDataItems, Map<String, List<String>> keyRefConstraints) {
        // generate Python for all the attributes in this class
        val allAttributes = rosettaClass.buildRDataType.getOwnAttributes
        // it is an empty class if there are no attribute and no conditions
        if (allAttributes.size() === 0 && rosettaClass.conditions.size() === 0) {
            return "pass";
        }
        // add each attribute
        var _builder = new StringConcatenation();
        var firstElement = true;
        for (RAttribute ra : allAttributes) {
            if (firstElement) {
                firstElement = false;
            } else {
                _builder.appendImmediate("", "");
            }
            _builder.append(generateAttribute(rosettaClass, ra, metaDataItems, keyRefConstraints));
        }
        return _builder.toString();
    }
    def generateAttribute(Data rosettaClass, RAttribute ra, Map<String, String> metaDataItems, Map<String, List<String>> keyRefConstraints) {
        /*
         * generate Python representation of attribute
         */
        var rt = ra.getRMetaAnnotatedType().getRType();
        var attrTypeName = null as String;
        // strip out the alias if there is one and align the attribute type name to the to the underlying type
        if (rt instanceof RAliasType) {
            rt = typeSystem.stripFromTypeAliases(rt);
        }
        attrTypeName = RuneToPythonMapper::toPythonType(rt); // alias must be of underlying type number or string
        if (rt instanceof RNumberType && (rt as RNumberType).isInteger() && !attrTypeName.equals("int")) {
            attrTypeName = "int"
        }
        // an empty attribute name is cause for an exception
        if (attrTypeName === null) {
            throw new Exception("Attribute type is null for " + ra.name + " in class " + rosettaClass.name);
        }
        // get the properties / parameters if there are any (applies to string and number)
        val attrProp = processProperties(rt)
        val cardinalityMap = processCardinality(ra)
        // process meta data
        val validators = processMetaDataAttributes(ra, attrTypeName, metaDataItems, keyRefConstraints)
        return createAttributeString (
            ra, 
            attrTypeName, 
            rt,
            metaDataItems, 
            validators, 
            attrProp, 
            cardinalityMap
        )
    }
    private def String createAttributeString (
        RAttribute ra, 
        String attrTypeNameIn, 
        RType rt, 
        Map<String, String> metaDataItems,
        ArrayList<String> validators, 
        HashMap<String, String> attrProp, 
        HashMap<String, String> cardinalityMap
        ) {
        val propString = createPropString (attrProp)
        val isRosettaBasicType = RuneToPythonMapper::isRosettaBasicType(rt);
        var attrName = RuneToPythonMapper.mangleName(ra.getName()) // mangle the attribute name if it is a Python keyword
        var metaPrefix = "";
        var metaSuffix = "";
        val attrTypeName = (!validators.isEmpty() && !metaDataItems.containsKey(attrTypeNameIn)) ? RuneToPythonMapper.getAttributeTypeWithMeta(attrTypeNameIn) : attrTypeNameIn;
        val attrTypeNameOut = (isRosettaBasicType || rt instanceof REnumType) ? attrTypeName : attrTypeName.replace('.', '_')
        if (!validators.isEmpty()) {
            metaPrefix = getMetaDataPrefix(validators)
            metaSuffix = getMetaDataSuffix(validators, attrTypeNameOut)
        } 
        else if (!isRosettaBasicType && !(rt instanceof REnumType)) {
            // all complex types need a serializer and a validator
            metaPrefix = "Annotated[";
            metaSuffix = ", " + attrTypeNameOut + ".serializer(), " + attrTypeNameOut + ".validator()]";
        }
        val attrDesc = (ra.definition === null) ? '' : ra.definition.replaceAll('\\s+', ' ').replace("'", "\\'");
        var _builder = new StringConcatenation();
        _builder.append(attrName);
        _builder.append(": ");
        // attribute string depends on whether there are props and whether there is cardinality

        if (!attrProp.isEmpty() && !cardinalityMap.isEmpty() && cardinalityMap.get("cardinalityString").length() > 0) {
            _builder.append(cardinalityMap.get("cardinalityPrefix"));
            _builder.append("Annotated[")
            _builder.append(attrTypeNameOut);
            _builder.append(", Field(");
            _builder.append(propString);
            if (metaSuffix.length() != 0) {
                _builder.append(metaSuffix);
            } else {
                _builder.append(")]");
            }
            _builder.append(cardinalityMap.get("cardinalitySuffix"));
            _builder.append(" = Field(");
            _builder.append(cardinalityMap.get("fieldDefault"));
            _builder.append(", description='");
            _builder.append(attrDesc);
            _builder.append("'");
            _builder.append(cardinalityMap.get("cardinalityString"));
            _builder.append(")");
        } else {
            _builder.append(cardinalityMap.get("cardinalityPrefix"));
            _builder.append(metaPrefix);
            _builder.append(attrTypeNameOut);
            _builder.append(metaSuffix);
            _builder.append(cardinalityMap.get("cardinalitySuffix"));
            _builder.append(" = Field(");
            _builder.append(cardinalityMap.get("fieldDefault"));
            _builder.append(", description='");
            _builder.append(attrDesc);
            _builder.append("'");
            _builder.append(cardinalityMap.get("cardinalityString"));
            if (propString.length() > 0) {
                _builder.append(", ");
                _builder.append(propString);
            }
            _builder.append(")");
        }
        if (ra.definition !== null) {
            _builder.newLine();
            _builder.append("\"\"\"");
            _builder.newLine();
            _builder.append(ra.definition);
            _builder.newLine();
            _builder.append("\"\"\"");
        }
        _builder.newLine();
        return _builder.toString();
    }
    private def String getMetaDataSuffix(ArrayList<String> validators, String attrTypeName) {
        var metaSuffix = "";
        if (!validators.isEmpty()) {
            metaSuffix = ", " + attrTypeName + ".serializer(), " + attrTypeName + ".validator((";
            var isFirstValidator = true;
            var isOne = true;
            for (validator : validators) {
                if (isFirstValidator) {
                    isFirstValidator = false
                } else {
                    metaSuffix += ", ";
                    if (isOne) {
                        isOne = false;
                    }
                }
                metaSuffix += ("'" + validator + "'");
            }
            if (isOne) {
                metaSuffix += ", ";
            }
            metaSuffix += "))]"
        }
        return metaSuffix
    }
    private def String getMetaDataPrefix(ArrayList<String> validators) {
        return (validators.isEmpty()) ? "" : "Annotated["
    }
    private def ArrayList<String> processMetaDataAttributes(
        RAttribute ra, 
        String attrTypeName, 
        Map<String, String> metaDataItems, 
        Map<String, List<String>> keyRefConstraints
    ) {
        /*
         * Attribute metadata generation rules: attributes are annotated according to their metadata and that
         * of their containing Type.  Note that a Type can only have "key" related metadata
         * 1. If the containing Type has metadata, an attribute gets annotated with the metadata of its containing Type
         * 2. Attributes get annotated with their metadata and the metadata of its containing Type
         * 3. If the containing Type and the attribute have no metadata, there are no annotations
         */

        val attrRMAT = ra.getRMetaAnnotatedType
        val validators = new ArrayList<String>
        val otherMeta = new ArrayList<String>()

        // containing Type has metadata
        if (metaDataItems.containsKey(attrTypeName)) {
            validators.add("@key")
            validators.add("@key:external")
        }

        // process attribute metadata
        if (attrRMAT.hasMeta) {
            attrRMAT.getMetaAttributes.forEach [ma |
                switch(ma.getName) {
                    case "key", case "id": {
                        validators.add("@key")
                        validators.add("@key:external")
                    }
                    case "reference": {
                        validators.add("@ref")
                        validators.add("@ref:external")
                    }
                    case "scheme": {
                        otherMeta.add("@scheme")
                    }
                    case "location": {
                        validators.add("@key:scoped")
                    }
                    case "address": {
                        validators.add("@ref:scoped")
                    }
                    default: {
                        println("---- unprocessed meta ... name: " + ma.name)
                    }
                }
            ]
        }
        // update the Type's key / ref constraints
        if (!validators.isEmpty) {
            keyRefConstraints.put(ra.name, validators)
        }
        validators.addAll(otherMeta);
        return validators;
    }

    private def HashMap<String, String> processProperties(RType rt) {
        val attrProp = new HashMap<String, String>
        if (rt instanceof RStringType) {
            rt.pattern.ifPresent[value | attrProp.put("pattern", "r'^" + value + "*$'")]
            rt.interval.min.ifPresent[value | if (value > 0) attrProp.put("min_length", value.toString)]
            rt.interval.max.ifPresent[value | attrProp.put("max_length", value.toString)]
        } else if (rt instanceof RNumberType) {
            if (!rt.isInteger) {
                rt.digits.ifPresent[value | attrProp.put("max_digits", value.toString)]
                rt.fractionalDigits.ifPresent[value | attrProp.put("decimal_places", value.toString)]
                rt.interval.min.ifPresent[value | attrProp.put("ge", value.toPlainString)]
                rt.interval.max.ifPresent[value | attrProp.put("le", value.toPlainString)]
            }
        }
        return attrProp
    }
    private def String createPropString(Map<String, String> attrProp) {
        var propString = "";
        var isFirst = true;
        for (attrPropEntry : attrProp.entrySet()) {
            if (isFirst) {
                isFirst = false;
            } else {
                propString += ", ";
            }
            propString += (attrPropEntry.key + "=" + attrPropEntry.value);
        }
        return propString;
//        return attrProp.entrySet.join(", ", "[", "]", [entry | entry.key + "=" + entry.value])
    }
    private def HashMap<String, String> processCardinality(RAttribute ra) {
        // process the cardinality of the attribute
        // ... it is a list if it is multi or the upper bound is greater than 1
        // ... it is optional if it is equal to 0
        // otherwise it is required
        // for a and b gt 1
        /*
         * numberTypes: list[Decimal] = Field(..., description='', min_length=1)
         *  attribute name: list[attribute type] = Field(<...|None>,
         *                                               description,
         *                                               [min_length=#],
         *                                               [max_length=#],
         *                                               [pattern="ssss"],
         *                                               [max_digits=#],
         *                                               [decimal_places=#]
         * list[Annotated[
         *     NumberWithMeta,
         *     NumberWithMeta.serializer(),
         *     NumberWithMeta.validator(('@ref', )),
         *     Field(decimal_places=2, max_digits=6)]] = Field(
         *     	    ...,
         *     	    description='',
         *          min_length=1)
         *
         * Optional always calls for None
         * for a list the argument is ... if upper bound is not 0 and None if it is
         * when the item is not optional and not a list use "..." (ellipsis)
         */
        val cardinalityMap = new HashMap<String, String>
        val cardinality = ra.getCardinality
        val lowerBound = cardinality.getMin
        val upperCardinality = cardinality.getMax
        val upperBoundIsGTOne = (upperCardinality.isPresent && upperCardinality.get > 1)

        var fieldDefault = ""
        var cardinalityPrefix = ""
        var cardinalitySuffix = ""
        var cardinalityString = ""

        switch lowerBound {
            case 0: {
                // 0..1 --> Optional but not a list
                // 0..* --> Optional[list] with no min_length, no max_length
                // 0..n --> Optional[list] and include max_length=n in Field
                cardinalityPrefix = "Optional["
                cardinalitySuffix = "]"
                fieldDefault = "None"
                if (cardinality.isMulti || upperBoundIsGTOne) {
                    cardinalityPrefix += "list["
                    cardinalitySuffix += "]"
                    if (upperBoundIsGTOne) {
                        cardinalityString = ", max_length=" + upperCardinality.get.toString
                    }
                }
            }
            case 1: {
                // 1..1 --> not optional, no list, no min_length, no max_length
                // 1..n --> list[min_length=1, max_length=n]
                // 1..* --> list[min_length=1]
                fieldDefault = "..."
                if (cardinality.isMulti || upperBoundIsGTOne) {
                    cardinalityPrefix = "list["
                    cardinalitySuffix = "]"
                    cardinalityString = ", min_length=1"
                    if (upperBoundIsGTOne) {
                        cardinalityString += ", max_length=" + upperCardinality.get.toString
                    }
                }
            }
            default: {
                // a..a --> list[min_length=a, max_length=a]
                // a..b --> list[min_length=a, max_length=b]
                // a..* --> list[min_length=a]
                cardinalityPrefix = "list["
                cardinalitySuffix = "]"
                cardinalityString = ", min_length=" + lowerBound.toString
                fieldDefault = "..."
                if (upperCardinality.isPresent) {
                    var upperBound = upperCardinality.get
                    if (upperBound > 1) {
                        cardinalityString += ", max_length=" + upperBound.toString
                    }
                }
            }
        }
        cardinalityMap.put("cardinalityPrefix", cardinalityPrefix)
        cardinalityMap.put("cardinalitySuffix", cardinalitySuffix)
        cardinalityMap.put("cardinalityString", cardinalityString)
        cardinalityMap.put("fieldDefault", fieldDefault)
        return cardinalityMap
    }

    def getImportsFromAttributes(Data rosettaClass) {
        // imports only needed for Enums since all the code is now in _bundle.py
        val allAttributes = rosettaClass.buildRDataType.getOwnAttributes.filter [
            (it.name !== "reference") && (it.name !== "meta") && (it.name !== "scheme")
        ].filter[!RuneToPythonMapper::isRosettaBasicType(it)]

        val imports = new HashSet<String>()
        for (attribute : allAttributes) {
            var rt = attribute.getRMetaAnnotatedType.getRType
            // get all non-Meta attributes
            if (rt instanceof RAliasType) {
                rt = typeSystem.stripFromTypeAliases(rt);
            }
            if (rt === null) {
                throw new Exception("Attribute type is null for " + attribute.name + " for class " + rosettaClass.name)
            }
            if (rt instanceof REnumType) {
                imports.add ('''import «rt.getQualifiedName»''')
            }
        }
        return imports;
    }
}
