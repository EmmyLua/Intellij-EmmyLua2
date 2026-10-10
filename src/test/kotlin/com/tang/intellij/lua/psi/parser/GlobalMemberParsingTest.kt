package com.tang.intellij.lua.psi.parser

import com.intellij.psi.PsiErrorElement
import com.intellij.testFramework.ExtensionTestUtil
import com.tang.intellij.lua.lang.LuaLanguageLevel
import com.tang.intellij.lua.lang.LuaLanguageLevelProvider
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.tang.intellij.lua.psi.LuaTypes

class GlobalMemberParsingTest : BasePlatformTestCase() {
    private var perFile = false
    private var level: LuaLanguageLevel? = LuaLanguageLevel.LUA52

    override fun setUp() {
        super.setUp()
        ExtensionTestUtil.maskExtensions(LuaLanguageLevelProvider.EP_NAME,
            listOf(LuaLanguageLevelProvider { _, file ->
                if (!perFile) level
                else if (file.name == "settings.lua") LuaLanguageLevel.LUA52 else LuaLanguageLevel.LUA55
            }), testRootDisposable)
    }

    private fun assertRejected(source: String) {
        val file = myFixture.configureByText("invalid.lua", source)
        assertFalse(source, PsiTreeUtil.findChildrenOfType(file, PsiErrorElement::class.java).isEmpty())
    }

    private fun assertParses(source: String) {
        val file = myFixture.configureByText("settings.lua", source)
        val errors = PsiTreeUtil.findChildrenOfType(file, PsiErrorElement::class.java)
        assertTrue(errors.joinToString { "${it.errorDescription} at ${it.textOffset}" }, errors.isEmpty())
    }

    fun testSettingsReadAndWrite() {
        assertParses("""
            local settings_table = {
                load_value = function(name) return settings.global[name] end,
                store_value = function(name, value) settings.global[name] = { value = value } end,
            }
        """.trimIndent())
        val offset = myFixture.file.text.indexOf("global")
        assertSame(LuaTypes.ID, myFixture.file.findElementAt(offset)!!.node.elementType)
    }

    fun testChainedMembersAndMethodCalls() {
        assertParses("local value = settings.global.global[name]; settings:global(name)")
    }

    fun testMemberFunctionDefinitions() {
        assertParses("function settings.global(name) return name end; function settings:global() end")
    }

    fun testNamedTableField() {
        assertParses("local settings = { global = {} }; return settings.global")
    }

    fun testLua55GlobalDeclarations() {
        level = LuaLanguageLevel.LUA55
        assertParses("global value = 1; global function callback() end; global *")
        assertSame(LuaTypes.GLOBAL, myFixture.file.findElementAt(0)!!.node.elementType)
    }

    fun testLua55RejectsGlobalMembers() {
        level = LuaLanguageLevel.LUA55
        listOf("return settings.global[name]", "settings:global()",
            "local t = { global = {} }", "function settings.global() end",
            "function settings:global() end").forEach { assertRejected(it) }
        assertParses("return settings[\"global\"][name]")
    }

    fun testUnknownVersionStaysStrict() {
        level = null
        assertRejected("return settings.global[name]")
    }

    fun testAllPre55VersionsAcceptGlobalMembers() {
        for (legacy in LuaLanguageLevel.entries.filter { it.version < 55 }) {
            level = legacy
            assertParses("local t = { global = {} }; return t.global")
        }
    }

    fun testFileSpecificVersion() {
        perFile = true
        assertParses("return settings.global")
        assertRejected("return settings.global")
    }

    fun testOrdinaryKeywordsStillRejectedAsMembers() {
        val file = myFixture.configureByText("invalid.lua", "return settings.end[name]")
        assertFalse(PsiTreeUtil.findChildrenOfType(file, PsiErrorElement::class.java).isEmpty())
    }
}
