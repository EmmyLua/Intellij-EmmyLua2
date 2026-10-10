package com.tang.intellij.lua.codeStyle;

import com.intellij.openapi.editor.EditorFactory;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider;
import com.intellij.testFramework.HeavyPlatformTestCase;
import com.redhat.devtools.lsp4ij.LSPIJUtils;
import com.tang.intellij.lua.lang.LuaLanguage;

public class LuaCodeStyleSettingsProviderTest extends HeavyPlatformTestCase {
    public void testPageAndEffectiveFormattingOptions() throws Exception {
        var provider = LanguageCodeStyleSettingsProvider.forLanguage(LuaLanguage.INSTANCE);
        assertInstanceOf(provider, LuaCodeStyleSettingsProvider.class);
        assertEquals("Lua", provider.getConfigurableDisplayName());
        var settings = new CodeStyleSettings();
        var options = settings.getCommonSettings(LuaLanguage.INSTANCE).getIndentOptions();
        assertNotNull(options);
        options.TAB_SIZE = 3;
        options.INDENT_SIZE = 7;
        options.USE_TAB_CHARACTER = false;
        var page = provider.createConfigurable(settings, settings.clone());
        try {
            assertNotNull(page.createComponent());
            page.reset();
            assertFalse(page.isModified());
            page.apply();
            assertEquals(3, options.TAB_SIZE);
            assertEquals(7, options.INDENT_SIZE);
            assertFalse(options.USE_TAB_CHARACTER);
        } finally { page.disposeUIResources(); }
        var manager = CodeStyleSettingsManager.getInstance(getProject());
        manager.setTemporarySettings(settings);
        try {
            var psi = PsiFileFactory.getInstance(getProject()).createFileFromText("sample.lua", LuaLanguage.INSTANCE, "local value = 1\n");
            var document = PsiDocumentManager.getInstance(getProject()).getDocument(psi);
            assertNotNull(document);
            var editor = EditorFactory.getInstance().createEditor(document, getProject());
            try {
                assertEquals(3, LSPIJUtils.getTabSize(editor));
                assertTrue(LSPIJUtils.isInsertSpaces(editor));
            } finally { EditorFactory.getInstance().releaseEditor(editor); }
            options.USE_TAB_CHARACTER = true;
            var tabEditor = EditorFactory.getInstance().createEditor(document, getProject());
            try { assertFalse(LSPIJUtils.isInsertSpaces(tabEditor)); }
            finally { EditorFactory.getInstance().releaseEditor(tabEditor); }
        } finally { manager.dropTemporarySettings(); }
    }
}
