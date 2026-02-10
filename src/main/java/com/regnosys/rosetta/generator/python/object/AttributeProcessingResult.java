package com.regnosys.rosetta.generator.python.object;

import java.util.List;

/**
 * Result of attribute processing, containing the class body code and delayed
 * annotation updates.
 */
public class AttributeProcessingResult {
    private final String attributeCode;
    private final List<String> annotationUpdates;

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
