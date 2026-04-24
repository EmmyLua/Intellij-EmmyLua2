package com.tang.intellij.lua.debugger.attach

import com.intellij.execution.process.ProcessInfo
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolder
import com.intellij.xdebugger.attach.XAttachProcessPresentationGroup
import com.tang.intellij.lua.lang.LuaIcons
import javax.swing.Icon

class EmmyAttachGroup : XAttachProcessPresentationGroup {

    companion object {
        val instance = EmmyAttachGroup()
    }

    override fun getGroupName() = "EmmyLua Attach Debugger"

    override fun getOrder() = 0

    override fun getItemDisplayText(
        project: Project,
        processInfo: ProcessInfo,
        userDataHolder: UserDataHolder
    ): String {
        val detail = userDataHolder.getUserData(EmmyAttachDebuggerProvider.DETAIL_KEY)
            ?.get(processInfo.pid)
        return if (detail != null) getDisplayName(processInfo, detail) else processInfo.executableName
    }

    override fun getItemIcon(
        project: Project,
        processInfo: ProcessInfo,
        userDataHolder: UserDataHolder
    ): Icon = LuaIcons.FILE

    override fun compare(a: ProcessInfo, b: ProcessInfo): Int =
        a.executableName.lowercase().compareTo(b.executableName.lowercase())
}
