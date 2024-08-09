package com.cppcxy.ide.formatter

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.*
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.SystemInfoRt
import com.intellij.openapi.util.TextRange
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException


interface ReformatAccept {
    fun accept(s: String)
    fun error(s: String)
}

object CodeFormatAdaptor {
    private val pluginSource: String?
        get() = PluginManagerCore.getPlugin(PluginId.getId("com.cppcxy.Intellij-EmmyLua"))?.pluginPath?.toFile()?.path


    private val codeFormatPath: String = "$pluginSource/CodeFormat/bin/$codeFormatExe"

    private val codeFormatExe: String
        get() {
            return if (SystemInfoRt.isWindows) {
                "win32-x64/CodeFormat.exe"
            } else if (SystemInfoRt.isMac) {
                if (System.getProperty("os.arch") == "arm64") {
                    "darwin-arm64/CodeFormat"
                } else {
                    "darwin-x64/CodeFormat"
                }
            } else {
                "linux-x64/CodeFormat"
            }
        }

    private val project: Project
        get() = ProjectManager.getInstance().openProjects.first()

    fun runCodeFormat(filePath: String?, text: String, reformatAccept: ReformatAccept) {
        val codeFormat = codeFormatPath

        val file = File(codeFormat)
        if (!file.exists()) {
            return reformatAccept.error("can not find CodeFormat")
        }
        if (!file.canExecute()) {
            val runtime = Runtime.getRuntime()
            val process = runtime.exec(arrayOf("chmod", "u+x", file.absolutePath))
            process.waitFor()
        }
        val commandLine = GeneralCommandLine()
            .withExePath(codeFormat)
            .withCharset(charset("utf8"))
            .withParameters(
                "format",
                "-i",
            )

        val workspaceDir = project.basePath?.let { File(it) }
        if (filePath != null) {
            commandLine.addParameters(
                "-d",
                "-f",
                filePath
            )
            if (workspaceDir?.isDirectory == true) {
                commandLine.addParameters("-w", workspaceDir.absolutePath)
            }
        } else {
            val root = workspaceDir?.absolutePath + "/.editorconfig"
            commandLine.addParameters("-c", root)
        }

        val handler = OSProcessHandler(commandLine)
        val stdin = handler.processInput
        stdin.write(text.toByteArray())
        stdin.flush()
        stdin.close()
        val stdout = ByteArrayOutputStream()
        val stderr = ByteArrayOutputStream()

        handler.addProcessListener(object : ProcessListener {
            override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                val bytes = event.text.toByteArray()
                when (outputType) {
                    ProcessOutputType.STDERR -> stderr.writeBytes(bytes)
                    ProcessOutputType.STDOUT -> stdout.writeBytes(bytes)
                }
            }

            override fun processTerminated(event: ProcessEvent) {
                if (event.exitCode == 0) {
                    reformatAccept.accept(stdout.toString("utf8"))
                } else {
                    reformatAccept.error(stderr.toString("utf8"))
                }
            }
        })

        handler.startNotify()
    }

    fun runCodeRangeFormat(filePath: String?, range: TextRange, text: String, reformatAccept: ReformatAccept) {
        val codeFormat = codeFormatPath

        val file = File(codeFormat)
        if (!file.exists()) {
            return reformatAccept.error("can not find CodeFormat")
        }
        val commandLine = GeneralCommandLine()
            .withExePath(codeFormat)
            .withCharset(charset("utf8"))
            .withParameters(
                "rangeformat",
                "-i",
                "--range-offset",
                "${range.startOffset}:${range.endOffset}",
                "--complete-output"
            )

        val workspaceDir = project.basePath?.let { File(it) }
        if (filePath != null) {
            commandLine.addParameters(
                "-d",
                "-f",
                filePath
            )
            if (workspaceDir?.isDirectory == true) {
                commandLine.addParameters("-w", workspaceDir.absolutePath)
            }
        } else {
            val root = workspaceDir?.absolutePath + "/.editorconfig"
            commandLine.addParameters("-c", root)
        }

        val handler = OSProcessHandler(commandLine)
        val stdin = handler.processInput
        stdin.write(text.toByteArray())
        stdin.flush()
        stdin.close()
        val stdout = ByteArrayOutputStream()
        val stderr = ByteArrayOutputStream()

        handler.addProcessListener(object : ProcessListener {
            override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                val bytes = event.text.toByteArray()
                when (outputType) {
                    ProcessOutputType.STDERR -> stderr.writeBytes(bytes)
                    ProcessOutputType.STDOUT -> stdout.writeBytes(bytes)
                }
            }

            override fun processTerminated(event: ProcessEvent) {
                if (event.exitCode == 0) {
                    reformatAccept.accept(stdout.toString("utf8"))
                } else {
                    reformatAccept.error(stderr.toString("utf8"))
                }
            }
        })

        handler.startNotify()
    }

    fun check(filePath: String?, text: String): Pair<Boolean, String> {
        val codeFormat = codeFormatPath

        val file = File(codeFormat)
        if (!file.exists()) {
            throw FileNotFoundException("can not find CodeFormat")
        }

        if (!file.canExecute()) {
            val runtime = Runtime.getRuntime()
            val process = runtime.exec(arrayOf("chmod", "u+x", file.absolutePath))
            process.waitFor()
        }
        val commandLine = GeneralCommandLine()
            .withExePath(codeFormat)
            .withCharset(charset("utf8"))
            .withParameters(
                "check",
                "-i",
                "--dump-json"
            )

        val workspaceDir = project.basePath?.let { File(it) }
        if (filePath != null) {
            commandLine.addParameters(
                "-d",
                "-f",
                filePath
            )
            if (workspaceDir?.isDirectory == true) {
                commandLine.addParameters("-w", workspaceDir.absolutePath)
            }
        } else {
            val configFile = workspaceDir?.absolutePath + "/.editorconfig"
            if (File(configFile).exists()) {
                commandLine.addParameters("-c", configFile)
            }
        }

        val handler = OSProcessHandler(commandLine)
        val stdin = handler.processInput
        stdin.write(text.toByteArray())
        stdin.flush()
        stdin.close()
        val stdout = ByteArrayOutputStream()
        val stderr = ByteArrayOutputStream()

        var exitCode = 0
        handler.addProcessListener(object : ProcessListener {
            override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                val bytes = event.text.toByteArray()
                when (outputType) {
                    ProcessOutputType.STDERR -> stderr.writeBytes(bytes)
                    ProcessOutputType.STDOUT -> stdout.writeBytes(bytes)
                }
            }

            override fun processTerminated(event: ProcessEvent) {
                exitCode = event.exitCode
            }
        })

        handler.startNotify()
        handler.waitFor()

        return if (exitCode == 0) {
            Pair(true, stdout.toString("utf8"))
        } else {
            Pair(false, stderr.toString("utf8"))
        }
    }
}