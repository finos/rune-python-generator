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
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.jgrapht.traverse.TopologicalOrderIterator;
import org.jgrapht.graph.GraphCycleProhibitedException;
/*
 * Generate Python from Rune Types
 */
class PythonModelObjectGenerator {

    @Inject extension RObjectFactory

    @Inject PythonMetaDataProcessor pythonMetaDataProcessor;
    @Inject PythonExpressionGenerator expressionGenerator;
    @Inject PythonAttributeProcessor pythonAttributeProcessor;
    @Inject PythonChoiceAliasProcessor pythonChoiceAliasProcessor;

    var Graph<String, DefaultEdge> dependencyDAG = null;

    def void beforeAllGenerate () {
        dependencyDAG = new DirectedAcyclicGraph<String, DefaultEdge>(typeof(DefaultEdge))
    }
    def List<String> afterAllGenerate () {
        val orderedList = newArrayList
        if (dependencyDAG !== null) {
            val topologicalOrderIterator = new TopologicalOrderIterator<String, DefaultEdge>(dependencyDAG)
            while (topologicalOrderIterator.hasNext) {
            	orderedList.add (topologicalOrderIterator.next())
            }
        }
        return orderedList;
    }
    /**
     * Generate Python from the collection of Rosetta classes (of type Data)
     * 
     * Inputs:
     * 
     * rosettaClasses - the collection of Rosetta Classes for this model
     * metaDataItems - a hash map of each "key" type found in meta data found in the classes and attributes of the class
     */
    def Map<String, ? extends CharSequence> generate(Iterable<Data> rosettaClasses, String version) {

        var metaDataKeys = pythonMetaDataProcessor.getMetaDataKeys(rosettaClasses.toList);
        val result = new HashMap

        for (Data rosettaClass : rosettaClasses) {
            val model = rosettaClass.eContainer as RosettaModel
            val nameSpace = Util::getNamespace(model)
            val pythonBody = generateClass(rosettaClass, metaDataKeys, nameSpace, version).toString.replace('\t', '  ')
            result.put(
                model.name + "." + rosettaClass.getName(),
                PythonModelGeneratorUtil::createImports(rosettaClass.getName()) + pythonBody
            )
            if (dependencyDAG !== null) {
                val className = model.name + "." + rosettaClass.getName()
                dependencyDAG.addVertex(className)
                val dependencies = pythonAttributeProcessor.getDependenciesFromAttributes (rosettaClass)
                for (dependency : dependencies) {
                    dependencyDAG.addVertex(dependency.toString())
                    if (!className.equals(dependency.toString())) {
                        try {
                            dependencyDAG.addEdge(className, dependency.toString());
                        } catch (GraphCycleProhibitedException e) {
                        }
                    }
                }
            }
        }
        return result;
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
        val importsFound = pythonAttributeProcessor.getImportsFromAttributes(rosettaClass)
        expressionGenerator.importsFound = importsFound;
        val classDefinition = generateBody(rosettaClass, metaDataKeys)

        var _builder = new StringConcatenation();
        if (superType !== null) {
            _builder.append("from ")
            _builder.append((superType.eContainer as RosettaModel).name)
            _builder.append(".")
            _builder.append(superType.name)
            _builder.append(" import ")
            _builder.append(superType.name)
            _builder.newLine();

        }
        _builder.newLine();

        _builder.append(classDefinition)
        _builder.append("\nimport ")
        _builder.append(nameSpace)
        _builder.newLine();

        var firstLine = true;
        for (importLine : importsFound) {
            if (firstLine) {
                firstLine = false;
            } else {
                _builder.newLine();

            }
            _builder.append(importLine)
        }
        return _builder.toString();
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
