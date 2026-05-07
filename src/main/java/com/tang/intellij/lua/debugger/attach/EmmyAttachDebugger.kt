package com.tang.intellij.lua.debugger.attach

import com.intellij.execution.process.ProcessInfo
import com.intellij.openapi.project.Project
import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebugProcessStarter
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.XDebuggerManager
import com.intellij.xdebugger.attach.XLocalAttachDebugger

/**
 * Implements the "Attach to Process" entry for a specific Lua process.
 * Created by [EmmyAttachDebuggerProvider] for each process that has a Lua runtime.
 */
class EmmyAttachDebugger(
    private val processInfo: ProcessInfo,
    private val detailInfo: ProcessDetailInfo
) : XLocalAttachDebugger {

    override fun getDebuggerDisplayName(): String = getDisplayName(processInfo, detailInfo)

    override fun attachDebugSession(project: Project, processInfo: ProcessInfo) {
        val displayName = "PID:${processInfo.pid}($debuggerDisplayName)"
        XDebuggerManager.getInstance(project)
            .startSessionAndShowTab(displayName, null, object : XDebugProcessStarter() {
                override fun start(session: XDebugSession): XDebugProcess =
                    EmmyAttachDebugProcess(session, processInfo)
            })
    }
}
