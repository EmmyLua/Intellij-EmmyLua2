package com.cppcxy.ide.lsp

import com.intellij.testFramework.ExtensionTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.tang.intellij.lua.editor.LuaGutterCacheManager

class EmmyLuaServerRoutingTest : BasePlatformTestCase() {
    private fun providers(vararg providers: EmmyLuaServerProvider) {
        ExtensionTestUtil.maskExtensions(EmmyLuaServerRouting.EP_NAME, providers.toList(), testRootDisposable)
    }

    fun testDefaultServerWithoutProviders() {
        providers()
        val file = myFixture.addFileToProject("plain.lua", "").virtualFile
        assertEquals("EmmyLua", EmmyLuaServerRouting.getServerId(project, file))
        assertTrue(EmmyLuaDocumentMatcher().match(file, project))
    }

    fun testClaimedFileExcludedButUnrelatedLuaRemainsEnabled() {
        val owned = myFixture.addFileToProject("a/control.lua", "").virtualFile
        val unrelated = myFixture.addFileToProject("plain.lua", "").virtualFile
        providers(EmmyLuaServerProvider { p, f -> if (p === project && f == owned) "mod-a" else null })
        assertEquals("mod-a", EmmyLuaServerRouting.getServerId(project, owned))
        assertFalse(EmmyLuaDocumentMatcher().match(owned, project))
        assertTrue(EmmyLuaDocumentMatcher().match(unrelated, project))
    }

    fun testFirstNonblankProviderWins() {
        providers(EmmyLuaServerProvider { _, _ -> null },
            EmmyLuaServerProvider { _, _ -> "  " },
            EmmyLuaServerProvider { _, _ -> "first" },
            EmmyLuaServerProvider { _, _ -> "second" })
        val file = myFixture.addFileToProject("control.lua", "").virtualFile
        assertEquals("first", EmmyLuaServerRouting.getServerId(project, file))
    }

    fun testRouteCanChangeWithoutCachedSelection() {
        var id: String? = "mod-a"
        providers(EmmyLuaServerProvider { _, _ -> id })
        val file = myFixture.addFileToProject("control.lua", "").virtualFile
        assertFalse(EmmyLuaDocumentMatcher().match(file, project))
        id = "mod-b"
        assertEquals("mod-b", EmmyLuaServerRouting.getServerId(project, file))
        id = null
        assertTrue(EmmyLuaDocumentMatcher().match(file, project))
    }

    fun testGutterCacheDoesNotCrossServerRoutes() {
        val a = LuaGutterCacheManager.Key(project, "mod-a", "file:///control.lua")
        val b = LuaGutterCacheManager.Key(project, "mod-b", a.uri)
        try {
            LuaGutterCacheManager.setCache(a, emptyList())
            assertNotNull(LuaGutterCacheManager.getCache(a))
            assertNull(LuaGutterCacheManager.getCache(b))
            LuaGutterCacheManager.setCache(b, emptyList())
            LuaGutterCacheManager.clearCache(a.uri)
            assertNull(LuaGutterCacheManager.getCache(a))
            assertNull(LuaGutterCacheManager.getCache(b))
            LuaGutterCacheManager.setCache(a, emptyList())
            LuaGutterCacheManager.clearProject(project)
            assertNull(LuaGutterCacheManager.getCache(a))
        } finally {
            LuaGutterCacheManager.clearProject(project)
        }
    }
}
