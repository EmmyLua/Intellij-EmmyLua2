package com.tang.intellij.lua.debugger.launch

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ColoredProcessHandler
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.util.Key
import com.intellij.xdebugger.XDebugSession
import com.tang.intellij.lua.debugger.LogConsoleType
import com.tang.intellij.lua.debugger.attach.detectArchByFile
import com.tang.intellij.lua.debugger.attach.pidToPort
import com.tang.intellij.lua.debugger.emmy.EmmyDebugProcessBase
import com.tang.intellij.lua.debugger.transport.TransportFactory
import com.tang.intellij.lua.debugger.transport.TransportMode
import com.tang.intellij.lua.psi.LuaFileUtil
import java.nio.charset.Charset

/**
 * Debug process that launches a new executable with the Emmy hook injected,
 * then connects to the injected debugger.
 *
 * Launch flow:
 *  1. Detect executable architecture.
 *  2. Run emmy_tool.exe launch (which starts the target and prints its PID).
 *  3. Derive the TCP port from the PID and connect.
 *  4. Signal emmy_tool.exe that the IDE is connected.
 */
class EmmyLaunchDebugProcess(
    session: XDebugSession,
    private val configuration: EmmyLaunchDebugConfiguration
) : EmmyDebugProcessBase(session) {

    private var toolProcessHandler: ColoredProcessHandler? = null

    override fun setupTransport() {
        launchTarget { pid ->
            val port = pidToPort(pid)
            transport = TransportFactory.create(TransportMode.CLIENT, "::1", port).apply {
                handler = StandardTransportHandler()
                start()
            }
            // Tell emmy_tool.exe that the IDE has connected so it can unblock the target.
            toolProcessHandler?.processInput?.let { out ->
                out.write("connected\n".toByteArray())
                out.flush()
            }
        }
    }

    private fun launchTarget(onConnected: (pid: Int) -> Unit) {
        val arch = detectArchByFile(configuration.program)
        val dirPath = LuaFileUtil.getPluginVirtualFile("debugger/emmy/windows/$arch")
            ?: run { error("emmy_tool.exe not found for arch $arch"); return }

        val commandLine = GeneralCommandLine("$dirPath/emmy_tool.exe").apply {
            setWorkDirectory(dirPath)
            addParameter("launch")
            if (configuration.useWindowsTerminal) addParameter("-create-new-window")
            addParameters(
                "-dll", "emmy_hook.dll",
                "-dir", "\"$dirPath\"",
                "-work", "\"${configuration.workingDirectory}\"",
                "-exe", "\"${configuration.program}\"",
                "-args", configuration.parameter
            )
            charset = Charset.forName("utf8")
        }

        var pidRead = false
        toolProcessHandler = ColoredProcessHandler(commandLine).also { handler ->
            handler.addProcessListener(object : ProcessListener {
                override fun startNotified(e: ProcessEvent) {}
                override fun processTerminated(e: ProcessEvent) { toolProcessHandler = null }
                override fun processWillTerminate(e: ProcessEvent, willBeDestroyed: Boolean) {}
                override fun onTextAvailable(e: ProcessEvent, outputType: Key<*>) {
                    when (outputType) {
                        ProcessOutputTypes.STDERR ->
                            print(e.text, LogConsoleType.NORMAL, ConsoleViewContentType.ERROR_OUTPUT)
                        ProcessOutputTypes.STDOUT -> {
                            if (!pidRead) {
                                pidRead = true
                                val pid = e.text.trim().toIntOrNull() ?: return
                                onConnected(pid)
                            } else {
                                print(e.text, LogConsoleType.NORMAL, ConsoleViewContentType.SYSTEM_OUTPUT)
                            }
                        }
                    }
                }
            })
            handler.startNotify()
        }
    }

    override fun onTransportDisconnect() {
        toolProcessHandler?.processInput?.let { out ->
            out.write("close\n".toByteArray())
            out.flush()
        }
        toolProcessHandler = null
        super.onTransportDisconnect()
    }
}
