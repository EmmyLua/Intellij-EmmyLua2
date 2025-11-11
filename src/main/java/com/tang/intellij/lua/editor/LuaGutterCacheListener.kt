package com.tang.intellij.lua.editor

import com.cppcxy.ide.lsp.GutterInfo
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.Service.Level.PROJECT
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.psi.PsiManager
import com.tang.intellij.lua.lang.LuaFileType
import com.tang.intellij.lua.psi.LuaPsiFile
import java.util.concurrent.ConcurrentHashMap

/**
 * Manager to handle gutter cache and trigger updates
 */
object LuaGutterCacheManager {
    private val gutterCache = ConcurrentHashMap<String, List<GutterInfo>>()
    private val cacheTimestamps = ConcurrentHashMap<String, Long>()

    fun clearCache(uri: String) {
        gutterCache.remove(uri)
        cacheTimestamps.remove(uri)
    }

    fun clearAllCache() {
        gutterCache.clear()
        cacheTimestamps.clear()
    }

    fun getCache(uri: String): List<GutterInfo>? {
        return gutterCache[uri]
    }

    fun setCache(uri: String, infos: List<GutterInfo>) {
        gutterCache[uri] = infos
        cacheTimestamps[uri] = System.currentTimeMillis()
    }

    fun getCacheAge(uri: String): Long {
        val timestamp = cacheTimestamps[uri] ?: return Long.MAX_VALUE
        return System.currentTimeMillis() - timestamp
    }

    fun isCacheStale(uri: String, maxAgeMs: Long = 1000): Boolean {
        return getCacheAge(uri) > maxAgeMs
    }
}

/**
 * Document listener to clear cache and restart analysis on document changes
 */
class LuaDocumentListener(private val project: Project) : DocumentListener {
    private val updateScheduler = mutableMapOf<Document, Long>()
    private val pendingUpdates = mutableMapOf<Document, Runnable>()

    override fun documentChanged(event: DocumentEvent) {
        val document = event.document
        val file = FileDocumentManager.getInstance().getFile(document) ?: return

        if (file.fileType !== LuaFileType.INSTANCE) return

        // Clear cache immediately for instant refresh
        LuaGutterCacheManager.clearCache(file.url)

        // Cancel any pending update
        pendingUpdates[document]?.let {
            // The runnable will be replaced
        }

        // Schedule restart of code analysis with shorter debounce (200ms)
        val now = System.currentTimeMillis()
        val lastUpdate = updateScheduler[document] ?: 0

        // Reduced debounce time to 200ms for better responsiveness
        val debounceTime = 200L

        val updateRunnable = Runnable {
            ApplicationManager.getApplication().invokeLater {
                val psiFile = PsiManager.getInstance(project).findFile(file)
                if (psiFile is LuaPsiFile && psiFile.isValid) {
                    DaemonCodeAnalyzer.getInstance(project).restart(psiFile)
                }
            }
        }

        pendingUpdates[document] = updateRunnable

        if (now - lastUpdate > debounceTime) {
            updateScheduler[document] = now
            // Execute immediately if enough time has passed
            updateRunnable.run()
            pendingUpdates.remove(document)
        } else {
            // Schedule for later
            ApplicationManager.getApplication().executeOnPooledThread {
                Thread.sleep(debounceTime)
                val currentRunnable = pendingUpdates[document]
                if (currentRunnable == updateRunnable) {
                    updateScheduler[document] = System.currentTimeMillis()
                    currentRunnable.run()
                    pendingUpdates.remove(document)
                }
            }
        }
    }
}

/**
 * File editor listener to trigger gutter update when files are opened
 */
class LuaFileEditorListener(private val project: Project) : FileEditorManagerListener {
    override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        if (file.fileType === LuaFileType.INSTANCE) {
            // Clear cache for newly opened file to ensure fresh data
            LuaGutterCacheManager.clearCache(file.url)

            // Trigger code analysis
            ApplicationManager.getApplication().invokeLater {
                val psiFile = PsiManager.getInstance(project).findFile(file)
                if (psiFile is LuaPsiFile) {
                    DaemonCodeAnalyzer.getInstance(project).restart(psiFile)
                }
            }
        }
    }
}

/**
 * Startup activity to register listeners
 */
class LuaGutterCacheStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        // Register document listener
        val documentListener = LuaDocumentListener(project)
        val parentDisposable = project.service<LuaGutterCacheListenerDisposable>()
        val appConnection = ApplicationManager.getApplication().messageBus.connect(parentDisposable)
        val projectConnection = project.messageBus.connect(parentDisposable)

        // Listen to document changes for all editors via the multicaster to avoid duplicate registrations
        EditorFactory.getInstance()
            .eventMulticaster
            .addDocumentListener(documentListener, parentDisposable)

        // Register file editor listener
        projectConnection.subscribe(
            FileEditorManagerListener.FILE_EDITOR_MANAGER,
            LuaFileEditorListener(project)
        )

        // Register bulk file listener to detect external changes
        appConnection.subscribe(
            VirtualFileManager.VFS_CHANGES,
            object : BulkFileListener {
                override fun after(events: List<VFileEvent>) {
                    for (event in events) {
                        val file = event.file
                        if (file != null && file.fileType === LuaFileType.INSTANCE) {
                            // Clear cache when file changes externally
                            LuaGutterCacheManager.clearCache(file.url)

                            // Trigger update
                            ApplicationManager.getApplication().invokeLater {
                                val psiFile = PsiManager.getInstance(project).findFile(file)
                                if (psiFile is LuaPsiFile && psiFile.isValid) {
                                    DaemonCodeAnalyzer.getInstance(project).restart(psiFile)
                                }
                            }
                        }
                    }
                }
            }
        )
    }
}

@Service(PROJECT)
class LuaGutterCacheListenerDisposable : Disposable {
    override fun dispose() {
        // IntelliJ will automatically dispose of it when the project is disposed
    }
}
