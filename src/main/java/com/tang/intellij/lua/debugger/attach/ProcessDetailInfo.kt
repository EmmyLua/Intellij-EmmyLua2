package com.tang.intellij.lua.debugger.attach

import com.intellij.execution.process.ProcessInfo

data class ProcessDetailInfo(
    val pid: Int = 0,
    val path: String = "",
    val title: String = "",
)

private const val MAX_DISPLAY_LEN = 60

fun getDisplayName(processInfo: ProcessInfo, detailInfo: ProcessDetailInfo): String {
    val s = if (detailInfo.title.isNotEmpty())
        "${processInfo.executableName} - ${detailInfo.title}"
    else
        processInfo.executableName

    return if (s.length > MAX_DISPLAY_LEN) "${s.substring(0, MAX_DISPLAY_LEN)}..." else s
}
