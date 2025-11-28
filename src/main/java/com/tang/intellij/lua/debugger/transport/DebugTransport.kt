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

package com.tang.intellij.lua.debugger.transport

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.tang.intellij.lua.debugger.model.DebugCommand
import com.tang.intellij.lua.debugger.model.DebugMessage
import com.tang.intellij.lua.debugger.model.parseCommand
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Transport connection mode
 */
enum class TransportMode {
    /** IDE connects to debugger (active connection) */
    CLIENT,

    /** IDE waits for debugger to connect (passive connection) */
    SERVER
}

/**
 * Handler for transport events
 */
interface TransportHandler {
    /**
     * Called when connection is established or failed
     * @param success true if connected successfully
     */
    fun onConnect(success: Boolean)

    /**
     * Called when connection is lost
     */
    fun onDisconnect()

    /**
     * Called when a message is received
     * @param command The command type
     * @param json The JSON payload
     */
    fun onMessage(command: DebugCommand, json: String)

    /**
     * Called when an error occurs
     * @param message Error message
     * @param exception Optional exception
     */
    fun onError(message: String, exception: Throwable? = null)

    /**
     * Called for log messages
     * @param message Log message
     */
    fun onLog(message: String)
}

/**
 * Base class for debug transport implementations
 * Handles the low-level communication with the Emmy debugger
 */
abstract class DebugTransport(
    protected val host: String,
    protected val port: Int
) {
    private val logger = Logger.getInstance(javaClass)

    var handler: TransportHandler? = null

    protected val messageQueue = LinkedBlockingQueue<DebugMessage>()
    protected val isRunning = AtomicBoolean(false)
    protected val isStopped = AtomicBoolean(false)

    /**
     * Start the transport connection
     */
    abstract fun start()

    /**
     * Stop the transport and clean up resources
     */
    open fun stop() {
        if (isStopped.getAndSet(true)) {
            return // Already stopped
        }

        isRunning.set(false)

        // Add stop signal to queue
        messageQueue.put(StopSignal)

        log("Transport stopped")
    }

    /**
     * Send a message to the debugger
     * @param message The message to send
     */
    fun send(message: DebugMessage) {
        if (isStopped.get()) {
            log("Cannot send message, transport is stopped")
            return
        }

        messageQueue.put(message)
    }

    /**
     * Check if transport is connected
     */
    abstract fun isConnected(): Boolean

    // Protected helper methods

    protected fun notifyConnected(success: Boolean) {
        if (success) {
            log("Connected to $host:$port")
        } else {
            log("Failed to connect to $host:$port")
        }
        handler?.onConnect(success)
    }

    protected fun notifyDisconnected() {
        log("Disconnected from $host:$port")
        handler?.onDisconnect()
    }

    protected fun notifyMessage(command: DebugCommand, json: String) {
        try {
            handler?.onMessage(command, json)
        } catch (e: Exception) {
            error("Error handling message: ${e.message}", e)
        }
    }

    protected fun error(message: String, exception: Throwable? = null) {
        logger.error(message, exception)
        handler?.onError(message, exception)
    }

    protected fun log(message: String) {
        logger.info(message)
        handler?.onLog(message)
    }
}

/**
 * Sentinel object to signal transport shutdown
 */
private object StopSignal : DebugMessage {
    override val cmd = -1
    override fun toJSON() = ""
}

/**
 * Socket-based transport using NIO channels
 */
abstract class SocketChannelTransport(
    host: String,
    port: Int
) : DebugTransport(host, port) {

    protected var socket: SocketChannel? = null

    /**
     * Start receive and send threads
     */
    protected fun startIO() {
        isRunning.set(true)

        // Start receiver thread
        ApplicationManager.getApplication().executeOnPooledThread {
            receiveLoop()
        }

        // Start sender thread
        ApplicationManager.getApplication().executeOnPooledThread {
            sendLoop()
        }
    }

    override fun stop() {
        super.stop()

        try {
            socket?.close()
        } catch (e: Exception) {
            error("Error closing socket", e)
        } finally {
            socket = null
        }
    }

    override fun isConnected(): Boolean {
        return socket?.isConnected == true
    }

    /**
     * Receive messages from debugger
     */
    private fun receiveLoop() {
        val inputStream = try {
            socket?.socket()?.getInputStream()
        } catch (e: Exception) {
            error("Failed to get input stream", e)
            isRunning.set(false)
            return
        }

        if (inputStream == null) {
            error("Input stream is null")
            isRunning.set(false)
            return
        }

        try {
            val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))

            while (isRunning.get() && !isStopped.get()) {
                try {
                    // Read command line
                    val cmdLine = reader.readLine() ?: break
                    val cmdValue = cmdLine.toIntOrNull() ?: continue

                    // Read JSON line
                    val json = reader.readLine() ?: break

                    // Parse and notify
                    val command = parseCommand(cmdValue)
                    notifyMessage(command, json)

                } catch (e: IOException) {
                    if (isRunning.get()) {
                        error("IO error while receiving", e)
                    }
                    break
                }
            }
        } catch (e: Exception) {
            error("Error in receive loop", e)
        } finally {
            // Set isRunning to false BEFORE notifying disconnect
            // This allows ServerTransport's accept loop to detect disconnection
            // and wait for new connections
            isRunning.set(false)
            notifyDisconnected()
            // Signal send thread to stop
            messageQueue.put(StopSignal)
        }
    }

    /**
     * Send messages to debugger
     */
    private fun sendLoop() {
        try {
            while (true) {
                val message = messageQueue.take()

                // Check for stop signal
                if (message === StopSignal || isStopped.get()) {
                    break
                }

                try {
                    val json = message.toJSON()
                    val data = "${message.cmd}\n$json\n"

                    socket?.write(ByteBuffer.wrap(data.toByteArray(Charsets.UTF_8)))
                        ?: break

                } catch (e: IOException) {
                    if (isRunning.get()) {
                        error("IO error while sending", e)
                    }
                    break
                }
            }
        } catch (e: Exception) {
            error("Error in send loop", e)
        }
    }
}

/**
 * Client transport - IDE connects to debugger
 * Use this when the debugger is already running and waiting for IDE connection
 */
class ClientTransport(
    host: String,
    port: Int
) : SocketChannelTransport(host, port) {

    override fun start() {
        log("Connecting to $host:$port...")

        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val channel = SocketChannel.open()
                val address = InetAddress.getByName(host)

                if (channel.connect(InetSocketAddress(address, port))) {
                    socket = channel
                    startIO()
                    notifyConnected(true)
                } else {
                    error("Failed to connect")
                    notifyConnected(false)
                }
            } catch (e: Exception) {
                error("Connection failed: ${e.message}", e)
                notifyConnected(false)
            }
        }
    }
}

/**
 * Server transport - IDE waits for debugger to connect
 * Use this when you want the debugger to initiate the connection
 * Supports reconnection - after client disconnects, will wait for new connection
 */
class ServerTransport(
    host: String,
    port: Int
) : SocketChannelTransport(host, port) {

    private var serverSocket: ServerSocketChannel? = null
    private val connectionLock = Object()
    
    override fun start() {
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val server = ServerSocketChannel.open()
                server.bind(InetSocketAddress(InetAddress.getByName(host), port))
                serverSocket = server

                log("Server listening on $host:$port, waiting for connection...")

                while (!isStopped.get()) {
                    try {
                        // Wait for connection
                        val channel = server.accept()

                        // Close any existing connection
                        synchronized(connectionLock) {
                            if (socket != null) {
                                try {
                                    socket?.close()
                                } catch (e: Exception) {
                                    // Ignore
                                }
                                socket = null
                            }
                        }
                        
                        // Setup new connection
                        synchronized(connectionLock) {
                            socket = channel
                            // Reset running state for new connection
                            isRunning.set(false)
                            messageQueue.clear()
                        }
                        
                        startIO()
                        notifyConnected(true)
                        
                        // Wait for this connection to close before accepting new one
                        // This is done by waiting for isRunning to become false
                        while (isRunning.get() && !isStopped.get()) {
                            Thread.sleep(100)
                        }
                        
                        // Connection ended, clean up socket
                        synchronized(connectionLock) {
                            try {
                                socket?.close()
                            } catch (e: Exception) {
                                // Ignore
                            }
                            socket = null
                        }
                        
                        if (!isStopped.get()) {
                            log("Client disconnected, waiting for new connection...")
                        }

                    } catch (e: IOException) {
                        if (!isStopped.get()) {
                            error("Error accepting connection", e)
                        }
                        break
                    } catch (e: InterruptedException) {
                        // Thread interrupted, exit
                        break
                    }
                }
            } catch (e: Exception) {
                error("Server failed: ${e.message}", e)
                notifyConnected(false)
            }
        }
    }

    override fun stop() {
        super.stop()

        try {
            serverSocket?.close()
        } catch (e: Exception) {
            error("Error closing server socket", e)
        } finally {
            serverSocket = null
        }
    }
}

/**
 * Factory for creating transport instances
 */
object TransportFactory {
    /**
     * Create a transport based on mode
     */
    fun create(mode: TransportMode, host: String, port: Int): DebugTransport {
        return when (mode) {
            TransportMode.CLIENT -> ClientTransport(host, port)
            TransportMode.SERVER -> ServerTransport(host, port)
        }
    }
}
