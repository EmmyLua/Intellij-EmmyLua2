package com.tang.intellij.lua.project

import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.module.ModuleTypeManager
import com.tang.intellij.lua.lang.LuaIcons
import javax.swing.Icon

/**
 * Lua Module Type
 */
class LuaModuleType : ModuleType<LuaModuleBuilder>("LUA_MODULE") {
    
    override fun createModuleBuilder(): LuaModuleBuilder = LuaModuleBuilder()
    
    override fun getName(): String = "Lua"
    
    override fun getDescription(): String = "Lua modules are used for developing Lua applications"
    
    override fun getNodeIcon(isOpened: Boolean): Icon = LuaIcons.FILE
    
    companion object {
        const val ID = "LUA_MODULE"
        
        val instance: LuaModuleType
            get() = ModuleTypeManager.getInstance().findByID(ID) as? LuaModuleType ?: LuaModuleType()
    }
}
