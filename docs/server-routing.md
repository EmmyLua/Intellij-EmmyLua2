# Routing files to externally managed EmmyLua servers

Plugins can implement `com.cppcxy.ide.lsp.EmmyLuaServerProvider` and register:

```xml
<extensions defaultExtensionNs="com.cppcxy.Intellij-EmmyLua">
    <serverProvider implementation="your.plugin.ModuleServerProvider"/>
</extensions>
```

`getServerId(Project, VirtualFile)` returns the LSP4IJ server ID for a file,
or null to abstain. The first nonblank result wins (IntelliJ extension ordering
applies); otherwise routing selects `EmmyLua`. Returning `EmmyLua` explicitly
also selects the default. Providers must perform fast, thread-safe lookups.

A non-default selection excludes the file from EmmyLua's default document
mapping. The provider must register and manage the replacement LSP4IJ server,
its document mappings, workspace/configuration, file synchronization and cleanup.
Its server interface must extend `EmmyLuaCustomApi` for gutter requests.
Return the assigned ID while a server is starting or unavailable; do not fall
back to the project-wide server on temporary failure.

Gutter requests use the selected ID, cached results are scoped by project and
server, and detail requests retain the ID that produced the gutter data. Stale
markers are ignored if routing has changed. Integrations changing ownership
must disconnect existing LSP document associations, refresh their mappings and
restart editor analysis; this extension point does not migrate live connections.
Invalidate gutter caches with `LuaGutterCacheManager.clearCache(file.url)` when
replacing a server under the same ID. Register stable server IDs unique to the
owning project and environment, and remove them on disposal.

Without a provider, the existing default-server behavior is preserved. This
hook does not itself start multiple servers or impose a module isolation model.
