package com.tang.intellij.lua.codeInsight.editorActions

import com.intellij.codeInsight.editorActions.ExtendWordSelectionHandlerBase
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.tang.intellij.lua.lang.type.LuaString
import com.tang.intellij.lua.psi.LuaTypes

/**
 * 处理Lua字符串中的单词选择
 * 当双击字符串内的单词时，只选择单词而不是整个字符串
 */
class LuaWordSelectionHandler : ExtendWordSelectionHandlerBase() {

    override fun canSelect(e: PsiElement): Boolean {
        return e.node.elementType == LuaTypes.STRING
    }

    override fun select(e: PsiElement, editorText: CharSequence, cursorOffset: Int, editor: Editor): List<TextRange>? {
        val text = e.text
        val content = LuaString.getContent(text)

        // 计算相对于字符串内容的光标位置
        val relativeOffset = cursorOffset - e.textRange.startOffset - content.start

        // 如果光标不在字符串内容区域内，返回null让默认处理器处理
        if (relativeOffset < 0 || relativeOffset >= content.value.length || content.value.isEmpty()) {
            return null
        }

        val stringContent = content.value

        // 查找当前光标位置的单词
        val wordRange = findWordAt(stringContent, relativeOffset) ?: return null

        val absoluteStart = e.textRange.startOffset + content.start + wordRange.first
        val absoluteEnd = e.textRange.startOffset + content.start + wordRange.second

        // 确保范围有效且在字符串内容范围内
        if (absoluteStart >= e.textRange.startOffset + content.start &&
            absoluteEnd <= e.textRange.startOffset + content.end &&
            absoluteStart < absoluteEnd
        ) {
            return listOf(TextRange(absoluteStart, absoluteEnd))
        }

        return null
    }

    /**
     * 在指定位置查找单词
     */
    private fun findWordAt(text: String, offset: Int): Pair<Int, Int>? {
        if (offset < 0 || offset >= text.length) return null

        var start = offset
        var end = offset

        // 如果当前字符不是单词字符，寻找最近的单词
        if (!isWordChar(text[offset])) {
            // 向左查找
            var leftPos = offset - 1
            while (leftPos >= 0 && !isWordChar(text[leftPos])) leftPos--

            // 向右查找
            var rightPos = offset
            while (rightPos < text.length && !isWordChar(text[rightPos])) rightPos++

            // 选择最近的单词
            when {
                leftPos >= 0 && (rightPos >= text.length || (offset - leftPos <= rightPos - offset)) -> {
                    start = leftPos
                    end = leftPos + 1
                }

                rightPos < text.length -> {
                    start = rightPos
                    end = rightPos + 1
                }

                else -> return null
            }
        }

        // 向左扩展到单词开始
        while (start > 0 && isWordChar(text[start - 1])) start--

        // 向右扩展到单词结束
        while (end < text.length && isWordChar(text[end])) end++

        return if (start < end) Pair(start, end) else null
    }

    /**
     * 判断字符是否为单词字符（字母、数字、下划线）
     */
    private fun isWordChar(char: Char): Boolean {
        return char.isLetterOrDigit() || char == '_'
    }
}
