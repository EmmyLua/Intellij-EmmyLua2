package com.cppcxy.ide.lsp;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Selects an externally managed EmmyLua LSP4IJ server for a file.
 * Providers must be fast, thread-safe, and avoid I/O or starting servers here.
 * The selected server must implement EmmyLuaCustomApi. Its registration,
 * document mappings, synchronization and lifecycle belong to the provider.
 */
public interface EmmyLuaServerProvider {
    /**
     * Returns a nonblank LSP4IJ server ID, or null to let the next provider decide.
     * The first provider returning an ID wins; extension ordering is supported.
     * Return a stable ID even while the server is starting: falling back to the
     * default server would attach the file to the wrong analysis environment.
     */
    @Nullable String getServerId(@NotNull Project project, @NotNull VirtualFile file);
}
