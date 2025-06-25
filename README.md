# 🚀 EmmyLua2 for IntelliJ IDEA

<div align="center">

[![JetBrains Plugin](https://img.shields.io/jetbrains/plugin/v/25076-emmylua2?style=for-the-badge&logo=jetbrains&logoColor=white&color=blue)](https://plugins.jetbrains.com/plugin/25076-emmylua2)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/25076-emmylua2?style=for-the-badge&color=green)](https://plugins.jetbrains.com/plugin/25076-emmylua2)
[![Rating](https://img.shields.io/jetbrains/plugin/r/stars/25076-emmylua2?style=for-the-badge&color=yellow)](https://plugins.jetbrains.com/plugin/25076-emmylua2)
[![License: MIT](https://img.shields.io/badge/License-MIT-purple.svg?style=for-the-badge)](https://opensource.org/licenses/MIT)

**Modern Lua development plugin providing powerful Lua language support for IntelliJ IDEA**

[📦 Install Plugin](https://plugins.jetbrains.com/plugin/25076-emmylua2) · [🐛 Report Issues](https://github.com/CppCXY/Intellij-EmmyLua2/issues) · [💡 Feature Requests](https://github.com/CppCXY/Intellij-EmmyLua2/discussions)

</div>

## ✨ Features

> Powered by the robust [emmylua-analyzer-rust](https://github.com/CppCXY/emmylua-analyzer-rust) engine

### 🎯 Core Features
- **Smart Code Completion** - Context-aware precise code suggestions
- **Syntax Highlighting** - Support for Lua 5.1/5.2/5.3/5.4 syntax
- **Error Detection** - Real-time syntax and semantic error reporting
- **Code Navigation** - Quick jump to definitions, find references
- **Refactoring Support** - Rename, extract variables and other refactoring operations

### 🔧 Advanced Features
- **EmmyLua Annotations Support** - Complete type annotation system
- **Debugger Integration** - Built-in EmmyLua debugger
- **Multi-platform Support** - Full support for Windows, macOS, Linux
- **Performance Optimization** - High-performance language server based on Rust

## 🛠️ Installation

### Install from JetBrains Marketplace (Recommended)

1. Open IntelliJ IDEA
2. Go to `File` → `Settings` → `Plugins`
3. Search for "EmmyLua2"
4. Click `Install` to install the plugin
5. Restart IDE

### Manual Installation

1. Download the latest version from [Releases](https://github.com/CppCXY/Intellij-EmmyLua2/releases)
2. In IntelliJ IDEA, go to `File` → `Settings` → `Plugins`
3. Click the gear icon → `Install Plugin from Disk...`
4. Select the downloaded plugin file
5. Restart IDE

## 🚀 Quick Start

### Create Lua Project

1. `File` → `New` → `Project`
2. Select `Lua` project type
3. Configure project settings
4. Start writing Lua code!

### Configure EmmyLua Annotations

```lua
---@class Player
---@field name string
---@field level number
local Player = {}

---Create new player
---@param name string Player name
---@param level number Player level
---@return Player
function Player:new(name, level)
    return setmetatable({
        name = name,
        level = level
    }, { __index = self })
end
```

## 🔧 Supported IDE Versions

| IDE Version | Plugin Version | Status |
|-------------|----------------|--------|
| 2024.3+     | 0.8.x         | ✅ Actively Supported |

## 🤝 Contributing

We welcome contributions of all kinds! Please check the [Contributing Guide](CONTRIBUTING.md) to learn how to participate in the project.

### Development Environment Setup

#### 📋 Prerequisites

- **JDK 17+** - Recommended to use OpenJDK or Oracle JDK
- **IntelliJ IDEA 2024.3+** - Ultimate or Community Edition
- **Git** - Version control tool

#### 🔨 Build Steps

1. **Clone Repository**
   ```bash
   git clone https://github.com/CppCXY/Intellij-EmmyLua2.git
   cd Intellij-EmmyLua2
   ```

2. **Import Project**
   - Open the project with IntelliJ IDEA
   - Wait for Gradle sync to complete

3. **Build Plugin**
   ```bash
   ./gradlew buildPlugin
   ```

4. **Run Development Environment**
   ```bash
   ./gradlew runIde
   ```

#### 🎯 Available Gradle Tasks

| Task | Description |
|------|-------------|
| `buildPlugin` | Build plugin distribution package |
| `runIde` | Run plugin in IDE sandbox |
| `test` | Run test suite |
| `downloadEmmyLuaAnalyzer` | Download Emmy Lua Analyzer |
| `installDependencies` | Install all dependencies |
| `cleanDependencies` | Clean downloaded dependencies |


## 🙏 Acknowledgments

- [emmylua-analyzer-rust](https://github.com/CppCXY/emmylua-analyzer-rust) - Core language analysis engine
- [EmmyLuaDebugger](https://github.com/EmmyLua/EmmyLuaDebugger) - Debugger support
- [LSP4IJ](https://github.com/redhat-developer/lsp4ij) - LSP client implementation

## 📄 License

This project is open source under the [MIT License](LICENSE).

---

<div align="center">

**⭐ If this project helps you, please give us a Star!**

Made with ❤️ by [CppCXY](https://github.com/CppCXY)

</div>


