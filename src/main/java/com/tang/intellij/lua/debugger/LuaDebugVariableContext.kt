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

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.xdebugger.XDebuggerUtil
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.frame.XStackFrame
import com.tang.intellij.lua.debugger.emmy.EmmyDebugStackFrame
import com.tang.intellij.lua.debugger.emmy.value.LuaXValue
import com.tang.intellij.lua.editor.LuaEditorUtil

/**
 * Context for tracking variable positions during debugging
 * Used to map variable names to their source positions for inline value display
 */
class LuaDebugVariableContext(
    private val stackFrame: XStackFrame
) {
    // Store multiple ranges for each variable (for multiple occurrences)
    private val variableRanges = mutableMapOf<String, MutableList<TextRange>>()
    private val variablePositions = mutableMapOf<String, MutableList<XSourcePosition>>()
    private val providers = mutableListOf<LuaDebugVariablePositionProvider>()
    private var editor: Editor? = null
    private var endLineOffset: Int = -1
    private var psiFile: PsiFile? = null
    private var sourcePosition: XSourcePosition? = null

    init {
        // Register PSI-based provider (more accurate than highlighter-based)
        providers.add(LuaPsiDebugVariablePositionProvider())

        println("LuaDebugVariableContext: Initializing...")

        // Initialize editor and PSI file if available
        sourcePosition = stackFrame.sourcePosition
        println("LuaDebugVariableContext: sourcePosition = $sourcePosition")

        if (sourcePosition != null && stackFrame is EmmyDebugStackFrame) {
            val project = stackFrame.process.session.project
            val editors = LuaEditorUtil.findEditors(project, sourcePosition!!.file)
            editor = editors.firstOrNull()

            println("LuaDebugVariableContext: Found ${editors.size} editors, using first: $editor")

            if (editor != null) {
                endLineOffset = editor!!.document.getLineEndOffset(sourcePosition!!.line)
                println("LuaDebugVariableContext: endLineOffset = $endLineOffset")

                // Get PSI file from document - must be done in ReadAction
                ReadAction.run<RuntimeException> {
                    psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor!!.document)
                    println("LuaDebugVariableContext: psiFile = $psiFile (${psiFile?.javaClass?.simpleName})")
                }

                // Configure context immediately after initialization
                println("LuaDebugVariableContext: Calling configureContext()...")
                configureContext()
                println("LuaDebugVariableContext: configureContext() completed")
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
     * Get the PSI file for this context
     */
    fun getPsiFile(): PsiFile? = psiFile

    /**
     * Get the source position for this context
     */
    fun getSourcePosition(): XSourcePosition? = sourcePosition

    /**
     * Add a variable range (supports multiple occurrences of the same variable)
     */
    fun addVariableRange(variableName: String, textRange: TextRange) {
        val ranges = variableRanges.getOrPut(variableName) { mutableListOf() }
        ranges.add(textRange)
    }

    /**
     * Add a variable position
     */
    fun addVariablePosition(variableName: String, position: XSourcePosition) {
        val positions = variablePositions.getOrPut(variableName) { mutableListOf() }
        positions.add(position)
    }

    /**
     * Get all source positions for a variable name
     * This is used to display inline values at all occurrences of the variable
     */
    fun getAllSourcePositions(name: String): List<XSourcePosition> {
        println("LuaDebugVariableContext.getAllSourcePositions: Looking for all positions of '$name'")

        // Return cached positions if available
        variablePositions[name]?.let {
            if (it.isNotEmpty()) {
                println("LuaDebugVariableContext.getAllSourcePositions: Found ${it.size} cached positions for '$name'")
                return it
            }
        }

        // Try to create positions from all ranges
        val ranges = variableRanges[name]
        if (ranges == null || ranges.isEmpty()) {
            println("LuaDebugVariableContext.getAllSourcePositions: No ranges found for '$name'")
            println("LuaDebugVariableContext.getAllSourcePositions: Available variables: ${variableRanges.keys}")
            return emptyList()
        }

        val file = getFile()
        if (file == null) {
            println("LuaDebugVariableContext.getAllSourcePositions: file is null")
            return emptyList()
        }

        val ed = editor
        if (ed == null) {
            println("LuaDebugVariableContext.getAllSourcePositions: editor is null")
            return emptyList()
        }

        // Create positions for all occurrences
        val positions = mutableListOf<XSourcePosition>()
        for (textRange in ranges) {
            val range = com.tang.intellij.lua.lang.LSPIJUtils.toRange(textRange, ed.document)
            println("LuaDebugVariableContext.getAllSourcePositions: Range for '$name': line ${range.start.line}, char ${range.start.character}-${range.end.character}")

            val position = XDebuggerUtil.getInstance()
                .createPosition(file, range.start.line, range.start.character)

            if (position != null) {
                println("LuaDebugVariableContext.getAllSourcePositions: Created position: $position")
                positions.add(position)
            }
        }

        // Cache the positions
        if (positions.isNotEmpty()) {
            variablePositions[name] = positions.toMutableList()
        }

        println("LuaDebugVariableContext.getAllSourcePositions: Returning ${positions.size} positions for '$name'")
        return positions
    }

    /**
     * Get source position for a variable name
     * Returns the first occurrence position (can be extended to return all)
     */
    fun getSourcePosition(name: String): XSourcePosition? {
        val positions = getAllSourcePositions(name)
        return positions.firstOrNull()
    }

    /**
     * Get all source positions for a variable value
     */
    fun getAllSourcePositionsFor(value: LuaXValue): List<XSourcePosition> {
        for (provider in providers) {
            // For now, use getSourcePosition - could be extended to support multiple positions from provider
            val position = provider.getSourcePosition(value, this)
            if (position != null) {
                // Return all positions for this variable name
                return getAllSourcePositions(value.name)
            }
        }
        return emptyList()
    }

    /**
     * Get source position for a variable value
     */
    fun getSourcePositionFor(value: LuaXValue): XSourcePosition? {
        return getAllSourcePositionsFor(value).firstOrNull()
    }
}
