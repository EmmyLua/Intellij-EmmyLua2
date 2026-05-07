package com.tang.intellij.lua.debugger.attach

import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.openapi.wm.WindowManager
import com.intellij.xdebugger.XDebugSession
import com.tang.intellij.lua.debugger.LogConsoleType
import com.tang.intellij.lua.debugger.attach.resolveToolPath
import com.tang.intellij.lua.debugger.emmy.EmmyDebugProcessBase
import com.tang.intellij.lua.debugger.transport.TransportFactory
import com.tang.intellij.lua.debugger.transport.TransportMode

/**
 * Debug process for the "Emmy Attach Debugger" run configuration.
 *
 * Supports two attach modes configured in [EmmyAttachDebugConfiguration]:
 *  - **Pid**: attach directly to the specified process ID.
 *  - **ProcessName**: search the process list; if multiple matches are found
 *    a popup lets the user pick one.
 */
class EmmyConfigAttachDebugProcess(
    session: XDebugSession,
    private val configuration: EmmyAttachDebugConfiguration
) : EmmyDebugProcessBase(session) {

    private var resolvedPid: Int = 0

    override fun sessionInitialized() {
        when (configuration.attachMode) {
            EmmyAttachMode.Pid -> {
                resolvedPid = configuration.pid.toIntOrNull() ?: 0
                super.sessionInitialized()
            }
            EmmyAttachMode.ProcessName -> resolveByName()
        }
    }

    private fun resolveByName() {
        val name = configuration.processName
        val all = listProcessesByEncoding(configuration.encoding)
        val candidates = if (name.isBlank()) all
                         else all.filter {
                             it.title.contains(name, ignoreCase = true) ||
                             it.path.contains(name, ignoreCase = true)
                         }

        when (candidates.size) {
            1 -> {
                resolvedPid = candidates.first().pid
                super.sessionInitialized()
            }
            else -> {
                val pool = if (candidates.isEmpty()) all else candidates
                val title = when {
                    candidates.isEmpty() -> "No match for \"$name\" — pick from all processes"
                    else                 -> "Choose process to attach"
                }
                showProcessChooser(pool, title)
            }
        }
    }

    private fun showProcessChooser(pool: List<ProcessDetailInfo>, title: String) {
        if (pool.isEmpty()) {
            val toolPath = resolveToolPath()
            if (toolPath == null) {
                error("emmy_tool.exe not found. Searched paths:\n${resolveToolPathDiagnostic()}")
            } else {
                error("emmy_tool.exe found at $toolPath but returned no processes. " +
                      "Check that it can run on this machine (try running it manually).")
            }
            session.stop()
            return
        }

        val sorted = pool.sortedBy { it.title.ifBlank { it.path }.lowercase() }
        val displayMap = LinkedHashMap<String, ProcessDetailInfo>()
        for (p in sorted) {
            val label = buildString {
                append(p.pid.toString().padStart(6))
                append("  ")
                append(p.title.ifBlank { p.path.substringAfterLast('\\').substringAfterLast('/') })
                if (p.path.isNotBlank()) {
                    append("  [")
                    append(p.path.substringAfterLast('\\').substringAfterLast('/'))
                    append("]")
                }
            }
            displayMap[label] = p
        }

        val popup = JBPopupFactory.getInstance()
            .createPopupChooserBuilder(displayMap.keys.toList())
            .setTitle(title)
            .setMovable(true)
            .setItemChosenCallback { key ->
                resolvedPid = displayMap[key]?.pid ?: 0
                super.sessionInitialized()
            }
            .createPopup()

        popup.addListener(object : JBPopupListener {
            override fun onClosed(event: LightweightWindowEvent) {
                if (!event.isOk) session.stop()
            }
        })
        val frame = WindowManager.getInstance().getFrame(session.project)
        if (frame != null) popup.showInCenterOf(frame) else popup.showInFocusCenter()
    }

    private fun log(msg: String) =
        println(msg, LogConsoleType.NORMAL, ConsoleViewContentType.SYSTEM_OUTPUT)

    override fun setupTransport() {
        if (resolvedPid == 0) {
            session.stop()
            return
        }

        val arch = when (configuration.winArch) {
            EmmyAttachWinArch.X86  -> EmmyWinArch.X86
            EmmyAttachWinArch.X64  -> EmmyWinArch.X64
            EmmyAttachWinArch.Auto -> {
                val detected = detectArchByPid(resolvedPid)
                log("Detected process arch: $detected (pid=$resolvedPid)")
                detected
            }
        }
        log("Attaching to pid=$resolvedPid arch=$arch")
        val success = attachToPid(resolvedPid, arch, this)
        if (!success) {
            error("Injection failed for pid=$resolvedPid arch=$arch. " +
                  "If the arch is wrong, set it manually in the run configuration. " +
                  "If the process is elevated, run the IDE as administrator.")
            session.stop()
            return
        }

        val port = pidToPort(resolvedPid)
        log("Connecting to debugger on port $port")
        transport = TransportFactory.create(TransportMode.CLIENT, "127.0.0.1", port).apply {
            handler = StandardTransportHandler()
            start()
        }
    }
}
