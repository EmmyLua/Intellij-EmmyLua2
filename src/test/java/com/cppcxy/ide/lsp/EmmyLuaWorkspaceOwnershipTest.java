package com.cppcxy.ide.lsp;

import org.eclipse.lsp4j.*;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.*;

public class EmmyLuaWorkspaceOwnershipTest {
    @Rule public final TemporaryFolder temporary = new TemporaryFolder();
    private Path root;
    private Path mod;

    @Before public void setUpRoots() throws IOException {
        root = temporary.getRoot().toPath().toRealPath();
        mod = root.resolve("mod");
    }

    private InitializeParams params(Path... roots) {
        var params = new InitializeParams();
        params.setRootUri(roots[0].toUri().toString());
        params.setRootPath(roots[0].toString());
        params.setWorkspaceFolders(java.util.Arrays.stream(roots)
                .map(path -> new WorkspaceFolder(path.toUri().toString(), path.getFileName().toString())).toList());
        var capabilities = new ClientCapabilities();
        capabilities.setWorkspace(new WorkspaceClientCapabilities());
        params.setCapabilities(capabilities);
        return params;
    }

    @Test public void excludesOwnedRootsButKeepsParentAndSimilarlyNamedSibling() {
        var params = params(root, mod, mod.resolve("scripts"), root.resolve("mod-extra"));
        EmmyLuaClientFeatures.excludeOwnedRoots(params, List.of(mod));
        assertEquals(List.of(root.toUri().toString(), root.resolve("mod-extra").toUri().toString()),
                params.getWorkspaceFolders().stream().map(WorkspaceFolder::getUri).toList());
        assertEquals(root.toUri().toString(), params.getRootUri());
    }

    @Test public void removesRootUriFallbackWhenEverythingIsOwned() {
        var params = params(mod);
        EmmyLuaClientFeatures.excludeOwnedRoots(params, List.of(mod));
        assertTrue(params.getWorkspaceFolders().isEmpty());
        assertNull(params.getRootUri());
    }

    @Test public void usesUnclaimedRootWhenOriginalRootIsOwned() {
        var plain = root.resolve("plain");
        var params = params(mod, plain);
        EmmyLuaClientFeatures.excludeOwnedRoots(params, List.of(mod));
        assertEquals(plain.toUri().toString(), params.getRootUri());
    }

    @Test public void preservesNonFileWorkspacesWithoutCreatingFilesystemPaths() {
        var params = params(mod);
        params.setWorkspaceFolders(List.of(new WorkspaceFolder(mod.toUri().toString(), "mod"),
                new WorkspaceFolder("untitled:/scratch", "scratch")));
        EmmyLuaClientFeatures.excludeOwnedRoots(params, List.of(mod));
        assertEquals("untitled:/scratch", params.getRootUri());
    }

    @Test public void excludesMissingFolderUnderSymlinkedOwnedRoot() throws IOException {
        var real = temporary.newFolder("real").toPath().toRealPath();
        var alias = linkTo(real);
        var params = params(alias.resolve("pending").resolve("child"), real.resolveSibling("unowned"));
        EmmyLuaClientFeatures.excludeOwnedRoots(params, List.of(real));
        assertEquals(1, params.getWorkspaceFolders().size());
        assertEquals(real.resolveSibling("unowned").toUri().toString(), params.getRootUri());
    }

    @Test public void excludesFolderWhenOwnedRootIsMissingUnderSymlink() throws IOException {
        var real = temporary.newFolder("real").toPath().toRealPath();
        var alias = linkTo(real);
        var pending = real.resolve("pending");
        var params = params(pending);
        EmmyLuaClientFeatures.excludeOwnedRoots(params, List.of(alias.resolve("pending")));
        assertTrue(params.getWorkspaceFolders().isEmpty());
        assertNull(params.getRootUri());
    }

    private Path linkTo(Path target) throws IOException {
        Path alias = target.resolveSibling("alias");
        try {
            return Files.createSymbolicLink(alias, target);
        } catch (UnsupportedOperationException e) {
            Assume.assumeNoException("Symbolic links are unavailable", e);
            throw e;
        } catch (IOException e) {
            if (System.getProperty("os.name").startsWith("Windows")) {
                Assume.assumeNoException("Symbolic link creation may require Windows privileges", e);
            }
            throw e;
        }
    }

    @Test public void leavesInitializationUntouchedWithoutOwnership() {
        var params = params(mod);
        EmmyLuaClientFeatures.excludeOwnedRoots(params, List.of());
        assertEquals(mod.toUri().toString(), params.getRootUri());
        assertEquals(1, params.getWorkspaceFolders().size());
        assertNull(params.getCapabilities().getWorkspace().getConfiguration());
    }
}
