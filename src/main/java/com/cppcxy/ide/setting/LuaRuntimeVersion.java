package com.cppcxy.ide.setting;

import com.tang.intellij.lua.lang.LuaLanguageLevel;

/** Persist analyzer-compatible IDs, independently of the labels shown in Settings. */
public enum LuaRuntimeVersion {
    LATEST("LuaLatest", "Latest", LuaLanguageLevel.LUA55),
    LUA51("Lua5.1", "Lua 5.1", LuaLanguageLevel.LUA51),
    LUA52("Lua5.2", "Lua 5.2", LuaLanguageLevel.LUA52),
    LUA53("Lua5.3", "Lua 5.3", LuaLanguageLevel.LUA53),
    LUA54("Lua5.4", "Lua 5.4", LuaLanguageLevel.LUA54),
    LUA55("Lua5.5", "Lua 5.5", LuaLanguageLevel.LUA55),
    LUAJIT("LuaJIT", "LuaJIT", LuaLanguageLevel.LUA51),
    LUAJIT2("LuaJIT2", "LuaJIT 2", LuaLanguageLevel.LUA51),
    LUAJIT3("LuaJIT3", "LuaJIT 3", LuaLanguageLevel.LUA51);

    public final String id;
    public final LuaLanguageLevel level;
    private final String label;
    LuaRuntimeVersion(String id, String label, LuaLanguageLevel level) {
        this.id = id;
        this.label = label;
        this.level = level;
    }
    public static LuaRuntimeVersion fromId(String id) {
        if (id != null) {
            String normalized = id.replace(" ", "");
            for (LuaRuntimeVersion version : values()) if (version.id.equals(normalized)) return version;
        }
        return LATEST;
    }
    @Override public String toString() { return label; }
}
