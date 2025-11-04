package com.tang.intellij.lua.project

import com.intellij.facet.ui.ValidationResult
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.DirectoryProjectGenerator
import com.intellij.platform.ProjectGeneratorPeer
import com.tang.intellij.lua.lang.LuaIcons
import java.awt.BorderLayout
import javax.swing.*

/**
 * Lua Direct Project Generator
 * Provides Lua project generation in the New Project wizard
 */
class LuaDirectoryProjectGenerator : DirectoryProjectGenerator<LuaProjectSettings> {
    
    override fun getName(): String = "Lua"
    
    override fun getLogo(): Icon = LuaIcons.FILE
    
    override fun createPeer(): ProjectGeneratorPeer<LuaProjectSettings> {
        return LuaProjectGeneratorPeer()
    }
    
    override fun generateProject(
        project: Project,
        baseDir: VirtualFile,
        settings: LuaProjectSettings,
        module: Module
    ) {
        // Create project structure based on project type
        when (settings.projectType) {
            LuaProjectType.PLAIN -> createPlainStructure(baseDir)
            LuaProjectType.GAME -> createGameStructure(baseDir)
            LuaProjectType.LIBRARY -> createLibraryStructure(baseDir)
        }
    }
    
    private fun createPlainStructure(root: VirtualFile) {
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
        
        // Create .gitignore
        val gitignore = root.createChildData(this, ".gitignore")
        gitignore.setBinaryContent("""
            # IDE
            .idea/
            *.iml
            
            # OS
            .DS_Store
            Thumbs.db
            
            # Lua
            luac.out
            *.luac
        """.trimIndent().toByteArray())
    }
    
    private fun createGameStructure(root: VirtualFile) {
        val srcDir = root.createChildDirectory(this, "src")
        
        // Create main.lua
        val mainFile = srcDir.createChildData(this, "main.lua")
        mainFile.setBinaryContent("""
            ---
            --- Game main entry point
            ---
            
            local Game = require("game.init")
            
            local function main()
                print("Game starting...")
                Game.init()
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
            
            ---@class Game
            local Game = {}
            
            function Game.init()
                print("Game initialized")
            end
            
            function Game.update(dt)
                -- Update game logic
            end
            
            function Game.draw()
                -- Render game
            end
            
            return Game
        """.trimIndent().toByteArray())
        
        // Create player example
        val playerFile = gameDir.createChildData(this, "player.lua")
        playerFile.setBinaryContent("""
            ---
            --- Player class
            ---
            
            ---@class Player
            ---@field x number
            ---@field y number
            local Player = {}
            Player.__index = Player
            
            ---@return Player
            function Player.new()
                local self = setmetatable({}, Player)
                self.x = 0
                self.y = 0
                return self
            end
            
            ---@param dt number
            function Player:update(dt)
                -- Update player
            end
            
            return Player
        """.trimIndent().toByteArray())
        
        // Create assets and config directories
        val assetsDir = root.createChildDirectory(this, "assets")
        assetsDir.createChildDirectory(this, "images")
        assetsDir.createChildDirectory(this, "sounds")
        
        val configDir = root.createChildDirectory(this, "config")
        val configFile = configDir.createChildData(this, "game.lua")
        configFile.setBinaryContent("""
            ---
            --- Game configuration
            ---
            
            return {
                title = "My Lua Game",
                version = "1.0.0",
                window = {
                    width = 800,
                    height = 600
                }
            }
        """.trimIndent().toByteArray())
        
        // Create README
        val readme = root.createChildData(this, "README.md")
        readme.setBinaryContent("""
            # Lua Game Project
            
            A Lua game project structure.
            
            ## Structure
            
            - `src/` - Source files
            - `src/main.lua` - Main entry point
            - `src/game/` - Game logic
            - `assets/` - Game assets (images, sounds)
            - `config/` - Configuration files
            
            ## Running
            
            ```bash
            lua src/main.lua
            ```
        """.trimIndent().toByteArray())
        
        // Create .gitignore
        val gitignore = root.createChildData(this, ".gitignore")
        gitignore.setBinaryContent("""
            # IDE
            .idea/
            *.iml
            
            # OS
            .DS_Store
            Thumbs.db
            
            # Lua
            luac.out
            *.luac
            
            # Build
            dist/
            build/
        """.trimIndent().toByteArray())
    }
    
    private fun createLibraryStructure(root: VirtualFile) {
        val srcDir = root.createChildDirectory(this, "src")
        val libName = root.name.replace("-", "_")
        
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
            
            ---@param message string
            function M.log(message)
                print("[${libName}] " .. message)
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
            
            package.path = package.path .. ";./src/?.lua"
            local $libName = require("$libName")
            
            local passed = 0
            local failed = 0
            
            local function test(name, fn)
                local success, err = pcall(fn)
                if success then
                    print("✓ " .. name)
                    passed = passed + 1
                else
                    print("✗ " .. name .. ": " .. tostring(err))
                    failed = failed + 1
                end
            end
            
            local function assert_equal(expected, actual, message)
                if expected ~= actual then
                    error(message or string.format("Expected %s but got %s", tostring(expected), tostring(actual)))
                end
            end
            
            -- Tests
            test("Version check", function()
                assert_equal("1.0.0", $libName.VERSION, "Version should be 1.0.0")
            end)
            
            test("Process function", function()
                local value = "test"
                assert_equal(value, $libName.process(value), "Process should return same value")
            end)
            
            -- Summary
            print(string.format("\n%d passed, %d failed", passed, failed))
            os.exit(failed == 0 and 0 or 1)
        """.trimIndent().toByteArray())
        
        // Create examples directory
        val examplesDir = root.createChildDirectory(this, "examples")
        val exampleFile = examplesDir.createChildData(this, "basic_usage.lua")
        exampleFile.setBinaryContent("""
            ---
            --- Basic usage example
            ---
            
            package.path = package.path .. ";./src/?.lua"
            local $libName = require("$libName")
            
            print("Library version: " .. $libName.VERSION)
            $libName.log("Hello from $libName!")
            
            local result = $libName.process("example")
            print("Result: " .. result)
        """.trimIndent().toByteArray())
        
        // Create README
        val readme = root.createChildData(this, "README.md")
        readme.setBinaryContent("""
            # $libName Library
            
            A Lua library project.
            
            ## Installation
            
            Copy `src/$libName.lua` to your project's Lua path.
            
            ## Usage
            
            ```lua
            local $libName = require("$libName")
            print($libName.VERSION)
            ```
            
            ## Structure
            
            - `src/` - Library source files
            - `tests/` - Test files
            - `examples/` - Usage examples
            
            ## Testing
            
            ```bash
            lua tests/${libName}_test.lua
            ```
            
            ## Examples
            
            ```bash
            lua examples/basic_usage.lua
            ```
        """.trimIndent().toByteArray())
        
        // Create .gitignore
        val gitignore = root.createChildData(this, ".gitignore")
        gitignore.setBinaryContent("""
            # IDE
            .idea/
            *.iml
            
            # OS
            .DS_Store
            Thumbs.db
            
            # Lua
            luac.out
            *.luac
        """.trimIndent().toByteArray())
    }
    
    override fun validate(baseDirPath: String): ValidationResult {
        return ValidationResult.OK
    }
}

/**
 * Lua Project Settings
 */
data class LuaProjectSettings(
    var projectType: LuaProjectType = LuaProjectType.PLAIN
)

/**
 * Project Generator Peer
 */
class LuaProjectGeneratorPeer : ProjectGeneratorPeer<LuaProjectSettings> {
    
    private val settings = LuaProjectSettings()
    private val panel = JPanel(BorderLayout())
    private val buttonGroup = ButtonGroup()
    
    init {
        createUI()
    }
    
    private fun createUI() {
        val contentPanel = JPanel()
        contentPanel.layout = BoxLayout(contentPanel, BoxLayout.Y_AXIS)
        contentPanel.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
        
        val titleLabel = JLabel("Project Type:")
        titleLabel.font = titleLabel.font.deriveFont(14f).deriveFont(java.awt.Font.BOLD)
        titleLabel.border = BorderFactory.createEmptyBorder(0, 0, 15, 0)
        contentPanel.add(titleLabel)
        
        // Add radio buttons for each project type
        for (type in LuaProjectType.values()) {
            val radioButton = JRadioButton(type.displayName)
            radioButton.isSelected = (type == settings.projectType)
            radioButton.border = BorderFactory.createEmptyBorder(5, 0, 5, 0)
            
            val descLabel = JLabel("<html><body style='margin-left: 25px; color: gray; max-width: 400px;'>${type.description}</body></html>")
            descLabel.border = BorderFactory.createEmptyBorder(0, 0, 15, 0)
            
            radioButton.addActionListener {
                if (radioButton.isSelected) {
                    settings.projectType = type
                }
            }
            
            buttonGroup.add(radioButton)
            
            contentPanel.add(radioButton)
            contentPanel.add(descLabel)
        }
        
        panel.add(contentPanel, BorderLayout.NORTH)
    }
    
    override fun getComponent(): JComponent = panel
    
    override fun getSettings(): LuaProjectSettings = settings
    
    override fun validate(): com.intellij.openapi.ui.ValidationInfo? = null
    
    override fun isBackgroundJobRunning(): Boolean = false
    
    override fun buildUI(settingsStep: com.intellij.ide.util.projectWizard.SettingsStep) {
        settingsStep.addSettingsComponent(component)
    }
}
