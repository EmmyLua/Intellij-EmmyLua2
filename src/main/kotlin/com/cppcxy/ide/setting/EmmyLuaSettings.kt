package com.cppcxy.ide.setting

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil


@State(name = "EmmyLuaSettings", storages = [Storage("EmmyLua2.xml")])
class EmmyLuaSettings : PersistentStateComponent<EmmyLuaSettings> {
    var location = ""

    companion object {
        @JvmStatic
        fun getInstance(): EmmyLuaSettings {
            return ApplicationManager.getApplication().getService(EmmyLuaSettings::class.java)
        }
    }

    override fun getState(): EmmyLuaSettings {
        return this
    }

    override fun loadState(state: EmmyLuaSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }
}