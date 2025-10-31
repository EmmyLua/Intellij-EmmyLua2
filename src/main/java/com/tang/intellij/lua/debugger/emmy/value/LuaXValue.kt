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
import com.intellij.util.ThreeState
import com.intellij.xdebugger.evaluation.XDebuggerEvaluator
import com.intellij.xdebugger.frame.*
import com.tang.intellij.lua.debugger.LuaXBoolPresentation
import com.tang.intellij.lua.debugger.LuaXNumberPresentation
import com.tang.intellij.lua.debugger.LuaXStringPresentation
import com.tang.intellij.lua.debugger.emmy.EmmyDebugStackFrame
import com.tang.intellij.lua.debugger.emmy.LuaValueType
import com.tang.intellij.lua.debugger.emmy.VariableValue
import com.tang.intellij.lua.lang.LuaIcons
import java.util.*

abstract class LuaXValue(val value: VariableValue) : XValue() {
    companion object {
        fun create(v: VariableValue, frame: EmmyDebugStackFrame): LuaXValue {
            return when(v.valueTypeValue) {
                LuaValueType.TSTRING -> StringXValue(v, frame)
                LuaValueType.TNUMBER -> NumberXValue(v, frame)
                LuaValueType.TBOOLEAN -> BoolXValue(v, frame)
                LuaValueType.TUSERDATA,
                LuaValueType.TTABLE -> TableXValue(v, frame)
                LuaValueType.GROUP -> GroupXValue(v, frame)
                else -> AnyXValue(v, frame)
            }
        }
    }

    val name: String get() {
        return value.nameValue
    }

    var parent: LuaXValue? = null
    
    /**
     * Compute inline debugger data for displaying variable values inline in the editor
     * This is called by IntelliJ to determine where to show the variable value
     */
    override fun computeInlineDebuggerData(callback: XInlineDebuggerDataCallback): ThreeState {
        println("LuaXValue.computeInlineDebuggerData: Called for variable '${name}' (${this.javaClass.simpleName})")
        
        try {
            // Compute and provide source position for inline display
            var hasValidPosition = false
            computeSourcePosition { position ->
                println("LuaXValue.computeInlineDebuggerData: Received position: $position for '${name}'")
                if (position != null) {
                    try {
                        callback.computed(position)
                        hasValidPosition = true
                        println("LuaXValue.computeInlineDebuggerData: Successfully called callback.computed() for '${name}' at $position")
                    } catch (e: Exception) {
                        println("LuaXValue.computeInlineDebuggerData: Exception calling callback.computed(): ${e.message}")
                        e.printStackTrace()
                    }
                }
            }
            
            // Return YES only if we found at least one valid position
            val result = if (hasValidPosition) ThreeState.YES else ThreeState.NO
            println("LuaXValue.computeInlineDebuggerData: Returning $result for '${name}'")
            return result
        } catch (e: Exception) {
            println("LuaXValue.computeInlineDebuggerData: Exception: ${e.message}")
            e.printStackTrace()
            return ThreeState.NO
        }
    }
    
    /**
     * Compute source position for this variable
     * Delegates to the stack frame to find the variable position
     */
    open fun computeSourcePosition(callback: (com.intellij.xdebugger.XSourcePosition?) -> Unit) {
        // Default implementation - subclasses can override
        callback(null)
    }
}

private object VariableComparator : Comparator<VariableValue> {
    override fun compare(o1: VariableValue, o2: VariableValue): Int {
        val w1 = if (o1.fake) 0 else 1
        val w2 = if (o2.fake) 0 else 1
        if (w1 != w2)
            return w1.compareTo(w2)
        return o1.nameValue.compareTo(o2.nameValue)
    }
}

class StringXValue(v: VariableValue, val frame: EmmyDebugStackFrame) : LuaXValue(v) {
    override fun computePresentation(xValueNode: XValueNode, place: XValuePlace) {
        xValueNode.setPresentation(null, LuaXStringPresentation(value.value), false)
    }
    
    override fun computeSourcePosition(callback: (com.intellij.xdebugger.XSourcePosition?) -> Unit) {
        println("StringXValue.computeSourcePosition: Called for '${name}'")
        try {
            // Get all positions for this variable, use the LAST one (closest to current line)
            val positions = frame.getVariableContext().getAllSourcePositionsFor(this)
            println("StringXValue.computeSourcePosition: Found ${positions.size} positions for '${name}'")
            if (positions.isNotEmpty()) {
                val lastPosition = positions.last()
                println("StringXValue.computeSourcePosition: Using last position: $lastPosition")
                callback(lastPosition)
            } else {
                println("StringXValue.computeSourcePosition: No positions found, calling callback with null")
                callback(null)
            }
        } catch (e: Exception) {
            println("StringXValue.computeSourcePosition: Exception: ${e.message}")
            e.printStackTrace()
            callback(null)
        }
    }
}

class NumberXValue(v: VariableValue, val frame: EmmyDebugStackFrame) : LuaXValue(v) {
    override fun computePresentation(xValueNode: XValueNode, place: XValuePlace) {
        xValueNode.setPresentation(null, LuaXNumberPresentation(value.value), false)
    }
    
    override fun computeSourcePosition(callback: (com.intellij.xdebugger.XSourcePosition?) -> Unit) {
        // Use the LAST occurrence (closest to current line)
        val positions = frame.getVariableContext().getAllSourcePositionsFor(this)
        callback(positions.lastOrNull())
    }
}

class BoolXValue(val v: VariableValue, val frame: EmmyDebugStackFrame) : LuaXValue(v) {
    override fun computePresentation(xValueNode: XValueNode, place: XValuePlace) {
        xValueNode.setPresentation(null, LuaXBoolPresentation(v.value), false)
    }
    
    override fun computeSourcePosition(callback: (com.intellij.xdebugger.XSourcePosition?) -> Unit) {
        // Use the LAST occurrence (closest to current line)
        val positions = frame.getVariableContext().getAllSourcePositionsFor(this)
        callback(positions.lastOrNull())
    }
}

class AnyXValue(val v: VariableValue, val frame: EmmyDebugStackFrame) : LuaXValue(v) {
    override fun computePresentation(xValueNode: XValueNode, place: XValuePlace) {
        xValueNode.setPresentation(null, v.valueTypeName, v.value, false)
    }
    
    override fun computeSourcePosition(callback: (com.intellij.xdebugger.XSourcePosition?) -> Unit) {
        // Use the LAST occurrence (closest to current line)
        val positions = frame.getVariableContext().getAllSourcePositionsFor(this)
        callback(positions.lastOrNull())
    }
}

class GroupXValue(v: VariableValue, val frame: EmmyDebugStackFrame) : LuaXValue(v) {
    private val children = mutableListOf<LuaXValue>()

    init {
        value.children?.
                sortedWith(VariableComparator)?.
                forEach {
                    children.add(create(it, frame))
                }
    }

    override fun computePresentation(xValueNode: XValueNode, place: XValuePlace) {
        xValueNode.setPresentation(AllIcons.Nodes.UpLevel, value.valueTypeName, value.value, true)
    }

    override fun computeChildren(node: XCompositeNode) {
        val cl = XValueChildrenList()
        children.forEach {
            it.parent = this
            cl.add(it.name, it)
        }
        node.addChildren(cl, true)
    }
    
    override fun computeSourcePosition(callback: (com.intellij.xdebugger.XSourcePosition?) -> Unit) {
        // Groups don't have inline display typically
        callback(null)
    }
}

class TableXValue(v: VariableValue, val frame: EmmyDebugStackFrame) : LuaXValue(v) {

    private val children = mutableListOf<LuaXValue>()

    init {
        value.children?.
                sortedWith(VariableComparator)?.
                forEach {
            children.add(create(it, frame))
        }
    }

    override fun computePresentation(xValueNode: XValueNode, place: XValuePlace) {
        var icon = AllIcons.Json.Object
        if (value.valueTypeName == "C#") {
            icon = LuaIcons.CSHARP
        }
        else if (value.valueTypeName == "C++") {
            icon = LuaIcons.CPP
        }
        xValueNode.setPresentation(icon, value.valueTypeName, value.value, true)
    }

    override fun computeChildren(node: XCompositeNode) {
        val ev = this.frame.evaluator
        if (ev != null) {
            ev.eval(evalExpr, value.cacheId, object : XDebuggerEvaluator.XEvaluationCallback {
                override fun errorOccurred(err: String) {
                    node.setErrorMessage(err)
                }

                override fun evaluated(value: XValue) {
                    if (value is TableXValue) {
                        val cl = XValueChildrenList()
                        children.clear()
                        children.addAll(value.children)
                        children.forEach {
                            it.parent = this@TableXValue
                            cl.add(it.name, it)
                        }
                        node.addChildren(cl, true)
                    }
                    else { // todo: table is nil?
                        node.setErrorMessage("nil")
                    }
                }

            }, 2)
        }
        else super.computeChildren(node)
    }
    
    override fun computeSourcePosition(callback: (com.intellij.xdebugger.XSourcePosition?) -> Unit) {
        // Use the LAST occurrence (closest to current line)
        val positions = frame.getVariableContext().getAllSourcePositionsFor(this)
        callback(positions.lastOrNull())
    }

    private val evalExpr: String
        get() {
            var name = name
            val properties = ArrayList<String>()
            var parent = this.parent
            while (parent != null) {
                if (!parent.value.fake) {
                    properties.add(name)
                    name = parent.name
                }
                parent = parent.parent
            }

            val sb = StringBuilder(name)
            for (i in properties.indices.reversed()) {
                val parentName = properties[i]
                if (parentName.startsWith("["))
                    sb.append(parentName)
                else
                    sb.append(String.format("[\"%s\"]", parentName))
            }
            return sb.toString()
        }
}