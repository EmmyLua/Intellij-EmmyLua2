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

import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.highlighter.HighlighterIterator
import com.intellij.openapi.util.TextRange
import com.intellij.psi.tree.IElementType
import com.intellij.xdebugger.XSourcePosition
import com.tang.intellij.lua.debugger.emmy.value.LuaXValue
import com.tang.intellij.lua.psi.LuaTypes

/**
 * Provider that uses syntax highlighting to find variable positions
 * This is the main strategy used by lsp4ij
 */
class LuaHighlighterDebugVariablePositionProvider : LuaDebugVariablePositionProvider {
    
    override fun configureContext(context: LuaDebugVariableContext) {
        val editor = context.getEditor()
        if (editor !is EditorEx) {
            return
        }
        
        val endLineOffset = context.getEndLineOffset()
        if (endLineOffset < 0) {
            return
        }
        
        try {
            val highlighter = editor.highlighter
            val iterator: HighlighterIterator = highlighter.createIterator(0)
            
            while (!iterator.atEnd()) {
                if (iterator.end > endLineOffset) {
                    break
                }
                
                val tokenType = iterator.tokenType
                if (isVariableToken(tokenType)) {
                    val start = iterator.start
                    val end = iterator.end
                    val textRange = TextRange(start, end)
                    val variableName = editor.document.getText(textRange).trim()
                    
                    if (variableName.isNotBlank() && !isKeyword(variableName)) {
                        context.addVariableRange(variableName, textRange)
                    }
                }
                
                iterator.advance()
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
     * Check if a token type represents a variable
     */
    private fun isVariableToken(tokenType: IElementType): Boolean {
        // Check for identifier tokens
        val tokenString = tokenType.toString()
        return tokenString.contains("ID", ignoreCase = true) ||
                tokenString.contains("IDENTIFIER", ignoreCase = true) ||
                tokenString.contains("NAME", ignoreCase = true) ||
                tokenType == LuaTypes.ID  // Lua specific identifier
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
