package com.regnosys.rosetta.generator.python.object;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.inject.Inject;
import com.regnosys.rosetta.generator.python.util.RuneToPythonMapper;
import com.regnosys.rosetta.rosetta.simple.Data;
import com.regnosys.rosetta.types.RDataType;
import com.regnosys.rosetta.types.RMetaAnnotatedType;
import com.regnosys.rosetta.types.RObjectFactory;

public class PythonMetaDataProcessor {

    @Inject
    private RObjectFactory rObjectFactory;

    /**
     * Builds a list of all keys.
     */
    public Map<String, String> getMetaDataKeys(List<Data> rosettaClasses) {
        Map<String, String> metaDataKeys = new HashMap<>();

        for (Data rosettaClass : rosettaClasses) {
            RDataType rcRData = rObjectFactory.buildRDataType(rosettaClass);
            String modelName = rosettaClass.getModel().getName();

            // capture if the class itself has a key 
            rcRData.getMetaAttributes().forEach(metaData -> {
                String metaType = metaData.getName();
                if ("key".equals(metaType)) {
                    metaDataKeys.put(modelName + '.' + rosettaClass.getName() + '.' + rosettaClass.getName(), metaType);
                }
            });
            // capture any keys of the class' attributes
            rcRData.getAllAttributes().forEach(ra -> {
                RMetaAnnotatedType attrRMAT = ra.getRMetaAnnotatedType();
                if (attrRMAT.hasMeta()) {
                    attrRMAT.getMetaAttributes().forEach(ma -> {
                        String metaType = ma.getName();
                        if ("key".equals(metaType) || "id".equals(metaType) || "location".equals(metaType)) {
                            metaDataKeys.put(modelName + '.' + 
                                    rosettaClass.getName() + '.' + 
                                    RuneToPythonMapper.mangleName(ra.getName()), 
                                    metaType);
                        }
                    });
                }
            });
        }

        return metaDataKeys;
    }
}
