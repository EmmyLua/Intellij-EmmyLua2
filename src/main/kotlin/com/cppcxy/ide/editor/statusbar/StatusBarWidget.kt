package com.cppcxy.ide.editor.statusbar


import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidget.WidgetPresentation
import com.intellij.openapi.wm.StatusBarWidgetFactory


class StatusBarWidgetFactory() : StatusBarWidgetFactory {
    override fun getId(): String {
        return ID
    }

    override fun getDisplayName(): String {
        return "EmmyLua"
    }

    override fun createWidget(project: Project): StatusBarWidget {
        return EmmyLuaBar()
    }

    override fun isEnabledByDefault(): Boolean {
        return true
    }

    override fun isAvailable(project: Project): Boolean {
        return true
    }

    class EmmyLuaBar() : StatusBarWidget, StatusBarWidget.TextPresentation {
        var toolTip = "EmmyLuaAnalyzer LSP"
        var message = "Lua"

        override fun ID(): String {
            return ID
        }

        override fun getPresentation(): WidgetPresentation {
            return this
        }

        override fun getAlignment(): Float {
            return 0.0f
        }

        override fun getText(): String {
            return message
        }

        override fun getTooltipText(): String {
            return toolTip
        }
    }

    companion object {
        private val ID: String = "EmmyLuaAnalyzer"
    }
}