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
import com.intellij.xdebugger.XDebugSession
import com.tang.intellij.lua.debugger.LogConsoleType
import com.tang.intellij.lua.debugger.model.DebugCommand
import com.tang.intellij.lua.debugger.transport.TransportFactory
import com.tang.intellij.lua.debugger.transport.TransportHandler
import com.tang.intellij.lua.debugger.transport.TransportMode

/**
 * Emmy Debug Process for the "Emmy Debugger(NEW)" configuration type.
 *
 * Supports two TCP modes configured via [EmmyDebugConfiguration]:
 *  - TCP_CLIENT: IDE connects to a running Lua process.
 *  - TCP_SERVER: IDE listens; the Lua process connects to the IDE.
 */
class EmmyDebugProcess(session: XDebugSession) : EmmyDebugProcessBase(session) {

    private val configuration = session.runProfile as EmmyDebugConfiguration

    override fun getSourceRoots(): List<String> = configuration.sourceRoots

    override fun setupTransport() {
        val mode = when (configuration.type) {
            EmmyDebugTransportType.TCP_CLIENT -> TransportMode.CLIENT
            EmmyDebugTransportType.TCP_SERVER -> TransportMode.SERVER
        }

        transport = TransportFactory.create(mode, configuration.host, configuration.port).apply {
            handler = EmmyTransportHandler()
            start()
        }
    }

    /**
     * Transport handler with server-mode reconnect support.
     *
     * In SERVER mode the IDE keeps the server socket alive so the Lua process
     * can reconnect after a previous session ends, without restarting the IDE.
     */
    private inner class EmmyTransportHandler : TransportHandler {

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

                if (configuration.type == EmmyDebugTransportType.TCP_SERVER) {
                    // Keep the server socket alive; ServerTransport will accept the next connection.
                    println(
                        "Server mode: waiting for new connection...",
                        LogConsoleType.NORMAL,
                        ConsoleViewContentType.SYSTEM_OUTPUT
                    )
                    breakpointManager.clear()
                } else {
                    stop()
                    session.stop()
                }
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
}
