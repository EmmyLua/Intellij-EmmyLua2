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
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.xdebugger.XSourcePosition
import com.tang.intellij.lua.debugger.emmy.value.LuaXValue
import com.tang.intellij.lua.psi.LuaFuncBody
import com.tang.intellij.lua.psi.LuaNameExpr
import com.tang.intellij.lua.psi.LuaPsiFile

/**
 * Provider that uses PSI tree to find variable positions
 * Collects all NameExpr from current position up to the first FuncBody or file root
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
                
                println("LuaPsiDebugVariablePositionProvider: Line offsets: $lineStartOffset - $lineEndOffset")
                
                // Find element at the breakpoint line
                val elementAtLine = psiFile.findElementAt(lineStartOffset)
                if (elementAtLine == null) {
                    println("LuaPsiDebugVariablePositionProvider: elementAtLine is null")
                    return@run
                }
                
                println("LuaPsiDebugVariablePositionProvider: Element at line: ${elementAtLine.text}")
                
                // Find the scope boundary (function body or file root)
                val scopeElement = findScopeElement(elementAtLine, psiFile)
                println("LuaPsiDebugVariablePositionProvider: Scope element: ${scopeElement.javaClass.simpleName}")
                
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
     * Collect all NameExpr in the scope up to the specified line end offset
     */
    private fun collectNameExprsInScope(
        scopeElement: PsiElement,
        lineEndOffset: Int,
        document: Document,
        context: LuaDebugVariableContext
    ) {
        // Find all NameExpr within the scope element
        val nameExprs = PsiTreeUtil.findChildrenOfType(scopeElement, LuaNameExpr::class.java)
        
        println("LuaPsiDebugVariablePositionProvider: Found ${nameExprs.size} NameExpr in scope")
        
        // For each variable, we want to keep track of ALL occurrences before or on current line
        // This allows inline values to be shown at each usage location
        var addedCount = 0
        for (nameExpr in nameExprs) {
            val textRange = nameExpr.textRange
            
            // Only include NameExpr that appear before or on the current line
            if (textRange.startOffset <= lineEndOffset) {
                val variableName = nameExpr.text.trim()
                
                if (variableName.isNotBlank() && !isKeyword(variableName)) {
                    println("LuaPsiDebugVariablePositionProvider: Adding variable '$variableName' at ${textRange.startOffset}")
                    // Use a composite key (variable name + offset) to store multiple occurrences
                    context.addVariableRange(variableName, textRange)
                    addedCount++
                }
            }
        }
        
        println("LuaPsiDebugVariablePositionProvider: Added $addedCount variables to context")
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
