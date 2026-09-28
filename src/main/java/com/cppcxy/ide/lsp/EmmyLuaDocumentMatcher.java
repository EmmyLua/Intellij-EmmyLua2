package com.cppcxy.ide.lsp;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.redhat.devtools.lsp4ij.DocumentMatcher;
import org.jetbrains.annotations.NotNull;

/** Leaves externally routed files to their provider's server mapping. */
public final class EmmyLuaDocumentMatcher implements DocumentMatcher {
    @Override
    public boolean match(@NotNull VirtualFile file, @NotNull Project project) {
        return EmmyLuaServerRouting.DEFAULT_SERVER_ID.equals(EmmyLuaServerRouting.getServerId(project, file));
    }
}
