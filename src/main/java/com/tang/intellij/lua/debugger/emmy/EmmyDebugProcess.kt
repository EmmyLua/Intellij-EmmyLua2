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
import com.tang.intellij.lua.debugger.transport.TransportFactory
import com.tang.intellij.lua.debugger.transport.TransportHandler
import com.tang.intellij.lua.debugger.transport.TransportMode
import com.tang.intellij.lua.psi.LuaFileManager
import com.tang.intellij.lua.psi.LuaFileUtil
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Modern Emmy Debug Process
 *
 * Clean separation of concerns:
 * - Transport layer handles communication
 * - Breakpoint manager handles breakpoints
 * - This class coordinates between IntelliJ and Emmy debugger
 */
class EmmyDebugProcess(session: XDebugSession) : LuaDebugProcess(session) {

    private val logger = Logger.getInstance(javaClass)
    private val configuration = session.runProfile as EmmyDebugConfiguration
    private val editorsProvider = LuaDebuggerEditorsProvider()

    // Core components
    private var transport: DebugTransport? = null
    private val breakpointManager = DebugBreakpointManager(session.project)

    // Evaluation handlers
    private val evalHandlers = ConcurrentHashMap<Int, EvalHandler>()

    // State
    private var isConnected = false

    /**
     * Evaluation handler interface
     */
    interface EvalHandler {
        fun onSuccess(variable: DebugVariable)
        fun onError(error: String)
    }

    // ================================================================================================
    // LIFECYCLE
    // ================================================================================================

    override fun sessionInitialized() {
        super.sessionInitialized()

        // Connect breakpoint manager to transport
        breakpointManager.onSendRequest = { request ->
            send(request as DebugMessage)
        }

        // Start transport on background thread
        ApplicationManager.getApplication().executeOnPooledThread {
            setupTransport()
        }
    }

    override fun stop() {
        logger.info("Stopping debug session")

        // Send stop command
        send(DebugActionRequest(DebugAction.Stop))

        // Cleanup
        transport?.stop()
        transport = null
        breakpointManager.clear()
        evalHandlers.clear()
        isConnected = false
    }

    // ================================================================================================
    // TRANSPORT SETUP
    // ================================================================================================

    private fun setupTransport() {
        try {
            // Create transport based on configuration
            val mode = when (configuration.type) {
                EmmyDebugTransportType.TCP_CLIENT -> TransportMode.CLIENT
                EmmyDebugTransportType.TCP_SERVER -> TransportMode.SERVER
            }

            transport = TransportFactory.create(mode, configuration.host, configuration.port).apply {
                handler = TransportHandlerImpl()
                start()
            }

        } catch (e: Exception) {
            logger.error("Failed to setup transport", e)
            error(e.localizedMessage ?: "Failed to setup transport")
            stop()
        }
    }

    /**
     * Transport event handler implementation
     */
    private inner class TransportHandlerImpl : TransportHandler {
        override fun onConnect(success: Boolean) {
            if (success) {
                isConnected = true
                ApplicationManager.getApplication().runReadAction {
                    sendInitialization()
                }
            } else {
                stop()
            }
        }

        override fun onDisconnect() {
            if (isConnected) {
                isConnected = false
                println("Disconnected from debugger", LogConsoleType.NORMAL, ConsoleViewContentType.SYSTEM_OUTPUT)
                stop()
                session.stop()
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

    // ================================================================================================
    // INITIALIZATION
    // ================================================================================================

    private fun sendInitialization() {
        // Send init request with Emmy helper code
        val helperPath = LuaFileUtil.getPluginVirtualFile("debugger/emmy/emmyHelper.lua")
        if (helperPath != null) {
            val code = File(helperPath).readText()
            val extensions = LuaFileManager.extensions
            send(InitRequest(code, extensions))
        } else {
            logger.error("Emmy helper file not found")
            val code = ""
            val extensions = LuaFileManager.extensions
            send(InitRequest(code, extensions))
        }

        breakpointManager.initializeBreakpoints()

        // Send ready signal
        send(ReadyRequest())
    }

    // ================================================================================================
    // MESSAGE HANDLING
    // ================================================================================================

    private fun handleMessage(command: DebugCommand, json: String) {
        try {
            when (command) {
                DebugCommand.BreakNotify -> handleBreakNotification(json)
                DebugCommand.EvalRsp -> handleEvalResponse(json)
                DebugCommand.LogNotify -> handleLogNotification(json)
                else -> logger.warn("Unhandled command: $command")
            }
        } catch (e: Exception) {
            logger.error("Error handling message: $command", e)
        }
    }

    private fun handleBreakNotification(json: String) {
        val notification = parseMessage<BreakpointNotification>(json) ?: return

        logger.info("Break at ${notification.stacks.firstOrNull()?.file}:${notification.stacks.firstOrNull()?.line}")

        // Create stack frames
        val frames = notification.stacks.map { EmmyDebugStackFrame(it, this) }

        // Find the best frame to show
        val topFrame = frames.firstOrNull { it.sourcePosition != null }
            ?: frames.firstOrNull { it.stackData.line > 0 }
            ?: frames.firstOrNull()

        if (topFrame == null) {
            logger.warn("No valid stack frame found")
            return
        }

        // Create execution stack
        val stack = LuaExecutionStack(frames)
        stack.setTopFrame(topFrame)

        // Check if we hit a breakpoint
        val sourcePos = topFrame.sourcePosition
        val breakpoint = if (sourcePos != null) {
            breakpointManager.getBreakpoint(sourcePos)
        } else null

        // Notify IntelliJ
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

        val handler = evalHandlers.remove(response.seq)
        if (handler != null) {
            if (response.success && response.value != null) {
                handler.onSuccess(response.value)
            } else {
                handler.onError(response.error ?: "Unknown error")
            }
        } else {
            logger.warn("No handler for eval response seq=${response.seq}")
        }
    }

    private fun handleLogNotification(json: String) {
        val notification = parseMessage<LogNotification>(json) ?: return

        val contentType = when (notification.type) {
            1 -> ConsoleViewContentType.LOG_WARNING_OUTPUT
            2 -> ConsoleViewContentType.ERROR_OUTPUT
            else -> ConsoleViewContentType.SYSTEM_OUTPUT
        }

        println(notification.message, LogConsoleType.NORMAL, contentType)
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
        // Add temporary breakpoint and continue
        position.file.canonicalPath?.let { file ->
            send(
                AddBreakpointRequest(
                    listOf(
                        DebugBreakpoint(file, position.line + 1, runToHere = true)
                    )
                )
            )
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

    /**
     * Evaluate expression at given stack level
     */
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

    private fun send(message: DebugMessage) {
        transport?.send(message)
    }
}
