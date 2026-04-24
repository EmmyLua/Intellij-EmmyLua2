package com.tang.intellij.lua.debugger.launch

import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebugProcessStarter
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.XDebuggerManager
import com.tang.intellij.lua.debugger.LuaRunner

class EmmyLaunchRunner : LuaRunner() {

    companion object {
        const val ID = "lua.emmyLaunch.runner"
    }

    private var configuration: EmmyLaunchDebugConfiguration? = null

    override fun getRunnerId() = ID

    override fun canRun(executorId: String, runProfile: RunProfile): Boolean {
        if (DefaultDebugExecutor.EXECUTOR_ID == executorId && runProfile is EmmyLaunchDebugConfiguration) {
            configuration = runProfile
            return true
        }
        return false
    }

    override fun doExecute(state: RunProfileState, environment: ExecutionEnvironment): RunContentDescriptor {
        val cfg = configuration!!
        val session = XDebuggerManager.getInstance(environment.project)
            .startSession(environment, object : XDebugProcessStarter() {
                override fun start(session: XDebugSession): XDebugProcess =
                    EmmyLaunchDebugProcess(session, cfg)
            })
        return session.runContentDescriptor
    }
}
