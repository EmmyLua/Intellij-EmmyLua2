package com.cppcxy.ide.lsp

import com.cppcxy.ide.setting.LuaRuntimeConfiguration
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.project.Project
import com.redhat.devtools.lsp4ij.server.OSProcessStreamConnectionProvider


class EmmyLuaAnalyzerServer (val project: Project) : OSProcessStreamConnectionProvider() {
    init {
        if (!EmmyLuaAnalyzerAdaptor.canExecute) {
            EmmyLuaAnalyzerAdaptor.addExecutePermission()
        }

        val commandLine = GeneralCommandLine(EmmyLuaAnalyzerAdaptor.emmyLuaLanguageServer)
        commandLine.withEnvironment("EMMYLUALS_CONFIG", LuaRuntimeConfiguration.get(project).serverConfigPath())
        super.setCommandLine(commandLine)
    }
}