package com.tang.intellij.lua.debugger.launch

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

class EmmyLaunchConfigurationType : ConfigurationType {
    override fun getIcon(): Icon = LuaIcons.FILE
    override fun getConfigurationTypeDescription() = "Emmy Launch Debugger"
    override fun getId() = "lua.emmyLaunch.debugger"
    override fun getDisplayName() = "Emmy Launch Debugger"
    override fun getConfigurationFactories(): Array<ConfigurationFactory> =
        arrayOf(EmmyLaunchConfigurationFactory(this))
}

class EmmyLaunchConfigurationFactory(type: EmmyLaunchConfigurationType) : LuaConfigurationFactory(type) {
    override fun createTemplateConfiguration(project: Project): RunConfiguration =
        EmmyLaunchDebugConfiguration(project, this)
}

class EmmyLaunchDebugConfiguration(
    project: Project,
    factory: EmmyLaunchConfigurationFactory
) : LuaRunConfiguration(project, factory), RunConfigurationWithSuppressedDefaultRunAction {

    var program = "lua"
    var workingDirectory = ""
    var parameter = ""
    var useWindowsTerminal = false

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        val group = SettingsEditorGroup<EmmyLaunchDebugConfiguration>()
        group.addEditor("emmy", EmmyLaunchDebugSettingsPanel(project))
        return group
    }

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState =
        LuaCommandLineState(environment)

    override fun getValidModules(): Collection<Module> = emptyList()

    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        JDOMExternalizerUtil.writeField(element, "Program", program)
        JDOMExternalizerUtil.writeField(element, "WorkingDirectory", workingDirectory)
        JDOMExternalizerUtil.writeField(element, "Parameter", parameter)
        JDOMExternalizerUtil.writeField(element, "UseWindowsTerminal", useWindowsTerminal.toString())
    }

    override fun readExternal(element: Element) {
        super.readExternal(element)
        JDOMExternalizerUtil.readField(element, "Program")?.let { program = it }
        JDOMExternalizerUtil.readField(element, "WorkingDirectory")?.let { workingDirectory = it }
        JDOMExternalizerUtil.readField(element, "Parameter")?.let { parameter = it }
        JDOMExternalizerUtil.readField(element, "UseWindowsTerminal")?.let {
            useWindowsTerminal = it == "true"
        }
    }
}
