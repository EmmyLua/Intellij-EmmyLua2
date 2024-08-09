import de.undercouch.gradle.tasks.download.Download
import org.gradle.internal.os.OperatingSystem

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.0.0-Beta4"
    id("org.jetbrains.intellij") version "1.17.2"
    id("de.undercouch.download") version "5.3.0"
}

data class BuildData(
    val ideaSDKShortVersion: String,
    // https://www.jetbrains.com/intellij-repository/releases
    val ideaSDKVersion: String,
    val sinceBuild: String,
    val untilBuild: String,
    val archiveName: String = "IntelliJ-SumnekoLua",
    val jvmTarget: String = "17",
    val targetCompatibilityLevel: JavaVersion = JavaVersion.VERSION_17,
    // https://github.com/JetBrains/gradle-intellij-plugin/issues/403#issuecomment-542890849
    val instrumentCodeCompilerVersion: String = ideaSDKVersion,
    val type: String = "IU"
)

val buildDataList = listOf(
    BuildData(
        ideaSDKShortVersion = "242",
        ideaSDKVersion = "LATEST-EAP-SNAPSHOT",
        sinceBuild = "233",
        untilBuild = "242.*",
    )
)

group = "com.cppcxy"
val emmyluaAnalyzerVersion = "0.6.3"
val emmyDebuggerVersion = "1.8.2"

val emmyluaAnalyzerProjectUrl = "https://github.com/CppCXY/EmmyLuaAnalyzer"

val buildVersion = System.getProperty("IDEA_VER") ?: buildDataList.first().ideaSDKShortVersion

val buildVersionData = buildDataList.find { it.ideaSDKShortVersion == buildVersion }!!

val runnerNumber = System.getenv("RUNNER_NUMBER") ?: "Dev"

version = "${emmyluaAnalyzerVersion}.${runnerNumber}-IDEA${buildVersion}"


task("download", type = Download::class) {
    src(
        arrayOf(
            "${emmyluaAnalyzerProjectUrl}/releases/download/${emmyluaAnalyzerVersion}/EmmyLua.LanguageServer-win32-x64.zip",
            "${emmyluaAnalyzerProjectUrl}/releases/download/${emmyluaAnalyzerVersion}/EmmyLua.LanguageServer-linux-x64.tar.gz",
            "${emmyluaAnalyzerProjectUrl}/releases/download/${emmyluaAnalyzerVersion}/EmmyLua.LanguageServer-darwin-arm64.zip",
            "${emmyluaAnalyzerProjectUrl}/releases/download/${emmyluaAnalyzerVersion}/EmmyLua.LanguageServer-darwin-x64.zip",
        )
    )
    dest("temp")
}

task("makeServer", type = Copy::class) {
    dependsOn("download")
    from(zipTree("temp/EmmyLua.LanguageServer-win32-x64.zip")) {
        into("server/")
    }
    from(tarTree("temp/EmmyLua.LanguageServer-linux-x64.tar.gz")) {
        into("server/EmmyLua.LanguageServer-linux-x64")
    }
    from(zipTree("temp/EmmyLua.LanguageServer-darwin-arm64.zip")) {
        into("server/")
    }
    from(zipTree("temp/EmmyLua.LanguageServer-darwin-x64.zip")) {
        into("server/")
    }

    destinationDir = file("temp")
}


task("install", type = Copy::class) {
    dependsOn("makeServer")
    from("temp/server") {
        into("server")
    }
    destinationDir = file("src/main/resources")
}

task("downloadEmmyDebugger", type = Download::class) {
    src(arrayOf(
        "https://github.com/EmmyLua/EmmyLuaDebugger/releases/download/${emmyDebuggerVersion}/darwin-arm64.zip",
        "https://github.com/EmmyLua/EmmyLuaDebugger/releases/download/${emmyDebuggerVersion}/darwin-x64.zip",
        "https://github.com/EmmyLua/EmmyLuaDebugger/releases/download/${emmyDebuggerVersion}/linux-x64.zip",
        "https://github.com/EmmyLua/EmmyLuaDebugger/releases/download/${emmyDebuggerVersion}/win32-x64.zip",
        "https://github.com/EmmyLua/EmmyLuaDebugger/releases/download/${emmyDebuggerVersion}/win32-x86.zip"
    ))

    dest("temp")
}

task("unzipEmmyDebugger", type = Copy::class) {
    dependsOn("downloadEmmyDebugger")
    from(zipTree("temp/win32-x86.zip")) {
        into("windows/x86")
    }
    from(zipTree("temp/win32-x64.zip")) {
        into("windows/x64")
    }
    from(zipTree("temp/darwin-x64.zip")) {
        into("mac/x64")
    }
    from(zipTree("temp/darwin-arm64.zip")) {
        into("mac/arm64")
    }
    from(zipTree("temp/linux-x64.zip")) {
        into("linux")
    }
    destinationDir = file("temp")
}

task("installEmmyDebugger", type = Copy::class) {
    dependsOn("unzipEmmyDebugger")
    from("temp/windows/x64/") {
        include("emmy_core.dll")
        into("debugger/emmy/windows/x64")
    }
    from("temp/windows/x86/") {
        include("emmy_core.dll")
        into("debugger/emmy/windows/x86")
    }
    from("temp/linux/") {
        include("emmy_core.so")
        into("debugger/emmy/linux")
    }
    from("temp/mac/x64") {
        include("emmy_core.dylib")
        into("debugger/emmy/mac/x64")
    }
    from("temp/mac/arm64") {
        include("emmy_core.dylib")
        into("debugger/emmy/mac/arm64")
    }
    destinationDir = file("src/main/resources")
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    pluginName.set("EmmyLua2")
    version.set(buildVersionData.ideaSDKVersion)
    type.set(buildVersionData.type) // Target IDE Platform
    sandboxDir.set("${project.buildDir}/${buildVersionData.ideaSDKShortVersion}/idea-sandbox")
    plugins.set(listOf("com.redhat.devtools.lsp4ij:0.3.0"))
}

repositories {
    maven(url = "https://www.jetbrains.com/intellij-repository/releases")
    mavenCentral()
}

sourceSets {
    main {
        java.srcDirs("gen")
        resources.srcDir("resources")
    }
}

tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = buildVersionData.jvmTarget
    }

    patchPluginXml {
        sinceBuild.set(buildVersionData.sinceBuild)
        untilBuild.set(buildVersionData.untilBuild)
    }

    instrumentCode {
        compilerVersion.set(buildVersionData.instrumentCodeCompilerVersion)
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }

    buildPlugin {
        dependsOn("install", "installEmmyDebugger")
    }
    // fix by https://youtrack.jetbrains.com/issue/IDEA-325747/IDE-always-actively-disables-LSP-plugins-if-I-ask-the-plugin-to-return-localized-diagnostic-messages.
    runIde {
        autoReloadPlugins.set(false)
    }

    prepareSandbox {
        doLast {
            copy {
                from("${project.projectDir}/src/main/resources/server")
                into("${destinationDir.path}/${pluginName.get()}/server")
            }
            copy {
                from("${project.projectDir}/src/main/resources/debugger")
                into("${destinationDir.path}/${pluginName.get()}/debugger")
            }
        }
    }
}
