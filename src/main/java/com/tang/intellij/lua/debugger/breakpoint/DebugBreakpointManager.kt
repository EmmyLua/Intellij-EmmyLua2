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

package com.tang.intellij.lua.debugger.breakpoint

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.xdebugger.XDebuggerManager
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.breakpoints.XLineBreakpoint
import com.redhat.devtools.lsp4ij.dap.breakpoints.DAPBreakpointType
import com.tang.intellij.lua.debugger.LuaLineBreakpointType
import com.tang.intellij.lua.debugger.model.AddBreakpointRequest
import com.tang.intellij.lua.debugger.model.DebugBreakpoint
import com.tang.intellij.lua.debugger.model.RemoveBreakpointRequest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Manages breakpoints for Emmy debugger
 *
 * Responsibilities:
 * - Track breakpoints and assign unique IDs
 * - Convert between IntelliJ breakpoints and Emmy protocol breakpoints
 * - Synchronize breakpoints with the debugger
 */
class DebugBreakpointManager(private val project: Project) {

    private val logger = Logger.getInstance(javaClass)

    // Breakpoint ID management
    private val idCounter = AtomicInteger(0)
    private val breakpointById = ConcurrentHashMap<Int, DebugBreakpoint>()
    private val idByBreakpoint = ConcurrentHashMap<XLineBreakpoint<*>, Int>()

    // Callback for sending breakpoint requests
    var onSendRequest: ((request: Any) -> Unit)? = null

    companion object {
        /**
         * User data key for storing breakpoint ID
         */
        private val BREAKPOINT_ID_KEY = Key.create<Int>("lua.debugger.breakpoint.id")
    }

    /**
     * Initialize breakpoints - send all existing breakpoints to debugger
     * This should be called after the debugger connects
     */
    fun initializeBreakpoints() {
        ApplicationManager.getApplication().runReadAction {
            val breakpoints = getAllLuaBreakpoints()

            logger.info("Initializing ${breakpoints.size} breakpoints")

            val debugBreakpoints = mutableListOf<DebugBreakpoint>()

            breakpoints.forEach { xBreakpoint ->
                xBreakpoint.sourcePosition?.let { position ->
                    convertToDebugBreakpoint(position, xBreakpoint)?.let { debugBp ->
                        val id = registerBreakpoint(xBreakpoint, debugBp)
                        xBreakpoint.putUserData(BREAKPOINT_ID_KEY, id)
                        debugBreakpoints.add(debugBp)

                        logger.info("Registered breakpoint: ${debugBp.file}:${debugBp.line} (ID: $id)")
                    }
                }
            }

            if (debugBreakpoints.isNotEmpty()) {
                sendAddRequest(debugBreakpoints)
            }
        }
    }

    /**
     * Register a new breakpoint
     */
    fun onBreakpointAdded(position: XSourcePosition, xBreakpoint: XLineBreakpoint<*>) {
        convertToDebugBreakpoint(position, xBreakpoint)?.let { debugBp ->
            val id = registerBreakpoint(xBreakpoint, debugBp)
            xBreakpoint.putUserData(BREAKPOINT_ID_KEY, id)

            logger.info("Added breakpoint: ${debugBp.file}:${debugBp.line} (ID: $id)")

            sendAddRequest(listOf(debugBp))
        }
    }

    /**
     * Unregister a breakpoint
     */
    fun onBreakpointRemoved(position: XSourcePosition, xBreakpoint: XLineBreakpoint<*>) {
        val id = xBreakpoint.getUserData(BREAKPOINT_ID_KEY) ?: return
        val debugBp = breakpointById.remove(id) ?: return
        idByBreakpoint.remove(xBreakpoint)

        logger.info("Removed breakpoint: ${debugBp.file}:${debugBp.line} (ID: $id)")

        sendRemoveRequest(listOf(debugBp))
    }

    /**
     * Clear all breakpoints
     */
    fun clear() {
        breakpointById.clear()
        idByBreakpoint.clear()
        idCounter.set(0)
    }

    /**
     * Get breakpoint by source position
     */
    fun getBreakpoint(position: XSourcePosition): XLineBreakpoint<*>? {
        return getAllLuaBreakpoints().find { bp ->
            bp.sourcePosition?.let { pos ->
                pos.file == position.file && pos.line == position.line
            } ?: false
        }
    }

    // Private helper methods

    private fun registerBreakpoint(xBreakpoint: XLineBreakpoint<*>, debugBp: DebugBreakpoint): Int {
        val id = idCounter.getAndIncrement()
        breakpointById[id] = debugBp
        idByBreakpoint[xBreakpoint] = id
        return id
    }

    private fun getAllLuaBreakpoints(): Collection<XLineBreakpoint<*>> {
        return XDebuggerManager.getInstance(project)
            .breakpointManager
            .allBreakpoints.filter { xBreakpoint ->
                xBreakpoint is XLineBreakpoint<*> &&
                        (xBreakpoint.type is LuaLineBreakpointType || xBreakpoint.type is DAPBreakpointType)
            }.map { it as XLineBreakpoint<*> }
    }

    private fun convertToDebugBreakpoint(
        position: XSourcePosition,
        xBreakpoint: XLineBreakpoint<*>
    ): DebugBreakpoint? {
        val file = position.file.canonicalPath ?: return null
        val line = position.line + 1 // Convert to 1-based

        return if (xBreakpoint.isLogMessage) {
            // Log point
            DebugBreakpoint(
                file = file,
                line = line,
                condition = null,
                logMessage = xBreakpoint.logExpressionObject?.expression
            )
        } else {
            // Regular breakpoint with optional condition
            DebugBreakpoint(
                file = file,
                line = line,
                condition = xBreakpoint.conditionExpression?.expression
            )
        }
    }

    private fun sendAddRequest(breakpoints: List<DebugBreakpoint>) {
        onSendRequest?.invoke(AddBreakpointRequest(breakpoints))
    }

    private fun sendRemoveRequest(breakpoints: List<DebugBreakpoint>) {
        onSendRequest?.invoke(RemoveBreakpointRequest(breakpoints))
    }
}
