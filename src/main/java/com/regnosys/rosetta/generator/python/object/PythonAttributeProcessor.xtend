package com.regnosys.rosetta.generator.python.object

import com.google.inject.Inject
import com.regnosys.rosetta.generator.python.util.PythonTranslator
import com.regnosys.rosetta.rosetta.simple.Data
import com.regnosys.rosetta.types.RAliasType
import com.regnosys.rosetta.types.RAttribute
import com.regnosys.rosetta.types.RObjectFactory
import com.regnosys.rosetta.types.TypeSystem
import com.regnosys.rosetta.types.builtin.RNumberType
import com.regnosys.rosetta.types.builtin.RStringType
import java.util.ArrayList
import java.util.HashMap
import org.eclipse.xtend2.lib.StringConcatenation
import java.util.Map
import com.regnosys.rosetta.types.REnumType
import java.util.List

/*
 * Generate Python from Rune Attributes
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
         * translate the attribute to its representation in Python
         */
        val attrRMAT = ra.getRMetaAnnotatedType();
        var attrRType = attrRMAT.getRType();
        // TODO: confirm refactoring of type properly handles enums
        var attrTypeName = null as String;
        // strip out the alias if there is one and align the attribute type name to the to the underlying type
        var isRosettaBasicType = false;
        if (attrRType instanceof RAliasType) {
            attrRType = typeSystem.stripFromTypeAliases(attrRType);
            attrTypeName = PythonTranslator::toPythonType(attrRType); // alias must be of underlying type number or string
            isRosettaBasicType = true;
        } else {
            attrTypeName = PythonTranslator::toPythonType(ra);
            isRosettaBasicType = PythonTranslator::isRosettaBasicType (ra);
        }
        // an empty attribute name is cause for an exception
        if (attrTypeName === null) {
            throw new Exception("Attribute type is null for " + ra.name + " in class " + rosettaClass.name);
        }
        var attrName = PythonTranslator.mangleName(ra.getName()) // mangle the attribute name if it is a Python keyword
        val attrDesc = (ra.definition === null) ? '' : ra.definition.replaceAll('\\s+', ' ').replace("'", "\\'");
        // get the properties / parameters if there are any (applies to string and number)
        val attrProp = new HashMap<String, String>();
        if (attrRType instanceof RStringType) {
            // TODO: there seems to be a default for strings to have min_length = 0 
            attrRType.getPattern().ifPresent[value|attrProp.put("pattern", "r'^" + value.toString() + "*$'")];
            attrRType.getInterval().getMin().ifPresent [ value |
                if (value > 0) {
                    attrProp.put("min_length", value.toString())
                }
            ]
            attrRType.getInterval().getMax().ifPresent[value|attrProp.put("max_length", value.toString())]
        } else if (attrRType instanceof RNumberType) {
            // TODO: determine whether there's an issue with letting integers pass through this mechanism
            if (!attrRType.isInteger()) {
                attrRType.getDigits().ifPresent[value|attrProp.put("max_digits", value.toString())];
                attrRType.getFractionalDigits().ifPresent[value|attrProp.put("decimal_places", value.toString())];
                attrRType.getInterval().getMin().ifPresent[value|attrProp.put("ge", value.toPlainString())]
                attrRType.getInterval().getMax().ifPresent[value|attrProp.put("le", value.toPlainString())]
            } else {
                attrTypeName = 'int';
            }
        }
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
        // process the cardinality of the attribute 
        // ... it is a list if it is multi or the upper bound is greater than 1 
        // ... it is optional if it is equal to 0
        // otherwise it is required
        // for a and b gt 1
        var fieldDefault      = "";
        var cardinalityPrefix = "";
        var cardinalitySuffix = "";
        var cardinalityString = "";
        /*
         * numberTypes: list[Decimal] = Field([], description='', min_length=1)
         *  attribute name: list[attribute type] = Field([], 
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
         *     	    [],
         *     	    description='',
         *          min_length=1)
         *
         * Optional always calls for None
         * for a list the argument is []
         * when the item is not optional and not a list use "..." (ellipsis)
         */
        var lowerBound = ra.cardinality.getMin();
        if (lowerBound == 0) {
            // 0..1 --> Optional but not a list
            // 0..* --> Optional[list] with no min_length, no max_length
            // 0..n --> Optional[list] and include max_length=n in Field
            cardinalityPrefix = "Optional[";
            cardinalitySuffix = "]";
            fieldDefault = "None"
            var upperCardinality  = ra.cardinality.getMax();
            var upperBoundIsGTOne = (upperCardinality.isPresent() && upperCardinality.get() > 1);
            if (ra.cardinality.isMulti() || upperBoundIsGTOne) {
                cardinalityPrefix += "list["
                cardinalitySuffix += "]"
                if (upperBoundIsGTOne) {
                    cardinalityString = ", max_length=" + String.valueOf(upperCardinality.get());
                }
            }
        } else if (lowerBound == 1) {
            // 1..1 --> not optional, no list, no min_length, no max_length
            // 1..n --> list[min_length=1, max_length=n]
            // 1..* --> list[min_length=1]
            var upperCardinality  = ra.cardinality.getMax();
            var upperBoundIsGTOne = (upperCardinality.isPresent() && upperCardinality.get() > 1);
            if (ra.cardinality.isMulti() || upperBoundIsGTOne) {
                cardinalityPrefix = "list[";
                cardinalitySuffix = "]";
                cardinalityString = ", min_length=1"
                fieldDefault = "[]"
                if (upperBoundIsGTOne) {
                    var upperBound = upperCardinality.get();
                    if (upperBound > 1) {
                        cardinalityString += ", max_length=" + String.valueOf(upperBound);
                    }
                }
            } else {
                fieldDefault = "...";
            }
        } else {
            // a..a --> list[min_length=a, max_length=a]
            // a..b --> list[min_length=a, max_length=b]
            // a..* --> list[min_length=a]        
            cardinalityPrefix = "list["
            cardinalitySuffix = "]"
            cardinalityString = ", min_length=" + String.valueOf(lowerBound);
            fieldDefault = "[]"
            var upperCardinality = ra.cardinality.getMax();
            if (upperCardinality.isPresent()) {
                var upperBound = upperCardinality.get();
                if (upperBound > 1) {
                    cardinalityString += ", max_length=" + String.valueOf(upperBound);
                }
            }
        }
        // process meta data
        var keysRefs = new ArrayList<String>()
        var otherMeta = new ArrayList<String>()
        // if the attribute of a type that is metadata, add "@key"
        val attributeIsMetaKey = metaDataItems.containsKey(attrTypeName);
        if (attributeIsMetaKey) {
            keysRefs.add("@key");
            keysRefs.add("@key:external")
        }
        // check whether the attribute has meta 
        if (attrRMAT.hasMeta()) {
            for (ma : attrRMAT.getMetaAttributes()) {
                // TODO: ignoring address "pointsTo"
                switch (ma.getName()) {
                    case "key", 
                    case "id": {
                        keysRefs.add("@key")
                        keysRefs.add("@key:external")
                    }
                    case "reference": {
                        keysRefs.add("@ref")
                        keysRefs.add("@ref:external")
                    }
                    case "scheme": {
                        otherMeta.add("@scheme")
                    }
                    case "location": {
                        keysRefs.add("@key:scoped")
                    }
                    case "address": {
                        keysRefs.add("@ref:scoped")
                    }
                    default: {
                        println('---- unprocessed meta ... name: ' + ma.getName())
                    }
                }
            }
        }
        val validators = new ArrayList<String>()
        validators.addAll(keysRefs);
        validators.addAll(otherMeta);
        if (!keysRefs.isEmpty()) {
            keyRefConstraints.put(ra.getName(), keysRefs)
        }

        var metaPrefix = "";
        var metaSuffix = "";
        var hasBeenAnnotated = false;
        if (!validators.isEmpty()) {
            if (!attributeIsMetaKey) {
                attrTypeName = PythonTranslator.getAttributeTypeWithMeta(attrTypeName);
            }
            metaPrefix = "Annotated[";
            hasBeenAnnotated = true;
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
        if (!hasBeenAnnotated && !isRosettaBasicType && !(attrRType instanceof REnumType)) { 
            if (metaPrefix.length() > 0) {
                println ("----- @@@@@ whoops ... metaPrefix: " + metaPrefix)
            }
            metaPrefix = "Annotated[";
            metaSuffix = ", " + attrTypeName + ".serializer(), " + attrTypeName + ".validator()]";
        }
        var _builder = new StringConcatenation();
        _builder.append(attrName);
        _builder.append(": ");
        // attribute string depends on whether there are props and whether there is cardinality
        if (!attrProp.isEmpty() && cardinalityString.length() > 0) {
            _builder.append(cardinalityPrefix);
            _builder.append("Annotated[")
            _builder.append(attrTypeName);
            _builder.append(", Field(");
            _builder.append(propString);
            if (metaSuffix.length() != 0) {
                _builder.append(metaSuffix);
            } else {
                _builder.append(")]");
            }
            _builder.append(cardinalitySuffix);
            _builder.append(" = Field(");
            _builder.append(fieldDefault);
            _builder.append(", description='");
            _builder.append(attrDesc);
            _builder.append("'");
            _builder.append(cardinalityString);
            _builder.append(")");
        } else {
            _builder.append(cardinalityPrefix);
            _builder.append(metaPrefix);
            _builder.append(attrTypeName);
            _builder.append(metaSuffix);
            _builder.append(cardinalitySuffix);
            _builder.append(" = Field(");
            _builder.append(fieldDefault);
            _builder.append(", description='");
            _builder.append(attrDesc);
            _builder.append("'");
            _builder.append(cardinalityString);
            if (propString.length() > 0) {
                _builder.append(", ");
                _builder.append(propString);
            }
            _builder.append(")");
        }
        if (ra.definition !== null) {
            _builder.append("\n\"\"\"\n");
            _builder.append(ra.definition);
            _builder.append("\n\"\"\"");
        }
        _builder.append("\n");
        return _builder.toString();
    }

    def getImportsFromAttributes(Data rosettaClass) {
        val allAttributes = rosettaClass.buildRDataType.getOwnAttributes.filter [
            (it.name !== "reference") && (it.name !== "meta") && (it.name !== "scheme")
        ].filter[!PythonTranslator::isRosettaBasicType(it)]

        val imports = newArrayList
        for (attribute : allAttributes) {
            var rt = attribute.getRMetaAnnotatedType.getRType
            // get all non-Meta attributes
            if (rt instanceof RAliasType) {
                rt = typeSystem.stripFromTypeAliases(rt);
            }
            if (rt === null) {
                throw new Exception("Attribute type is null for " + attribute.name + " for class " + rosettaClass.name)
            }
            if (!PythonTranslator::isRosettaBasicType(rt.getName())) { // need imports for derived types
                imports.add('''import «rt.getQualifiedName»''')
            }
        }
        return imports.toSet.toList
    }
}
