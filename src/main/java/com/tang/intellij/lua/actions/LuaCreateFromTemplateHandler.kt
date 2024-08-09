package com.tang.intellij.lua.actions

import com.intellij.ide.fileTemplates.DefaultCreateFromTemplateHandler
import com.intellij.ide.fileTemplates.FileTemplate
import com.tang.intellij.lua.lang.LuaFileType


/**
 *
 * Created by tangzx on 2016/12/24.
 */
class LuaCreateFromTemplateHandler : DefaultCreateFromTemplateHandler() {
    override fun handlesTemplate(template: FileTemplate): Boolean {
        return template.isTemplateOfType(LuaFileType.INSTANCE)
    }
}