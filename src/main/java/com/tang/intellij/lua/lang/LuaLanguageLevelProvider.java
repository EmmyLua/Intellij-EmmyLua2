package com.tang.intellij.lua.lang;

import com.intellij.lang.PsiBuilder;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.resolve.FileContextUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Supplies the effective runtime language level without starting or querying a server. */
public interface LuaLanguageLevelProvider {
    ExtensionPointName<LuaLanguageLevelProvider> EP_NAME =
            ExtensionPointName.create("com.cppcxy.Intellij-EmmyLua.languageLevelProvider");
    Key<LuaLanguageLevel> PARSE_LEVEL = Key.create("emmylua.parse.language.level");

    /** Return null to abstain. Implementations must be fast, thread-safe and avoid PSI access. */
    @Nullable LuaLanguageLevel getLanguageLevel(@NotNull Project project, @NotNull VirtualFile file);

    static @NotNull LuaLanguageLevel resolve(@NotNull PsiBuilder builder) {
        LuaLanguageLevel cached = builder.getUserData(PARSE_LEVEL);
        if (cached != null) return cached;
        PsiFile psiFile = builder.getUserDataUnprotected(FileContextUtil.CONTAINING_FILE_KEY);
        VirtualFile file = psiFile == null ? null : psiFile.getOriginalFile().getVirtualFile();
        LuaLanguageLevel level = LuaLanguageLevel.LUA55;
        if (file != null) {
            for (LuaLanguageLevelProvider provider : EP_NAME.getExtensionList()) {
                LuaLanguageLevel candidate = provider.getLanguageLevel(builder.getProject(), file);
                if (candidate != null) {
                    level = candidate;
                    break;
                }
            }
        }
        // Unknown runtimes stay strict. Cache only for this parse, not for the file.
        builder.putUserData(PARSE_LEVEL, level);
        return level;
    }
}
