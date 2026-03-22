package com.regnosys.rosetta.generator.python.object;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.regnosys.rosetta.generator.python.PythonCodeGeneratorContext;
import com.regnosys.rosetta.generator.python.util.PythonCodeWriter;
import com.regnosys.rosetta.generator.python.util.RuneToPythonMapper;
import com.regnosys.rosetta.rosetta.RosettaModel;
import com.regnosys.rosetta.rosetta.simple.Data;
import com.regnosys.rosetta.types.RAliasType;
import com.regnosys.rosetta.types.RAttribute;
import com.regnosys.rosetta.types.RCardinality;
import com.regnosys.rosetta.types.RChoiceType;
import com.regnosys.rosetta.types.RDataType;
import com.regnosys.rosetta.types.REnumType;
import com.regnosys.rosetta.types.RMetaAnnotatedType;
import com.regnosys.rosetta.types.RMetaAttribute;
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
    public AttributeProcessingResult generateAllAttributes(Data rc,
        Map<String, List<String>> keyRefConstraints,
        PythonCodeGeneratorContext context) {
        RDataType buildRDataType = rObjectFactory.buildRDataType(rc);
        Collection<RAttribute> allAttributes = buildRDataType.getOwnAttributes();

        if (allAttributes.isEmpty() && rc.getConditions().isEmpty()) {
            return new AttributeProcessingResult("pass", Collections.emptyList());
        }

        PythonCodeWriter writer = new PythonCodeWriter();
        List<String> annotationUpdates = new ArrayList<>();
        for (RAttribute ra : allAttributes) {
            AttributeResult result = generateAttribute(rc, ra, keyRefConstraints, context);
            writer.appendBlock(result.attributeCode());
            result.annotationUpdate().ifPresent(annotationUpdates::add);
        }
        return new AttributeProcessingResult(writer.toString(), annotationUpdates);
    }

    private AttributeResult generateAttribute(Data rc, RAttribute ra,
            Map<String, List<String>> keyRefConstraints,
            PythonCodeGeneratorContext context) {
        MetaDataResult metaDataResult = processMetaDataAttributes(ra, keyRefConstraints);
        return createAttributeResult(rc, ra, metaDataResult, context);
    }

    private AttributeResult createAttributeResult(Data rc, RAttribute ra, MetaDataResult metaDataResult, PythonCodeGeneratorContext context) {
        RosettaModel model = (RosettaModel) rc.eContainer();
        String className = model.getName() + "." + rc.getName();
        boolean isStandalone = context.getStandaloneClasses().contains(className);
        String attrName = RuneToPythonMapper.mangleName(ra.getName());
        PythonTypeHint hint = deriveTypeHint(rc, ra, metaDataResult, context);

        RType rt = ra.getRMetaAnnotatedType().getRType();
        if (rt instanceof RAliasType) {
            rt = typeSystem.stripFromTypeAliases(rt);
        }
        boolean isDelayed = !RuneToPythonMapper.isRosettaBasicType(rt) && !(rt instanceof REnumType);

        String pythonType;
        Optional<String> annotationUpdate = Optional.empty();
        if (isDelayed && !isStandalone) {
            // Phase 1 placeholder: use 'None' so pydantic does not attempt to resolve the type name
            // at class-definition time. Pydantic adds vars(cls) to localns, so a field whose name
            // matches its type name (e.g. CollateralIssuerType: Optional[CollateralIssuerType])
            // would resolve the type to the FieldInfo default rather than the imported class,
            // triggering ArbitraryTypeWarning. The Phase 2 annotation update below replaces 'None'
            // with the real Annotated type at module level, and model_rebuild() picks it up.
            pythonType = "None";
            String update = String.format("__annotations__[\"%s\"] = %s", attrName, hint.build(true));
            annotationUpdate = Optional.of(update);
        } else {
            pythonType = hint.build(true);
        }

        String fieldDecl = generateFieldDeclaration(ra, attrName, pythonType, hint.fieldProperties().isPresent());
        return new AttributeResult(fieldDecl, annotationUpdate);
    }

    private PythonTypeHint deriveTypeHint(Data rc, RAttribute ra, MetaDataResult metaDataResult, PythonCodeGeneratorContext context) {
        RType rt = ra.getRMetaAnnotatedType().getRType();
        if (rt instanceof RAliasType alias) {
            rt = typeSystem.stripFromTypeAliases(alias);
        }

        RosettaModel model = (RosettaModel) rc.eContainer();
        String className = model.getName() + "." + rc.getName();
        boolean isSourceStandalone = context.getStandaloneClasses().contains(className);

        String typeRefName;
        String fqnAttributeNamespace = rt.getNamespace().toString();
        String fqnAttributeName = fqnAttributeNamespace + "." + rt.getName();
        
        if (RuneToPythonMapper.isRosettaBasicType(rt)) {
            typeRefName = RuneToPythonMapper.toPythonType(rt);
        } else if (rt instanceof REnumType) {
            typeRefName = fqnAttributeName + "." + rt.getName();
        } else {
            // Data Type
            boolean isTargetStandalone = context.getStandaloneClasses().contains(fqnAttributeName);
            if (isSourceStandalone || isTargetStandalone) {
                // Use short name (imported via 'from module import Class').
                // For standalone source classes, pydantic adds vars(cls) to localns when resolving
                // annotations. If the field name equals the type name, the class attribute (FieldInfo)
                // would shadow the imported class. Alias the import as '_TypeName' to avoid this.
                // For bundled source classes, Phase 2 annotation updates run at module level where
                // vars(cls) is not in scope, so no alias is needed.
                if (isSourceStandalone) {
                    String attrPythonName = RuneToPythonMapper.mangleName(ra.getName());
                    boolean hasNamingConflict = attrPythonName.equals(rt.getName());
                    typeRefName = hasNamingConflict ? ("_" + rt.getName()) : rt.getName();
                } else {
                    typeRefName = rt.getName();
                }
            } else {
                // Internal bundle reference
                typeRefName = com.regnosys.rosetta.generator.python.util.PythonCodeGeneratorUtil.toFlattenedName(fqnAttributeName);
            }
        }

        String attrTypeNameIn = typeRefName;
        if (attrTypeNameIn == null) {
            throw new RuntimeException("Attribute type is null for " + ra.getName());
        }

        ValidationProperties validationProps = processProperties(rt);
        List<String> validators = metaDataResult.getValidators();
        RCardinality cardinality = ra.getCardinality();
        int max = cardinality.getMax().orElse(-1);
        boolean isList = (cardinality.isMulti() || max > 1);

        String baseTypeName = (!validators.isEmpty())
                ? RuneToPythonMapper.getAttributeTypeWithMeta(attrTypeNameIn)
                : attrTypeNameIn;

        String propString = validationProps.toFieldArgs();
        Optional<String> fieldProps = (isList && !propString.isEmpty()) ? Optional.of(propString) : Optional.empty();

        boolean isDelayed = !RuneToPythonMapper.isRosettaBasicType(rt) && !(rt instanceof REnumType);
        // STANDALONE strategy: omit Annotated wrapper unless there are validators
        // BUNDLED strategy: always use Annotated for isDelayed to support Phase 2 updates
        boolean needsRuneAnns = (isDelayed && !isSourceStandalone) || !validators.isEmpty();
        Optional<String> runeAnns = needsRuneAnns ? Optional.of(getRuneAnnotationArgs(validators, baseTypeName))
                : Optional.empty();

        return new PythonTypeHint(
                baseTypeName,
                metaDataResult.hasReference(),
                fieldProps,
                isList,
                cardinality.getMin() == 0,
                runeAnns);
    }

    private String generateFieldDeclaration(RAttribute ra, String attrName, String pythonType,
            boolean propertiesAppliedToInnerType) {
        RType rt = ra.getRMetaAnnotatedType().getRType();
        if (rt instanceof RAliasType) {
            rt = typeSystem.stripFromTypeAliases(rt);
        }
        ValidationProperties validationProps = processProperties(rt);
        CardinalityInfo cardinalityInfo = processCardinality(ra);
        String propString = validationProps.toFieldArgs();

        String attrDesc = (ra.getDefinition() == null)
                ? ""
                : ra.getDefinition().replaceAll("\\s+", " ").replace("'", "\\'");

        String fieldProps = (!propString.isEmpty() && !propertiesAppliedToInnerType) ? ", " + propString : "";
        String fieldDecl = String.format("%s: %s = Field(%s, description='%s'%s%s)",
                attrName,
                pythonType,
                cardinalityInfo.fieldDefault(),
                attrDesc,
                cardinalityInfo.cardinalityString(),
                fieldProps);

        if (ra.getDefinition() != null) {
            fieldDecl += String.format("\n\"\"\"\n%s\n\"\"\"", ra.getDefinition());
        }
        return fieldDecl;
    }

    private String getRuneAnnotationArgs(List<String> validators, String typeName) {
        String serializer = String.format("%s.serializer()", typeName);
        String validator;
        if (validators.isEmpty()) {
            validator = String.format("%s.validator()", typeName);
        } else {
            String joinedValidators = validators.stream()
                    .map(v -> String.format("'%s'", v))
                    .collect(Collectors.joining(", "));
            if (validators.size() == 1) {
                joinedValidators += ", ";
            }
            validator = String.format("%s.validator((%s))", typeName, joinedValidators);
        }
        return String.format("%s, %s", serializer, validator);
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
            Map<String, List<String>> keyRefConstraints) {
        List<String> validators = new ArrayList<>();
        List<String> otherMeta = new ArrayList<>();

        RMetaAnnotatedType attrRMAT = ra.getRMetaAnnotatedType();
        boolean hasReference = false;
        if (attrRMAT.hasAttributeMeta()) {
            for (RMetaAttribute ma : attrRMAT.getMetaAttributes()) {
                switch (ma.getName()) {
                    case "key", "id" -> {
                        validators.add("@key");
                        validators.add("@key:external");
                    }
                    case "reference" -> {
                        validators.add("@ref");
                        validators.add("@ref:external");
                        hasReference = true;
                    }
                    case "scheme" -> otherMeta.add("@scheme");
                    case "location" -> validators.add("@key:scoped");
                    case "address" -> {
                        validators.add("@ref:scoped");
                        hasReference = true;
                    }
                    default -> throw new IllegalStateException("Unsupported metadata attribute: " + ma.getName());
                }
            }
        }
        if (!validators.isEmpty()) {
            keyRefConstraints.put(ra.getName(), new ArrayList<>(validators));
        }
        validators.addAll(otherMeta);
        return new MetaDataResult(validators, hasReference);
    }

    private ValidationProperties processProperties(RType rt) {
        ValidationProperties.Builder builder = ValidationProperties.builder();
        switch (rt) {
            case RStringType stringType -> {
                stringType
                    .getPattern()
                    .ifPresent(value -> builder.pattern(String.format("r'^%s*$'", value)));
                stringType
                    .getInterval()
                    .getMin()
                    .filter(v -> v > 0)
                    .ifPresent(v -> builder.minLength(v));
                stringType
                    .getInterval()
                    .getMax()
                    .ifPresent(v -> builder.maxLength(v));
            }
            case RNumberType numberType -> {
                if (!numberType.isInteger()) {
                    numberType.getDigits().ifPresent(v -> builder.maxDigits(v));
                    numberType.getFractionalDigits().ifPresent(v -> builder.decimalPlaces(v));
                    numberType
                        .getInterval()
                        .getMin()
                        .ifPresent(v -> builder.ge(String.format("Decimal('%s')", v.toPlainString())));
                    numberType
                        .getInterval()
                        .getMax()
                        .ifPresent(v -> builder.le(String.format("Decimal('%s')", v.toPlainString())));
                }
            }
            default -> {
            }
        }
        return builder.build();
    }

    private CardinalityInfo processCardinality(RAttribute ra) {
        RCardinality cardinality = ra.getCardinality();
        int lowerBound = cardinality.getMin();
        Optional<Integer> upperCardinality = cardinality.getMax();
        boolean upperBoundIsGTOne = (upperCardinality.isPresent() && upperCardinality.get() > 1);

        String fieldDefault = "";
        String cardinalityString = "";

        // Default constraints
        switch (lowerBound) {
            case 0 -> {
                fieldDefault = "None";
                if (cardinality.isMulti() || upperBoundIsGTOne) {
                    if (upperBoundIsGTOne) {
                        cardinalityString = String.format(", max_length=%d", upperCardinality.get());
                    }
                }
            }
            case 1 -> {
                fieldDefault = "...";
                if (cardinality.isMulti() || upperBoundIsGTOne) {
                    cardinalityString = ", min_length=1";
                    if (upperBoundIsGTOne) {
                        cardinalityString += String.format(", max_length=%d", upperCardinality.get());
                    }
                }
            }
            default -> {
                fieldDefault = "...";
                cardinalityString = String.format(", min_length=%d", lowerBound);
                if (upperCardinality.isPresent()) {
                    int upperBound = upperCardinality.get();
                    if (upperBound > 1) {
                        cardinalityString += String.format(", max_length=%d", upperBound);
                    }
                }
            }
        }

        return new CardinalityInfo(fieldDefault, cardinalityString);
    }

    /**
     * Collects import statements for the attributes of a Data type.
     *
     * @param rc                  The Data type whose attributes are inspected.
     * @param imports             The set to add import statements to.
     * @param standaloneClasses   The set of fully-qualified names that are standalone.
     * @param includeBundledTypes When {@code true} (standalone file context), imports
     *                            are emitted for all Data types including bundled ones.
     *                            When {@code false} (bundle header context), imports are
     *                            only emitted for enums and standalone Data types —
     *                            bundled types must not be imported via their proxy stub
     *                            because that would create a circular import back into
     *                            the bundle itself.
     */
    public void getImportsFromAttributes(Data rc, Set<String> imports,
            Set<String> standaloneClasses, boolean includeBundledTypes) {
        RDataType buildRDataType = rObjectFactory.buildRDataType(rc);
        Collection<RAttribute> allAttributes = buildRDataType.getOwnAttributes();
        RosettaModel rcModel = (RosettaModel) rc.eContainer();
        String rcFqn = rcModel.getName() + "." + rc.getName();

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
                            "Attribute type is null for "
                            + attr.getName()
                            + " for class "
                            + rc.getName());
                }

                // Normalize RChoiceType to RDataType for import generation
                if (rt instanceof RChoiceType rChoiceType) {
                    rt = rChoiceType.asRDataType();
                }
                if (rt instanceof REnumType rEnumType) {
                    imports.add(String.format("import %s", rEnumType.getQualifiedName()));
                } else if (rt instanceof RDataType rDataType) {
                    String fullNamespace = rDataType.getNamespace().toString();
                    String fqn = fullNamespace + "." + rDataType.getName();
                    if (fqn.equals(rcFqn)) {
                        // Skip self-import: the type references itself (e.g. via [metadata reference]).
                        // Emitting 'from <module> import <Class>' in the file that defines <Class>
                        // causes a circular import error during Python's partial module initialization.
                        continue;
                    }
                    if (includeBundledTypes || standaloneClasses.contains(fqn)) {
                        if (includeBundledTypes) {
                            // Standalone file context: pydantic adds vars(cls) to localns, so a field
                            // whose name matches its type name would shadow the import. Alias as '_TypeName'.
                            String attrPythonName = RuneToPythonMapper.mangleName(attr.getName());
                            boolean hasNamingConflict = attrPythonName.equals(rDataType.getName());
                            if (hasNamingConflict) {
                                imports.add(String.format("from %s.%s import %s as _%s",
                                        fullNamespace, rDataType.getName(), rDataType.getName(), rDataType.getName()));
                            } else {
                                imports.add(String.format("from %s.%s import %s",
                                        fullNamespace, rDataType.getName(), rDataType.getName()));
                            }
                        } else {
                            // Bundle header context: Phase 2 annotation updates run at module level,
                            // vars(cls) is not in scope — no alias needed.
                            imports.add(String.format("from %s.%s import %s",
                                    fullNamespace, rDataType.getName(), rDataType.getName()));
                        }
                    }
                }
            }
        }
    }

    private static class MetaDataResult {
        /**
         * The validators for the attribute.
         */
        private final List<String> validators;
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
        MetaDataResult(List<String> validators, boolean hasReference) {
            this.validators = validators;
            this.hasReference = hasReference;
        }

        public List<String> getValidators() {
            return validators;
        }

        public boolean hasReference() {
            return hasReference;
        }
    }
    private record CardinalityInfo(String fieldDefault, String cardinalityString) {
    }

    private record AttributeResult(String attributeCode, Optional<String> annotationUpdate) {
    }
    private record PythonTypeHint(
            String baseType,
            boolean hasReference,
            Optional<String> fieldProperties,
            boolean isList,
            boolean isOptional,
            Optional<String> runeAnnotations) {

        public String build(boolean includeRuneAnnotations) {
            String type = baseType;
            if (hasReference) {
                type += " | BaseReference";
            }
            if (fieldProperties.isPresent()) {
                type = String.format("Annotated[%s, Field(%s)]", type, fieldProperties.get());
            }
            if (isList) {
                type = String.format("list[%s | None]", type);
            }
            if (isOptional) {
                type = String.format("Optional[%s]", type);
            }
            if (includeRuneAnnotations && runeAnnotations.isPresent()) {
                type = String.format("Annotated[%s, %s]", type, runeAnnotations.get());
            }
            return type;
        }
    }

    private record ValidationProperties(
            Optional<String> pattern,
            Optional<Integer> minLength,
            Optional<Integer> maxLength,
            Optional<Integer> maxDigits,
            Optional<Integer> decimalPlaces,
            Optional<String> ge,
            Optional<String> le) {

        public static Builder builder() {
            return new Builder();
        }

        public String toFieldArgs() {
            List<String> args = new ArrayList<>();
            minLength.ifPresent(v -> args.add(String.format("min_length=%d", v)));
            pattern.ifPresent(v -> args.add(String.format("pattern=%s", v)));
            maxLength.ifPresent(v -> args.add(String.format("max_length=%d", v)));
            maxDigits.ifPresent(v -> args.add(String.format("max_digits=%d", v)));
            decimalPlaces.ifPresent(v -> args.add(String.format("decimal_places=%d", v)));
            ge.ifPresent(v -> args.add(String.format("ge=%s", v)));
            le.ifPresent(v -> args.add(String.format("le=%s", v)));
            return String.join(", ", args);
        }

        public static class Builder {
            /** The regex pattern for validation. */
            private Optional<String> pattern = Optional.empty();
            /** The minimum length for validation. */
            private Optional<Integer> minLength = Optional.empty();
            /** The maximum length for validation. */
            private Optional<Integer> maxLength = Optional.empty();
            /** The maximum number of digits for validation. */
            private Optional<Integer> maxDigits = Optional.empty();
            /** The number of decimal places for validation. */
            private Optional<Integer> decimalPlaces = Optional.empty();
            /** The greater-than-or-equal value for validation. */
            private Optional<String> ge = Optional.empty();
            /** The less-than-or-equal value for validation. */
            private Optional<String> le = Optional.empty();

            public Builder pattern(String patternIn) {
                this.pattern = Optional.of(patternIn);
                return this;
            }

            public Builder minLength(int minLengthIn) {
                this.minLength = Optional.of(minLengthIn);
                return this;
            }

            public Builder maxLength(int maxLengthIn) {
                this.maxLength = Optional.of(maxLengthIn);
                return this;
            }

            public Builder maxDigits(int maxDigitsIn) {
                this.maxDigits = Optional.of(maxDigitsIn);
                return this;
            }

            public Builder decimalPlaces(int decimalPlacesIn) {
                this.decimalPlaces = Optional.of(decimalPlacesIn);
                return this;
            }

            public Builder ge(String geIn) {
                this.ge = Optional.of(geIn);
                return this;
            }

            public Builder le(String leIn) {
                this.le = Optional.of(leIn);
                return this;
            }

            public ValidationProperties build() {
                return new ValidationProperties(pattern,
                    minLength,
                    maxLength,
                    maxDigits,
                    decimalPlaces,
                    ge,
                    le);
            }
        }
    }
}
