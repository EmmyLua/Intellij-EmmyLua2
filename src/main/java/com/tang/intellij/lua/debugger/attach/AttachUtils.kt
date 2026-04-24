package com.tang.intellij.lua.debugger.attach

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.util.Key
import com.tang.intellij.lua.debugger.LogConsoleType
import com.tang.intellij.lua.debugger.emmy.EmmyDebugProcessBase
import com.tang.intellij.lua.psi.LuaFileUtil

enum class EmmyWinArch(val desc: String) {
    X86("x86"), X64("x64");

    override fun toString() = desc
}

/**
 * Detect the architecture of a running process by PID.
 * Exit code 0 → x64, non-zero → x86.
 */
fun detectArchByPid(pid: Int): EmmyWinArch {
    val tool = resolveToolPath() ?: return EmmyWinArch.X64
    val process = GeneralCommandLine(tool)
        .apply { addParameters("arch_pid", "$pid") }
        .createProcess()
    process.waitFor()
    return if (process.exitValue() == 0) EmmyWinArch.X64 else EmmyWinArch.X86
}

/**
 * Detect the architecture of an executable file.
 * Exit code 0 → x64, non-zero → x86.
 */
fun detectArchByFile(exePath: String): EmmyWinArch {
    val tool = LuaFileUtil.getPluginVirtualFile("debugger/emmy/windows/x64/emmy_tool.exe")
        ?: resolveToolPath()
        ?: return EmmyWinArch.X64
    val process = GeneralCommandLine(tool)
        .apply { addParameters("arch_file", exePath) }
        .createProcess()
    process.waitFor()
    return if (process.exitValue() == 0) EmmyWinArch.X64 else EmmyWinArch.X86
}

/**
 * Map a PID to a TCP port in the range [0x400, 0xFFFF].
 */
fun pidToPort(pid: Int): Int {
    var port = pid
    while (port > 0xffff) port -= 0xffff
    while (port < 0x400) port += 0x400
    return port
}

/**
 * Inject emmy_hook.dll into [pid] using emmy_tool.exe.
 * Logs stdout/stderr to the debug console of [process].
 * @return true if injection succeeded (exit code 0).
 */
fun attachToPid(pid: Int, arch: EmmyWinArch, process: EmmyDebugProcessBase): Boolean {
    val dirPath = LuaFileUtil.getPluginVirtualFile("debugger/emmy/windows/$arch") ?: return false
    // SetDllDirectoryA 在部分 Windows 版本下不接受正斜杠，统一转为反斜杠
    val nativeDirPath = dirPath.replace('/', '\\')
    val commandLine = GeneralCommandLine("$nativeDirPath\\emmy_tool.exe").apply {
        addParameters("attach", "-p", "$pid", "-dir", nativeDirPath, "-dll", "emmy_hook.dll")
    }
    val handler = OSProcessHandler(commandLine)
    handler.addProcessListener(object : ProcessListener {
        override fun startNotified(e: ProcessEvent) {}
        override fun processTerminated(e: ProcessEvent) {}
        override fun processWillTerminate(e: ProcessEvent, willBeDestroyed: Boolean) {}
        override fun onTextAvailable(e: ProcessEvent, outputType: Key<*>) {
            val contentType = if (outputType == ProcessOutputTypes.STDERR)
                ConsoleViewContentType.ERROR_OUTPUT
            else
                ConsoleViewContentType.SYSTEM_OUTPUT
            process.print(e.text, LogConsoleType.NORMAL, contentType)
        }
    })
    handler.startNotify()
    handler.waitFor()
    return handler.exitCode == 0
}
