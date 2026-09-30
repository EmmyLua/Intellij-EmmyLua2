# Native parser language level

`global` member names are accepted only for an explicitly supplied Lua language
level below 5.5. Lua 5.5 and unknown levels retain the reserved `GLOBAL` token.
Quoted keys remain valid. Global declarations are parsed as before.

Integrations can implement `com.tang.intellij.lua.lang.LuaLanguageLevelProvider`
and register `languageLevelProvider` in the `com.cppcxy.Intellij-EmmyLua` extension
namespace. Return the runtime level for the project/file, or null to abstain.
The first non-null result wins. Calls occur during parsing: use fast, thread-safe
lookups without PSI access, filesystem scans, or synchronous language-server calls.

The provider must reflect the runtime configuration it manages. No provider means
strict Lua 5.5 behavior; the IDE parser does not itself evaluate `.emmyrc.lua`, read
analyzer config files, or infer a runtime from source text. This hook is independent
of the server-routing extension. It does not change the language server's settings.

The level is cached only within one parse. When a provider's runtime changes,
request reparsing of affected files (not merely a diagnostics refresh). The hook
currently gates only the legacy `global` member compatibility exception; it does
not introduce general version-specific grammar validation.
