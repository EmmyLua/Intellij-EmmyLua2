package com.tang.intellij.lua.editor

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.tang.intellij.lua.psi.LuaPsiFile
import java.util.concurrent.ConcurrentHashMap

/**
 * Manager to handle gutter cache and trigger updates
 */
object LuaGutterCacheManager {
    private val gutterCache = ConcurrentHashMap<String, List<com.cppcxy.ide.lsp.GutterInfo>>()
    private val cacheTimestamps = ConcurrentHashMap<String, Long>()
    
    fun clearCache(uri: String) {
        gutterCache.remove(uri)
        cacheTimestamps.remove(uri)
    }
    
    fun clearAllCache() {
        gutterCache.clear()
        cacheTimestamps.clear()
    }
    
    fun getCache(uri: String): List<com.cppcxy.ide.lsp.GutterInfo>? {
        return gutterCache[uri]
    }
    
    fun setCache(uri: String, infos: List<com.cppcxy.ide.lsp.GutterInfo>) {
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
        
        if (file.extension != "lua") return
        
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
        if (file.extension == "lua") {
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
        val connection = ApplicationManager.getApplication().messageBus.connect(project)
        
        // Listen to editor creation events to attach document listener
        val editorFactory = com.intellij.openapi.editor.EditorFactory.getInstance()
        editorFactory.addEditorFactoryListener(
            object : com.intellij.openapi.editor.event.EditorFactoryListener {
                override fun editorCreated(event: com.intellij.openapi.editor.event.EditorFactoryEvent) {
                    event.editor.document.addDocumentListener(documentListener, project)
                }
            },
            project
        )
        
        // Register file editor listener
        project.messageBus
            .connect()
            .subscribe(
                FileEditorManagerListener.FILE_EDITOR_MANAGER,
                LuaFileEditorListener(project)
            )
        
        // Register bulk file listener to detect external changes
        connection.subscribe(
            com.intellij.openapi.vfs.VirtualFileManager.VFS_CHANGES,
            object : com.intellij.openapi.vfs.newvfs.BulkFileListener {
                override fun after(events: List<com.intellij.openapi.vfs.newvfs.events.VFileEvent>) {
                    for (event in events) {
                        val file = event.file
                        if (file != null && file.extension == "lua") {
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
