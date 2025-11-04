package com.tang.intellij.lua.editor

import com.cppcxy.ide.lsp.EmmyLuaCustomApi
import com.cppcxy.ide.lsp.GutterInfo
import com.cppcxy.ide.lsp.GutterKind
import com.cppcxy.ide.lsp.GutterParams
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.codeInsight.daemon.impl.PsiElementListNavigator
import com.intellij.icons.AllIcons
import com.intellij.ide.util.DefaultPsiElementCellRenderer
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.util.TextRange
import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.redhat.devtools.lsp4ij.LanguageServerItem
import com.redhat.devtools.lsp4ij.LanguageServerManager
import com.tang.intellij.lua.lang.LuaIcons
import com.tang.intellij.lua.psi.LuaPsiFile
import org.eclipse.lsp4j.Position
import java.awt.event.MouseEvent
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.swing.Icon

/**
 * Line marker provider that shows gutter icons for Lua code structure
 * The information is retrieved from LSP via custom request
 */
class LuaLineMarkerProvider : LineMarkerProvider {
    companion object {
        private val LOG = Logger.getInstance(LuaLineMarkerProvider::class.java)
    }

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        // We collect line markers at file level, not per element
        return null
    }

    override fun collectSlowLineMarkers(
        elements: List<PsiElement>,
        result: MutableCollection<in LineMarkerInfo<*>>
    ) {
        if (elements.isEmpty()) return
        
        val file = elements.first().containingFile
        if (file !is LuaPsiFile) return
        
        val project = file.project
        val virtualFile = file.virtualFile ?: return
        val uri = virtualFile.url
        
        // Try to get cached gutter info
        var gutterInfos = LuaGutterCacheManager.getCache(uri)
        
        if (gutterInfos == null) {
            // Request gutter information from LSP synchronously
            try {
                val languageServerFuture = LanguageServerManager.getInstance(project)
                    .getLanguageServer("EmmyLua")
                
                // Get the language server with timeout
                val languageServerItem = try {
                    languageServerFuture.get(1, TimeUnit.SECONDS)
                } catch (e: Exception) {
                    LOG.debug("Timeout or error getting language server", e)
                    null
                }
                
                if (languageServerItem != null) {
                    try {
                        val server = languageServerItem.server as? EmmyLuaCustomApi
                        if (server != null) {
                            val params = GutterParams(uri)
                            val gutterFuture = server.getGutter(params)
                            
                            // Wait for gutter info with timeout
                            gutterInfos = try {
                                gutterFuture.get(2, TimeUnit.SECONDS)
                            } catch (e: Exception) {
                                LOG.debug("Timeout or error getting gutter info", e)
                                emptyList()
                            }
                            
                            // Cache the result
                            if (gutterInfos != null && gutterInfos.isNotEmpty()) {
                                LuaGutterCacheManager.setCache(uri, gutterInfos)
                            }
                        }
                    } catch (e: Exception) {
                        LOG.warn("Error casting to EmmyLuaCustomApi", e)
                    }
                }
            } catch (e: Exception) {
                LOG.warn("Error collecting gutter info", e)
                return
            }
        }
        
        if (gutterInfos == null || gutterInfos.isEmpty()) return
        
        // Convert LSP positions to PSI elements and create line markers
        for (gutterInfo in gutterInfos) {
            try {
                val range = gutterInfo.range
                val startOffset = positionToOffset(file, range.start)
                val endOffset = positionToOffset(file, range.end)
                
                if (startOffset < 0 || endOffset < 0 || startOffset >= file.textLength) {
                    continue
                }
                
                // Find the element at this position
                val element = file.findElementAt(startOffset) ?: continue
                
                // Create line marker with navigation handler
                val icon = getIconForKind(gutterInfo.kind)
                val tooltip = gutterInfo.detail ?: gutterInfo.kind.name
                
                // Create navigation handler for Override kind
                val navHandler = if (gutterInfo.kind == GutterKind.Override) {
                    createOverrideNavigationHandler(element, gutterInfo)
                } else {
                    null
                }
                
                val markerInfo = LineMarkerInfo(
                    element,
                    TextRange(startOffset, endOffset),
                    icon,
                    { tooltip },
                    navHandler,
                    GutterIconRenderer.Alignment.LEFT,
                    { tooltip }
                )
                
                result.add(markerInfo)
            } catch (e: Exception) {
                LOG.warn("Error creating line marker", e)
            }
        }
    }
    
    /**
     * Create navigation handler for Override markers
     */
    private fun createOverrideNavigationHandler(
        element: PsiElement,
        gutterInfo: GutterInfo
    ): (MouseEvent, PsiElement) -> Unit {
        return { _, psi ->
            // For now, we'll create a simple navigation to the element itself
            // In a full implementation, you would:
            // 1. Request override targets from LSP
            // 2. Convert LSP locations to PsiElements
            // 3. Show popup with list of targets
            
            val navigatableElement = psi as? NavigatablePsiElement
            if (navigatableElement != null) {
                // Show a simple popup (you can enhance this later)
                // For now, just navigate to the element itself
                // TODO: Request actual override targets from LSP and show them in a list
                navigatableElement.navigate(true)
            }
        }
    }
    
    /**
     * Convert LSP Position to PSI offset
     */
    private fun positionToOffset(file: PsiFile, position: Position): Int {
        val document = file.viewProvider.document ?: return -1
        
        val line = position.line
        val character = position.character
        
        if (line < 0 || line >= document.lineCount) {
            return -1
        }
        
        val lineStartOffset = document.getLineStartOffset(line)
        return lineStartOffset + character
    }
    
    /**
     * Get icon for the gutter kind
     */
    private fun getIconForKind(kind: GutterKind): Icon {
        return when (kind) {
            GutterKind.Class -> LuaIcons.CLASS
            GutterKind.Enum -> LuaIcons.ENUM
            GutterKind.Alias -> LuaIcons.Alias
            GutterKind.Method -> LuaIcons.CLASS_METHOD
            GutterKind.Module -> LuaIcons.FILE
            GutterKind.Override -> AllIcons.Gutter.OverridingMethod
        }
    }
}
