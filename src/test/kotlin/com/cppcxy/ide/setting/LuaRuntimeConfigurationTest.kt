package com.cppcxy.ide.setting

import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.ExtensionTestUtil
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.redhat.devtools.lsp4ij.LSPIJUtils
import com.tang.intellij.lua.lang.LuaLanguageLevel
import com.tang.intellij.lua.lang.LuaLanguageLevelProvider
import java.nio.file.Files
import javax.swing.JComboBox
import javax.swing.JPanel

class LuaRuntimeConfigurationTest : BasePlatformTestCase() {
    private lateinit var oldVersion: String
    private val settings get() = EmmyLuaSettings.getInstance()

    override fun setUp() {
        super.setUp()
        oldVersion = settings.luaVersion
        settings.luaVersion = "LuaLatest"
    }

    override fun tearDown() {
        try { settings.luaVersion = oldVersion } finally { super.tearDown() }
    }

    fun testJsonRuntimeAndDiagnosticsOnly() {
        assertNull(LuaRuntimeConfiguration.jsonLanguageLevel("""{"diagnostics":{"disable":["unnecessary-if"]}}"""))
        assertEquals(LuaLanguageLevel.LUA52, LuaRuntimeConfiguration.jsonLanguageLevel("""{"runtime":{"version":"Lua 5.2"}}"""))
        assertEquals(LuaLanguageLevel.LUA51, LuaRuntimeConfiguration.jsonLanguageLevel("""{"runtime.version":"LuaJIT"}"""))
        assertEquals(LuaLanguageLevel.LUA55, LuaRuntimeConfiguration.jsonLanguageLevel("""{"runtime":{"version":"unknown"}}"""))
        assertEquals(LuaLanguageLevel.LUA55, LuaRuntimeConfiguration.jsonLanguageLevel("{broken"))
    }

    fun testDefaultFileAndExternalOverride() {
        val directory = Files.createTempDirectory("emmy-runtime-test")
        try {
            val config = LuaRuntimeConfiguration.defaultConfigPath(directory, LuaRuntimeVersion.LUA52, null)
            assertEquals("{\"runtime\":{\"version\":\"Lua5.2\"}}\n", Files.readString(java.nio.file.Path.of(config)))
            assertEquals("/custom/config.lua", LuaRuntimeConfiguration.defaultConfigPath(directory, LuaRuntimeVersion.LUA55, "/custom/config.lua"))
            val latest = LuaRuntimeConfiguration.defaultConfigPath(directory, LuaRuntimeVersion.LATEST, null)
            assertFalse(config == latest)
        } finally {
            Files.walk(directory).use { paths -> paths.sorted(Comparator.reverseOrder()).forEach { Files.delete(it) } }
        }
    }

    fun testSelectedDefaultAndIntegrationPriority() {
        settings.luaVersion = "Lua5.2"
        val file = myFixture.configureByText("settings.lua", "return settings.global")
        assertTrue(PsiTreeUtil.findChildrenOfType(file, PsiErrorElement::class.java).isEmpty())
        ExtensionTestUtil.maskExtensions(LuaLanguageLevelProvider.EP_NAME,
            listOf(LuaLanguageLevelProvider { _, _ -> LuaLanguageLevel.LUA55 }), testRootDisposable)
        val strict = myFixture.configureByText("strict.lua", "return settings.global")
        assertFalse(PsiTreeUtil.findChildrenOfType(strict, PsiErrorElement::class.java).isEmpty())
    }

    fun testConfigurationPrecedenceAndChange() {
        val source = myFixture.addFileToProject("settings.lua", "return settings.global").virtualFile
        val root = LSPIJUtils.getRoots(project).first()
        val config = WriteAction.compute<com.intellij.openapi.vfs.VirtualFile, RuntimeException> {
            root.createChildData(this, ".luarc.json").also { VfsUtil.saveText(it, """{"runtime":{"version":"Lua5.2"}}""") }
        }
        val runtime = LuaRuntimeConfiguration.get(project)
        try {
            assertEquals(LuaLanguageLevel.LUA52, runtime.languageLevel(source))
            WriteAction.run<RuntimeException> { VfsUtil.saveText(config, """{"diagnostics":{"disable":[]}}""") }
            assertEquals(LuaLanguageLevel.LUA55, runtime.languageLevel(source))
            settings.luaVersion = "Lua5.3"
            assertEquals(LuaLanguageLevel.LUA53, runtime.languageLevel(source))
            val emmy = WriteAction.compute<com.intellij.openapi.vfs.VirtualFile, RuntimeException> {
                root.createChildData(this, ".emmyrc.json").also { VfsUtil.saveText(it, """{"runtime":{"version":"Lua5.4"}}""") }
            }
            try {
                assertEquals(LuaLanguageLevel.LUA54, runtime.languageLevel(source))
                val lua = WriteAction.compute<com.intellij.openapi.vfs.VirtualFile, RuntimeException> {
                    root.createChildData(this, ".emmyrc.lua").also { VfsUtil.saveText(it, "return { runtime = { version = 'Lua5.2' } }") }
                }
                try { assertEquals(LuaLanguageLevel.LUA55, runtime.languageLevel(source)) }
                finally { WriteAction.run<RuntimeException> { lua.delete(this) } }
                assertEquals(LuaLanguageLevel.LUA54, runtime.languageLevel(source))
            } finally { WriteAction.run<RuntimeException> { emmy.delete(this) } }
        } finally { WriteAction.run<RuntimeException> { config.delete(this) } }
    }

    fun testSavedConfigurationReparsesOpenFile() {
        settings.luaVersion = "Lua5.2"
        val source = myFixture.addFileToProject("settings.lua", "return settings.global").virtualFile
        myFixture.configureFromExistingVirtualFile(source)
        assertTrue(PsiTreeUtil.findChildrenOfType(myFixture.file, PsiErrorElement::class.java).isEmpty())
        val root = LSPIJUtils.getRoots(project).first()
        val config = WriteAction.compute<com.intellij.openapi.vfs.VirtualFile, RuntimeException> {
            root.createChildData(this, ".luarc.json").also { VfsUtil.saveText(it, """{"runtime":{"version":"Lua5.5"}}""") }
        }
        try {
            PlatformTestUtil.waitWithEventsDispatching("config creation reparses Lua", {
                PsiTreeUtil.findChildrenOfType(myFixture.file, PsiErrorElement::class.java).isNotEmpty()
            }, 10)
            WriteAction.run<RuntimeException> { VfsUtil.saveText(config, """{"runtime":{"version":"Lua5.2"}}""") }
            PlatformTestUtil.waitWithEventsDispatching("config edit reparses Lua", {
                PsiTreeUtil.findChildrenOfType(myFixture.file, PsiErrorElement::class.java).isEmpty()
            }, 10)
            settings.luaVersion = "Lua5.5"
            WriteAction.run<RuntimeException> { config.delete(this) }
            PlatformTestUtil.waitWithEventsDispatching("config deletion restores default", {
                PsiTreeUtil.findChildrenOfType(myFixture.file, PsiErrorElement::class.java).isNotEmpty()
            }, 10)
        } finally { if (config.isValid) WriteAction.run<RuntimeException> { config.delete(this) } }
    }

    fun testSettingsApplyReparsesExistingFile() {
        settings.luaVersion = "Lua5.2"
        myFixture.configureByText("settings.lua", "return settings.global")
        assertTrue(PsiTreeUtil.findChildrenOfType(myFixture.file, PsiErrorElement::class.java).isEmpty())
        val configurable = EmmyLuaSettingsPanel()
        try {
            val panel = configurable.createComponent() as JPanel
            val combo = panel.components.filterIsInstance<JComboBox<*>>().single()
            assertFalse(configurable.isModified)
            combo.selectedItem = LuaRuntimeVersion.LUA55
            assertEquals("Lua5.2", settings.luaVersion)
            assertTrue(configurable.isModified)
            configurable.reset()
            assertEquals(LuaRuntimeVersion.LUA52, combo.selectedItem)
            combo.selectedItem = LuaRuntimeVersion.LUA55
            configurable.apply()
            assertEquals("Lua5.5", settings.luaVersion)
            assertFalse(configurable.isModified)
            PlatformTestUtil.waitWithEventsDispatching("parser refresh", {
                PsiTreeUtil.findChildrenOfType(myFixture.file, PsiErrorElement::class.java).isNotEmpty()
            }, 10)
        } finally { configurable.disposeUIResources() }
    }
}
