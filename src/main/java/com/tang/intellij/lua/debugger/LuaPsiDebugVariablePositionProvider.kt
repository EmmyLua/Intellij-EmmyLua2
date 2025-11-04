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
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.xdebugger.XSourcePosition
import com.tang.intellij.lua.debugger.emmy.value.LuaXValue
import com.tang.intellij.lua.psi.LuaFuncBody
import com.tang.intellij.lua.psi.LuaNameDef
import com.tang.intellij.lua.psi.LuaNameExpr
import com.tang.intellij.lua.psi.LuaParamNameDef

/**
 * Provider that uses PSI tree to find variable positions
 * Collects all NameExpr, NameDef (local variables), and ParamNameDef (function parameters)
 * from current position up to the first FuncBody or file root
 */
class LuaPsiDebugVariablePositionProvider : LuaDebugVariablePositionProvider {

    override fun configureContext(context: LuaDebugVariableContext) {
        val editor = context.getEditor() ?: return

        val sourcePosition = context.getSourcePosition() ?: return

        val psiFile = context.getPsiFile() ?: return

        try {
            // Wrap all PSI operations in ReadAction
            ReadAction.run<RuntimeException> {
                val document = editor.document
                val lineStartOffset = document.getLineStartOffset(sourcePosition.line)
                val lineEndOffset = document.getLineEndOffset(sourcePosition.line)

                // Find element at the breakpoint line
                val elementAtLine = psiFile.findElementAt(lineStartOffset)
                if (elementAtLine == null) {
                    println("LuaPsiDebugVariablePositionProvider: elementAtLine is null")
                    return@run
                }

                // Find the scope boundary (function body or file root)
                val scopeElement = findScopeElement(elementAtLine, psiFile)

                // Collect all NameExpr from line start up to the scope boundary
                collectNameExprsInScope(scopeElement, lineEndOffset, document, context)
            }
        } catch (e: Exception) {
            // Ignore errors during scanning
            e.printStackTrace()
        }
    }

    override fun getSourcePosition(value: LuaXValue, context: LuaDebugVariableContext): XSourcePosition? {
        return context.getSourcePosition(value.name)
    }

    /**
     * Find the scope element (FuncBody or PsiFile) containing the current element
     */
    private fun findScopeElement(element: PsiElement, file: PsiFile): PsiElement {
        var current: PsiElement? = element

        while (current != null && current != file) {
            if (current is LuaFuncBody) {
                return current
            }
            current = current.parent
        }

        return file
    }

    /**
     * Collect all NameExpr, NameDef, and ParamNameDef in the scope up to the specified line end offset
     */
    private fun collectNameExprsInScope(
        scopeElement: PsiElement,
        lineEndOffset: Int,
        document: Document,
        context: LuaDebugVariableContext
    ) {
        // Find all NameExpr (variable usages) within the scope element
        val nameExprs = PsiTreeUtil.findChildrenOfType(scopeElement, LuaNameExpr::class.java)

        // Find all NameDef (local variable definitions) within the scope element
        val nameDefs = PsiTreeUtil.findChildrenOfType(scopeElement, LuaNameDef::class.java)

        // Find all ParamNameDef (function parameter definitions) within the scope element
        val paramNameDefs = PsiTreeUtil.findChildrenOfType(scopeElement, LuaParamNameDef::class.java)

        // Collect all elements with their positions into a single list
        data class ElementWithPosition(val name: String, val offset: Int, val textRange: TextRange, val type: String)

        val allElements = mutableListOf<ElementWithPosition>()

        // Collect NameExpr (variable usages)
        for (nameExpr in nameExprs) {
            val textRange = nameExpr.textRange

            // Only include NameExpr that appear before or on the current line
            if (textRange.startOffset <= lineEndOffset) {
                val variableName = nameExpr.text.trim()

                if (variableName.isNotBlank() && !isKeyword(variableName)) {
                    allElements.add(ElementWithPosition(variableName, textRange.startOffset, textRange, "NameExpr"))
                }
            }
        }

        // Collect NameDef (local variable definitions)
        for (nameDef in nameDefs) {
            // Skip if this is a ParamNameDef (already handled below)
            if (nameDef is LuaParamNameDef) continue

            val textRange = nameDef.textRange

            // Only include NameDef that appear before or on the current line
            if (textRange.startOffset <= lineEndOffset) {
                val variableName = nameDef.text.trim()

                if (variableName.isNotBlank() && !isKeyword(variableName)) {
                    allElements.add(ElementWithPosition(variableName, textRange.startOffset, textRange, "NameDef"))
                }
            }
        }

        // Collect ParamNameDef (function parameter definitions)
        for (paramNameDef in paramNameDefs) {
            val textRange = paramNameDef.textRange

            // Only include ParamNameDef that appear before or on the current line
            if (textRange.startOffset <= lineEndOffset) {
                val variableName = paramNameDef.text.trim()

                if (variableName.isNotBlank() && !isKeyword(variableName)) {
                    allElements.add(ElementWithPosition(variableName, textRange.startOffset, textRange, "ParamNameDef"))
                }
            }
        }

        // Sort all elements by their start offset to ensure correct order
        allElements.sortBy { it.offset }
        // Add them to context in sorted order
        var addedCount = 0
        for (element in allElements) {
            context.addVariableRange(element.name, element.textRange)
            addedCount++
        }
    }

    /**
     * Check if a string is a Lua keyword (should not be treated as variable)
     */
    private fun isKeyword(text: String): Boolean {
        return text in setOf(
            "and", "break", "do", "else", "elseif", "end", "false",
            "for", "function", "if", "in", "local", "nil", "not",
            "or", "repeat", "return", "then", "true", "until", "while",
            "goto", "self"
        )
    }
}
