package com.regnosys.rosetta.generator.python.object;

import java.util.List;

/**
 * Result of attribute processing, containing the class body code and delayed
 * annotation updates.
 */
public final class AttributeProcessingResult {
    /**
     * The attribute code.
     */
    private final String attributeCode;
    /**
     * The annotation updates.
     */
    private final List<String> annotationUpdates;

    /**
     * The AttributeProcessingResult constructor.
     * 
     * @param attributeCode     the attribute code
     * @param annotationUpdates the annotation updates
     */
    public AttributeProcessingResult(String attributeCode, List<String> annotationUpdates) {
        this.attributeCode = attributeCode;
        this.annotationUpdates = annotationUpdates;
    }

    public String getAttributeCode() {
        return attributeCode;
    }

    public List<String> getAnnotationUpdates() {
        return annotationUpdates;
    }
}
