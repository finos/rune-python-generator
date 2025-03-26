package com.regnosys.rosetta.generator.python.expressions
import com.regnosys.rosetta.generator.java.enums.EnumHelper
import com.regnosys.rosetta.rosetta.RosettaCallableWithArgs
import com.regnosys.rosetta.rosetta.RosettaEnumValue
import com.regnosys.rosetta.rosetta.RosettaEnumValueReference
import com.regnosys.rosetta.rosetta.RosettaEnumeration
import com.regnosys.rosetta.rosetta.RosettaFeature
import com.regnosys.rosetta.rosetta.RosettaMetaType
import com.regnosys.rosetta.rosetta.RosettaModel
import com.regnosys.rosetta.rosetta.expression.AsKeyOperation
import com.regnosys.rosetta.rosetta.expression.ChoiceOperation
import com.regnosys.rosetta.rosetta.expression.ClosureParameter
import com.regnosys.rosetta.rosetta.expression.DistinctOperation
import com.regnosys.rosetta.rosetta.expression.FilterOperation
import com.regnosys.rosetta.rosetta.expression.FirstOperation
import com.regnosys.rosetta.rosetta.expression.FlattenOperation
// import com.regnosys.rosetta.rosetta.expression.InlineFunction
import com.regnosys.rosetta.rosetta.expression.LastOperation
import com.regnosys.rosetta.rosetta.expression.ListLiteral
import com.regnosys.rosetta.rosetta.expression.MapOperation
import com.regnosys.rosetta.rosetta.expression.ModifiableBinaryOperation
import com.regnosys.rosetta.rosetta.expression.Necessity
import com.regnosys.rosetta.rosetta.expression.OneOfOperation
import com.regnosys.rosetta.rosetta.expression.RosettaAbsentExpression
import com.regnosys.rosetta.rosetta.expression.RosettaBinaryOperation
import com.regnosys.rosetta.rosetta.expression.RosettaBooleanLiteral
import com.regnosys.rosetta.rosetta.expression.RosettaConditionalExpression
import com.regnosys.rosetta.rosetta.expression.RosettaConstructorExpression
import com.regnosys.rosetta.rosetta.expression.RosettaCountOperation
import com.regnosys.rosetta.rosetta.expression.RosettaExistsExpression
import com.regnosys.rosetta.rosetta.expression.RosettaExpression
import com.regnosys.rosetta.rosetta.expression.RosettaFeatureCall
import com.regnosys.rosetta.rosetta.expression.RosettaImplicitVariable
import com.regnosys.rosetta.rosetta.expression.RosettaIntLiteral
import com.regnosys.rosetta.rosetta.expression.RosettaNumberLiteral
import com.regnosys.rosetta.rosetta.expression.RosettaOnlyElement
import com.regnosys.rosetta.rosetta.expression.RosettaOnlyExistsExpression
import com.regnosys.rosetta.rosetta.expression.RosettaReference
import com.regnosys.rosetta.rosetta.expression.RosettaStringLiteral
import com.regnosys.rosetta.rosetta.expression.RosettaSymbolReference
import com.regnosys.rosetta.rosetta.expression.SortOperation
import com.regnosys.rosetta.rosetta.expression.SumOperation
import com.regnosys.rosetta.rosetta.expression.ThenOperation
import com.regnosys.rosetta.rosetta.expression.ToStringOperation
import com.regnosys.rosetta.rosetta.expression.ToEnumOperation
import com.regnosys.rosetta.rosetta.expression.RosettaDeepFeatureCall
import com.regnosys.rosetta.rosetta.expression.MinOperation
import com.regnosys.rosetta.rosetta.expression.MaxOperation
import com.regnosys.rosetta.rosetta.expression.SwitchOperation
import com.regnosys.rosetta.rosetta.simple.Attribute
import com.regnosys.rosetta.rosetta.simple.Condition
import com.regnosys.rosetta.rosetta.simple.Data
import com.regnosys.rosetta.rosetta.simple.ShortcutDeclaration
import com.regnosys.rosetta.rosetta.simple.impl.FunctionImpl
import java.util.ArrayList
import java.util.List

class PythonExpressionGenerator {

    public var List<String> importsFound
    public var ifCondBlocks = new ArrayList<String>()
    public var switchCondBlocks = new ArrayList<String>()

    def String generateConditions(Data cls) {
        var n_condition = 0;
        var res = '';
        for (Condition cond : cls.conditions) {
            res += generateConditionBoilerPlate(cond, n_condition)
            if (cond.isConstraintCondition)
                res += generateConstraintCondition(cls, cond)
            else
                res += generateExpressionCondition(cond)
            n_condition += 1;
        }
        return res
    }

    def generateFunctionConditions(List<Condition> conditions, String condition_type) {
        var n_condition = 0;
        var res = '';
        for (Condition cond : conditions) {
            res += generateFunctionConditionBoilerPlate(cond, n_condition, condition_type)
            res += generateExpressionCondition(cond)
            n_condition += 1;
        }

        return res
    }

    def generateExpressionThenElse(RosettaExpression expr, List<Integer> iflvl) {
        ifCondBlocks = new ArrayList<String>()
        generateExpression(expr, iflvl.get(0), false)
        var blocks = ""
        if (!ifCondBlocks.isEmpty()) {
            iflvl.set(0, iflvl.get(0) + 1)
            blocks = '''    «FOR arg : ifCondBlocks»«arg»«ENDFOR»'''
        }
        return '''«blocks»'''
    }

    def String generateExpression(RosettaExpression expr, int iflvl, boolean isLambda) {
        switch (expr) {
            // literals
            RosettaNumberLiteral: '''«expr.value»'''
            RosettaIntLiteral: '''«expr.value»'''
            RosettaStringLiteral: '''"«expr.value»"'''
            RosettaBooleanLiteral: expr.value.toString().equals("true") ? "True" : "False"
            // xText operations
            AsKeyOperation: '''{«generateExpression(expr.argument, iflvl, isLambda)»: True}'''
            DistinctOperation: '''set(«generateExpression(expr.argument, iflvl, isLambda)»)'''
            FilterOperation: generateFilterOperation(expr, iflvl, isLambda)
            FirstOperation: '''«generateExpression(expr.argument, iflvl, isLambda)»[0]'''
            FlattenOperation: '''rune_flatten_list(«generateExpression(expr.argument, iflvl, isLambda)»)'''
            ListLiteral: '''[«FOR arg : expr.elements SEPARATOR ', '»«generateExpression(arg, iflvl,isLambda)»«ENDFOR»]'''
            LastOperation: '''«generateExpression(expr.argument, iflvl, isLambda)»[-1]'''
            MapOperation: generateMapOperation(expr, iflvl, isLambda)
            MaxOperation: '''max(«generateExpression(expr.getArgument(), iflvl, isLambda)»)'''
            MinOperation: '''min(«generateExpression(expr.getArgument(), iflvl, isLambda)»)'''
            SortOperation: '''sorted(«generateExpression(expr.argument, iflvl, isLambda)»)'''            ThenOperation: generateThenOperation(expr, iflvl, isLambda)
            SumOperation: '''sum(«generateExpression(expr.argument, iflvl, isLambda)»)'''
            SwitchOperation: generateSwitchOperation(expr, iflvl, isLambda)
            ToEnumOperation: '''«expr.enumeration.name»(«generateExpression(expr.argument, iflvl, isLambda)»)'''
            ToStringOperation: '''rune_str(«generateExpression(expr.argument, iflvl, isLambda)»)'''
            // Rune Operations
            RosettaAbsentExpression: '''(not rune_attr_exists(«generateExpression(expr.argument, iflvl, isLambda)»))'''
            RosettaBinaryOperation: generateBinaryExpression(expr, iflvl, isLambda)
            RosettaConditionalExpression: generateConditionalExpression(expr, iflvl, isLambda)
            RosettaDeepFeatureCall: '''rune_resolve_deep_attr(self, "«expr.feature.name»")'''
            RosettaEnumValueReference: '''«expr.enumeration».«EnumHelper.convertValue(expr.value)»'''
            RosettaExistsExpression: '''rune_attr_exists(«generateExpression(expr.argument, iflvl, isLambda)»)'''
            RosettaFeatureCall: generateFeatureCall(expr, iflvl, isLambda)
            RosettaOnlyElement: '''rune_get_only_element(«generateExpression(expr.argument, iflvl, isLambda)»)'''
            RosettaReference: generateReference(expr, iflvl, isLambda)
            RosettaOnlyExistsExpression: '''rune_check_one_of(self, «generateExpression(expr.getArgs().get(0), iflvl, isLambda)»)'''
            RosettaCountOperation: '''rune_count(«generateExpression(expr.argument, iflvl,isLambda)»)'''
            RosettaConstructorExpression: generateConstructorExpression(expr, iflvl, isLambda)
            default:{
                throw new UnsupportedOperationException("Unsupported expression type of " + expr?.class?.simpleName)
            }
        }
    }

    private def boolean isConstraintCondition(Condition cond) {
        return isOneOf(cond) || isChoice(cond)
    }

    private def boolean isOneOf(Condition cond) {
        return cond.expression instanceof OneOfOperation
    }

    private def boolean isChoice(Condition cond) {
        return cond.expression instanceof ChoiceOperation
    }

    private def generateConditionBoilerPlate(Condition cond, int n_condition) {
        '''
            
            @rune_condition
            def condition_«n_condition»_«cond.name»(self):
                «IF cond.definition!==null»
                    """
                    «cond.definition»
                    """
                «ENDIF»
                item = self
        '''
    }

    private def generateFunctionConditionBoilerPlate(Condition cond, int n_condition, String condition_type) {
        '''
            
            @rune_local_condition(«condition_type»)
            def condition_«n_condition»_«cond.name»(self):
                «IF cond.definition!==null»
                    """
                    «cond.definition»
                    """
                «ENDIF»
        '''
    }

    private def generateConstraintCondition(Data cls, Condition cond) {
        val expression = cond.expression
        var attributes = cls.attributes
        var necessity = "necessity=True"
        if (expression instanceof ChoiceOperation) {
            attributes = expression.attributes
            if (expression.necessity == Necessity.OPTIONAL) {
                necessity = "necessity=False"
            }
        }
        '''    return rune_check_one_of(self, «FOR a : attributes SEPARATOR ", "»'«a.name»'«ENDFOR», «necessity»)
        '''
    }

    private def generateExpressionCondition(Condition c) {
        ifCondBlocks = new ArrayList<String>()
        switchCondBlocks = new ArrayList<String>()
        var expr = generateExpression(c.expression, 0, false)
        var blocks = ""
        var switch_blocks = ""
        if (!ifCondBlocks.isEmpty()) {
            blocks = '''    «FOR arg : ifCondBlocks»«arg»«ENDFOR»'''
        }
        if (!switchCondBlocks.isEmpty()) {
            switch_blocks = '''    «FOR arg : switchCondBlocks»«arg»«ENDFOR»'''
        }
        if (switch_blocks.equals(""))
            return '''«blocks»    return «expr»
            '''
        else
            return '''«switch_blocks»    «expr»'''
    }

    private def String generateConditionalExpression(RosettaConditionalExpression expr, int iflvl, boolean isLambda) {
        val ifExpr = generateExpression(expr.getIf(), iflvl + 1, isLambda)
        val ifThen = generateExpression(expr.ifthen, iflvl + 1, isLambda)
        val elseThen = (expr.elsethen !== null && expr.full) ? generateExpression(expr.elsethen, iflvl + 1, isLambda) : 'True'
        val ifBlocks = '''
            def _then_fn«iflvl»():
                return «ifThen»
            
            def _else_fn«iflvl»():
                return «elseThen»
            
        '''
        ifCondBlocks.add(ifBlocks)
        '''if_cond_fn(«ifExpr», _then_fn«iflvl», _else_fn«iflvl»)'''
    }

    private def String generateFeatureCall(RosettaFeatureCall expr, int iflvl, boolean isLambda) {
        if (expr.feature instanceof RosettaEnumValue) {
            val symbol = (expr.receiver as RosettaSymbolReference).symbol
            val model = symbol.eContainer as RosettaModel
            addImportsFromConditions(symbol.name, model.name)
            return generateEnumString(expr.feature as RosettaEnumValue)
        }
        var right = expr.feature.name
        if (right == "None") 
            right = "NONE"
        var receiver = generateExpression(expr.receiver, iflvl, isLambda)
        return (receiver === null) ? '''«right»''' : '''rune_resolve_attr(«receiver», "«right»")'''
    }

    private def String generateThenOperation(ThenOperation expr, int iflvl, boolean isLambda) {
        val funcExpr = expr.function
        val argExpr = generateExpression(expr.argument, iflvl, isLambda)
        val body = generateExpression(funcExpr.body, iflvl, true)
        val funcParams = funcExpr.parameters.map[it.name].join(", ")
        val lambdaFunction = (funcParams.empty) ? '''(lambda item: «body»)''' : '''(lambda «funcParams»: «body»)'''
        return '''«lambdaFunction»(«argExpr»)'''
    }

    private def String generateFilterOperation(FilterOperation expr, int iflvl, boolean isLambda) {
        val argument = generateExpression(expr.argument, iflvl, isLambda)
        val filterExpression = generateExpression(expr.function.body, iflvl, true)
        return '''rune_filter(«argument», lambda item: «filterExpression»)'''
    }

    private def String generateMapOperation(MapOperation expr, int iflvl, boolean isLambda) {
        val inlineFunc = expr.function
        val funcBody = generateExpression(inlineFunc.body, iflvl, true)
        val lambdaFunction = "lambda item: " + funcBody
        val argument = generateExpression(expr.argument, iflvl, isLambda)
        return '''list(map(«lambdaFunction», «argument»))'''
    }

    private def String generateConstructorExpression(RosettaConstructorExpression expr, int iflvl, boolean isLambda) {
        val type = expr.typeCall?.type?.name
        val keyValuePairs = expr.values
        if (type !== null) {
            '''«type»(«FOR pair : keyValuePairs SEPARATOR ', '»«pair.key.name»=«generateExpression(pair.value, iflvl, isLambda)»«ENDFOR»)'''
        } else {
            '''{«FOR pair : keyValuePairs SEPARATOR ', '»'«pair.key.name»': «generateExpression(pair.value, iflvl, isLambda)»«ENDFOR»}'''
        }
    }

    private def String generateSwitchOperation(SwitchOperation expr, int iflvl, boolean isLambda) {
        val attr = generateExpression(expr.argument, 0, isLambda)
        var funcNames = new ArrayList<String>()
        for (thenExpr : expr.cases) {
            val thenExprDef = generateExpression(thenExpr.getExpression(), iflvl + 1, isLambda)
            val funcName = '''_then_«generateExpression(thenExpr.getGuard().getLiteralGuard(),0,isLambda)»'''
            funcNames.add(funcName)
            val blockThen = '''
                def «funcName»():
                    return «thenExprDef»
            '''
            switchCondBlocks.add(blockThen)
        }
        val defaultExprDef = generateExpression(expr.getDefault(), 0, isLambda)
        val defaultFuncName = '''_then_default'''
        funcNames.add(defaultFuncName)
        val blockDefaultThen = 
        '''
            def «defaultFuncName»():
                return «defaultExprDef»
        '''
        switchCondBlocks.add(blockDefaultThen)
        return
        '''
        match «attr»:
                «FOR i : 0 ..< expr.cases.length»
                case «generateExpression(expr.cases.get(i).getGuard().getLiteralGuard(), 0, isLambda)»:
                    return «funcNames.get(i)»()
                «ENDFOR»
                case _:
                    return «funcNames.get(funcNames.size - 1)»()
        '''        
    }

    private def String generateReference(RosettaReference expr, int iflvl, boolean isLambda) {
        switch (expr) {
            RosettaImplicitVariable: '''«expr.name»'''
            RosettaSymbolReference:  generateSymbolReference(expr, iflvl, isLambda)
        }
    }

    private def String  generateSymbolReference(RosettaSymbolReference expr, int iflvl, boolean isLambda) {
        val symbol = expr.symbol
        switch (symbol) {
            Data, RosettaEnumeration: '''«symbol.name»'''
            Attribute: generateAttributeReference(symbol, isLambda)
            RosettaEnumValue: generateEnumString(symbol)
            RosettaCallableWithArgs: generateCallableWithArgsCall(symbol, expr, iflvl, isLambda)
            ShortcutDeclaration, ClosureParameter: '''rune_resolve_attr(self, "«symbol.name»")'''
            default: throw new UnsupportedOperationException("Unsupported symbol reference for: " + symbol.class.simpleName)
        }
    }

    private def String generateAttributeReference(Attribute s, boolean isLambda) {
        if (isLambda) {
            var notInput = true
            if (s.eContainer instanceof FunctionImpl) {
                var FunctionImpl c = s.eContainer as FunctionImpl
                for (inputAttr : c.inputs) {
                    if (inputAttr.name.equals(s.name)) {
                        notInput = false
                    }
                }
            }
            return (notInput) ? '''rune_resolve_attr(item, "«s.name»")''' : '''rune_resolve_attr(self, "«s.name»")'''
        } else {
            return '''rune_resolve_attr(self, "«s.name»")'''
        }
    }

	private def String generateEnumString (RosettaEnumValue rev) {
		// translate the enum value to a fully qualified name as long as the value is not None
		
		val value = EnumHelper.convertValue(rev)
		val parent = rev.getEnumeration()
		val parentName = parent.getName()
		val modelName = parent.getModel().getName()
        return '''«modelName».«parentName».«parentName».«value»'''
	} 

    private def String generateCallableWithArgsCall(RosettaCallableWithArgs s, RosettaSymbolReference expr, int iflvl, boolean isLambda) {
        if (s instanceof FunctionImpl)
            addImportsFromConditions(s.getName(), (s.eContainer as RosettaModel).name + "." + "functions")
        else
            addImportsFromConditions(s.name, (s.eContainer as RosettaModel).name)
        var args = '''«FOR arg : expr.args SEPARATOR ', '»«generateExpression(arg, iflvl, isLambda)»«ENDFOR»'''
        '''«s.name»(«args»)'''
    }

    private def String generateBinaryExpression(RosettaBinaryOperation expr, int iflvl, boolean isLambda) {
        if (expr instanceof ModifiableBinaryOperation) {
            if (expr.cardMod === null) {
                throw new UnsupportedOperationException("ModifiableBinaryOperation with expressions with no cardinality")
            }
            if (expr.operator == "<>") {
                '''rune_any_elements(«generateExpression(expr.left, iflvl,isLambda)», "«expr.operator»", «generateExpression(expr.right, iflvl, isLambda)»)'''
            } else {
                '''rune_all_elements(«generateExpression(expr.left, iflvl, isLambda)», "«expr.operator»", «generateExpression(expr.right, iflvl, isLambda)»)'''
            } 
        } else {
            switch expr.operator {
                case ("="): '''(«generateExpression(expr.left, iflvl, isLambda)» == «generateExpression(expr.right, iflvl, isLambda)»)'''
                case ("<>"): '''(«generateExpression(expr.left, iflvl, isLambda)» != «generateExpression(expr.right, iflvl, isLambda)»)'''
                case ("contains"): '''rune_contains(«generateExpression(expr.left, iflvl, isLambda)», «generateExpression(expr.right, iflvl, isLambda)»)'''
                case ("disjoint"): '''rune_disjoint(«generateExpression(expr.left, iflvl,isLambda)», «generateExpression(expr.right, iflvl,isLambda)»)'''
                case ("join"): '''«generateExpression(expr.left, iflvl, isLambda)».join(«generateExpression(expr.right, iflvl, isLambda)»)'''
                default: '''(«generateExpression(expr.left, iflvl, isLambda)» «expr.operator» «generateExpression(expr.right, iflvl, isLambda)»)'''
            }
        }
    }

    def addImportsFromConditions(String variable, String namespace) {
        val import = '''from «namespace».«variable» import «variable»'''
        if (importsFound !== null && !importsFound.contains(import)) {
            importsFound.add(import)
        }
    }
}
