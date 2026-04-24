package com.tang.intellij.lua.debugger.attach

import com.intellij.execution.process.ProcessInfo
import com.intellij.xdebugger.XDebugSession
import com.tang.intellij.lua.debugger.emmy.EmmyDebugProcessBase
import com.tang.intellij.lua.debugger.transport.TransportFactory
import com.tang.intellij.lua.debugger.transport.TransportMode

/**
 * Debug process that attaches to an already-running process selected via
 * the "Attach to Process" menu (Run → Attach to Process…).
 *
 * Injection flow:
 *  1. Detect process architecture by PID.
 *  2. Inject emmy_hook.dll via emmy_tool.exe attach.
 *  3. Connect to the injected debugger on IPv6 localhost, port derived from PID.
 */
class EmmyAttachDebugProcess(
    session: XDebugSession,
    private val processInfo: ProcessInfo
) : EmmyDebugProcessBase(session) {

    override fun setupTransport() {
        val arch = detectArchByPid(processInfo.pid)
        val success = attachToPid(processInfo.pid, arch, this)
        if (!success) {
            session.stop()
            return
        }

        val port = pidToPort(processInfo.pid)
        transport = TransportFactory.create(TransportMode.CLIENT, "127.0.0.1", port).apply {
            handler = StandardTransportHandler()
            start()
        }
    }
}
