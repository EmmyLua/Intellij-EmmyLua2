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
package com.tang.intellij.lua.lang

import com.intellij.icons.AllIcons
import com.intellij.openapi.util.IconLoader.getIcon
import com.intellij.ui.LayeredIcon
import com.intellij.ui.RowIcon
import javax.swing.Icon

/**
 * Created by tangzx on 2015/11/15.
 * Email:love.tangzx@qq.com
 */
object LuaIcons {
    private fun getIcon(path: String): Icon {
        return getIcon(path, LuaIcons::class.java)
    }

    val CPP = getIcon("/icons/cpp.png")
    val CSHARP = getIcon("/icons/csharp.png")
    val FILE = getIcon("/icons/lua.png")
    val CLASS = AllIcons.Nodes.Class
    val Alias = AllIcons.Nodes.AbstractClass
    val CLASS_FIELD = AllIcons.Nodes.Field
    val CLASS_METHOD = AllIcons.Nodes.Method
    val ENUM = AllIcons.Nodes.Enum
    val SNIPPET = AllIcons.Nodes.Variable
    val CLASS_METHOD_OVERRIDING: Icon = RowIcon(AllIcons.Nodes.Method, AllIcons.Gutter.OverridingMethod)
//    val GLOBAL_FUNCTION: Icon = LayeredIcon(AllIcons.Nodes.Function, AllIcons.Nodes.StaticMark)
//    val GLOBAL_VAR: Icon = LayeredIcon(AllIcons.Nodes.Variable, AllIcons.Nodes.StaticMark)
    val LOCAL_VAR = AllIcons.Nodes.Variable
    val LOCAL_FUNCTION = AllIcons.Nodes.Function
    val PARAMETER = AllIcons.Nodes.Parameter
    val WORD = AllIcons.Actions.Edit
    val PUBLIC = AllIcons.Nodes.C_public
    val PROTECTED = AllIcons.Nodes.C_protected
    val PRIVATE = AllIcons.Nodes.C_private
    val STRING_ARG_HISTORY = AllIcons.Vcs.History
    val STRING_LITERAL = AllIcons.Nodes.Aspect

}
