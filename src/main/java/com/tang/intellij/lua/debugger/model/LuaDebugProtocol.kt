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

package com.tang.intellij.lua.debugger.model

import com.google.gson.Gson

/**
 * Emmy Debugger Protocol - Clean and well-documented protocol definitions
 *
 * This file contains all protocol-related classes for communication between
 * the IDE and the Emmy debugger. The protocol uses a simple line-based format:
 * Line 1: Command ID (int)
 * Line 2: JSON payload
 */

// ================================================================================================
// ENUMS
// ================================================================================================

/**
 * All available debugger commands
 */
enum class DebugCommand {
    Unknown,

    // Initialization
    InitReq, InitRsp,

    // Ready state
    ReadyReq, ReadyRsp,

    // Breakpoint management
    AddBreakPointReq, AddBreakPointRsp,
    RemoveBreakPointReq, RemoveBreakPointRsp,

    // Debug actions (step, continue, etc.)
    ActionReq, ActionRsp,

    // Expression evaluation
    EvalReq, EvalRsp,

    // Notifications from debugger to IDE
    BreakNotify,       // Debugger hit a breakpoint
    AttachedNotify,    // Debugger attached successfully
    LogNotify,         // Log message from debugger

    // Hook management
    StartHookReq, StartHookRsp
}

/**
 * Debug actions that can be performed
 */
enum class DebugAction {
    Break,      // Pause execution
    Continue,   // Resume execution
    StepOver,   // Step over current line
    StepIn,     // Step into function
    StepOut,    // Step out of function
    Stop        // Stop debugging
}

/**
 * Lua value types matching Lua's internal types
 */
enum class LuaValueType {
    TNIL,
    TBOOLEAN,
    TLIGHTUSERDATA,
    TNUMBER,
    TSTRING,
    TTABLE,
    TFUNCTION,
    TUSERDATA,
    TTHREAD,

    // Special types for grouping
    GROUP
}

// ================================================================================================
// BASE MESSAGE CLASSES
// ================================================================================================

/**
 * Base interface for all protocol messages
 */
interface DebugMessage {
    val cmd: Int
    fun toJSON(): String
}

/**
 * Base implementation for protocol messages
 */
open class BaseDebugMessage(command: DebugCommand) : DebugMessage {
    override val cmd: Int = command.ordinal

    override fun toJSON(): String = Gson().toJson(this)

    companion object {
        private var sequenceCounter = 0

        /**
         * Generate unique sequence number for request/response matching
         */
        fun nextSequence(): Int = sequenceCounter++
    }
}

// ================================================================================================
// REQUEST MESSAGES (IDE -> Debugger)
// ================================================================================================

/**
 * Initialize debugger with helper code and file extensions
 * @param emmyHelper Lua helper code to inject into the debugger
 * @param ext Array of file extensions to debug (e.g., ["lua", "txt"])
 */
data class InitRequest(
    val emmyHelper: String,
    val ext: Array<String>
) : BaseDebugMessage(DebugCommand.InitReq) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as InitRequest
        return emmyHelper == other.emmyHelper && ext.contentEquals(other.ext)
    }

    override fun hashCode(): Int {
        var result = emmyHelper.hashCode()
        result = 31 * result + ext.contentHashCode()
        return result
    }
}

/**
 * Signal that IDE is ready to start debugging
 */
class ReadyRequest : BaseDebugMessage(DebugCommand.ReadyReq)

/**
 * Perform a debug action (step, continue, etc.)
 * @param action The action to perform
 */
data class DebugActionRequest(
    val action: Int
) : BaseDebugMessage(DebugCommand.ActionReq) {
    constructor(actionType: DebugAction) : this(actionType.ordinal)
}

/**
 * Add breakpoints to the debugger
 * @param breakPoints List of breakpoints to add
 */
data class AddBreakpointRequest(
    val breakPoints: List<DebugBreakpoint>
) : BaseDebugMessage(DebugCommand.AddBreakPointReq)

/**
 * Remove breakpoints from the debugger
 * @param breakPoints List of breakpoints to remove
 */
data class RemoveBreakpointRequest(
    val breakPoints: List<DebugBreakpoint>
) : BaseDebugMessage(DebugCommand.RemoveBreakPointReq)

/**
 * Evaluate an expression in the debugger context
 * @param expr Expression to evaluate
 * @param stackLevel Stack level to evaluate in (0 = current frame)
 * @param cacheId Cache ID for table expansion
 * @param depth Maximum depth for nested tables
 * @param seq Sequence number for matching responses
 */
data class EvalRequest(
    val expr: String,
    val stackLevel: Int,
    val cacheId: Int,
    val depth: Int,
    val seq: Int = nextSequence()
) : BaseDebugMessage(DebugCommand.EvalReq)

// ================================================================================================
// RESPONSE/NOTIFICATION MESSAGES (Debugger -> IDE)
// ================================================================================================

/**
 * Notification that debugger hit a breakpoint
 * @param stacks Call stack at the breakpoint
 */
data class BreakpointNotification(
    val stacks: List<DebugStackFrame>
)

/**
 * Notification that debugger attached successfully
 * @param state Current debugger state
 */
data class AttachedNotification(
    val state: Long
)

/**
 * Log message from the debugger
 * @param type Log type (0=info, 1=warning, 2=error)
 * @param message Log message text
 */
data class LogNotification(
    val type: Int,
    val message: String
)

/**
 * Response to an eval request
 * @param seq Sequence number matching the request
 * @param success Whether evaluation succeeded
 * @param error Error message if failed
 * @param value Evaluated value if succeeded
 */
data class EvalResponse(
    val seq: Int,
    val success: Boolean,
    val error: String?,
    val value: DebugVariable?
)

// ================================================================================================
// DATA STRUCTURES
// ================================================================================================

/**
 * Represents a breakpoint in the debugger
 * @param file File path (canonical/absolute)
 * @param line Line number (1-based)
 * @param condition Optional condition expression
 * @param logMessage Optional log message (for logpoints)
 * @param hitCondition Optional hit count condition
 * @param runToHere Whether this is a "run to here" temporary breakpoint
 */
data class DebugBreakpoint(
    val file: String,
    val line: Int,
    val condition: String? = null,
    val logMessage: String? = null,
    val hitCondition: String? = null,
    val runToHere: Boolean = false
)

/**
 * Represents a stack frame in the call stack
 * @param file Source file path
 * @param line Line number (1-based)
 * @param functionName Function name
 * @param level Stack level (0 = current frame)
 * @param localVariables Local variables in this frame
 * @param upvalueVariables Upvalue variables (closures)
 */
data class DebugStackFrame(
    val file: String,
    val line: Int,
    val functionName: String,
    val level: Int,
    val localVariables: List<DebugVariable>,
    val upvalueVariables: List<DebugVariable>
)

/**
 * Represents a variable/value in the debugger
 * @param name Variable name
 * @param nameType Type of the name (for table keys)
 * @param value String representation of the value
 * @param valueType Lua type of the value
 * @param valueTypeName User-friendly type name
 * @param cacheId Cache ID for lazy loading of children
 * @param children Child variables (for tables/objects)
 */
data class DebugVariable(
    val name: String,
    val nameType: Int,
    val value: String,
    val valueType: Int,
    val valueTypeName: String,
    val cacheId: Int,
    val children: List<DebugVariable>?
) {
    /**
     * Get the name type as enum
     */
    val nameTypeValue: LuaValueType
        get() = LuaValueType.values().getOrNull(nameType) ?: LuaValueType.TSTRING

    /**
     * Get display name (handles table keys)
     */
    val displayName: String
        get() = if (nameTypeValue == LuaValueType.TSTRING) name else "[$name]"

    /**
     * Get the value type as enum
     */
    val valueTypeValue: LuaValueType
        get() = LuaValueType.values().getOrNull(valueType) ?: LuaValueType.TSTRING

    /**
     * Check if this is a fake/synthetic variable (like groups)
     */
    val isFake: Boolean
        get() = valueTypeValue.ordinal > LuaValueType.TTHREAD.ordinal

    /**
     * Check if this variable has children
     */
    val hasChildren: Boolean
        get() = children != null && children.isNotEmpty()
}

// ================================================================================================
// PROTOCOL UTILITIES
// ================================================================================================

/**
 * Parse incoming message command
 */
fun parseCommand(cmdValue: Int): DebugCommand {
    return DebugCommand.values().getOrNull(cmdValue) ?: DebugCommand.Unknown
}

/**
 * Parse JSON payload into specific message type
 */
inline fun <reified T> parseMessage(json: String): T? {
    return try {
        Gson().fromJson(json, T::class.java)
    } catch (e: Exception) {
        null
    }
}
