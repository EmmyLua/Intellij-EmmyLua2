package com.cppcxy.ide.lsp;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;

/**
 * Selects an externally managed EmmyLua LSP4IJ server for a file.
 * Providers must be fast, thread-safe, and avoid I/O or starting servers here.
 * The selected server must implement EmmyLuaCustomApi. Its registration,
 * document mappings, synchronization and lifecycle belong to the provider.
 */
public interface EmmyLuaServerProvider {
    /**
     * Directories whose Lua files are handled by this provider. The default server
     * omits these workspace roots and excludes them from scans of parent folders.
     * Explicit analyzer library/package entries remain available as dependencies.
     *
     * Return an immutable snapshot of absolute paths, without I/O or starting servers.
     * Report roots as soon as their files are reserved, even before the owned server
     * has started. After changing ownership, call
     * {@link EmmyLuaServerRouting#ownershipChanged(Project)} and refresh affected editors.
     * Providers that only route individual files can leave this empty.
     */
    default @NotNull List<Path> getOwnedRoots(@NotNull Project project) {
        return List.of();
    }

    /**
     * Returns a nonblank LSP4IJ server ID, or null to let the next provider decide.
     * The first provider returning an ID wins; extension ordering is supported.
     * Return a stable ID even while the server is starting: falling back to the
     * default server would attach the file to the wrong analysis environment.
     */
    @Nullable String getServerId(@NotNull Project project, @NotNull VirtualFile file);
}
