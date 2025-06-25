import de.undercouch.gradle.tasks.download.Download

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.20-Beta2"
    id("org.jetbrains.intellij.platform") version "2.6.0"
    id("de.undercouch.download") version "5.6.0"
}

// ============= 项目配置 =============
group = "com.cppcxy"

// 版本配置
object Versions {
    const val emmyluaAnalyzer = "0.8.1"
    const val release = "0.8.4"
    const val emmyDebugger = "1.8.6"
    const val jvm = "17"
    const val ideaSDK = "2024.3.2.1"
}

// 构建数据配置
data class BuildData(
    val ideaSDKShortVersion: String,
    val ideaSDKVersion: String,
    val sinceBuild: String,
    val untilBuild: String,
    val type: String = "IC"
)

private val buildDataList = listOf(
    BuildData(
        ideaSDKShortVersion = "243",
        ideaSDKVersion = "2024.3",
        sinceBuild = "243",
        untilBuild = "251.*"
    )
)

// 动态版本配置
private val buildVersion = System.getProperty("IDEA_VER") ?: buildDataList.first().ideaSDKShortVersion
private val buildVersionData = buildDataList.find { it.ideaSDKShortVersion == buildVersion }
    ?: error("Unsupported IDEA version: $buildVersion")
private val runnerNumber = System.getenv("RUNNER_NUMBER") ?: "Dev"

version = "${Versions.release}.${runnerNumber}-IDEA${buildVersion}"

// 下载URL配置
object DownloadUrls {
    private const val emmyluaAnalyzerProjectUrl = "https://github.com/CppCXY/emmylua-analyzer-rust"
    private const val emmyDebuggerProjectUrl = "https://github.com/EmmyLua/EmmyLuaDebugger"
    
    val emmyluaAnalyzer = arrayOf(
        "$emmyluaAnalyzerProjectUrl/releases/download/${Versions.emmyluaAnalyzer}/emmylua_ls-win32-x64.zip",
        "$emmyluaAnalyzerProjectUrl/releases/download/${Versions.emmyluaAnalyzer}/emmylua_ls-linux-x64.tar.gz",
        "$emmyluaAnalyzerProjectUrl/releases/download/${Versions.emmyluaAnalyzer}/emmylua_ls-darwin-arm64.tar.gz",
        "$emmyluaAnalyzerProjectUrl/releases/download/${Versions.emmyluaAnalyzer}/emmylua_ls-darwin-x64.tar.gz"
    )
    
    val emmyDebugger = arrayOf(
        "$emmyDebuggerProjectUrl/releases/download/${Versions.emmyDebugger}/darwin-arm64.zip",
        "$emmyDebuggerProjectUrl/releases/download/${Versions.emmyDebugger}/darwin-x64.zip",
        "$emmyDebuggerProjectUrl/releases/download/${Versions.emmyDebugger}/linux-x64.zip",
        "$emmyDebuggerProjectUrl/releases/download/${Versions.emmyDebugger}/win32-x64.zip",
        "$emmyDebuggerProjectUrl/releases/download/${Versions.emmyDebugger}/win32-x86.zip"
    )
}

// ============= 任务配置 =============

// 下载Emmy Lua分析器
val downloadEmmyLuaAnalyzer by tasks.registering(Download::class) {
    group = "build setup"
    description = "Download Emmy Lua Analyzer for all platforms"
    
    src(DownloadUrls.emmyluaAnalyzer)
    dest("temp/analyzer")
    overwrite(false)
}

// 下载Emmy调试器
val downloadEmmyDebugger by tasks.registering(Download::class) {
    group = "build setup"
    description = "Download Emmy Debugger for all platforms"
    
    src(DownloadUrls.emmyDebugger)
    dest("temp/debugger")
    overwrite(false)
}

// 解压所有下载的文件
val extractDependencies by tasks.registering(Copy::class) {
    group = "build setup"
    description = "Extract downloaded Emmy dependencies"
    
    dependsOn(downloadEmmyLuaAnalyzer, downloadEmmyDebugger)
    
    // 解压语言服务器
    from(zipTree("temp/analyzer/emmylua_ls-win32-x64.zip")) {
        into("server/win32-x64")
    }
    from(tarTree("temp/analyzer/emmylua_ls-linux-x64.tar.gz")) {
        into("server/linux-x64")
    }
    from(tarTree("temp/analyzer/emmylua_ls-darwin-arm64.tar.gz")) {
        into("server/darwin-arm64")
    }
    from(tarTree("temp/analyzer/emmylua_ls-darwin-x64.tar.gz")) {
        into("server/darwin-x64")
    }
    
    // 解压调试器
    from(zipTree("temp/debugger/win32-x86.zip")) {
        into("debugger/windows/x86")
    }
    from(zipTree("temp/debugger/win32-x64.zip")) {
        into("debugger/windows/x64")
    }
    from(zipTree("temp/debugger/darwin-x64.zip")) {
        into("debugger/mac/x64")
    }
    from(zipTree("temp/debugger/darwin-arm64.zip")) {
        into("debugger/mac/arm64")
    }
    from(zipTree("temp/debugger/linux-x64.zip")) {
        into("debugger/linux")
    }
    
    destinationDir = file("temp/extracted")
}

// 安装依赖到资源目录
val installDependencies by tasks.registering(Copy::class) {
    group = "build setup"
    description = "Install Emmy dependencies to resources directory"
    
    dependsOn(extractDependencies)
    
    // 复制语言服务器
    from("temp/extracted/server") {
        into("server")
    }
    
    // 复制调试器核心文件
    listOf(
        Triple("temp/extracted/debugger/windows/x64", "emmy_core.dll", "debugger/emmy/windows/x64"),
        Triple("temp/extracted/debugger/windows/x86", "emmy_core.dll", "debugger/emmy/windows/x86"),
        Triple("temp/extracted/debugger/linux", "emmy_core.so", "debugger/emmy/linux"),
        Triple("temp/extracted/debugger/mac/x64", "emmy_core.dylib", "debugger/emmy/mac/x64"),
        Triple("temp/extracted/debugger/mac/arm64", "emmy_core.dylib", "debugger/emmy/mac/arm64")
    ).forEach { (sourcePath, fileName, targetPath) ->
        from(sourcePath) {
            include(fileName)
            into(targetPath)
        }
    }
    
    destinationDir = file("src/main/resources")
}

// 复制资源到沙盒的任务
val copyResourcesToSandbox by tasks.registering(Copy::class) {
    group = "build setup"
    description = "Copy resources to sandbox"
    
    dependsOn(installDependencies)
    
    from("src/main/resources/server") {
        into("server")
    }
    from("src/main/resources/debugger") {
        into("debugger")
    }
    
    // 目标目录将在执行时由prepareSandbox任务设置
    destinationDir = layout.buildDirectory.dir("idea-sandbox/plugins/EmmyLua2").get().asFile
}

// 清理任务
val cleanDependencies by tasks.registering(Delete::class) {
    group = "build setup"
    description = "Clean downloaded and extracted dependencies"
    
    delete("temp")
}

// ============= 仓库配置 =============
repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

// ============= 依赖配置 =============
dependencies {
    intellijPlatform {
        intellijIdeaCommunity(Versions.ideaSDK)
        bundledPlugins("com.intellij.java", "org.jetbrains.kotlin")
        plugins("com.redhat.devtools.lsp4ij:0.14.0")
    }
}

// ============= 源码集配置 =============
sourceSets {
    main {
        java.srcDirs("gen")
        resources.srcDir("resources")
    }
}

// ============= IntelliJ 平台配置 =============
intellijPlatform {
    buildSearchableOptions = false
    projectName = "IntelliJ-EmmyLua2"

    pluginConfiguration {
        name = "EmmyLua2"
    }

    publishing {
        token = System.getenv("PUBLISH_TOKEN")
    }

    signing {
        certificateChain = System.getenv("CERTIFICATE_CHAIN")
        privateKey = System.getenv("PRIVATE_KEY")
        password = System.getenv("PRIVATE_KEY_PASSWORD")
    }
}

// ============= 任务配置 =============
tasks {
    // Java 编译配置
    withType<JavaCompile> {
        sourceCompatibility = Versions.jvm
        targetCompatibility = Versions.jvm
        options.encoding = "UTF-8"
    }

    // Kotlin 编译配置
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.fromTarget(Versions.jvm))
        }
    }

    // 插件XML配置
    patchPluginXml {
        dependsOn(installDependencies)
        sinceBuild.set(buildVersionData.sinceBuild)
        untilBuild.set(buildVersionData.untilBuild)
    }

    // 构建插件
    buildPlugin {
        dependsOn(installDependencies)
    }

    // 准备沙盒环境
    prepareSandbox {
        dependsOn(copyResourcesToSandbox)
    }

    // 清理任务
    clean {
        dependsOn(cleanDependencies)
    }
}
