package com.cppcxy.ide.lsp;

import com.redhat.devtools.lsp4ij.client.features.LSPClientFeatures;
import org.eclipse.lsp4j.ClientCapabilities;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.WorkspaceClientCapabilities;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;

/** Keeps the default analyzer out of source roots handled by integration-owned servers. */
public final class EmmyLuaClientFeatures extends LSPClientFeatures {
    // One immutable snapshot per initialization, also read by workspace/configuration.
    private volatile List<Path> ownedRoots = List.of();

    @Override
    public void initializeParams(@NotNull InitializeParams params) {
        super.initializeParams(params);
        ownedRoots = EmmyLuaServerRouting.getOwnedRoots(getProject());
        excludeOwnedRoots(params, ownedRoots);
    }

    List<Path> ownedRoots() {
        return ownedRoots;
    }

    static void excludeOwnedRoots(InitializeParams params, List<Path> owned) {
        if (owned.isEmpty()) return;
        var canonicalRoots = owned.stream().map(EmmyLuaClientFeatures::canonical).toList();
        List<WorkspaceFolder> folders = params.getWorkspaceFolders();
        if (folders != null) {
            folders = folders.stream().filter(folder -> !isOwned(folder.getUri(), canonicalRoots)).toList();
            params.setWorkspaceFolders(folders);
        }
        // An empty workspaceFolders list otherwise falls back to rootUri in the analyzer.
        if (isOwned(params.getRootUri(), canonicalRoots)) {
            String uri = folders == null || folders.isEmpty() ? null : folders.get(0).getUri();
            params.setRootUri(uri);
        }
        var capabilities = params.getCapabilities();
        if (capabilities == null) {
            capabilities = new ClientCapabilities();
            params.setCapabilities(capabilities);
        }
        var workspace = capabilities.getWorkspace();
        if (workspace == null) {
            workspace = new WorkspaceClientCapabilities();
            capabilities.setWorkspace(workspace);
        }
        workspace.setConfiguration(true);
    }

    private static boolean isOwned(String uri, List<Path> canonicalRoots) {
        Path file = localPath(uri);
        if (file == null) return false;
        Path path = canonical(file);
        return canonicalRoots.stream().anyMatch(path::startsWith);
    }

    private static Path localPath(String value) {
        if (value == null) return null;
        try {
            URI uri = URI.create(value);
            return "file".equalsIgnoreCase(uri.getScheme()) ? Path.of(uri) : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    static Path canonical(Path path) {
        Path absolute = path.toAbsolutePath();
        // A workspace folder may not exist yet. Resolve symlinks in its existing
        // parent path before comparing it to an owned root.
        for (Path ancestor = absolute; ancestor != null; ancestor = ancestor.getParent()) {
            try {
                Path resolved = ancestor.toRealPath();
                if (ancestor.equals(absolute)) return resolved;
                var suffix = absolute.subpath(ancestor.getNameCount(), absolute.getNameCount());
                return resolved.resolve(suffix).normalize();
            } catch (NoSuchFileException ignored) {
                // Try the next parent until the existing prefix can be resolved.
            } catch (IOException ignored) {
                return absolute.normalize();
            }
        }
        return absolute.normalize();
    }
}
