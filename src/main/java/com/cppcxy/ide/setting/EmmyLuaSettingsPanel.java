package com.cppcxy.ide.setting;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.util.NlsContexts;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class EmmyLuaSettingsPanel implements SearchableConfigurable, Configurable.NoScroll {
    private JPanel myPanel;
    private JTextField location;

    private EmmyLuaSettings settings = EmmyLuaSettings.getInstance();

    public EmmyLuaSettingsPanel() {
        location.setText(settings.getLocation());
    }

    @Override
    public @NotNull @NonNls String getId() {
        return "EmmyLuaSettingsPanel";
    }

    @Override
    public @NlsContexts.ConfigurableName String getDisplayName() {
        return "EmmyLua Settings";
    }

    @Override
    public @Nullable JComponent createComponent() {
        return myPanel;
    }

    @Override
    public boolean isModified() {
        return true;
    }

    @Override
    public void apply() {
          settings.setLocation(location.getText());
    }
}
