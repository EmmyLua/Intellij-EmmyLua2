/*
 * Copyright (c) 2017. tangzx(love.tangzx@qq.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tang.intellij.lua.debugger.emmy

import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.breakpoints.XBreakpointHandler
import com.intellij.xdebugger.breakpoints.XBreakpointProperties
import com.intellij.xdebugger.breakpoints.XLineBreakpoint
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider
import com.intellij.xdebugger.frame.XSuspendContext
import com.tang.intellij.lua.debugger.*
import com.tang.intellij.lua.debugger.breakpoint.DebugBreakpointManager
import com.tang.intellij.lua.debugger.model.*
import com.tang.intellij.lua.debugger.transport.DebugTransport
import com.tang.intellij.lua.debugger.transport.TransportHandler
import com.tang.intellij.lua.psi.LuaFileManager
import com.tang.intellij.lua.psi.LuaFileUtil
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Base class for Emmy debugger processes.
 *
 * Handles all Emmy protocol logic (messages, breakpoints, evaluation, debug actions).
 * Subclasses only need to implement [setupTransport] to establish the connection.
 */
abstract class EmmyDebugProcessBase(session: XDebugSession) : LuaDebugProcess(session) {

    private val logger = Logger.getInstance(javaClass)
    private val editorsProvider = LuaDebuggerEditorsProvider()

    // Core components
    protected var transport: DebugTransport? = null
    val breakpointManager = DebugBreakpointManager(session.project)

    // Evaluation handlers keyed by request sequence number
    private val evalHandlers = ConcurrentHashMap<Int, EvalHandler>()

    protected var isConnected = false

    /**
     * Evaluation result callback interface.
     */
    interface EvalHandler {
        fun onSuccess(variable: DebugVariable)
        fun onError(error: String)
    }

    /**
     * Source roots used for resolving Lua file paths to source files.
     * Override in subclasses that expose a source-roots configuration field.
     */
    open fun getSourceRoots(): List<String> = emptyList()

    // ================================================================================================
    // LIFECYCLE
    // ================================================================================================

    override fun sessionInitialized() {
        super.sessionInitialized()

        breakpointManager.onSendRequest = { request ->
            send(request as DebugMessage)
        }

        ApplicationManager.getApplication().executeOnPooledThread {
            setupTransport()
        }
    }

    /**
     * Establish the transport connection.
     * Subclasses must create [transport], assign a handler, and start it.
     * Use [StandardTransportHandler] for the typical connect → init → run flow.
     */
    protected abstract fun setupTransport()

    override fun stop() {
        logger.info("Stopping debug session")
        send(DebugActionRequest(DebugAction.Stop))
        transport?.stop()
        transport = null
        breakpointManager.clear()
        evalHandlers.clear()
        isConnected = false
    }

    // ================================================================================================
    // INITIALIZATION
    // ================================================================================================

    protected fun sendInitialization() {
        val helperPath = LuaFileUtil.getPluginVirtualFile("debugger/emmy/emmyHelper.lua")
        val code = if (helperPath != null) {
            File(helperPath).readText()
        } else {
            logger.error("Emmy helper file not found")
            ""
        }
        val extensions = LuaFileManager.extensions
        send(InitRequest(code, extensions))
        breakpointManager.initializeBreakpoints()
        send(ReadyRequest())
    }

    // ================================================================================================
    // MESSAGE HANDLING
    // ================================================================================================

    protected fun handleMessage(command: DebugCommand, json: String) {
        try {
            when (command) {
                DebugCommand.BreakNotify    -> handleBreakNotification(json)
                DebugCommand.EvalRsp        -> handleEvalResponse(json)
                DebugCommand.LogNotify      -> handleLogNotification(json)
                DebugCommand.AttachedNotify -> handleAttachedNotification(json)
                else -> logger.warn("Unhandled command: $command")
            }
        } catch (e: Exception) {
            logger.error("Error handling message: $command", e)
        }
    }

    private fun handleBreakNotification(json: String) {
        val notification = parseMessage<BreakpointNotification>(json) ?: return

        val frames = notification.stacks.map { EmmyDebugStackFrame(it, this) }
        val topFrame = frames.firstOrNull { it.sourcePosition != null }
            ?: frames.firstOrNull { it.stackData.line > 0 }
            ?: frames.firstOrNull()

        if (topFrame == null) {
            logger.warn("No valid stack frame found")
            return
        }

        val stack = LuaExecutionStack(frames)
        stack.setTopFrame(topFrame)

        val sourcePos = topFrame.sourcePosition
        val breakpoint = if (sourcePos != null) breakpointManager.getBreakpoint(sourcePos) else null

        ApplicationManager.getApplication().invokeLater {
            if (breakpoint != null) {
                session.breakpointReached(breakpoint, null, LuaSuspendContext(stack))
            } else {
                session.positionReached(LuaSuspendContext(stack))
            }
            session.showExecutionPoint()
        }
    }

    private fun handleEvalResponse(json: String) {
        val response = parseMessage<EvalResponse>(json) ?: return
        val handler = evalHandlers.remove(response.seq) ?: run {
            logger.warn("No handler for eval response seq=${response.seq}")
            return
        }
        if (response.success && response.value != null) {
            handler.onSuccess(response.value)
        } else {
            handler.onError(response.error ?: "Unknown error")
        }
    }

    private fun handleLogNotification(json: String) {
        val notification = parseMessage<LogNotification>(json) ?: return
        val contentType = when (notification.type) {
            1    -> ConsoleViewContentType.LOG_WARNING_OUTPUT
            2    -> ConsoleViewContentType.ERROR_OUTPUT
            else -> ConsoleViewContentType.SYSTEM_OUTPUT
        }
        println(notification.message, LogConsoleType.NORMAL, contentType)
    }

    /**
     * Called when the debugger sends an AttachedNotify message.
     * Override to log the attached Lua state address.
     */
    protected open fun handleAttachedNotification(json: String) {
        val notification = parseMessage<AttachedNotification>(json) ?: return
        println(
            "Attached to lua state 0x${notification.state.toString(16)}",
            LogConsoleType.NORMAL,
            ConsoleViewContentType.SYSTEM_OUTPUT
        )
    }

    // ================================================================================================
    // DEBUG ACTIONS
    // ================================================================================================

    override fun run() {
        send(DebugActionRequest(DebugAction.Continue))
    }

    override fun startPausing() {
        send(DebugActionRequest(DebugAction.Break))
    }

    override fun startStepOver(context: XSuspendContext?) {
        send(DebugActionRequest(DebugAction.StepOver))
    }

    override fun startStepInto(context: XSuspendContext?) {
        send(DebugActionRequest(DebugAction.StepIn))
    }

    override fun startStepOut(context: XSuspendContext?) {
        send(DebugActionRequest(DebugAction.StepOut))
    }

    override fun runToPosition(position: XSourcePosition, context: XSuspendContext?) {
        position.file.canonicalPath?.let { file ->
            send(AddBreakpointRequest(listOf(DebugBreakpoint(file, position.line + 1, runToHere = true))))
            run()
        }
    }

    // ================================================================================================
    // BREAKPOINT HANDLER
    // ================================================================================================

    override fun getBreakpointHandlers(): Array<XBreakpointHandler<*>> {
        return arrayOf(object : XBreakpointHandler<XLineBreakpoint<XBreakpointProperties<*>>>(
            LuaLineBreakpointType::class.java
        ) {
            override fun registerBreakpoint(breakpoint: XLineBreakpoint<XBreakpointProperties<*>>) {
                breakpoint.sourcePosition?.let { position ->
                    breakpointManager.onBreakpointAdded(position, breakpoint)
                }
            }

            override fun unregisterBreakpoint(
                breakpoint: XLineBreakpoint<XBreakpointProperties<*>>,
                temporary: Boolean
            ) {
                breakpoint.sourcePosition?.let { position ->
                    breakpointManager.onBreakpointRemoved(position, breakpoint)
                }
            }
        })
    }

    // ================================================================================================
    // EVALUATION
    // ================================================================================================

    override fun getEditorsProvider(): XDebuggerEditorsProvider = editorsProvider

    fun evaluate(
        expression: String,
        stackLevel: Int,
        cacheId: Int,
        depth: Int,
        handler: EvalHandler
    ) {
        val request = EvalRequest(expression, stackLevel, cacheId, depth)
        evalHandlers[request.seq] = handler
        send(request)
    }

    // ================================================================================================
    // UTILITY
    // ================================================================================================

    protected fun send(message: DebugMessage) {
        transport?.send(message)
    }

    /**
     * Standard transport handler for the typical attach/launch flow:
     * connect → sendInitialization → handle messages → stop on disconnect.
     *
     * For more complex reconnect behaviour (e.g. TCP server mode), subclasses
     * should implement their own [TransportHandler] instead.
     */
    protected inner class StandardTransportHandler : TransportHandler {

        override fun onConnect(success: Boolean) {
            if (success) {
                isConnected = true
                ApplicationManager.getApplication().runReadAction {
                    sendInitialization()
                }
            } else {
                stop()
                session.stop()
            }
        }

        override fun onDisconnect() {
            if (isConnected) {
                isConnected = false
                println("Disconnected from debugger", LogConsoleType.NORMAL, ConsoleViewContentType.SYSTEM_OUTPUT)
                onTransportDisconnect()
            }
        }

        override fun onMessage(command: DebugCommand, json: String) {
            handleMessage(command, json)
        }

        override fun onError(message: String, exception: Throwable?) {
            error(message)
        }

        override fun onLog(message: String) {
            println(message, LogConsoleType.NORMAL, ConsoleViewContentType.SYSTEM_OUTPUT)
        }
    }

    /**
     * Called by [StandardTransportHandler] when the connection drops.
     * Default behaviour: stop this process and the debug session.
     * Override in subclasses that need extra cleanup (e.g. killing a child process).
     */
    protected open fun onTransportDisconnect() {
        stop()
        session.stop()
    }
}
