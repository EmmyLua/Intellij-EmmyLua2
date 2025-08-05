package com.cppcxy.ide.lsp

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.project.Project
import com.redhat.devtools.lsp4ij.server.OSProcessStreamConnectionProvider


class EmmyLuaAnalyzerServer (val project: Project) : OSProcessStreamConnectionProvider() {
    init {
        if (!EmmyLuaAnalyzerAdaptor.canExecute) {
            EmmyLuaAnalyzerAdaptor.addExecutePermission()
        }

        val commandLine = GeneralCommandLine(EmmyLuaAnalyzerAdaptor.emmyLuaLanguageServer)
        super.setCommandLine(commandLine)
    }
}