package com.tang.intellij.lua.lang;

import com.intellij.lang.Commenter;
import com.intellij.lang.Language;
import com.tang.intellij.lua.comment.LuaCommenter;

public class LuaLanguage extends Language {
    public static final LuaLanguage INSTANCE = new LuaLanguage();

    private LuaLanguage() {
        super("Lua");
    }
}