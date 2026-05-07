package com.tang.intellij.lua.debugger.attach

import com.intellij.execution.process.ProcessInfo
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.SystemInfoRt
import com.intellij.openapi.util.UserDataHolder
import com.intellij.xdebugger.attach.XAttachDebuggerProvider
import com.intellij.xdebugger.attach.XAttachHost
import com.intellij.xdebugger.attach.XLocalAttachDebugger

/**
 * Provides Emmy attach debuggers for the "Run → Attach to Process" dialog.
 * Only active on Windows; shows an entry for each process that contains a Lua runtime.
 */
class EmmyAttachDebuggerProvider : XAttachDebuggerProvider {

    companion object {
        val DETAIL_KEY = Key.create<Map<Int, ProcessDetailInfo>>("EmmyAttachDebuggerProvider.detail")
    }

    override fun isAttachHostApplicable(attachHost: XAttachHost) = true

    override fun getAvailableDebuggers(
        project: Project,
        attachHost: XAttachHost,
        processInfo: ProcessInfo,
        userDataHolder: UserDataHolder
    ): List<XLocalAttachDebugger> {
        if (!SystemInfoRt.isWindows) return emptyList()

        // Build the process map once per "Attach to Process" dialog open.
        if (userDataHolder.getUserData(DETAIL_KEY) == null) {
            if (archToolPath == null) {
                ApplicationManager.getApplication().invokeLater {
                    Notifications.Bus.notify(
                        Notification(
                            "EmmyLua",
                            "Emmy Attach Debugger",
                            "'emmy_tool.exe' not found. Please reinstall the EmmyLua plugin.",
                            NotificationType.WARNING
                        ).apply { isImportant = true }
                    )
                }
                return emptyList()
            }
            userDataHolder.putUserData(DETAIL_KEY, listProcesses())
        }

        // Only offer attachment for .exe files that have a known Lua path.
        if (!processInfo.executableName.endsWith(".exe")) return emptyList()
        val detail = userDataHolder.getUserData(DETAIL_KEY)?.get(processInfo.pid)
            ?: return emptyList()
        if (detail.path.isEmpty()) return emptyList()

        return listOf(EmmyAttachDebugger(processInfo, detail))
    }

    override fun getPresentationGroup() = EmmyAttachGroup.instance
}
