package com.tang.intellij.lua.project

import com.intellij.ide.util.projectWizard.ModuleBuilder
import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.Disposable
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.tang.intellij.lua.lang.LuaIcons
import java.io.File

/**
 * Lua Project Builder
 */
class LuaModuleBuilder : ModuleBuilder() {
    
    private var projectType: LuaProjectType = LuaProjectType.PLAIN
    
    override fun getModuleType(): ModuleType<*> = LuaModuleType.instance
    
    override fun setupRootModel(modifiableRootModel: ModifiableRootModel) {
        val contentEntry = doAddContentEntry(modifiableRootModel) ?: return
        val root = contentEntry.file ?: return
        
        val project = modifiableRootModel.project
        
        // Create project structure based on project type
        when (projectType) {
            LuaProjectType.PLAIN -> createPlainStructure(root)
            LuaProjectType.GAME -> createGameStructure(root)
            LuaProjectType.LIBRARY -> createLibraryStructure(root)
        }
        
        // Refresh filesystem
        LocalFileSystem.getInstance().refresh(false)
    }
    
    private fun createPlainStructure(root: com.intellij.openapi.vfs.VirtualFile) {
        val srcDir = root.createChildDirectory(this, "src")
        val mainFile = srcDir.createChildData(this, "main.lua")
        mainFile.setBinaryContent("""
            ---
            --- Main entry point
            ---
            
            local function main()
                print("Hello, Lua!")
            end
            
            main()
        """.trimIndent().toByteArray())
        
        // Create README
        val readme = root.createChildData(this, "README.md")
        readme.setBinaryContent("""
            # Lua Project
            
            A simple Lua project.
            
            ## Structure
            
            - `src/` - Source files
            - `src/main.lua` - Main entry point
            
            ## Running
            
            ```bash
            lua src/main.lua
            ```
        """.trimIndent().toByteArray())
    }
    
    private fun createGameStructure(root: com.intellij.openapi.vfs.VirtualFile) {
        val srcDir = root.createChildDirectory(this, "src")
        
        // Create main.lua
        val mainFile = srcDir.createChildData(this, "main.lua")
        mainFile.setBinaryContent("""
            ---
            --- Game main entry point
            ---
            
            require("game.init")
            
            local function main()
                print("Game starting...")
                -- Game initialization
            end
            
            main()
        """.trimIndent().toByteArray())
        
        // Create game directory
        val gameDir = srcDir.createChildDirectory(this, "game")
        
        val initFile = gameDir.createChildData(this, "init.lua")
        initFile.setBinaryContent("""
            ---
            --- Game initialization
            ---
            
            local Game = {}
            
            function Game.init()
                print("Game initialized")
            end
            
            return Game
        """.trimIndent().toByteArray())
        
        // Create assets and config directories
        root.createChildDirectory(this, "assets")
        root.createChildDirectory(this, "config")
        
        // Create README
        val readme = root.createChildData(this, "README.md")
        readme.setBinaryContent("""
            # Lua Game Project
            
            A Lua game project structure.
            
            ## Structure
            
            - `src/` - Source files
            - `src/main.lua` - Main entry point
            - `src/game/` - Game logic
            - `assets/` - Game assets
            - `config/` - Configuration files
            
            ## Running
            
            ```bash
            lua src/main.lua
            ```
        """.trimIndent().toByteArray())
    }
    
    private fun createLibraryStructure(root: com.intellij.openapi.vfs.VirtualFile) {
        val srcDir = root.createChildDirectory(this, "src")
        val libName = root.name
        
        // Create main library file
        val libFile = srcDir.createChildData(this, "$libName.lua")
        libFile.setBinaryContent("""
            ---
            --- $libName library
            ---
            
            ---@class $libName
            local M = {}
            
            M.VERSION = "1.0.0"
            
            ---Example function
            ---@param value any
            ---@return any
            function M.process(value)
                return value
            end
            
            return M
        """.trimIndent().toByteArray())
        
        // Create tests directory
        val testsDir = root.createChildDirectory(this, "tests")
        val testFile = testsDir.createChildData(this, "${libName}_test.lua")
        testFile.setBinaryContent("""
            ---
            --- $libName tests
            ---
            
            local $libName = require("src.$libName")
            
            local function test_version()
                assert($libName.VERSION == "1.0.0", "Version check failed")
                print("✓ Version test passed")
            end
            
            local function run_tests()
                print("Running tests...")
                test_version()
                print("All tests passed!")
            end
            
            run_tests()
        """.trimIndent().toByteArray())
        
        // Create README
        val readme = root.createChildData(this, "README.md")
        readme.setBinaryContent("""
            # $libName Library
            
            A Lua library project.
            
            ## Structure
            
            - `src/` - Library source files
            - `tests/` - Test files
            
            ## Usage
            
            ```lua
            local $libName = require("$libName")
            print($libName.VERSION)
            ```
            
            ## Testing
            
            ```bash
            lua tests/${libName}_test.lua
            ```
        """.trimIndent().toByteArray())
    }
    
    override fun getCustomOptionsStep(context: WizardContext, parentDisposable: Disposable): ModuleWizardStep? {
        return LuaProjectTypeStep(this)
    }
    
    fun setProjectType(type: LuaProjectType) {
        this.projectType = type
    }
    
    fun getProjectTypeInternal(): LuaProjectType = projectType
}

/**
 * Lua project types
 */
enum class LuaProjectType(val displayName: String, val description: String) {
    PLAIN("Plain Lua", "A simple Lua project with basic structure"),
    GAME("Game Project", "A Lua game project with assets and game logic structure"),
    LIBRARY("Library", "A Lua library project with tests")
}
