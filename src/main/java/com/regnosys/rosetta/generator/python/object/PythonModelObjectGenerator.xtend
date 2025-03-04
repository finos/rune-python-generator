package com.regnosys.rosetta.generator.python.object

import com.google.inject.Inject
import com.regnosys.rosetta.generator.python.expressions.PythonExpressionGenerator
import com.regnosys.rosetta.generator.python.util.PythonModelGeneratorUtil
import com.regnosys.rosetta.generator.python.util.Util
import com.regnosys.rosetta.rosetta.RosettaModel
import com.regnosys.rosetta.rosetta.simple.Data
import com.regnosys.rosetta.types.RObjectFactory
import java.util.HashMap
import java.util.List
import java.util.Map
import org.eclipse.xtend2.lib.StringConcatenation

/*
 * Generate Python from Rune Types
 */
class PythonModelObjectGenerator {

    @Inject extension RObjectFactory

    @Inject PythonMetaDataProcessor pythonMetaDataProcessor;
    @Inject PythonExpressionGenerator expressionGenerator;
    @Inject PythonAttributeProcessor pythonAttributeProcessor;
    @Inject PythonChoiceAliasProcessor pythonChoiceAliasProcessor;

    List<String> importsFound;
    
    /**
     * Generate Python from the collection of Rosetta classes (of type Data)
     * 
     * Inputs:
     * 
     * rosettaClasses - the collection of Rosetta Classes for this model
     * metaDataItems - a hash map of each "key" type found in meta data found in the classes and attributes of the class
     */
    def Map<String, ? extends CharSequence> generate(Iterable<Data> rosettaClasses, /*Iterable<RosettaMetaType> metaDataItems, */String version) {

        var metaDataKeys = pythonMetaDataProcessor.getMetaDataKeys(rosettaClasses.toList);
        
        val result = new HashMap

        for (Data rosettaClass : rosettaClasses) {
            val model = rosettaClass.eContainer as RosettaModel
            val nameSpace = Util::getNamespace(model)
            val pythonBody = generateClass(rosettaClass, metaDataKeys, nameSpace, version).toString.replace('\t', '  ')
            result.put(
                PythonModelGeneratorUtil::toPyFileName(model.name, rosettaClass.getName()),
                PythonModelGeneratorUtil::createImports(rosettaClass.getName()) + pythonBody
            )
        }
        result;
    }

    private def generateClass(Data rosettaClass, Map<String, String> metaDataKeys, String nameSpace, String version) {
        // generate Python for the class
        // ... get the imports from the attributes
        // ... generate the body of the class
        // ... create the class string:
        // ...... an import that refers to the super class
        // ...... the class definition
        // ...... all the imports
        var superType = rosettaClass.superType
        if (superType !== null && superType.name === null) {
            throw new Exception("The class superType for " + rosettaClass.name + " exists but its name is null")
        }
        importsFound = pythonAttributeProcessor.getImportsFromAttributes(rosettaClass)
        expressionGenerator.importsFound = importsFound;
        val classDefinition = generateBody(rosettaClass, metaDataKeys)

        return '''
            «IF superType!==null»from «(superType.eContainer as RosettaModel).name».«superType.name» import «superType.name»«ENDIF»
            
            «classDefinition»
            
            import «nameSpace» 
            «FOR importLine : importsFound SEPARATOR "\n"»«importLine»«ENDFOR»
        '''
    }

    private def keyRefConstraintsToString (Map<String, List<String>> keyRefConstraints) {
        var _builder = new StringConcatenation();
        if (!keyRefConstraints.isEmpty()) {
            _builder.append("\n_KEY_REF_CONSTRAINTS = {\n");
            var isFirst = true;
            for (keyRef : keyRefConstraints.entrySet()) {
                if (isFirst) {
                    isFirst = false;
                } else {
                    _builder.append (",\n");
                }
                _builder.append ("\t'");
                _builder.append (keyRef.key);
                _builder.append ("': {");
                var isFirstItem = true;
                for (item : keyRef.value) {
                    if (isFirstItem) {
                        isFirstItem = false;
                    } else {
                        _builder.append (", ");
                    }
                    _builder.append ("'");
                    _builder.append (item);
                    _builder.append ("'");
                }
                _builder.append ("}");
            }
            _builder.append("\n}");
        }
        return _builder.toString();
    }
    private def generateBody(Data rosettaClass,  Map<String, String> metaDataKeys) {
        // generate the main body of the class
        // ... first generate choice aliases
        // ... then add the class definition
        // ... then add all attributes
        // ... then add any conditions
        val rosettaDataType = rosettaClass.buildRDataType
        val choiceAliasesAsAString = pythonChoiceAliasProcessor.generateChoiceAliasesAsString(rosettaDataType);
        val keyRefConstraints = new HashMap<String, List<String>> ();
        return '''
            class «rosettaClass.name»«IF rosettaClass.superType === null»«ENDIF»«IF rosettaClass.superType !== null»(«rosettaClass.superType.name»):«ELSE»(BaseDataClass):«ENDIF»
                «choiceAliasesAsAString»
                «IF rosettaClass.definition !== null»
                    """
                    «rosettaClass.definition»
                    """
                «ENDIF»
                «pythonAttributeProcessor.generateAllAttributes(rosettaClass, metaDataKeys, keyRefConstraints)»
                «IF !keyRefConstraints.isEmpty()»
                    «keyRefConstraintsToString(keyRefConstraints)»
                «ENDIF»
                «expressionGenerator.generateConditions(rosettaClass)»
        '''
    }
}
