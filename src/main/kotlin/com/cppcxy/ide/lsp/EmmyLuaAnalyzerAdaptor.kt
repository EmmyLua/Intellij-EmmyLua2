package com.cppcxy.ide.lsp

import com.cppcxy.ide.setting.EmmyLuaSettings
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.util.SystemInfoRt
import java.io.File

object EmmyLuaAnalyzerAdaptor {
    private val pluginSource: String?
        get() = PluginManagerCore.getPlugin(PluginId.getId("com.cppcxy.Intellij-EmmyLua"))?.pluginPath?.toFile()?.path

    private val exe: String
        get() {
            return if (SystemInfoRt.isWindows) {
                "win32-x64/emmylua_ls.exe"
            } else if (SystemInfoRt.isMac) {
                if (System.getProperty("os.arch") == "arm64") {
                    "darwin-arm64/emmylua_ls"
                } else {
                    "darwin-x64/emmylua_ls"
                }
            } else {
                "linux-x64/emmylua_ls"
            }
        }

    val emmyLuaLanguageServer: String
        get() {
            if (EmmyLuaSettings.getInstance().location.isNotEmpty()) {
                return EmmyLuaSettings.getInstance().location
            }
            return "$pluginSource/server/$exe"
        }

    val canExecute: Boolean
        get() {
            val file = File(emmyLuaLanguageServer)
            return file.exists() && file.canExecute()
        }

    fun addExecutePermission() {
        val file = File(emmyLuaLanguageServer)
        if (file.exists() && !file.canExecute()) {
            val runtime = Runtime.getRuntime()
            val process = runtime.exec(arrayOf("chmod", "u+x", file.absolutePath))
            process.waitFor()
        }
    }
}
