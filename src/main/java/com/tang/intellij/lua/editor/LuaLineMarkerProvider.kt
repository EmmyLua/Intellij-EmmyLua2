package com.tang.intellij.lua.editor

import com.cppcxy.ide.lsp.*
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.redhat.devtools.lsp4ij.LanguageServerManager
import com.tang.intellij.lua.lang.LuaIcons
import com.tang.intellij.lua.psi.LuaPsiFile
import org.eclipse.lsp4j.Position
import java.awt.event.MouseEvent
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

        // Only process once per file - check if this is the first element in the file
        val firstElement = elements.firstOrNull() ?: return
        if (firstElement.textOffset > 0) {
            // This is not the beginning of the file, skip to avoid duplicate processing
            return
        }

        val project = file.project
        val virtualFile = file.virtualFile ?: return
        val uri = virtualFile.url

        // Check if we need to refresh the cache
        // - Cache is missing
        // - Cache is older than 2 seconds (to account for document changes)
        val needsRefresh = LuaGutterCacheManager.getCache(uri) == null ||
                LuaGutterCacheManager.isCacheStale(uri, 2000)

        // Try to get cached gutter info
        var gutterInfos = if (needsRefresh) null else LuaGutterCacheManager.getCache(uri)

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

        // Track processed positions to avoid duplicates
        val processedKeys = mutableSetOf<String>()

        // Convert LSP positions to PSI elements and create line markers
        for (gutterInfo in gutterInfos) {
            try {
                val range = gutterInfo.range
                val startOffset = positionToOffset(file, range.start)
                val endOffset = positionToOffset(file, range.end)

                if (startOffset < 0 || endOffset < 0 || startOffset >= file.textLength) {
                    continue
                }

                // Create unique key for deduplication: line + kind
                val line = range.start.line
                val uniqueKey = "$line:${gutterInfo.kind}"

                // Skip if already processed
                if (processedKeys.contains(uniqueKey)) {
                    LOG.debug("Skipping duplicate gutter at line $line for kind ${gutterInfo.kind}")
                    continue
                }
                processedKeys.add(uniqueKey)

                // Find the element at this position
                val element = file.findElementAt(startOffset) ?: continue

                // Create line marker with navigation handler
                val icon = getIconForKind(gutterInfo.kind)
                val tooltip = gutterInfo.detail ?: gutterInfo.kind.name

                // Create navigation handler - all kinds are clickable if they have data
                val navHandler = if (gutterInfo.data != null) {
                    createNavigationHandler(project, element, gutterInfo)
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
     * Create navigation handler that requests detail from LSP and shows popup
     */
    private fun createNavigationHandler(
        project: Project,
        element: PsiElement,
        gutterInfo: GutterInfo
    ): (MouseEvent, PsiElement) -> Unit {
        return { mouseEvent, _ ->
            // Special handling for Method kind - parse data directly
            if (gutterInfo.kind == GutterKind.Override && gutterInfo.data is String) {
                val dataStr = gutterInfo.data
                val parts = dataStr.split("#")
                if (parts.size == 3) {
                    try {
                        val filepath = parts[0]
                        val line = parts[1].toInt()
                        val col = parts[2].toInt()

                        ApplicationManager.getApplication().invokeLater {
                            navigateToLocation(project, filepath, line, col)
                        }
                    } catch (e: Exception) {
                        LOG.warn("Failed to parse Method data: $dataStr", e)
                    }
                }
            } else {
                // Request detail information from LSP
                ApplicationManager.getApplication().executeOnPooledThread {
                    try {
                        val languageServerFuture = LanguageServerManager.getInstance(project)
                            .getLanguageServer("EmmyLua")

                        val languageServerItem = try {
                            languageServerFuture.get(1, TimeUnit.SECONDS)
                        } catch (e: Exception) {
                            LOG.warn("Timeout getting language server for gutter detail", e)
                            null
                        }

                        if (languageServerItem != null) {
                            val server = languageServerItem.server as? EmmyLuaCustomApi
                            if (server != null && gutterInfo.data != null) {
                                val params = GutterDetailParams(gutterInfo.data)
                                val detailFuture = server.getGutterDetail(params)

                                val response = try {
                                    detailFuture.get(2, TimeUnit.SECONDS)
                                } catch (e: Exception) {
                                    LOG.warn("Timeout getting gutter detail", e)
                                    null
                                }

                                if (response != null && response.locations.isNotEmpty()) {
                                    // Convert locations to NavigatablePsiElements in read action
                                    val targets =
                                        ApplicationManager.getApplication().runReadAction<List<NavigatablePsiElement>> {
                                            val list = mutableListOf<NavigatablePsiElement>()

                                            for (location in response.locations) {
                                                val virtualFile =
                                                    VirtualFileManager.getInstance().findFileByUrl(location.uri)
                                                if (virtualFile != null) {
                                                    val psiFile = PsiManager.getInstance(project).findFile(virtualFile)
                                                    if (psiFile != null) {
                                                        // Create a NavigatablePsiElement wrapper for the location
                                                        // Use location's kind if available, otherwise use gutterInfo's kind
                                                        val navigatable = LuaGutterNavigatableElement(
                                                            psiFile,
                                                            location.line,
                                                            location.uri,
                                                            location.kind ?: gutterInfo.kind
                                                        )
                                                        list.add(navigatable)
                                                    }
                                                }
                                            }

                                            list
                                        }

                                    // Show popup with navigation targets
                                    if (targets.isNotEmpty()) {
                                        ApplicationManager.getApplication().invokeLater {
                                            if (targets.size == 1) {
                                                // Single target, navigate directly
                                                targets[0].navigate(true)
                                            } else {
                                                // Multiple targets, show popup
                                                val popupTitle = getPopupTitle(gutterInfo.kind)
                                                showNavigationPopup(mouseEvent, targets, popupTitle)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        LOG.warn("Error handling gutter navigation", e)
                    }
                }
            }
        }
    }

    /**
     * Get popup title based on gutter kind
     */
    private fun getPopupTitle(kind: GutterKind): String {
        return when (kind) {
            GutterKind.Class -> "Go to Subclass"
            GutterKind.Override -> "Go to Overriding Methods"
            GutterKind.Method -> "Go to Implementation"
            GutterKind.Alias -> "Go to Type Alias"
            GutterKind.Enum -> "Go to Enum Usage"
            GutterKind.Module -> "Go to Module"
        }
    }

    /**
     * Show navigation popup with multiple targets
     * Using ItemPresentation instead of PsiElement for popup model
     */
    private fun showNavigationPopup(
        mouseEvent: MouseEvent,
        targets: List<NavigatablePsiElement>,
        title: String
    ) {
        // Create wrapper objects that contain ItemPresentation
        val navigationItems = targets.mapNotNull { target ->
            if (target is LuaGutterNavigatableElement) {
                NavigationItem(target)
            } else {
                null
            }
        }

        if (navigationItems.isEmpty()) return

        // Create popup with ItemPresentation-based items
        com.intellij.openapi.ui.popup.JBPopupFactory.getInstance()
            .createPopupChooserBuilder(navigationItems)
            .setTitle(title)
            .setRenderer(com.intellij.ui.SimpleListCellRenderer.create { label, value, _ ->
                if (value != null) {
                    ApplicationManager.getApplication().runReadAction {
                        val presentation = value.element.presentation
                        label.text = presentation.presentableText ?: "Unknown"
                        label.icon = presentation.getIcon(false)
                    }
                }
            })
            .setItemChosenCallback { selectedItem ->
                selectedItem.element.navigate(true)
            }
            .createPopup()
            .show(com.intellij.ui.awt.RelativePoint(mouseEvent))
    }

    /**
     * Wrapper class for navigation items that exposes ItemPresentation
     */
    private data class NavigationItem(val element: LuaGutterNavigatableElement)

    /**
     * Navigate to a specific location
     */
    private fun navigateToLocation(project: Project, filepath: String, line: Int, col: Int) {
        ApplicationManager.getApplication().runReadAction {
            try {
                // Try to find the file by URL
                val uri = if (filepath.startsWith("file://")) {
                    filepath
                } else {
                    "file://$filepath"
                }

                val virtualFile = VirtualFileManager.getInstance().findFileByUrl(uri)
                if (virtualFile != null) {
                    val document = com.intellij.openapi.fileEditor.FileDocumentManager.getInstance()
                        .getDocument(virtualFile)
                    if (document != null && line >= 0 && line < document.lineCount) {
                        val lineStartOffset = document.getLineStartOffset(line)
                        val offset = lineStartOffset + col
                        val descriptor = OpenFileDescriptor(project, virtualFile, offset)
                        descriptor.navigate(true)
                    }
                } else {
                    LOG.warn("Cannot find file: $uri")
                }
            } catch (e: Exception) {
                LOG.warn("Error navigating to location: $filepath:$line:$col", e)
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

/**
 * Navigatable element wrapper for gutter locations
 */
class LuaGutterNavigatableElement(
    private val psiFile: PsiFile,
    private val line: Int,
    private val uri: String,
    private val kind: GutterKind? = null  // Icon kind for this location
) : com.intellij.psi.impl.FakePsiElement(), NavigatablePsiElement {

    override fun getParent(): PsiElement = psiFile

    override fun navigate(requestFocus: Boolean) {
        ApplicationManager.getApplication().runReadAction {
            val project = psiFile.project
            val virtualFile = psiFile.virtualFile ?: return@runReadAction

            val document = com.intellij.openapi.fileEditor.FileDocumentManager.getInstance()
                .getDocument(virtualFile) ?: return@runReadAction

            if (line >= 0 && line < document.lineCount) {
                val offset = document.getLineStartOffset(line)
                val descriptor = OpenFileDescriptor(project, virtualFile, offset)
                descriptor.navigate(requestFocus)
            }
        }
    }

    override fun canNavigate(): Boolean = true
    override fun canNavigateToSource(): Boolean = true

    override fun getName(): String {
        return ApplicationManager.getApplication().runReadAction<String> {
            val fileName = psiFile.name
            val lineNumber = line + 1

            // Try to get the context from the line
            val virtualFile = psiFile.virtualFile
            if (virtualFile != null) {
                val document = com.intellij.openapi.fileEditor.FileDocumentManager.getInstance()
                    .getDocument(virtualFile)
                if (document != null && line >= 0 && line < document.lineCount) {
                    val lineStartOffset = document.getLineStartOffset(line)
                    val lineEndOffset = document.getLineEndOffset(line)
                    val lineText = document.getText(TextRange(lineStartOffset, lineEndOffset)).trim()

                    // Extract meaningful part (e.g., function name, class name)
                    if (lineText.isNotEmpty()) {
                        // Try to extract identifier from line
                        val identifier = extractIdentifier(lineText)
                        if (identifier != null) {
                            return@runReadAction "$identifier ($fileName:$lineNumber)"
                        }
                    }

                    // Fallback to showing first 50 chars of the line
                    val preview = if (lineText.length > 50) {
                        lineText.substring(0, 50) + "..."
                    } else {
                        lineText
                    }
                    return@runReadAction "$preview ($fileName:$lineNumber)"
                }
            }

            "$fileName:$lineNumber"
        }
    }

    /**
     * Extract identifier from line text (function name, class name, etc.)
     */
    private fun extractIdentifier(lineText: String): String? {
        // Try common Lua patterns
        val patterns = listOf(
            Regex("""function\s+([a-zA-Z_][a-zA-Z0-9_.:]*)\s*\("""),  // function name()
            Regex("""local\s+function\s+([a-zA-Z_][a-zA-Z0-9_]*)\s*\("""),  // local function name()
            Regex("""([a-zA-Z_][a-zA-Z0-9_.]*)\s*=\s*function\s*\("""),  // name = function()
            Regex("""---@class\s+([a-zA-Z_][a-zA-Z0-9_]*)"""),  // ---@class ClassName
            Regex("""local\s+([a-zA-Z_][a-zA-Z0-9_]*)\s*="""),  // local var = 
        )

        for (pattern in patterns) {
            val match = pattern.find(lineText)
            if (match != null && match.groupValues.size > 1) {
                return match.groupValues[1]
            }
        }

        return null
    }

    override fun getPresentation(): com.intellij.navigation.ItemPresentation {
        return object : com.intellij.navigation.ItemPresentation {
            override fun getPresentableText(): String {
                return ApplicationManager.getApplication().runReadAction<String> {
                    // Show just the identifier or line preview without file info
                    val fileName = psiFile.name
                    val lineNumber = line + 1

                    val virtualFile = psiFile.virtualFile
                    if (virtualFile != null) {
                        val document = com.intellij.openapi.fileEditor.FileDocumentManager.getInstance()
                            .getDocument(virtualFile)
                        if (document != null && line >= 0 && line < document.lineCount) {
                            val lineStartOffset = document.getLineStartOffset(line)
                            val lineEndOffset = document.getLineEndOffset(line)
                            val lineText = document.getText(TextRange(lineStartOffset, lineEndOffset)).trim()

                            val identifier = extractIdentifier(lineText)
                            if (identifier != null) {
                                return@runReadAction identifier
                            }

                            val preview = if (lineText.length > 60) {
                                lineText.substring(0, 60) + "..."
                            } else {
                                lineText
                            }
                            return@runReadAction preview
                        }
                    }

                    "Line $lineNumber"
                }
            }

            override fun getLocationString(): String {
                return ApplicationManager.getApplication().runReadAction<String> {
                    // Show file path and line number as location
                    val relativePath = psiFile.virtualFile?.path ?: uri
                    "$relativePath:${line + 1}"
                }
            }

            override fun getIcon(unused: Boolean): javax.swing.Icon? {
                return ApplicationManager.getApplication().runReadAction<javax.swing.Icon?> {
                    // Use kind-specific icon if available, otherwise use file icon
                    if (kind != null) {
                        getIconForKind(kind)
                    } else {
                        psiFile.getIcon(0) ?: LuaIcons.FILE
                    }
                }
            }
        }
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

    override fun getContainingFile(): PsiFile = psiFile
}
