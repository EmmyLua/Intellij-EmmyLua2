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

package com.tang.intellij.lua.debugger.emmy.value

import com.intellij.icons.AllIcons
import com.intellij.xdebugger.frame.*
import com.tang.intellij.lua.debugger.LuaDebugVariableContext
import com.tang.intellij.lua.debugger.LuaXBoolPresentation
import com.tang.intellij.lua.debugger.LuaXNumberPresentation
import com.tang.intellij.lua.debugger.LuaXStringPresentation
import com.tang.intellij.lua.debugger.emmy.EmmyDebugProcess
import com.tang.intellij.lua.debugger.emmy.EmmyDebugStackFrame
import com.tang.intellij.lua.debugger.model.DebugVariable
import com.tang.intellij.lua.debugger.model.LuaValueType
import com.tang.intellij.lua.lang.LuaIcons

/**
 * Factory for creating XValue instances from debug variables
 */
object LuaXValueFactory {
    fun create(variable: DebugVariable, frame: EmmyDebugStackFrame): LuaXValue {
        return when (variable.valueTypeValue) {
            LuaValueType.TSTRING -> StringXValue(variable, frame)
            LuaValueType.TNUMBER -> NumberXValue(variable, frame)
            LuaValueType.TBOOLEAN -> BoolXValue(variable, frame)
            LuaValueType.TTABLE, LuaValueType.TUSERDATA -> TableXValue(variable, frame)
            LuaValueType.GROUP -> GroupXValue(variable, frame)
            else -> SimpleXValue(variable, frame)
        }
    }
}

/**
 * Base class for Lua debug values
 */
sealed class LuaXValue(val variable: DebugVariable) : XValue() {

    val name: String get() = variable.displayName

    var parent: LuaXValue? = null

    // Lazy variable context for inline values support
    protected val variableContext: LuaDebugVariableContext? by lazy {
        (stackFrame() as? EmmyDebugStackFrame)?.let { frame ->
            frame.getVariableContext()
        }
    }

    companion object {
        fun create(variable: DebugVariable, frame: EmmyDebugStackFrame): LuaXValue {
            return LuaXValueFactory.create(variable, frame)
        }
    }

    /**
     * Get the stack frame this value belongs to
     */
    protected abstract fun stackFrame(): Any?

    /**
     * Get evaluation expression for inline values
     */
    override fun getEvaluationExpression(): String {
        return name
    }

    /**
     * Compute source position for inline values
     */
    override fun computeSourcePosition(callback: XNavigatable) {
        println("LuaXValue.computeSourcePosition: Computing for variable '${name}'")

        val ctx = variableContext
        if (ctx == null) {
            println("LuaXValue.computeSourcePosition: variableContext is null for '${name}'")
            return
        }

        val position = ctx.getSourcePosition(name)
        if (position != null) {
            println("LuaXValue.computeSourcePosition: Found position for '${name}': line ${position.line}")
            callback.setSourcePosition(position)
        } else {
            println("LuaXValue.computeSourcePosition: No position found for '${name}'")
        }
    }

    /**
     * Build full expression path for eval
     */
    protected fun buildExpressionPath(): String {
        val parts = mutableListOf<String>()
        var current: LuaXValue? = this

        while (current != null) {
            if (!current.variable.isFake) {
                val name = current.name
                parts.add(if (name.startsWith("[")) name else name)
            }
            current = current.parent
        }

        if (parts.isEmpty()) return name

        val rootName = parts.removeLast()
        val sb = StringBuilder(rootName)

        for (part in parts.reversed()) {
            if (part.startsWith("[")) {
                sb.append(part)
            } else {
                sb.append("[\"$part\"]")
            }
        }

        return sb.toString()
    }
}

/**
 * Simple value (nil, function, thread, etc.)
 */
class SimpleXValue(
    variable: DebugVariable,
    private val frame: EmmyDebugStackFrame
) : LuaXValue(variable) {

    override fun stackFrame() = frame

    override fun computePresentation(node: XValueNode, place: XValuePlace) {
        node.setPresentation(null, variable.valueTypeName, variable.value, false)
    }
}

/**
 * String value
 */
class StringXValue(
    variable: DebugVariable,
    private val frame: EmmyDebugStackFrame
) : LuaXValue(variable) {

    override fun stackFrame() = frame

    override fun computePresentation(node: XValueNode, place: XValuePlace) {
        node.setPresentation(null, LuaXStringPresentation(variable.value), false)
    }
}

/**
 * Number value
 */
class NumberXValue(
    variable: DebugVariable,
    private val frame: EmmyDebugStackFrame
) : LuaXValue(variable) {

    override fun stackFrame() = frame

    override fun computePresentation(node: XValueNode, place: XValuePlace) {
        node.setPresentation(null, LuaXNumberPresentation(variable.value), false)
    }
}

/**
 * Boolean value
 */
class BoolXValue(
    variable: DebugVariable,
    private val frame: EmmyDebugStackFrame
) : LuaXValue(variable) {

    override fun stackFrame() = frame

    override fun computePresentation(node: XValueNode, place: XValuePlace) {
        node.setPresentation(null, LuaXBoolPresentation(variable.value), false)
    }
}

/**
 * Group value (for organizing variables)
 */
class GroupXValue(
    variable: DebugVariable,
    private val frame: EmmyDebugStackFrame
) : LuaXValue(variable) {

    override fun stackFrame() = frame

    private val children: List<LuaXValue> by lazy {
        variable.children?.sortedWith(
            compareBy(
            { if (it.isFake) 0 else 1 },
            { it.displayName }
        ))?.map { LuaXValueFactory.create(it, frame) } ?: emptyList()
    }

    override fun computePresentation(node: XValueNode, place: XValuePlace) {
        node.setPresentation(
            AllIcons.Nodes.UpLevel,
            variable.valueTypeName,
            variable.value,
            true
        )
    }

    override fun computeChildren(node: XCompositeNode) {
        val list = XValueChildrenList()
        children.forEach { child ->
            child.parent = this
            list.add(child.name, child)
        }
        node.addChildren(list, true)
    }
}

/**
 * Table/userdata value
 */
class TableXValue(
    variable: DebugVariable,
    private val frame: EmmyDebugStackFrame
) : LuaXValue(variable) {

    override fun stackFrame() = frame

    private val children: List<LuaXValue> by lazy {
        variable.children?.sortedWith(
            compareBy(
            { if (it.isFake) 0 else 1 },
            { it.displayName }
        ))?.map { LuaXValueFactory.create(it, frame) } ?: emptyList()
    }

    override fun computePresentation(node: XValueNode, place: XValuePlace) {
        val icon = when (variable.valueTypeName) {
            "C#" -> LuaIcons.CSHARP
            "C++" -> LuaIcons.CPP
            else -> AllIcons.Json.Object
        }

        node.setPresentation(icon, variable.valueTypeName, variable.value, true)
    }

    override fun computeChildren(node: XCompositeNode) {
        // If children are already loaded, display them
        if (variable.hasChildren && children.isNotEmpty()) {
            val list = XValueChildrenList()
            children.forEach { child ->
                child.parent = this
                list.add(child.name, child)
            }
            node.addChildren(list, true)
            return
        }

        // Otherwise, need to evaluate to get children
        val expression = buildExpressionPath()

        frame.process.evaluate(
            expression,
            frame.stackData.level,
            variable.cacheId,
            2, // depth
            object : EmmyDebugProcess.EvalHandler {
                override fun onSuccess(variable: DebugVariable) {
                    val list = XValueChildrenList()
                    variable.children?.sortedWith(
                        compareBy(
                        { if (it.isFake) 0 else 1 },
                        { it.displayName }
                    ))?.forEach { child ->
                        val childValue = LuaXValueFactory.create(child, frame)
                        childValue.parent = this@TableXValue
                        list.add(childValue.name, childValue)
                    }
                    node.addChildren(list, true)
                }

                override fun onError(error: String) {
                    node.setErrorMessage(error)
                }
            }
        )
    }
}
