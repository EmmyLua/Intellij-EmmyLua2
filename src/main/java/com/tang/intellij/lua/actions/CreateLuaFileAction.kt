package com.tang.intellij.lua.actions

import com.intellij.ide.actions.CreateFileFromTemplateAction
import com.intellij.ide.actions.CreateFileFromTemplateDialog
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.tang.intellij.lua.lang.LuaIcons

/**
 * Enhanced Lua file creation action with Java-style dialog
 */
class CreateLuaFileAction : CreateFileFromTemplateAction("Lua File", "Create a new Lua file", LuaIcons.FILE), DumbAware {
    
    override fun buildDialog(project: Project, directory: PsiDirectory, builder: CreateFileFromTemplateDialog.Builder) {
        builder.setTitle("New Lua Class/File")
            .addKind("Class", LuaIcons.FILE, "LuaClass.lua")
            .addKind("Module", LuaIcons.FILE, "LuaModule.lua")
            .addKind("Enum", LuaIcons.FILE, "LuaEnum.lua")
            .addKind("Unity", LuaIcons.FILE, "LuaUnity.lua")
            .addKind("Empty File", LuaIcons.FILE, "LuaEmpty.lua")
    }

    override fun getActionName(directory: PsiDirectory, newName: String, templateName: String): String {
        return "Create Lua File: $newName"
    }
}


