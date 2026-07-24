package com.tang.intellij.lua.debugger.attach

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.util.ExecUtil
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import com.tang.intellij.lua.psi.LuaFileUtil
import java.io.File
import java.nio.charset.Charset

/**
 * Path to emmy_tool.exe used for process listing and architecture detection.
 * Prefers x86 (works on both arches), falls back to x64.
 * Returns null if neither is found.
 */
fun resolveToolPath(): String? =
    LuaFileUtil.getPluginVirtualFile("debugger/emmy/windows/x86/emmy_tool.exe")
        ?: LuaFileUtil.getPluginVirtualFile("debugger/emmy/windows/x64/emmy_tool.exe")

/** @deprecated Use [resolveToolPath] directly. Kept for call-site compatibility. */
val archToolPath: String?
    get() = resolveToolPath()

fun listProcesses(): Map<Int, ProcessDetailInfo> {
    val toolPath = resolveToolPath() ?: return emptyMap()
    val output = ExecUtil.execAndGetOutput(
        GeneralCommandLine(toolPath).apply { addParameter("list_processes") }
    )
    return parseProcessOutput(output.stdout)
}

fun listProcessesByEncoding(encoding: String): List<ProcessDetailInfo> {
    val toolPath = resolveToolPath() ?: return emptyList()
    val output = ExecUtil.execAndGetOutput(
        GeneralCommandLine(toolPath).apply {
            charset = Charset.forName(encoding)
            addParameter("list_processes")
        }
    )
    return parseProcessOutput(output.stdout).values.toList()
}

private fun parseProcessOutput(text: String): Map<Int, ProcessDetailInfo> {
    val result = mutableMapOf<Int, ProcessDetailInfo>()
    val lines = text.split("\n")
    val count = lines.size / 4
    for (i in 0 until count) {
        val pid   = lines[i * 4 + 0].trim().toIntOrNull() ?: continue
        val title = lines[i * 4 + 1]
        val path  = lines[i * 4 + 2]
        result[pid] = ProcessDetailInfo(pid, path, title)
    }
    return result
}
