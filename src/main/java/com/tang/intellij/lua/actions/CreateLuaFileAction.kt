package com.tang.intellij.lua.actions

import com.intellij.ide.actions.CreateFileFromTemplateAction
import com.intellij.ide.actions.CreateFileFromTemplateDialog
import com.intellij.psi.PsiDirectory
import com.tang.intellij.lua.lang.LuaIcons
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;


/**
 *
 * Created by tangzx on 2016/12/24.
 */
class CreateLuaFileAction : CreateFileFromTemplateAction(CREATE_LUA_FILE, "", LuaIcons.FILE), DumbAware {
    override fun buildDialog(project: Project, directory: PsiDirectory, builder: CreateFileFromTemplateDialog.Builder) {
        builder.setTitle(CREATE_LUA_FILE)
            .addKind("Source File", LuaIcons.FILE, "NewLua.lua")
    }

    override fun getActionName(psiDirectory: PsiDirectory, s: String, s1: String): String {
        return CREATE_LUA_FILE
    }

    companion object {
        private const val CREATE_LUA_FILE = "New Lua File"
    }
}

