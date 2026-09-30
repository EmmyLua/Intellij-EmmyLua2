package com.tang.intellij.lua.codeStyle;

import com.intellij.application.options.CodeStyleAbstractConfigurable;
import com.intellij.application.options.CodeStyleAbstractPanel;
import com.intellij.application.options.IndentOptionsEditor;
import com.intellij.application.options.TabbedLanguageCodeStylePanel;
import com.intellij.lang.Language;
import com.intellij.psi.codeStyle.CodeStyleConfigurable;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsCustomizable;
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider;
import com.tang.intellij.lua.lang.LuaLanguage;
import org.jetbrains.annotations.NotNull;

/** Standard Lua indentation settings, also consumed by LSP4IJ formatting requests. */
public final class LuaCodeStyleSettingsProvider extends LanguageCodeStyleSettingsProvider {
    @Override public @NotNull Language getLanguage() { return LuaLanguage.INSTANCE; }
    @Override public String getConfigurableDisplayName() { return "Lua"; }
    @Override public IndentOptionsEditor getIndentOptionsEditor() { return new IndentOptionsEditor(); }
    @Override public void customizeSettings(@NotNull CodeStyleSettingsCustomizable consumer, @NotNull SettingsType type) {
        if (type == SettingsType.INDENT_SETTINGS)
            consumer.showStandardOptions("USE_TAB_CHARACTER", "TAB_SIZE", "INDENT_SIZE");
    }
    @Override public String getCodeSample(@NotNull SettingsType type) {
        return "local function update(entity)\n    if entity.valid then\n        entity.active = true\n    end\nend\n";
    }
    @Override public @NotNull CodeStyleConfigurable createConfigurable(@NotNull CodeStyleSettings settings,
                                                                       @NotNull CodeStyleSettings modelSettings) {
        return new CodeStyleAbstractConfigurable(settings, modelSettings, getConfigurableDisplayName()) {
            @Override protected @NotNull CodeStyleAbstractPanel createPanel(@NotNull CodeStyleSettings panelSettings) {
                return new TabbedLanguageCodeStylePanel(LuaLanguage.INSTANCE, getCurrentSettings(), panelSettings) {
                    @Override protected void initTabs(CodeStyleSettings settings) {
                        addIndentOptionsTab(settings);
                    }
                };
            }
        };
    }
}
