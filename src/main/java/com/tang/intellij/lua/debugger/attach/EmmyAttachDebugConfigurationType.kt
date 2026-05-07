package com.tang.intellij.lua.debugger.attach

import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.RunConfigurationWithSuppressedDefaultRunAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.options.SettingsEditorGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.JDOMExternalizerUtil
import com.tang.intellij.lua.debugger.LuaCommandLineState
import com.tang.intellij.lua.debugger.LuaConfigurationFactory
import com.tang.intellij.lua.debugger.LuaRunConfiguration
import com.tang.intellij.lua.lang.LuaIcons
import org.jdom.Element
import javax.swing.Icon

class EmmyAttachConfigurationType : ConfigurationType {
    override fun getIcon(): Icon = LuaIcons.FILE
    override fun getConfigurationTypeDescription() = "Emmy Attach Debugger"
    override fun getId() = "lua.emmyAttach.debugger"
    override fun getDisplayName() = "Emmy Attach Debugger"
    override fun getConfigurationFactories(): Array<ConfigurationFactory> =
        arrayOf(EmmyAttachConfigurationFactory(this))
}

enum class EmmyAttachMode(private val desc: String) {
    Pid("Pid"),
    ProcessName("ProcessName");

    override fun toString() = desc
}

enum class EmmyAttachWinArch(val desc: String) {
    Auto("Auto detect"),
    X86("x86"),
    X64("x64");

    override fun toString() = desc
}

class EmmyAttachConfigurationFactory(type: EmmyAttachConfigurationType) : LuaConfigurationFactory(type) {
    override fun createTemplateConfiguration(project: Project): RunConfiguration =
        EmmyAttachDebugConfiguration(project, this)
}

class EmmyAttachDebugConfiguration(
    project: Project,
    factory: EmmyAttachConfigurationFactory
) : LuaRunConfiguration(project, factory), RunConfigurationWithSuppressedDefaultRunAction {

    var attachMode = EmmyAttachMode.Pid
    var pid = "0"
    var processName = ""
    var encoding = "gbk"
    var winArch = EmmyAttachWinArch.Auto

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        val group = SettingsEditorGroup<EmmyAttachDebugConfiguration>()
        group.addEditor("emmy", EmmyAttachDebugSettingsPanel(project))
        return group
    }

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState =
        LuaCommandLineState(environment)

    override fun getValidModules(): Collection<Module> = emptyList()

    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        JDOMExternalizerUtil.writeField(element, "AttachMode", attachMode.ordinal.toString())
        JDOMExternalizerUtil.writeField(element, "Pid", pid)
        JDOMExternalizerUtil.writeField(element, "ProcessName", processName)
        JDOMExternalizerUtil.writeField(element, "Encoding", encoding)
        JDOMExternalizerUtil.writeField(element, "WinArch", winArch.ordinal.toString())
    }

    override fun readExternal(element: Element) {
        super.readExternal(element)
        JDOMExternalizerUtil.readField(element, "AttachMode")?.let { value ->
            val i = value.toIntOrNull() ?: return@let
            attachMode = EmmyAttachMode.values().find { it.ordinal == i } ?: EmmyAttachMode.Pid
        }
        JDOMExternalizerUtil.readField(element, "Pid")?.let { pid = it }
        JDOMExternalizerUtil.readField(element, "ProcessName")?.let { processName = it }
        JDOMExternalizerUtil.readField(element, "Encoding")?.let { encoding = it }
        JDOMExternalizerUtil.readField(element, "WinArch")?.let { value ->
            val i = value.toIntOrNull() ?: return@let
            winArch = EmmyAttachWinArch.values().find { it.ordinal == i } ?: EmmyAttachWinArch.Auto
        }
    }
}
