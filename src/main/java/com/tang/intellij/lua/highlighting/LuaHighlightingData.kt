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

package com.tang.intellij.lua.highlighting

import com.intellij.execution.process.ConsoleHighlighter
import com.intellij.ide.highlighter.custom.CustomHighlighterColors
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.ui.JBColor
import java.awt.Color
import java.awt.Font

/**
 * Lua 高亮数据 - 增强版，支持可配置和明显的 LSP 高亮
 * Created by TangZX on 2016/11/22.
 */
object LuaHighlightingData {
    // 原有的基础高亮样式
    val CLASS_NAME = TextAttributesKey.createTextAttributesKey("LUA_CLASS_NAME", DefaultLanguageHighlighterColors.CLASS_NAME)
    val LOCAL_VAR = TextAttributesKey.createTextAttributesKey("LUA_LOCAL_VAR", DefaultLanguageHighlighterColors.LOCAL_VARIABLE)
    val PARAMETER = TextAttributesKey.createTextAttributesKey("LUA_PARAMETER", CustomHighlighterColors.CUSTOM_KEYWORD3_ATTRIBUTES)
    val FIELD = TextAttributesKey.createTextAttributesKey("LUA_FIELD")
    val GLOBAL_VAR = TextAttributesKey.createTextAttributesKey("LUA_GLOBAL_VAR", DefaultLanguageHighlighterColors.STATIC_FIELD)
    val KEYWORD = TextAttributesKey.createTextAttributesKey("LUA_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD)
    val SELF = TextAttributesKey.createTextAttributesKey("LUA_SELF", CustomHighlighterColors.CUSTOM_KEYWORD2_ATTRIBUTES)
    val LINE_COMMENT = TextAttributesKey.createTextAttributesKey("LUA_LINE_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT)
    val DOC_COMMENT = TextAttributesKey.createTextAttributesKey("LUA_DOC_COMMENT", DefaultLanguageHighlighterColors.DOC_COMMENT)
    val NUMBER = TextAttributesKey.createTextAttributesKey("LUA_NUMBER", DefaultLanguageHighlighterColors.NUMBER)
    val STRING = TextAttributesKey.createTextAttributesKey("LUA_STRING", DefaultLanguageHighlighterColors.STRING)
    val BRACKETS = TextAttributesKey.createTextAttributesKey("LUA_BRACKETS", DefaultLanguageHighlighterColors.BRACKETS)
    val BRACES = TextAttributesKey.createTextAttributesKey("LUA_BRACES", DefaultLanguageHighlighterColors.BRACES)
    val PARENTHESES = TextAttributesKey.createTextAttributesKey("LUA_PARENTHESES", DefaultLanguageHighlighterColors.PARENTHESES)
    val DOT = TextAttributesKey.createTextAttributesKey("LUA_DOT", DefaultLanguageHighlighterColors.DOT)
    val OPERATORS = TextAttributesKey.createTextAttributesKey("LUA_OPERATORS", DefaultLanguageHighlighterColors.OPERATION_SIGN)
    val SEMICOLON = TextAttributesKey.createTextAttributesKey("LUA_SEMICOLON", DefaultLanguageHighlighterColors.SEMICOLON)
    val COMMA = TextAttributesKey.createTextAttributesKey("LUA_COMMA", DefaultLanguageHighlighterColors.COMMA)
    val PRIMITIVE_TYPE = TextAttributesKey.createTextAttributesKey("LUA_PRIMITIVE_TYPE",  DefaultLanguageHighlighterColors.KEYWORD)
    val INSTANCE_METHOD = TextAttributesKey.createTextAttributesKey("LUA_INSTANCE_METHOD", DefaultLanguageHighlighterColors.INSTANCE_METHOD)
    val STATIC_METHOD = TextAttributesKey.createTextAttributesKey("LUA_STATIC_METHOD", DefaultLanguageHighlighterColors.STATIC_METHOD)

    //region
    val REGION_HEADER = TextAttributesKey.createTextAttributesKey("LUA_REGION_START", DefaultLanguageHighlighterColors.DOC_COMMENT)
    val REGION_DESC = TextAttributesKey.createTextAttributesKey("LUA_REGION_DESC")

    // ===== LSP 增强高亮样式 - 更加明显和可配置 =====

    /**
     * LSP 只读参数 - 使用蓝色背景高亮
     */
    val LSP_READ_ONLY_PARAM = TextAttributesKey.createTextAttributesKey(
        "LUA_LSP_READ_ONLY_PARAM",
        TextAttributes().apply {
            foregroundColor = JBColor(Color(0x0070C0), Color(0x6CB4EE))  // 蓝色
            backgroundColor = JBColor(Color(0xE6F2FF), Color(0x1A2332))   // 浅蓝背景
            fontType = Font.ITALIC
        }
    )

    /**
     * LSP 全局变量 - 使用粉红色高亮，加粗
     */
    val LSP_GLOBAL_VAR = TextAttributesKey.createTextAttributesKey(
        "LUA_LSP_GLOBAL_VAR",
        TextAttributes().apply {
            foregroundColor = JBColor(Color(0xFF6699), Color(0xFF88AA))  // 粉红色
            backgroundColor = JBColor(Color(0xFFF0F5), Color(0x2A1A22))   // 浅粉背景
            fontType = Font.BOLD
        }
    )

    /**
     * LSP 只读局部变 - 使用绿色高亮
     */
    val LSP_READ_ONLY_LOCAL = TextAttributesKey.createTextAttributesKey(
        "LUA_LSP_READ_ONLY_LOCAL",
        TextAttributes().apply {
            foregroundColor = JBColor(Color(0x0000FF), Color(0x589DF6))
            fontType = Font.ITALIC
        }
    )

    /**
     * LSP 可变局部变量 - 使用类似Java的可变量颜色
     */
    val LSP_MUT_LOCAL = TextAttributesKey.createTextAttributesKey(
        "LUA_LSP_MUT_LOCAL",
        TextAttributes().apply {
            foregroundColor = JBColor(Color(0x0000FF), Color(0x589DF6))  // 蓝色，类似Java变量
            fontType = Font.PLAIN
        }
    )

    /**
     * LSP 可变参数 - 使用类似Java参数的颜色，带下划线强调
     */
    val LSP_MUT_PARAM = TextAttributesKey.createTextAttributesKey(
        "LUA_LSP_MUT_PARAM",
        TextAttributes().apply {
            foregroundColor = JBColor(Color(0x871094), Color(0xB381C5))  // 紫色，类似Java参数
            backgroundColor = JBColor(Color(0xF8F0FF), Color(0x2A1A2A))   // 浅紫背景
            fontType = Font.BOLD
            effectType = com.intellij.openapi.editor.markup.EffectType.LINE_UNDERSCORE
            effectColor = JBColor(Color(0x871094), Color(0xB381C5))
        }
    )

    /**
     * LSP 文档强调 - 使用金色背景
     */
    val LSP_DOC_EM = TextAttributesKey.createTextAttributesKey(
        "LUA_LSP_DOC_EM",
        TextAttributes().apply {
            foregroundColor = JBColor(Color(0xB8860B), Color(0xFFD700))  // 金色
            backgroundColor = JBColor(Color(0xFFFBF0), Color(0x2D2A1A))   // 浅金背景
            fontType = Font.ITALIC
        }
    )

    /**
     * LSP 文档加粗 - 使用深红色，加粗
     */
    val LSP_DOC_STRONG = TextAttributesKey.createTextAttributesKey(
        "LUA_LSP_DOC_STRONG",
        TextAttributes().apply {
            foregroundColor = JBColor(Color(0x8B0000), Color(0xFF6B6B))  // 深红色
            backgroundColor = JBColor(Color(0xFFF0F0), Color(0x2D1A1A))   // 浅红背景
            fontType = Font.BOLD
        }
    )

    /**
     * 根据 AnnotatorType 获取对应的高亮键
     */
    fun getLspHighlightKey(type: com.cppcxy.ide.lsp.AnnotatorType): TextAttributesKey {
        return when (type) {
            com.cppcxy.ide.lsp.AnnotatorType.ReadOnlyParam -> LSP_READ_ONLY_PARAM
            com.cppcxy.ide.lsp.AnnotatorType.Global -> LSP_GLOBAL_VAR
            com.cppcxy.ide.lsp.AnnotatorType.ReadOnlyLocal -> LSP_READ_ONLY_LOCAL
            com.cppcxy.ide.lsp.AnnotatorType.MutLocal -> LSP_MUT_LOCAL
            com.cppcxy.ide.lsp.AnnotatorType.MutParam -> LSP_MUT_PARAM
            com.cppcxy.ide.lsp.AnnotatorType.DocEm -> LSP_DOC_EM
            com.cppcxy.ide.lsp.AnnotatorType.DocStrong -> LSP_DOC_STRONG
        }
    }

    /**
     * 创建自定义高亮样式的工具方法
     */
    fun createCustomHighlight(
        name: String,
        foreground: Color? = null,
        background: Color? = null,
        fontType: Int = Font.PLAIN,
        effectType: com.intellij.openapi.editor.markup.EffectType? = null,
        effectColor: Color? = null
    ): TextAttributesKey {
        return TextAttributesKey.createTextAttributesKey(
            name,
            TextAttributes().apply {
                foreground?.let { foregroundColor = JBColor(it, it) }
                background?.let { backgroundColor = JBColor(it, it) }
                this.fontType = fontType
                effectType?.let { this.effectType = it }
                effectColor?.let { this.effectColor = JBColor(it, it) }
            }
        )
    }
}