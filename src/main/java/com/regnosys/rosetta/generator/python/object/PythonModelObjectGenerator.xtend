package com.regnosys.rosetta.generator.python.object

// TODO: fix unit tests
import com.google.inject.Inject
import com.regnosys.rosetta.generator.python.expressions.PythonExpressionGenerator
import com.regnosys.rosetta.generator.python.util.PythonCodeGeneratorUtil
import com.regnosys.rosetta.rosetta.RosettaModel
import com.regnosys.rosetta.rosetta.simple.Data
import com.regnosys.rosetta.types.RObjectFactory
import java.util.HashMap
import java.util.HashSet
import java.util.ArrayList
import java.util.Set
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
    @Inject RObjectFactory rObjectFactory;

    @Inject PythonExpressionGenerator expressionGenerator;
    @Inject PythonAttributeProcessor pythonAttributeProcessor;
    @Inject PythonChoiceAliasProcessor pythonChoiceAliasProcessor;

    var Graph<String, DefaultEdge> dependencyDAG = null;
    var Set<String> imports = null;

    def void beforeAllGenerate () {
        dependencyDAG = new DirectedAcyclicGraph<String, DefaultEdge>(typeof(DefaultEdge))
        imports = new HashSet<String>()
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
        val result = new HashMap

        for (Data rosettaClass : rosettaClasses) {
            val model = rosettaClass.eContainer as RosettaModel
            val nameSpace = PythonCodeGeneratorUtil::getNamespace(model)
            // ensure there are no tabs
            val pythonClass = generateClass(rosettaClass, nameSpace, version).toString.replace('\t', '    ')
            // use "." as a delimiter to preserve the use of "_" in the name
            val className = model.name + '.' + rosettaClass.getName()
            result.put(className, pythonClass)
            if (dependencyDAG !== null) {
                dependencyDAG.addVertex(className)
                if (rosettaClass.superType !== null) {
                    val superClass = rosettaClass.superType;
                    val superModel = superClass.eContainer as RosettaModel
                    val superClassName = superModel.getName() + '.' + superClass.getName()
                    addDependency(className, superClassName)
                } 
            }
        }
        return result;
    }

    def Map<String, ? extends CharSequence> afterAllGenerate (String namespace, Map<String, CharSequence> objects) {
        // create bundle and stub classes
        val result = new HashMap
        if (dependencyDAG !== null) {
            var _bundle = new StringConcatenation();
            val topologicalOrderIterator = new TopologicalOrderIterator<String, DefaultEdge>(dependencyDAG)
            // for each element in the ordered collection add the generated class to the bundle and add a stub class to the results
            var isFirst = true;
            while (topologicalOrderIterator.hasNext) {
                if (isFirst) {
                    _bundle.append(PythonCodeGeneratorUtil.createImports());
                    for (import : imports) {
                        _bundle.append(import)
                        _bundle.newLine()
                    }
                    isFirst = false;
                }
                val name = topologicalOrderIterator.next().toString();
                val object = objects.get(name);
                if (object !== null){
                    // append the class to the bundle
                    _bundle.newLine()
                    _bundle.newLine()
                    _bundle.append(object);
                    // create the stub

                    val parsedName = name.split('\\.')
                    val stubFileName = "src/" + parsedName.join('/') + '.py'
                    var _stubContents = new StringConcatenation();
                    _stubContents.append("# pylint: disable=unused-import");
                    _stubContents.newLine()
                    _stubContents.append("from ");
                    _stubContents.append(parsedName.head());
                    _stubContents.append("._bundle import ")
                    _stubContents.append(name.replace('.', '_'))
                    _stubContents.append(" as ")
                    _stubContents.append(parsedName.last())
                    _stubContents.newLine()
                    _stubContents.newLine()
                    _stubContents.append("# EOF")
                    _stubContents.newLine()
                    result.put (stubFileName, _stubContents.toString())
                }
            }
            _bundle.append("# EOF");
            _bundle.newLine();
            result.put("src/" + namespace + "/_bundle.py", _bundle.toString());
        }
        return result;
    }
    private def addDependency (String className, String dependencyName) {
        dependencyDAG.addVertex(dependencyName)
        if (!className.equals(dependencyName)) {
            try {
                dependencyDAG.addEdge(dependencyName, className);
            } catch (GraphCycleProhibitedException e) {
            }
        }
    }
    private def generateClass(Data rosettaClass, String nameSpace, String version) {
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
        imports.addAll(importsFound);
        expressionGenerator.importsFound = new ArrayList<String>(importsFound);
        val classDefinition = generateBody(rosettaClass)

        var _builder = new StringConcatenation();
        _builder.append(classDefinition)
        return classDefinition;
    }

	private def String getClassMetaDataString(Data rosettaClass) {
	    // generate _ALLOWED_METADATA string for the type
	    val rcRData = rObjectFactory.buildRDataType(rosettaClass)
    	val isFirstArray = newArrayList(true)
	    val _builder = new StringConcatenation
	
	    rcRData.metaAttributes.forEach[metaData |
	        if (isFirstArray.get(0)) {
	            isFirstArray.set(0, false)
	            _builder.append("_ALLOWED_METADATA = {")
	        } else {
	            _builder.append(", ")
	        }
	        switch metaData.name {
	            case "key": _builder.append("'@key', '@key:external'")
	            case "scheme": _builder.append("'@scheme'")
	        }
	    ]
	    if (!isFirstArray.get(0)) {
	        _builder.append("}")
	        _builder.newLine
	    }
	    return _builder.toString
	}

    private def keyRefConstraintsToString (Map<String, List<String>> keyRefConstraints) {
        var _builder = new StringConcatenation();
        if (!keyRefConstraints.isEmpty()) {
            _builder.newLine()
            _builder.append("_KEY_REF_CONSTRAINTS = {\n");
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
    private def String getFullyQualifiedName (Data rosettaClass) {
        val model = rosettaClass.eContainer as RosettaModel
        return model.getName() + "." + rosettaClass.getName()
    }
    private def getBundleClassName (Data rosettaClass) {
        return getFullyQualifiedName(rosettaClass).replace(".", "_")
    }
    private def generateBody(Data rosettaClass) {
        // generate the main body of the class
        // ... first generate choice aliases
        // ... then add the class definition
        // ... then add all attributes
        // ... then add any conditions
        val rosettaDataType = rosettaClass.buildRDataType
        val choiceAliasesAsAString = pythonChoiceAliasProcessor.generateChoiceAliasesAsString(rosettaDataType);
        val keyRefConstraints = new HashMap<String, List<String>> ();
        return
        '''
        class «getBundleClassName(rosettaClass)»(«(rosettaClass.superType !== null) ? getBundleClassName(rosettaClass.superType) : "BaseDataClass"»):
            «getClassMetaDataString(rosettaClass)»
            «choiceAliasesAsAString»
            «IF rosettaClass.definition !== null»
                """
                «rosettaClass.definition»
                """
            «ENDIF»
            _FQRTN = '«getFullyQualifiedName(rosettaClass)»'
            «pythonAttributeProcessor.generateAllAttributes(rosettaClass, keyRefConstraints)»
            «IF !keyRefConstraints.isEmpty()»
                «keyRefConstraintsToString(keyRefConstraints)»
            «ENDIF»
            «expressionGenerator.generateTypeOrFunctionConditions(rosettaClass)»
        '''
    }
}
