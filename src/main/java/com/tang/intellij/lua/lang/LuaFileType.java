package com.tang.intellij.lua.lang;

import com.intellij.openapi.fileTypes.LanguageFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class LuaFileType extends LanguageFileType {

    public static final LuaFileType INSTANCE = new LuaFileType();

    private LuaFileType() {
        super(LuaLanguage.INSTANCE);
    }

    @NotNull
    @Override
    public String getName() {
        return "lua";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Lua language file";
    }

    @NotNull
    @Override
    public String getDefaultExtension() {
        return "lua";
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return LuaIcons.INSTANCE.getFILE();
    }
}
