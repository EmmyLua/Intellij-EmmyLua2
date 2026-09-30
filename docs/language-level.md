# Lua runtime configuration and native parsing

Settings → Languages & Frameworks → EmmyLua Settings includes **Default Lua version**:
Latest, Lua 5.1–5.5, and the analyzer's LuaJIT variants. The application setting
applies to the standard EmmyLua server in all projects. Apply restarts running
standard EmmyLua servers and reparses affected Lua files; Cancel/Reset leave the
saved value unchanged. Integration-owned servers (for example Factorio's per-mod
servers) keep their own configuration and are not restarted by this setting.

The server receives a small generated defaults file through `EMMYLUALS_CONFIG`,
inside the IDE system cache. EmmyLua loads this after its home/global configuration
and before the first workspace root's `.luarc.json`, `.emmyrc.json`, and
`.emmyrc.lua`, in that order. A project runtime setting therefore overrides the
selected default; diagnostics-only project configuration inherits it. User files
are never rewritten. An existing `EMMYLUALS_CONFIG` environment variable is kept
instead of generating defaults, so it bypasses the selector.

Native parsing first asks registered `LuaLanguageLevelProvider` integrations.
When none claims the file, it uses the selected default and the standard server's
first workspace root JSON runtime overrides (nested `runtime.version` or the
flat `"runtime.version"` key). Saved root configuration changes trigger reparsing
and a standard-server restart. Native parsing never executes `.emmyrc.lua`:
when that file is present, or configuration is unreadable/unknown, it stays
conservatively at Lua 5.5. The server still evaluates Lua configuration normally.
An external environment config without a statically readable runtime also stays
conservative in native parsing, since home/global settings may influence it.

`global` member names are accepted only below Lua 5.5. Lua 5.5 and unknown levels
retain the reserved `GLOBAL` token. Quoted keys remain valid. Global declarations
are parsed as before. This setting gates the legacy `global` compatibility
exception; it does not introduce full version-specific validation of native grammar.
The language server applies its own runtime-dependent parsing and analysis.

## Integration hook

Implement `com.tang.intellij.lua.lang.LuaLanguageLevelProvider` and register
`languageLevelProvider` in the `com.cppcxy.Intellij-EmmyLua` extension namespace.
Return the managed runtime for a project/file, or null to abstain. The first
non-null result wins, ahead of the default. Calls occur during parsing: use fast,
thread-safe lookups without PSI access, filesystem scans or server calls.

The provider must reflect the runtime configuration it manages. The hook is
independent of server routing and does not itself change server configuration.
The level is cached only within one parse. When a provider's runtime changes,
request reparsing of affected files, not merely a diagnostics refresh.
