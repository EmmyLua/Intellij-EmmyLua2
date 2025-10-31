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

import com.intellij.openapi.application.ReadAction
import com.intellij.ui.ColoredTextContainer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.frame.XCompositeNode
import com.intellij.xdebugger.frame.XStackFrame
import com.intellij.xdebugger.frame.XValueChildrenList
import com.intellij.xdebugger.impl.XSourcePositionImpl
import com.tang.intellij.lua.debugger.LuaDebugVariableContext
import com.tang.intellij.lua.debugger.emmy.value.LuaXValue
import com.tang.intellij.lua.psi.LuaFileUtil
import java.util.concurrent.atomic.AtomicBoolean

class EmmyDebugStackFrame(val data: Stack, val process: EmmyDebugProcessBase) : XStackFrame() {
    private val values = XValueChildrenList()
    private var evaluator: EmmyEvaluator? = null
    private var _sourcePosition: XSourcePosition? = null
    private val sourcePositionInitialized = java.util.concurrent.atomic.AtomicBoolean(false)
    
    // Variable context for inline values
    private var variableContext: LuaDebugVariableContext? = null

    init {
        data.localVariables.forEach {
            addValue(LuaXValue.create(it, this))
        }
        data.upvalueVariables.forEach {
            addValue(LuaXValue.create(it, this))
        }
    }

    override fun getEvaluator(): EmmyEvaluator? {
        if (evaluator == null)
            evaluator = EmmyEvaluator(this, process)
        return evaluator
    }

    override fun customizePresentation(component: ColoredTextContainer) {
        component.append("${data.file}:${data.functionName}:${data.line}", SimpleTextAttributes.REGULAR_ATTRIBUTES)
    }

    private fun addValue(node: LuaXValue) {
        values.add(node.name, node)
    }

    override fun computeChildren(node: XCompositeNode) {
        node.addChildren(values, true)
    }

    override fun getSourcePosition(): XSourcePosition? {
        // Initialize source position with read access
        if (!sourcePositionInitialized.get()) {
            com.intellij.openapi.application.ReadAction.run<RuntimeException> {
                if (!sourcePositionInitialized.get()) {
                    _sourcePosition = try {
                        val file = LuaFileUtil.findFile(process.session.project, data.file)
                        if (file == null) null else XSourcePositionImpl.create(file, data.line - 1)
                    } catch (e: Exception) {
                        null
                    }
                    sourcePositionInitialized.set(true)
                }
            }
        }
        return _sourcePosition
    }
    
    /**
     * Get or create variable context for inline value support
     */
    fun getVariableContext(): LuaDebugVariableContext {
        if (variableContext == null) {
            variableContext = LuaDebugVariableContext(this)
            variableContext!!.configureContext()
        }
        return variableContext!!
    }
    
    /**
     * Get source position for a variable value
     */
    fun getSourcePositionFor(value: LuaXValue): XSourcePosition? {
        return getVariableContext().getSourcePositionFor(value)
    }
}