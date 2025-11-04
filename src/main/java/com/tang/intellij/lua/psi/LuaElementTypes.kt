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

package com.tang.intellij.lua.psi

//import com.tang.intellij.lua.stubs.*

object LuaElementTypes {
    val BINARY_OPS by lazy {
        arrayOf(
            LuaTypes.CONCAT,
            LuaTypes.LE, LuaTypes.EQ, LuaTypes.LT, LuaTypes.NE, LuaTypes.GE, LuaTypes.GT,
            LuaTypes.AND, LuaTypes.OR,
            LuaTypes.BIT_AND, LuaTypes.BIT_LTLT, LuaTypes.BIT_OR, LuaTypes.BIT_RTRT, LuaTypes.BIT_TILDE, LuaTypes.EXP,
            LuaTypes.PLUS, LuaTypes.MINUS, LuaTypes.MULT, LuaTypes.DIV, LuaTypes.DOUBLE_DIV, LuaTypes.MOD
        )
    }
    val UNARY_OPS by lazy {
        arrayOf(
            LuaTypes.MINUS, LuaTypes.GETN
        )
    }

    val LOCAL_DEF = LuaElementType("LOCAL_DEF")
    val SINGLE_ARG = LuaElementType("LOCAL_DEF")
    val LIST_ARGS = LuaElementType("LOCAL_DEF")

    val EXPR_LIST = LuaElementType("LOCAL_DEF")
    val NAME_LIST = LuaElementType("LOCAL_DEF")
    val ASSIGN_STAT = LuaElementType("LOCAL_DEF")
    val VAR_LIST = LuaElementType("LOCAL_DEF")
    val LOCAL_FUNC_DEF = LuaElementType("LOCAL_DEF")
    val FUNC_BODY = LuaElementType("LOCAL_DEF")
    val CLASS_METHOD_NAME = LuaElementType("LOCAL_DEF")

    val CLOSURE_EXPR = LuaElementType("LOCAL_DEF")
    val PAREN_EXPR = LuaElementType("LOCAL_DEF")
    val CALL_EXPR = LuaElementType("LOCAL_DEF")
    val UNARY_EXPR = LuaElementType("LOCAL_DEF")
    val BINARY_EXPR = LuaElementType("LOCAL_DEF")

    val RETURN_STAT = LuaElementType("LOCAL_DEF")
    val DO_STAT = LuaElementType("LOCAL_DEF")
    val IF_STAT = LuaElementType("LOCAL_DEF")
    val EXPR_STAT = LuaElementType("LOCAL_DEF")
}