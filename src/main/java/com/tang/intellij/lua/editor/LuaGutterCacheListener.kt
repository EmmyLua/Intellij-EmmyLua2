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
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.FileEditorManagerListener.FILE_EDITOR_MANAGER
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import com.tang.intellij.lua.lang.LuaFileType
import com.tang.intellij.lua.psi.LuaPsiFile
import java.util.concurrent.ConcurrentHashMap

/**
 * Manager to handle gutter cache and trigger updates
 */
object LuaGutterCacheManager {
    data class Key(val project: Project, val serverId: String, val uri: String)

    private val gutterCache = ConcurrentHashMap<Key, List<GutterInfo>>()
    private val cacheTimestamps = ConcurrentHashMap<Key, Long>()

    fun clearCache(uri: String) {
        gutterCache.keys.removeIf { it.uri == uri }
        cacheTimestamps.keys.removeIf { it.uri == uri }
    }

    fun clearProject(project: Project) {
        gutterCache.keys.removeIf { it.project === project }
        cacheTimestamps.keys.removeIf { it.project === project }
    }

    fun getCache(key: Key): List<GutterInfo>? = gutterCache[key]

    fun setCache(key: Key, infos: List<GutterInfo>) {
        gutterCache[key] = infos
        cacheTimestamps[key] = System.currentTimeMillis()
    }

    fun isCacheStale(key: Key, maxAgeMs: Long = 1000): Boolean {
        val timestamp = cacheTimestamps[key] ?: return true
        return System.currentTimeMillis() - timestamp > maxAgeMs
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
        val psiDocumentManager = PsiDocumentManager.getInstance(project)
        val psiFile = psiDocumentManager.getCachedPsiFile(document) as? LuaPsiFile ?: return
        val virtualFile = psiFile.virtualFile ?: return

        if (!virtualFile.isValid || virtualFile.fileType !== LuaFileType.INSTANCE) return

        // Clear the cache immediately for instant refresh
        LuaGutterCacheManager.clearCache(virtualFile.url)

        // Cancel any pending update
        pendingUpdates[document]?.let {
            // The runnable will be replaced
        }

        // Schedule restart of code analysis with shorter debouncing (200ms)
        val now = System.currentTimeMillis()
        val lastUpdate = updateScheduler[document] ?: 0

        // Reduced debounce time to 200ms for better responsiveness
        val debounceTime = 200L

        val updateRunnable = Runnable {
            ApplicationManager.getApplication().invokeLater {
                if (project.isDisposed) return@invokeLater
                val latestPsi = psiDocumentManager.getPsiFile(document)
                if (latestPsi is LuaPsiFile && latestPsi.isValid) {
                    DaemonCodeAnalyzer.getInstance(project).restart(latestPsi)
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
            // Clear cache for the newly opened file to ensure fresh data
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
        val parentDisposable = project.service<LuaGutterCacheListenerDisposable>()

        // Prevent duplicate registration
        if (parentDisposable.isInitialized) {
            return
        }
        parentDisposable.isInitialized = true

        // Register document listener
        val documentListener = LuaDocumentListener(project)
        val appConnection = ApplicationManager.getApplication().messageBus.connect(parentDisposable)
        val projectConnection = project.messageBus.connect(parentDisposable)

        // Listen to editor creation events to attach the document listener
        EditorFactory.getInstance()
            .eventMulticaster
            .addDocumentListener(documentListener, parentDisposable)

        // Register file editor listener
        projectConnection.subscribe(FILE_EDITOR_MANAGER, LuaFileEditorListener(project))

        // Register bulk file listener to detect external changes
        appConnection.subscribe(
            VirtualFileManager.VFS_CHANGES,
            object : BulkFileListener {
                override fun after(events: List<VFileEvent>) {
                    for (event in events) {
                        val file = event.file
                        if (file != null && file.isValid && file.fileType === LuaFileType.INSTANCE) {
                            // Clear cache when the file changes externally
                            LuaGutterCacheManager.clearCache(file.url)

                            // Skip delete events - no need to restart analysis on deleted files
                            if (event is VFileDeleteEvent) {
                                continue
                            }

                            // Trigger update
                            ApplicationManager.getApplication().invokeLater {
                                // Double-check file is still valid when the callback executes
                                if (file.isValid) {
                                    val psiFile = PsiManager.getInstance(project).findFile(file)
                                    if (psiFile is LuaPsiFile && psiFile.isValid) {
                                        DaemonCodeAnalyzer.getInstance(project).restart(psiFile)
                                    }
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
class LuaGutterCacheListenerDisposable(private val project: Project) : Disposable {
    @Volatile
    var isInitialized: Boolean = false

    override fun dispose() {
        isInitialized = false
        LuaGutterCacheManager.clearProject(project)
    }
}
