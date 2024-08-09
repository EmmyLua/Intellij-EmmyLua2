package com.tang.intellij.lua.folding

import com.intellij.lang.folding.CustomFoldingProvider
import com.intellij.openapi.util.text.StringUtil


class LuaFoldingProvider : CustomFoldingProvider() {
    override fun isCustomRegionStart(elementText: String) =
        elementText.contains("{{{") && elementText.matches("--\\s*\\{\\{\\{.*".toRegex())

    override fun isCustomRegionEnd(elementText: String) = elementText.contains("}}}")

    override fun getPlaceholderText(elementText: String): String {
        val textAfterMarker = elementText.replaceFirst("--\\s*\\{\\{\\{(.*)".toRegex(), "$1")
        val result = if (elementText.startsWith("/*")) StringUtil.trimEnd(textAfterMarker, "*/").trim { it <= ' ' } else textAfterMarker.trim { it <= ' ' }
        return if (result.isEmpty()) "..." else result
    }

    override fun getDescription() = "{{{...}}} Comments"

    override fun getStartString() = "{{{ ?"

    override fun getEndString() = "}}}"
}