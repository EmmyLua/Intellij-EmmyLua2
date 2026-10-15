package com.cppcxy.ide.lsp;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.redhat.devtools.lsp4ij.LanguageServerManager;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.List;

/** File-aware routing shared by the default document mapping and custom requests. */
public final class EmmyLuaServerRouting {
    public static final String DEFAULT_SERVER_ID = "EmmyLua";
    public static final ExtensionPointName<EmmyLuaServerProvider> EP_NAME =
            ExtensionPointName.create("com.cppcxy.Intellij-EmmyLua.serverProvider");

    private EmmyLuaServerRouting() {}

    /** Snapshot of all externally managed source directories, preserving provider order. */
    public static @NotNull List<Path> getOwnedRoots(@NotNull Project project) {
        return EP_NAME.getExtensionList().stream()
                .flatMap(provider -> provider.getOwnedRoots(project).stream())
                .map(path -> path.toAbsolutePath().normalize()).distinct().toList();
    }

    /**
     * Discard the default server's old analysis scope and document connections.
     * It starts with the new ownership snapshot when an unclaimed file next needs it.
     * Call after publishing the new roots, on the EDT, then refresh affected editors.
     */
    public static void ownershipChanged(@NotNull Project project) {
        ApplicationManager.getApplication().assertIsDispatchThread();
        if (!project.isDisposed()) {
            LanguageServerManager.getInstance(project).stop(DEFAULT_SERVER_ID,
                    new LanguageServerManager.StopOptions().setWillDisable(false));
        }
    }

    public static @NotNull String getServerId(@NotNull Project project, @NotNull VirtualFile file) {
        for (var provider : EP_NAME.getExtensionList()) {
            String id = provider.getServerId(project, file);
            if (id != null && !id.isBlank()) return id;
        }
        return DEFAULT_SERVER_ID;
    }
}
