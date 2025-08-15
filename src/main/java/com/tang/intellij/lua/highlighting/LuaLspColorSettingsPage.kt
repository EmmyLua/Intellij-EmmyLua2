package com.tang.intellij.lua.highlighting

import com.intellij.application.options.colors.ColorAndFontOptions
import com.intellij.application.options.colors.ColorAndFontSettingsListener
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import com.intellij.psi.codeStyle.DisplayPriority
import com.intellij.psi.codeStyle.DisplayPrioritySortable
import com.tang.intellij.lua.lang.LuaIcons
import javax.swing.Icon

/**
 * Lua LSP 高亮配置页面
 * 允许用户自定义 LSP 相关的高亮样式
 */
class LuaLspColorSettingsPage : ColorSettingsPage, DisplayPrioritySortable {

    companion object {
        private val DESCRIPTORS = arrayOf(
            // LSP 特定高亮样式
            AttributesDescriptor("LSP Read-only Parameter", LuaHighlightingData.LSP_READ_ONLY_PARAM),
            AttributesDescriptor("LSP Global Variable", LuaHighlightingData.LSP_GLOBAL_VAR),
            AttributesDescriptor("LSP Read-only Local Variable", LuaHighlightingData.LSP_READ_ONLY_LOCAL),
            AttributesDescriptor("LSP Mutable Local Variable", LuaHighlightingData.LSP_MUT_LOCAL),
            AttributesDescriptor("LSP Mutable Parameter", LuaHighlightingData.LSP_MUT_PARAM),
            AttributesDescriptor("LSP Documentation Emphasis", LuaHighlightingData.LSP_DOC_EM),
            AttributesDescriptor("LSP Documentation Strong", LuaHighlightingData.LSP_DOC_STRONG),

            // 基础高亮样式
            AttributesDescriptor("Keyword", LuaHighlightingData.KEYWORD),
            AttributesDescriptor("Local Variable", LuaHighlightingData.LOCAL_VAR),
            AttributesDescriptor("Parameter", LuaHighlightingData.PARAMETER),
            AttributesDescriptor("Global Variable", LuaHighlightingData.GLOBAL_VAR),
            AttributesDescriptor("Field", LuaHighlightingData.FIELD),
            AttributesDescriptor("String", LuaHighlightingData.STRING),
            AttributesDescriptor("Number", LuaHighlightingData.NUMBER),
            AttributesDescriptor("Comment", LuaHighlightingData.LINE_COMMENT),
            AttributesDescriptor("Documentation Comment", LuaHighlightingData.DOC_COMMENT),
            AttributesDescriptor("Class Name", LuaHighlightingData.CLASS_NAME),
            AttributesDescriptor("Instance Method", LuaHighlightingData.INSTANCE_METHOD),
            AttributesDescriptor("Static Method", LuaHighlightingData.STATIC_METHOD)
        )

        // 示例代码，展示各种高亮样式
        private const val DEMO_TEXT = """
-- LSP Enhanced Highlighting Demo
---@param <lsp_readonly_param>readOnlyParam</lsp_readonly_param> string Read-only parameter
---@param <lsp_mut_param>mutParam</lsp_mut_param> number Mutable parameter
---@class MyClass
---@field <lsp_global>globalVar</lsp_global> string Global variable

local function <instance_method>myFunction</instance_method>(<lsp_readonly_param>readOnlyParam</lsp_readonly_param>, <lsp_mut_param>mutParam</lsp_mut_param>)
    local <lsp_readonly_local>readOnlyLocal</lsp_readonly_local> = "hello"
    local <lsp_mut_local>mutLocal</lsp_mut_local> = 42
    
    -- <lsp_doc_em>This is documentation emphasis text</lsp_doc_em>
    -- <lsp_doc_strong>This is documentation strong text</lsp_doc_strong>
    
    <lsp_global>globalVar</lsp_global> = <lsp_readonly_local>readOnlyLocal</lsp_readonly_local> .. tostring(<lsp_mut_local>mutLocal</lsp_mut_local>)
    <lsp_mut_param>mutParam</lsp_mut_param> = <lsp_mut_param>mutParam</lsp_mut_param> + 1
    
    return <lsp_readonly_param>readOnlyParam</lsp_readonly_param>, <lsp_mut_param>mutParam</lsp_mut_param>
end

-- Standard highlighting styles
local <local_var>normalLocal</local_var> = <number>123</number>
local <local_var>myTable</local_var> = {
    <field>field1</field> = <string>"value1"</string>,
    <field>field2</field> = <number>456</number>
}

<global_var>print</global_var>(<local_var>myTable</local_var>.<field>field1</field>)
"""
    }

    override fun getAttributeDescriptors(): Array<AttributesDescriptor> = DESCRIPTORS

    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY

    override fun getDisplayName(): String = "Lua LSP"

    override fun getIcon(): Icon? = LuaIcons.FILE

    override fun getHighlighter() = LuaSyntaxHighlighterFactory().getSyntaxHighlighter(null, null)

    override fun getDemoText(): String = DEMO_TEXT

    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey>? {
        return mapOf(
            "lsp_readonly_param" to LuaHighlightingData.LSP_READ_ONLY_PARAM,
            "lsp_global" to LuaHighlightingData.LSP_GLOBAL_VAR,
            "lsp_readonly_local" to LuaHighlightingData.LSP_READ_ONLY_LOCAL,
            "lsp_mut_local" to LuaHighlightingData.LSP_MUT_LOCAL,
            "lsp_mut_param" to LuaHighlightingData.LSP_MUT_PARAM,
            "lsp_doc_em" to LuaHighlightingData.LSP_DOC_EM,
            "lsp_doc_strong" to LuaHighlightingData.LSP_DOC_STRONG,

            "local_var" to LuaHighlightingData.LOCAL_VAR,
            "parameter" to LuaHighlightingData.PARAMETER,
            "global_var" to LuaHighlightingData.GLOBAL_VAR,
            "field" to LuaHighlightingData.FIELD,
            "string" to LuaHighlightingData.STRING,
            "number" to LuaHighlightingData.NUMBER,
            "instance_method" to LuaHighlightingData.INSTANCE_METHOD
        )
    }

    override fun getPriority(): DisplayPriority = DisplayPriority.LANGUAGE_SETTINGS
}
