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

package com.tang.intellij.lua.lang

import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range

/**
 * Utility class for LSP-like conversions
 * Similar to lsp4ij's LSPIJUtils but for Lua debugging
 */
object LSPIJUtils {

    /**
     * Convert TextRange to LSP Range
     */
    fun toRange(textRange: TextRange, document: Document): Range {
        val start = toPosition(textRange.startOffset, document)
        val end = toPosition(textRange.endOffset, document)
        return Range(start, end)
    }

    /**
     * Convert offset to LSP Position
     */
    fun toPosition(offset: Int, document: Document): Position {
        val lineNumber = document.getLineNumber(offset)
        val lineStartOffset = document.getLineStartOffset(lineNumber)
        val column = offset - lineStartOffset
        return Position(lineNumber, column)
    }
}
