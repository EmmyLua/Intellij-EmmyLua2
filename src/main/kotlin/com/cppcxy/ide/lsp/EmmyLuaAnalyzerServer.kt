package com.cppcxy.ide.lsp

import com.intellij.openapi.project.Project
import com.redhat.devtools.lsp4ij.server.ProcessStreamConnectionProvider

class EmmyLuaAnalyzerServer (val project: Project) : ProcessStreamConnectionProvider() {
    init {
        if (!EmmyLuaAnalyzerAdaptor.canExecute) {
            EmmyLuaAnalyzerAdaptor.addExecutePermission()
        }

        val commands = listOf(EmmyLuaAnalyzerAdaptor.emmyLuaLanguageServer)
        super.setCommands(commands)
    }
}