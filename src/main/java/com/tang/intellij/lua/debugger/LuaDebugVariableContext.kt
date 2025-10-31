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

package com.tang.intellij.lua.debugger

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.xdebugger.XDebuggerUtil
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.frame.XStackFrame
import com.tang.intellij.lua.debugger.emmy.EmmyDebugStackFrame
import com.tang.intellij.lua.debugger.emmy.value.LuaXValue
import com.tang.intellij.lua.editor.LuaEditorUtil
import com.tang.intellij.lua.lang.LSPIJUtils

/**
 * Context for tracking variable positions during debugging
 * Used to map variable names to their source positions for inline value display
 */
class LuaDebugVariableContext(
    private val stackFrame: XStackFrame
) {
    private val variableRanges = mutableMapOf<String, TextRange>()
    private val variablePositions = mutableMapOf<String, XSourcePosition>()
    private val providers = mutableListOf<LuaDebugVariablePositionProvider>()
    private var editor: Editor? = null
    private var endLineOffset: Int = -1
    
    init {
        // Register default provider
        providers.add(LuaHighlighterDebugVariablePositionProvider())
        
        // Initialize editor if available
        val sourcePosition = stackFrame.sourcePosition
        if (sourcePosition != null && stackFrame is EmmyDebugStackFrame) {
            val project = stackFrame.process.session.project
            val editors = LuaEditorUtil.findEditors(project, sourcePosition.file)
            editor = editors.firstOrNull()
            if (editor != null) {
                endLineOffset = editor!!.document.getLineEndOffset(sourcePosition.line)
            }
        }
    }
    
    /**
     * Configure context by scanning for variables
     */
    fun configureContext() {
        providers.forEach { it.configureContext(this) }
    }
    
    /**
     * Get the file for this context
     */
    fun getFile(): VirtualFile? = stackFrame.sourcePosition?.file
    
    /**
     * Get the editor for this context
     */
    fun getEditor(): Editor? = editor
    
    /**
     * Get the end line offset
     */
    fun getEndLineOffset(): Int = endLineOffset
    
    /**
     * Add a variable range
     */
    fun addVariableRange(variableName: String, textRange: TextRange) {
        variableRanges[variableName] = textRange
    }
    
    /**
     * Add a variable position
     */
    fun addVariablePosition(variableName: String, position: XSourcePosition) {
        variablePositions[variableName] = position
    }
    
    /**
     * Get source position for a variable name
     */
    fun getSourcePosition(name: String): XSourcePosition? {
        // Return cached position if available
        variablePositions[name]?.let { return it }
        
        // Try to create position from range
        val textRange = variableRanges[name] ?: return null
        val file = getFile() ?: return null
        val ed = editor ?: return null
        
        val range = com.tang.intellij.lua.lang.LSPIJUtils.toRange(textRange, ed.document)
        val position = XDebuggerUtil.getInstance()
            .createPosition(file, range.start.line, range.end.character)
        
        if (position != null) {
            addVariablePosition(name, position)
        }
        
        return position
    }
    
    /**
     * Get source position for a variable value
     */
    fun getSourcePositionFor(value: LuaXValue): XSourcePosition? {
        for (provider in providers) {
            val position = provider.getSourcePosition(value, this)
            if (position != null) {
                return position
            }
        }
        return null
    }
}
