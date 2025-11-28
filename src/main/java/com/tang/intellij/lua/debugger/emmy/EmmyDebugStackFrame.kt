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
import com.tang.intellij.lua.debugger.model.DebugStackFrame
import com.tang.intellij.lua.psi.LuaFileUtil

/**
 * Stack frame for Emmy debugger - represents one frame in the call stack
 */
class EmmyDebugStackFrame(
    val stackData: DebugStackFrame,
    val process: EmmyDebugProcess
) : XStackFrame() {

    private val evaluator = EmmyEvaluator(this, process)
    private var sourcePosition: XSourcePosition? = null
    private var sourcePositionInitialized = false

    // Variable context for inline values
    private var variableContext: LuaDebugVariableContext? = null

    override fun getEvaluator() = evaluator

    /**
     * Get or create variable context for inline values
     */
    fun getVariableContext(): LuaDebugVariableContext {
        if (variableContext == null) {
            variableContext = LuaDebugVariableContext(this)
        }
        return variableContext!!
    }

    override fun customizePresentation(component: ColoredTextContainer) {
        val fileName = stackData.file.substringAfterLast('/')
            .substringAfterLast('\\')
        component.append(
            "$fileName:${stackData.functionName}:${stackData.line}",
            SimpleTextAttributes.REGULAR_ATTRIBUTES
        )
    }

    override fun computeChildren(node: XCompositeNode) {
        val children = XValueChildrenList()

        // Add local variables
        stackData.localVariables.forEach { variable ->
            val value = LuaXValue.create(variable, this)
            children.add(value.name, value)
        }

        // Add upvalues
        stackData.upvalueVariables.forEach { variable ->
            val value = LuaXValue.create(variable, this)
            children.add(value.name, value)
        }

        node.addChildren(children, true)
    }

    override fun getSourcePosition(): XSourcePosition? {
        if (!sourcePositionInitialized) {
            sourcePosition = ReadAction.compute<XSourcePosition?, RuntimeException> {
                try {
                    val sourceRoots = process.getSourceRoots()
                    val file = LuaFileUtil.findFile(process.session.project, stackData.file, sourceRoots)
                    if (file != null) {
                        XSourcePositionImpl.create(file, stackData.line - 1)
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    null
                }
            }
            sourcePositionInitialized = true
        }
        return sourcePosition
    }
}

