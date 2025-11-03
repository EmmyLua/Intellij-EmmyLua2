/*
 * Copyright (c) 2017. tangzx(love.tangzx@qq.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tang.intellij.lua.debugger.emmy

import com.intellij.xdebugger.evaluation.XDebuggerEvaluator
import com.tang.intellij.lua.debugger.emmy.value.LuaXValueFactory

/**
 * Evaluator for Emmy debugger - evaluates expressions in the debug context
 */
class EmmyEvaluator(
    private val frame: EmmyDebugStackFrame,
    private val process: EmmyDebugProcess
) : XDebuggerEvaluator() {
    
    override fun evaluate(
        expression: String,
        callback: XEvaluationCallback,
        expressionPosition: com.intellij.xdebugger.XSourcePosition?
    ) {
        evaluate(expression, 0, callback)
    }
    
    /**
     * Evaluate expression with custom cache ID
     */
    fun evaluate(
        expression: String,
        cacheId: Int,
        callback: XEvaluationCallback
    ) {
        process.evaluate(
            expression,
            frame.stackData.level,
            cacheId,
            1, // depth
            object : EmmyDebugProcess.EvalHandler {
                override fun onSuccess(variable: com.tang.intellij.lua.debugger.model.DebugVariable) {
                    val value = LuaXValueFactory.create(variable, frame)
                    callback.evaluated(value)
                }
                
                override fun onError(error: String) {
                    callback.errorOccurred(error)
                }
            }
        )
    }
}
