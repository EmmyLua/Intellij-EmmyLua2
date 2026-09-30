package com.cppcxy.ide.setting;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.NotNull;
import javax.swing.*;

public class EmmyLuaSettingsPanel implements SearchableConfigurable, Configurable.NoScroll {
    private JPanel panel;
    private JTextField location;
    private JComboBox<LuaRuntimeVersion> luaVersion;
    private final EmmyLuaSettings settings = EmmyLuaSettings.getInstance();

    @Override public @NotNull String getId() { return "EmmyLuaSettingsPanel"; }
    @Override public @NotNull String getDisplayName() { return "EmmyLua Settings"; }

    @Override public JComponent createComponent() {
        if (panel == null) {
            location = new JTextField();
            luaVersion = new JComboBox<>(LuaRuntimeVersion.values());
            panel = FormBuilder.createFormBuilder()
                .addLabeledComponent("EmmyLua Custom Location:", location)
                .addLabeledComponent("Default Lua version:", luaVersion)
                .addComponent(new JLabel("Project configuration and integration-managed runtimes take precedence."))
                .addComponentFillVertically(new JPanel(), 0).getPanel();
            reset();
        }
        return panel;
    }

    @Override public boolean isModified() {
        return panel != null && (!location.getText().equals(settings.getLocation()) ||
            luaVersion.getSelectedItem() != LuaRuntimeVersion.fromId(settings.getLuaVersion()));
    }

    @Override public void apply() {
        if (!isModified()) return;
        settings.setLocation(location.getText());
        settings.setLuaVersion(((LuaRuntimeVersion) luaVersion.getSelectedItem()).id);
        for (var project : ProjectManager.getInstance().getOpenProjects()) {
            if (!project.isDisposed()) LuaRuntimeConfiguration.get(project).refresh();
        }
    }

    @Override public void reset() {
        if (panel == null) return;
        location.setText(settings.getLocation());
        luaVersion.setSelectedItem(LuaRuntimeVersion.fromId(settings.getLuaVersion()));
    }

    @Override public void disposeUIResources() {
        panel = null;
        location = null;
        luaVersion = null;
    }
}
