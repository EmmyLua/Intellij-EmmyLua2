package com.tang.intellij.lua.lang;

import com.intellij.lang.Language;

public class LuaLanguage extends Language {
    public static final LuaLanguage INSTANCE = new LuaLanguage();

    private LuaLanguage() {
        super("Lua");
    }
}