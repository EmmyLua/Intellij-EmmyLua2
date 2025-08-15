package com.cppcxy.ide.lsp

import com.intellij.openapi.project.Project
import com.redhat.devtools.lsp4ij.LanguageServerFactory
import com.redhat.devtools.lsp4ij.server.StreamConnectionProvider
import org.eclipse.lsp4j.services.LanguageServer

class EmmyLuaAnalyzerFactory : LanguageServerFactory {
    override fun createConnectionProvider(project: Project): StreamConnectionProvider {
        return EmmyLuaAnalyzerServer(project)
    }

    override fun getServerInterface(): Class<out LanguageServer?> {
        return EmmyLuaCustomApi::class.java
    }
}