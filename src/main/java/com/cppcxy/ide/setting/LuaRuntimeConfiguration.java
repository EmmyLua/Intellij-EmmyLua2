package com.cppcxy.ide.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.util.Alarm;
import com.intellij.util.EnvironmentUtil;
import com.intellij.util.FileContentUtil;
import com.redhat.devtools.lsp4ij.LSPIJUtils;
import com.redhat.devtools.lsp4ij.LanguageServiceAccessor;
import com.tang.intellij.lua.lang.LuaLanguageLevel;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Defaults for the standard EmmyLua server. Integration-owned servers remain independent. */
@Service(Service.Level.PROJECT)
public final class LuaRuntimeConfiguration implements Disposable {
    private static final List<String> CONFIG_NAMES = List.of(".luarc.json", ".emmyrc.json", ".emmyrc.lua");
    private final Project project;
    private final Alarm alarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD, this);
    private final Set<VirtualFile> parsedFiles = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<VirtualFile, ConfigVersion> cache = new ConcurrentHashMap<>();
    private record ConfigVersion(long stamp, LuaLanguageLevel level) {}

    public LuaRuntimeConfiguration(Project project) {
        this.project = project;
        project.getMessageBus().connect(this).subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener() {
            @Override public void after(@NotNull List<? extends VFileEvent> events) {
                if (events.stream().anyMatch(event -> isConfiguration(event.getPath()) ||
                        (event.getFile() != null && isConfiguration(event.getFile().getPath())))) {
                    refresh();
                }
            }
        });
    }

    public static LuaRuntimeConfiguration get(Project project) {
        return project.getService(LuaRuntimeConfiguration.class);
    }

    /** Called while parsing; reads small VFS files only and caches their decoded runtime. */
    public LuaLanguageLevel languageLevel(VirtualFile source) {
        parsedFiles.add(source);
        String external = externalConfig();
        LuaLanguageLevel level = external == null ?
                LuaRuntimeVersion.fromId(EmmyLuaSettings.getInstance().getLuaVersion()).level : LuaLanguageLevel.LUA55;
        if (external != null) {
            VirtualFile file = LocalFileSystem.getInstance().findFileByPath(external);
            if (file != null) level = overlay(file, level);
        }
        VirtualFile root = LSPIJUtils.getRoots(project).stream().findFirst().orElse(null);
        if (root != null) {
            for (String name : CONFIG_NAMES) {
                VirtualFile config = root.findChild(name);
                if (config != null) level = overlay(config, level);
            }
        }
        return level;
    }

    private LuaLanguageLevel overlay(VirtualFile file, LuaLanguageLevel fallback) {
        ConfigVersion version = cache.compute(file, (key, old) -> {
            if (old != null && old.stamp == file.getModificationStamp()) return old;
            LuaLanguageLevel level;
            try {
                // Executable configuration is evaluated only by the server, never by the IDE.
                level = file.getName().endsWith(".lua") || file.getLength() > 1024 * 1024 ?
                        LuaLanguageLevel.LUA55 : jsonLanguageLevel(VfsUtilCore.loadText(file));
            } catch (IOException | RuntimeException e) {
                level = LuaLanguageLevel.LUA55;
            }
            return new ConfigVersion(file.getModificationStamp(), level);
        });
        return version.level == null ? fallback : version.level;
    }

    static LuaLanguageLevel jsonLanguageLevel(String json) {
        try {
            JsonElement value = JsonParser.parseString(json);
            if (!value.isJsonObject()) return LuaLanguageLevel.LUA55;
            var object = value.getAsJsonObject();
            JsonElement version = object.get("runtime.version");
            if (version == null && object.has("runtime")) {
                JsonElement runtime = object.get("runtime");
                if (!runtime.isJsonObject()) return LuaLanguageLevel.LUA55;
                version = runtime.getAsJsonObject().get("version");
            }
            if (version == null) return null;
            if (!version.isJsonPrimitive() || !version.getAsJsonPrimitive().isString()) return LuaLanguageLevel.LUA55;
            return LuaRuntimeVersion.fromId(version.getAsString()).level;
        } catch (RuntimeException e) {
            return LuaLanguageLevel.LUA55;
        }
    }

    private boolean isConfiguration(String path) {
        if (path.equals(externalConfig())) return true;
        for (VirtualFile root : LSPIJUtils.getRoots(project)) {
            for (String name : CONFIG_NAMES) if (path.equals(root.getPath() + "/" + name)) return true;
        }
        return false;
    }

    private static String externalConfig() {
        String value = EnvironmentUtil.getValue("EMMYLUALS_CONFIG");
        return value == null || value.isBlank() ? null : value;
    }

    /** Supply defaults before workspace configuration, without replacing an explicit environment override. */
    public String serverConfigPath() throws IOException {
        return defaultConfigPath(PathManager.getSystemDir().resolve("emmylua/runtime"),
                LuaRuntimeVersion.fromId(EmmyLuaSettings.getInstance().getLuaVersion()), externalConfig());
    }

    static String defaultConfigPath(Path directory, LuaRuntimeVersion version, String external) throws IOException {
        if (external != null && !external.isBlank()) return external;
        Files.createDirectories(directory);
        Path file = directory.resolve(version.id + ".json");
        String content = "{\"runtime\":{\"version\":\"" + version.id + "\"}}\n";
        // Different versions have different paths; parallel projects cannot overwrite one another's default.
        synchronized (LuaRuntimeConfiguration.class) {
            if (!Files.exists(file) || !Files.readString(file).equals(content)) Files.writeString(file, content);
        }
        return file.toString();
    }

    public void refresh() {
        cache.clear();
        ApplicationManager.getApplication().invokeLater(() -> {
            if (project.isDisposed()) return;
            alarm.cancelAllRequests();
            alarm.addRequest(() -> {
                if (project.isDisposed()) return;
                var files = new ArrayList<>(parsedFiles);
                files.removeIf(file -> !file.isValid());
                parsedFiles.retainAll(files);
                WriteAction.run(() -> FileContentUtil.reparseFiles(project, files, true));
                for (var server : LanguageServiceAccessor.getInstance(project).getStartedServers()) {
                    if ("EmmyLua".equals(server.getServerDefinition().getId())) server.restart();
                }
            }, 100);
        }, project.getDisposed());
    }

    @Override public void dispose() {
        cache.clear();
        parsedFiles.clear();
    }
}
