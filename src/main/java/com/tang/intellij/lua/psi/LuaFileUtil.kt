/*
 * Copyright (c) 2017. tangzx(love.tangzx@qq.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tang.intellij.lua.psi

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.ProjectAndLibrariesScope
import java.io.File

/**
 *
 * Created by tangzx on 2017/1/4.
 */
object LuaFileUtil {

    private val pluginVirtualDirectory: VirtualFile?
        get() {
            val descriptor = PluginManagerCore.getPlugin(PluginId.getId("com.cppcxy.Intellij-EmmyLua"))
            if (descriptor != null) {
                return VirtualFileManager.getInstance().findFileByNioPath(descriptor.pluginPath)
            }

            return null
        }

    /**
     * Find a file by short URL with additional source roots for searching.
     * 
     * @param project The project context
     * @param shortUrl The file path to find (can be relative or absolute)
     * @param additionalSourceRoots Additional directories to search in (for debugger path resolution)
     * @return The found VirtualFile or null
     */
    fun findFile(project: Project, shortUrl: String?, additionalSourceRoots: List<String> = emptyList()): VirtualFile? {
        var fixedShortUrl = shortUrl ?: return null

        // Check if the path is absolute
        if (File(fixedShortUrl).isAbsolute) {
            val virtualFile = VfsUtil.findFileByIoFile(File(fixedShortUrl), true)
            if (virtualFile != null && virtualFile.exists()) {
                return virtualFile
            }
            return null
        }

        // "./x.lua" => "x.lua"
        if (fixedShortUrl.startsWith("./") || fixedShortUrl.startsWith(".\\")) {
            fixedShortUrl = fixedShortUrl.substring(2)
        }
        
        // First, try to find in additional source roots
        if (additionalSourceRoots.isNotEmpty()) {
            val foundInSourceRoots = findInSourceRoots(fixedShortUrl, additionalSourceRoots)
            if (foundInSourceRoots != null) {
                return foundInSourceRoots
            }
        }
        
        // Check if the fixedShortUrl already has an extension
        val hasExtension = fixedShortUrl.contains(".")
        if (hasExtension) {
            val virtualFile = findVirtualFile(project, fixedShortUrl)
            if (virtualFile != null && virtualFile.exists()) {
                return virtualFile
            }
            return null
        } else {
            val extensions = LuaFileManager.extensions
            for (extension in extensions) {
                val fileName = if (extension.isEmpty()) fixedShortUrl else "$fixedShortUrl$extension"
                val virtualFile = findVirtualFile(project, fileName)
                if (virtualFile != null && virtualFile.exists()) {
                    return virtualFile
                }
            }
        }
        return null
    }
    
    /**
     * Find a file in the specified source roots.
     */
    private fun findInSourceRoots(relativePath: String, sourceRoots: List<String>): VirtualFile? {
        for (sourceRoot in sourceRoots) {
            if (sourceRoot.isBlank()) continue
            
            val rootFile = File(sourceRoot)
            if (!rootFile.exists() || !rootFile.isDirectory) continue
            
            // Try direct path
            val targetFile = File(rootFile, relativePath)
            if (targetFile.exists() && targetFile.isFile) {
                val virtualFile = VfsUtil.findFileByIoFile(targetFile, true)
                if (virtualFile != null && virtualFile.exists()) {
                    return virtualFile
                }
            }
            
            // If no extension, try with Lua extensions
            if (!relativePath.contains(".")) {
                val extensions = LuaFileManager.extensions
                for (extension in extensions) {
                    val fileName = if (extension.isEmpty()) relativePath else "$relativePath$extension"
                    val fileWithExt = File(rootFile, fileName)
                    if (fileWithExt.exists() && fileWithExt.isFile) {
                        val virtualFile = VfsUtil.findFileByIoFile(fileWithExt, true)
                        if (virtualFile != null && virtualFile.exists()) {
                            return virtualFile
                        }
                    }
                }
            }
        }
        return null
    }

    fun findVirtualFile(project: Project, filename: String): VirtualFile? {
        val files = FilenameIndex.getVirtualFilesByName(filename, ProjectAndLibrariesScope(project))
        var perfect: VirtualFile? = null
        var perfectMatch = Int.MAX_VALUE
        for (file in files) {
            val path = file.canonicalPath
            if (path != null && perfectMatch > path.length && path.endsWith(filename)) {
                perfect = file
                perfectMatch = path.length
            }
        }

        if (perfect != null) {
            return perfect
        }

        return VfsUtil.findRelativeFile(filename, project.baseDir)
    }

    fun getPluginVirtualFile(path: String): String? {
        val directory = pluginVirtualDirectory
        if (directory != null) {
            var fullPath = directory.path + "/classes/" + path
            if (File(fullPath).exists())
                return fullPath
            fullPath = directory.path + "/" + path
            if (File(fullPath).exists())
                return fullPath
        }
        return null
    }
}
