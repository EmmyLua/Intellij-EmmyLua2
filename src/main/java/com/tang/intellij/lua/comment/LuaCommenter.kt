package com.tang.intellij.lua.comment

import com.intellij.codeInsight.generation.IndentedCommenter

class LuaCommenter : IndentedCommenter {
    override fun getLineCommentPrefix(): String {
        return "--"
    }

    override fun getBlockCommentPrefix(): String {
        return "--[["
    }

    override fun getBlockCommentSuffix(): String {
        return "]]"
    }

    override fun getCommentedBlockCommentPrefix(): String? {
        return null
    }

    override fun getCommentedBlockCommentSuffix(): String? {
        return null
    }

    override fun forceIndentedLineComment(): Boolean {
        return true
    }
}