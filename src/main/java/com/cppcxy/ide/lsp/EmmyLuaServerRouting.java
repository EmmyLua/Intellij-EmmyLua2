package com.cppcxy.ide.lsp;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

/** File-aware routing shared by the default document mapping and custom requests. */
public final class EmmyLuaServerRouting {
    public static final String DEFAULT_SERVER_ID = "EmmyLua";
    public static final ExtensionPointName<EmmyLuaServerProvider> EP_NAME =
            ExtensionPointName.create("com.cppcxy.Intellij-EmmyLua.serverProvider");

    private EmmyLuaServerRouting() {}

    public static @NotNull String getServerId(@NotNull Project project, @NotNull VirtualFile file) {
        for (var provider : EP_NAME.getExtensionList()) {
            String id = provider.getServerId(project, file);
            if (id != null && !id.isBlank()) return id;
        }
        return DEFAULT_SERVER_ID;
    }
}
