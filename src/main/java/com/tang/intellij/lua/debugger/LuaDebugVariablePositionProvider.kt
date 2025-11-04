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

package com.tang.intellij.lua.debugger

import com.intellij.xdebugger.XSourcePosition
import com.tang.intellij.lua.debugger.emmy.value.LuaXValue

/**
 * Interface for resolving source positions of debug variables
 */
interface LuaDebugVariablePositionProvider {
    /**
     * Configure the context by scanning for variables
     */
    fun configureContext(context: LuaDebugVariableContext)

    /**
     * Get source position for a variable value
     */
    fun getSourcePosition(value: LuaXValue, context: LuaDebugVariableContext): XSourcePosition?
}
