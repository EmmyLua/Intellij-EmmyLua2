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
    const val emmyluaAnalyzer = "0.16.0"
    const val emmyDebugger = "1.8.7"
    const val jvm = "17"
    const val ideaSDK = "2025.2"
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
        ideaSDKShortVersion = "252",
        ideaSDKVersion = "2025.2",
        sinceBuild = "252",
        untilBuild = "253.*"
    )
)

// 动态版本配置
private val buildVersion = System.getProperty("IDEA_VER") ?: buildDataList.first().ideaSDKShortVersion
private val buildVersionData = buildDataList.find { it.ideaSDKShortVersion == buildVersion }
    ?: error("Unsupported IDEA version: $buildVersion")
private val runnerNumber = System.getenv("RUNNER_NUMBER") ?: "Dev"

version = "${Versions.emmyluaAnalyzer}.${runnerNumber}-IDEA${buildVersion}"

// 下载URL配置
object DownloadUrls {
    private const val emmyluaAnalyzerProjectUrl = "https://github.com/CppCXY/emmylua-analyzer-rust"
    private const val emmyDebuggerProjectUrl = "https://github.com/EmmyLua/EmmyLuaDebugger"

    // GitHub 镜像配置
    private val githubMirror = System.getProperty("github.mirror") ?: ""
    private fun applyMirror(url: String): String {
        return if (githubMirror.isNotEmpty() && url.startsWith("https://github.com/")) {
            url.replace("https://github.com/", githubMirror)
        } else url
    }

    val emmyluaAnalyzer = arrayOf(
        "$emmyluaAnalyzerProjectUrl/releases/download/${Versions.emmyluaAnalyzer}/emmylua_ls-win32-x64.zip",
        "$emmyluaAnalyzerProjectUrl/releases/download/${Versions.emmyluaAnalyzer}/emmylua_ls-linux-x64.tar.gz",
        "$emmyluaAnalyzerProjectUrl/releases/download/${Versions.emmyluaAnalyzer}/emmylua_ls-darwin-arm64.tar.gz",
        "$emmyluaAnalyzerProjectUrl/releases/download/${Versions.emmyluaAnalyzer}/emmylua_ls-darwin-x64.tar.gz"
    ).map { applyMirror(it) }.toTypedArray()

    val emmyDebugger = arrayOf(
        "$emmyDebuggerProjectUrl/releases/download/${Versions.emmyDebugger}/darwin-arm64.zip",
        "$emmyDebuggerProjectUrl/releases/download/${Versions.emmyDebugger}/darwin-x64.zip",
        "$emmyDebuggerProjectUrl/releases/download/${Versions.emmyDebugger}/linux-x64.zip",
        "$emmyDebuggerProjectUrl/releases/download/${Versions.emmyDebugger}/win32-x64.zip",
        "$emmyDebuggerProjectUrl/releases/download/${Versions.emmyDebugger}/win32-x86.zip"
    ).map { applyMirror(it) }.toTypedArray()
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
    duplicatesStrategy = DuplicatesStrategy.WARN

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
    duplicatesStrategy = DuplicatesStrategy.WARN

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

// 清理任务
val cleanDependencies by tasks.registering(Delete::class) {
    group = "build setup"
    description = "Clean downloaded and extracted dependencies"

    delete("temp")
}

// ============= 仓库配置 =============
repositories {
    // 添加阿里云镜像仓库优先使用
    maven {
        url = uri("https://maven.aliyun.com/repository/central")
        name = "AliyunMavenCentral"
    }
    maven {
        url = uri("https://maven.aliyun.com/repository/gradle-plugin")
        name = "AliyunGradlePlugin"
    }
    maven {
        url = uri("https://maven.aliyun.com/repository/google")
        name = "AliyunGoogle"
    }

    // 保留原有仓库作为备用
    mavenCentral()
    google()
    gradlePluginPortal()

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
        resources.srcDirs("resources")
        // 排除二进制文件，它们将通过 prepareSandbox 任务单独处理
        resources.exclude("debugger/**/*")
        resources.exclude("server/**/*")
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

        // 确保二进制文件被包含在插件分发包中
        from("src/main/resources/server") {
            into("server")
        }

        from("src/main/resources/debugger") {
            into("debugger")
        }
    }

    // 准备沙盒环境
    prepareSandbox {
        dependsOn(installDependencies)

        from("src/main/resources/server") {
            into("server")
        }

        from("src/main/resources/debugger") {
            into("debugger")
        }

    }

    // 清理任务
    clean {
        dependsOn(cleanDependencies)
    }
}
