import de.undercouch.gradle.tasks.download.Download

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.20-Beta2"
    id("org.jetbrains.intellij.platform") version "2.2.1"
    id("de.undercouch.download") version "5.6.0"
}

group = "com.cppcxy"
val emmyluaAnalyzerVersion = "0.7.1"
val emmyDebuggerVersion = "1.8.2"

val emmyluaAnalyzerProjectUrl = "https://github.com/CppCXY/EmmyLuaAnalyzer"
val emmyluaCodeStyleProjectUrl = "https://github.com/CppCXY/EmmyLuaCodeStyle"

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

task("downloadEmmyDebugger", type = Download::class) {
    src(
        arrayOf(
            "https://github.com/EmmyLua/EmmyLuaDebugger/releases/download/${emmyDebuggerVersion}/darwin-arm64.zip",
            "https://github.com/EmmyLua/EmmyLuaDebugger/releases/download/${emmyDebuggerVersion}/darwin-x64.zip",
            "https://github.com/EmmyLua/EmmyLuaDebugger/releases/download/${emmyDebuggerVersion}/linux-x64.zip",
            "https://github.com/EmmyLua/EmmyLuaDebugger/releases/download/${emmyDebuggerVersion}/win32-x64.zip",
            "https://github.com/EmmyLua/EmmyLuaDebugger/releases/download/${emmyDebuggerVersion}/win32-x86.zip"
        )
    )

    dest("temp/debugger")
}

task("unzip", type = Copy::class) {
    dependsOn("download", "downloadEmmyDebugger")
    // language server
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
    // debugger
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

    destinationDir = file("temp/unzip")
}

task("install", type = Copy::class) {
    dependsOn("unzip")
    from("temp/unzip/server") {
        into("server")
    }
    from("temp/unzip/debugger/windows/x64/") {
        include("emmy_core.dll")
        into("debugger/emmy/windows/x64")
    }
    from("temp/unzip/debugger/windows/x86/") {
        include("emmy_core.dll")
        into("debugger/emmy/windows/x86")
    }
    from("temp/unzip/debugger/linux/") {
        include("emmy_core.so")
        into("debugger/emmy/linux")
    }
    from("temp/unzip/debugger/mac/x64") {
        include("emmy_core.dylib")
        into("debugger/emmy/mac/x64")
    }
    from("temp/unzip/debugger/mac/arm64") {
        include("emmy_core.dylib")
        into("debugger/emmy/mac/arm64")
    }


    destinationDir = file("src/main/resources")
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellijPlatform {
    buildSearchableOptions = false

    projectName = "IntelliJ-EmmyLua2"

    pluginConfiguration {
        name = "EmmyLua2"

        ideaVersion {
            sinceBuild = "251"
            untilBuild = "251.*"
        }
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

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2024.3.2.1")
        bundledPlugins("com.intellij.java", "org.jetbrains.kotlin")

        plugins("com.redhat.devtools.lsp4ij:0.9.0")
    }
}

sourceSets {
    main {
        java.srcDirs("gen")
        resources.srcDir("resources")
    }
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }

    kotlin {
        compilerOptions {
            jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
        }
    }

    patchPluginXml {
        dependsOn("install")
    }

    buildPlugin {
        dependsOn("install")
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
